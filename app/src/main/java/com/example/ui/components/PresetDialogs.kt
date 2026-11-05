package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Mk20Preset
import com.example.ui.theme.LedAmber
import com.example.ui.theme.LedEmerald
import com.example.ui.theme.MetalBorder
import com.example.ui.theme.PedalCardDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.DeepLinkUtils

@Composable
fun SavePresetDialog(
    preset: Mk20Preset,
    onDismiss: () -> Unit,
    onSave: (name: String, style: String) -> Unit
) {
    var name by remember { mutableStateOf(preset.title) }
    var style by remember { mutableStateOf(preset.style) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PedalCardDark,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        icon = {
            Icon(Icons.Default.Bookmark, contentDescription = null, tint = LedEmerald)
        },
        title = {
            Text("Salvar Preset na Pedaleira", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Dê um nome ao seu timbre para salvá-lo no dispositivo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do Preset") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LedEmerald,
                        unfocusedBorderColor = MetalBorder,
                        focusedLabelColor = LedEmerald
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_preset_name_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = style,
                    onValueChange = { style = it },
                    label = { Text("Estilo / Categoria") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LedEmerald,
                        unfocusedBorderColor = MetalBorder,
                        focusedLabelColor = LedEmerald
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, style) },
                colors = ButtonDefaults.buttonColors(containerColor = LedEmerald),
                modifier = Modifier.testTag("confirm_save_preset_button")
            ) {
                Text("Salvar Preset", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        }
    )
}

@Composable
fun SharePresetDialog(
    preset: Mk20Preset,
    onDismiss: () -> Unit,
    onCopyLink: (String) -> Unit,
    onShareApp: () -> Unit,
    onExportJsonFile: () -> Unit
) {
    val shareUrl = remember(preset) { DeepLinkUtils.createShareUrl(preset, useWebScheme = true) }
    val deepLink = remember(preset) { DeepLinkUtils.createShareUrl(preset, useWebScheme = false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PedalCardDark,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        icon = {
            Icon(Icons.Default.Share, contentDescription = null, tint = LedEmerald)
        },
        title = {
            Text("Compartilhar Preset MK-20", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Gere um link único para que outros guitarristas possam abrir diretamente este timbre na pedaleira:",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Link Display Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0C101A))
                        .border(1.dp, MetalBorder, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Link, contentDescription = null, tint = LedEmerald, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Link Único do Preset",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = LedEmerald
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = shareUrl,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = TextSecondary,
                            maxLines = 3
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons
                Button(
                    onClick = { onCopyLink(shareUrl) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_copy_link_button")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copiar Link Único", fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onShareApp,
                    colors = ButtonDefaults.buttonColors(containerColor = LedEmerald),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_share_system_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Compartilhar com Amigos / Apps", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onExportJsonFile,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MetalBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_export_file_button")
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Salvar como Arquivo .json", color = TextPrimary, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar", color = LedEmerald)
            }
        }
    )
}

@Composable
fun SettingsDialog(
    currentApiKey: String,
    onSaveKey: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var apiKeyInput by remember { mutableStateOf(currentApiKey) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PedalCardDark,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        icon = {
            Icon(Icons.Default.Key, contentDescription = null, tint = LedEmerald)
        },
        title = {
            Text("Configurações da Gemini IA", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "O Preset20 AI utiliza a IA Gemini para analisar músicas, links do YouTube e timbres específicos da MK-20. Você pode inserir sua própria chave de API aqui.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    label = { Text("Chave da API Gemini (Opcional)") },
                    placeholder = { Text("AIzaSy...") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LedEmerald,
                        unfocusedBorderColor = MetalBorder,
                        focusedLabelColor = LedEmerald
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("api_key_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        runCatching {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/apikey"))
                            context.startActivity(intent)
                        }
                    },
                    border = BorderStroke(1.dp, LedAmber),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_open_aistudio_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = null,
                        tint = LedAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Gerar Chave (Site)",
                        color = LedAmber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Caso não informe uma chave, o gerador usará o banco de dados oficial de timbres da MK-20 em modo inteligente.",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveKey(apiKeyInput.trim())
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = LedEmerald),
                modifier = Modifier.testTag("save_api_key_button")
            ) {
                Text("Salvar Chave", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        }
    )
}

@Composable
fun ProUpgradeDialog(
    isProUnlocked: Boolean,
    dailyUsed: Int,
    maxDailyFree: Int,
    productPrice: String,
    userEmail: String = "",
    cloudBackupCount: Int = 0,
    isCloudSyncing: Boolean = false,
    onBuyProClick: () -> Unit,
    onRestorePurchasesClick: () -> Unit,
    onSyncCloudClick: () -> Unit = {},
    onSignOutClick: () -> Unit = {},
    onDebugActivatePro: () -> Unit = {},
    onDebugResetFree: () -> Unit = {},
    onDebugExhaustQuota: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val remaining = (maxDailyFree - dailyUsed).coerceAtLeast(0)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PedalCardDark,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(LedAmber.copy(alpha = 0.16f))
                    .border(1.5.dp, LedAmber, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isProUnlocked) Icons.Default.Verified else Icons.Default.Star,
                    contentDescription = null,
                    tint = LedAmber,
                    modifier = Modifier.size(26.dp)
                )
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (isProUnlocked) "Preset20 AI PRO Ativo" else "Preset20 AI PRO • Vitalício",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = LedAmber.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, LedAmber.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = "PAGAMENTO ÚNICO • SEM MENSALIDADE",
                        color = LedAmber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Status Card (Quota today or PRO Active)
                Surface(
                    color = Color(0xFF0C101A),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isProUnlocked) LedEmerald else if (remaining == 0) Color(0xFFFF5252) else MetalBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        if (isProUnlocked) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = LedEmerald,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ACESSO VITALÍCIO DESBLOQUEADO",
                                    color = LedEmerald,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Você tem gerações ilimitadas de presets todos os dias, sem nenhuma restrição!",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Plano Gratuito Hoje:",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "$remaining de $maxDailyFree restantes",
                                    color = if (remaining == 0) Color(0xFFFF5252) else LedEmerald,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            // 4 LED quota segments
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for (i in 0 until maxDailyFree) {
                                    val isAvailable = i < remaining
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                if (isAvailable) LedEmerald else Color(0xFF283044)
                                            )
                                    )
                                }
                            }
                            if (remaining == 0) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Você já utilizou seus 4 presets gratuitos de hoje. Desbloqueie o PRO com pagamento único para continuar criando agora mesmo sem limites!",
                                    color = Color(0xFFFF8A80),
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }

                // Benefits Card
                Surface(
                    color = Color(0xFF0F1522),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MetalBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "O QUE VOCÊ LIBERA PARA SEMPRE:",
                            color = LedAmber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )

                        ProBenefitRow(
                            title = "Presets Ilimitados Todos os Dias",
                            subtitle = "Gere quantos timbres quiser sem limite de 4 por dia."
                        )
                        ProBenefitRow(
                            title = "Violão, Guitarra e Baixo 100% Liberados",
                            subtitle = "Busca por música, Link do YouTube e Vibe por Microfone sem travas."
                        )
                        ProBenefitRow(
                            title = "Exportação .mk20 (412B) Ilimitada",
                            subtitle = "Exporte e compartilhe arquivos prontos para M-EFCS Mobile e PC."
                        )
                        ProBenefitRow(
                            title = "Pagamento Único (Sem Assinatura)",
                            subtitle = "Pague uma única vez na Google Play e o app é seu para sempre."
                        )
                    }
                }

                if (!isProUnlocked) {
                    // Main Google Play One-Time Purchase Button
                    Button(
                        onClick = onBuyProClick,
                        colors = ButtonDefaults.buttonColors(containerColor = LedAmber),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_buy_pro_lifetime")
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Liberar Tudo • $productPrice (Único)",
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }

                    // Restore Purchase Button
                    OutlinedButton(
                        onClick = onRestorePurchasesClick,
                        border = BorderStroke(1.dp, MetalBorder),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_restore_purchases")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = null,
                            tint = TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Já comprei • Restaurar da Google Play",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Conta Google Conectada + Backup em Nuvem
                Surface(
                    color = Color(0xFF0C121E),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, LedEmerald.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = LedEmerald,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "CONTA GOOGLE & BACKUP NA NUVEM",
                                        color = LedEmerald,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                    Text(
                                        text = userEmail.ifBlank { "Sincronização ativa na nuvem" },
                                        color = TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            if (userEmail.isNotBlank()) {
                                TextButton(
                                    onClick = {
                                        onDismiss()
                                        onSignOutClick()
                                    },
                                    modifier = Modifier.testTag("btn_google_sign_out")
                                ) {
                                    Text("Trocar Conta", color = TextSecondary, fontSize = 10.sp)
                                }
                            }
                        }

                        Text(
                            text = "Sua cota diária (4/4), status PRO Vitalício e presets salvos ($cloudBackupCount na nuvem) estão blindados na sua conta Google contra reinstalação.",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )

                        OutlinedButton(
                            onClick = onSyncCloudClick,
                            enabled = !isCloudSyncing,
                            border = BorderStroke(1.dp, LedEmerald.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_dialog_sync_cloud")
                        ) {
                            Text(
                                text = if (isCloudSyncing) "Sincronizando com a Nuvem..." else "Sincronizar Presets na Nuvem Agora",
                                color = LedEmerald,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun ProBenefitRow(
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = LedEmerald,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
        }
    }
}

