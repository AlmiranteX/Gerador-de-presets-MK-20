package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import com.example.ui.components.AuthScreen
import com.example.ui.components.signOutFromGoogle
import com.example.ui.screens.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.PresetViewModel
import com.example.util.configureAppCheck
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {

    private val viewModel: PresetViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI, ViewModels, or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                AppRootContent(
                    viewModel = viewModel,
                    onAuthenticatedReady = {
                        handleIntent(intent)
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (Firebase.auth.currentUser != null) {
            handleIntent(intent)
        }
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        // Handle Deep Links (mk20://preset?data=... or https://mk20tone.app/preset?data=...)
        val dataUri = intent.data
        if (dataUri != null) {
            viewModel.handleIncomingUri(dataUri)
            return
        }

        // Handle shared text / JSON from other apps (ACTION_SEND)
        if (intent.action == Intent.ACTION_SEND) {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                viewModel.handleIncomingText(sharedText)
            }
        }
    }
}

@Composable
fun AppRootContent(
    viewModel: PresetViewModel,
    onAuthenticatedReady: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    val user = currentUser
    if (user == null) {
        LaunchedEffect(Unit) {
            viewModel.onUserSignedOut()
        }
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        LaunchedEffect(user.uid) {
            viewModel.onUserAuthenticated()
            onAuthenticatedReady()
        }
        MainScreen(
            viewModel = viewModel,
            userEmail = user.email ?: user.displayName ?: "Conta Google Conectada",
            onSignOut = {
                viewModel.onUserSignedOut()
                signOutFromGoogle(
                    context = context,
                    credentialManager = credentialManager,
                    onSignOutComplete = {
                        currentUser = null
                    },
                    scope = scope
                )
            }
        )
    }
}
