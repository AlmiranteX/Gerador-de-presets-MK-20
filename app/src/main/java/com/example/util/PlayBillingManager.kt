package com.example.util

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Gerenciador oficial do Google Play Billing (Pagamento Único / INAPP)
 * Produto Vitalício: "preset20_pro_vitalicio" (Sem mensalidade).
 */
class PlayBillingManager(
    context: Context,
    private val onProUnlocked: () -> Unit,
    private val onBillingMessage: (String) -> Unit
) {
    companion object {
        const val PRODUCT_ID_PRO_LIFETIME = "preset20_pro_vitalicio"
        const val DEFAULT_DISPLAY_PRICE = "R$ 24,90"
    }

    private val appContext = context.applicationContext

    private val _productPrice = MutableStateFlow(DEFAULT_DISPLAY_PRICE)
    val productPrice: StateFlow<String> = _productPrice.asStateFlow()

    private val _isBillingConnected = MutableStateFlow(false)
    val isBillingConnected: StateFlow<Boolean> = _isBillingConnected.asStateFlow()

    private var cachedProductDetails: ProductDetails? = null

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    handlePurchase(purchase)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                onBillingMessage("Compra cancelada pelo usuário.")
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                onProUnlocked()
                onBillingMessage("Você já possui o Preset20 AI PRO Vitalício! Acesso restaurado.")
            }
            else -> {
                onBillingMessage("Não foi possível concluir a compra Google Play (${billingResult.debugMessage}).")
            }
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    fun startConnection() {
        if (billingClient.isReady) {
            _isBillingConnected.value = true
            queryProductDetails()
            queryExistingPurchases(silent = true)
            return
        }

        try {
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        _isBillingConnected.value = true
                        queryProductDetails()
                        queryExistingPurchases(silent = true)
                    } else {
                        _isBillingConnected.value = false
                    }
                }

                override fun onBillingServiceDisconnected() {
                    _isBillingConnected.value = false
                }
            })
        } catch (_: Exception) {
            _isBillingConnected.value = false
        }
    }

    private fun queryProductDetails() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID_PRO_LIFETIME)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val detail = productDetailsList.firstOrNull { it.productId == PRODUCT_ID_PRO_LIFETIME }
                if (detail != null) {
                    cachedProductDetails = detail
                    val formatted = detail.oneTimePurchaseOfferDetails?.formattedPrice
                    if (!formatted.isNullOrBlank()) {
                        _productPrice.value = formatted
                    }
                }
            }
        }
    }

    fun queryExistingPurchases(silent: Boolean = false) {
        if (!billingClient.isReady) {
            if (!silent) {
                startConnection()
                onBillingMessage("Conectando à Google Play Store para verificar suas compras...")
            }
            return
        }

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val ownedPro = purchasesList.any { purchase ->
                    purchase.products.contains(PRODUCT_ID_PRO_LIFETIME) &&
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                if (ownedPro) {
                    purchasesList.forEach { handlePurchase(it, silent = true) }
                    onProUnlocked()
                    if (!silent) {
                        onBillingMessage("Compra encontrada na sua conta Google! Preset20 AI PRO Vitalício restaurado com sucesso.")
                    }
                } else if (!silent) {
                    onBillingMessage("Nenhuma compra anterior do Preset20 AI PRO foi encontrada nesta conta Google.")
                }
            } else if (!silent) {
                onBillingMessage("Não foi possível consultar a Google Play Store no momento.")
            }
        }
    }

    fun launchOneTimePurchase(activity: Activity?) {
        if (activity == null) {
            onBillingMessage("Não foi possível iniciar o pagamento: contexto de tela indisponível.")
            return
        }

        if (!billingClient.isReady) {
            startConnection()
            onBillingMessage("Conectando à Google Play Store... Tente novamente em alguns segundos.")
            return
        }

        val details = cachedProductDetails
        if (details == null) {
            queryProductDetails()
            onBillingMessage("Carregando informações da oferta na Google Play Store. Tente novamente em instantes.")
            return
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    private fun handlePurchase(purchase: Purchase, silent: Boolean = false) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        onProUnlocked()
                        if (!silent) {
                            onBillingMessage("Pagamento único confirmado! Preset20 AI PRO Vitalício liberado para sempre!")
                        }
                    }
                }
            } else {
                onProUnlocked()
                if (!silent) {
                    onBillingMessage("Preset20 AI PRO Vitalício ativo!")
                }
            }
        } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            if (!silent) {
                onBillingMessage("Pagamento pendente de confirmação pelo Google Play (ex: Pix/Boleto). Assim que compensar, o PRO será liberado automaticamente!")
            }
        }
    }

    fun endConnection() {
        try {
            if (billingClient.isReady) {
                billingClient.endConnection()
            }
        } catch (_: Exception) {}
    }
}
