package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.NaringAiTheme
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.EmeraldPrimary
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.Color
import com.example.data.repository.AiNutritionService

private const val TAG = "MainActivity"
private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val BACKUP_PREFS_NAME = "naringai_appcheck_backup"
private const val BACKUP_DEBUG_TOKEN_KEY = "saved_debug_token"

/**
 * Configures Firebase and installs the appropriate App Check provider factory.
 * In debug builds, DebugAppCheckProviderFactory is installed and the debug token from Intent
 * (or backed-up user debug token) is synchronously committed into SharedPreferences.
 * Returns true if Firebase instance and App Check are verified ready.
 */
fun configureAppCheck(context: Context, intent: Intent?): Boolean {
    return try {
        // 1. Ensure FirebaseApp is initialized
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
            Log.d(TAG, "FirebaseApp initialized.")
        }

        val appCheck = FirebaseAppCheck.getInstance()
        appCheck.setTokenAutoRefreshEnabled(true)

        if (BuildConfig.DEBUG) {
            val persistenceKey = FirebaseApp.getInstance().persistenceKey
            val prefs = context.getSharedPreferences(
                "$DEBUG_STORE_PREFIX$persistenceKey",
                Context.MODE_PRIVATE
            )
            val backupPrefs = context.getSharedPreferences(BACKUP_PREFS_NAME, Context.MODE_PRIVATE)

            val intentDebugToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)?.trim()
            if (!intentDebugToken.isNullOrEmpty()) {
                // Synchronous commit is mandatory so token is persisted before provider factory reads it
                prefs.edit()
                    .putString(DEBUG_SECRET_KEY, intentDebugToken)
                    .commit()
                backupPrefs.edit()
                    .putString(BACKUP_DEBUG_TOKEN_KEY, intentDebugToken)
                    .commit()
                Log.d(TAG, "App Check debug secret configured from Intent extra.")
            } else {
                val existingStored = prefs.getString(DEBUG_SECRET_KEY, null)?.trim()
                val backedUpToken = backupPrefs.getString(BACKUP_DEBUG_TOKEN_KEY, null)?.trim()
                if (existingStored.isNullOrEmpty() && !backedUpToken.isNullOrEmpty()) {
                    prefs.edit()
                        .putString(DEBUG_SECRET_KEY, backedUpToken)
                        .commit()
                    Log.d(TAG, "App Check debug secret restored from backup.")
                } else if (!existingStored.isNullOrEmpty() && backedUpToken.isNullOrEmpty()) {
                    backupPrefs.edit()
                        .putString(BACKUP_DEBUG_TOKEN_KEY, existingStored)
                        .commit()
                } else {
                    Log.d(TAG, "Using stored App Check debug secret.")
                }
            }

            appCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance(),
                true
            )
            Log.d(TAG, "DebugAppCheckProviderFactory installed with auto-refresh.")
        } else {
            // Release builds: Check for provisioned reCAPTCHA Enterprise site key first, otherwise Play Integrity
            val resId = context.resources.getIdentifier("recaptcha_site_key", "string", context.packageName)
            val siteKey = if (resId != 0) context.getString(resId) else ""
            if (siteKey.isNotBlank()) {
                appCheck.installAppCheckProviderFactory(
                    RecaptchaAppCheckProviderFactory.getInstance(siteKey),
                    true
                )
                Log.d(TAG, "RecaptchaAppCheckProviderFactory installed for release builds.")
            } else {
                appCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance(),
                    true
                )
                Log.d(TAG, "PlayIntegrityAppCheckProviderFactory installed for release builds.")
            }
        }
        true
    } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize Firebase App Check", e)
        false
    }
}

class MainActivity : ComponentActivity() {
    private var isFirebaseReady by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        com.example.data.model.SwedishFoodDatabase.initialize(applicationContext)

        // Mandatory App Check configuration with launch intent before any UI or Firebase calls
        isFirebaseReady = configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            NaringAiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppRoot(
                        isFirebaseReady = isFirebaseReady,
                        onRetryFirebase = {
                            isFirebaseReady = configureAppCheck(this, intent)
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        isFirebaseReady = configureAppCheck(this, intent)
    }
}

@Composable
fun AppRoot(
    isFirebaseReady: Boolean,
    onRetryFirebase: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Automatically warm up / force-refresh App Check token on startup so AI & Firestore work immediately
    // without requiring the user to open Diagnostics and press Spara & Ominstallera + Refresh.
    LaunchedEffect(isFirebaseReady) {
        if (isFirebaseReady) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val appCheck = FirebaseAppCheck.getInstance()
                    appCheck.getAppCheckToken(true).addOnSuccessListener {
                        Log.d(TAG, "Startup App Check token refreshed automatically.")
                    }.addOnFailureListener { err ->
                        Log.w(TAG, "Startup App Check token refresh warning: ${err.message}")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Startup App Check warm-up skipped: ${e.message}")
                }
            }
        }
    }

    if (!isFirebaseReady) {
        // Firebase initialization guard: prevents any Firebase or AiNutritionService calls until ready
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(EmeraldPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = "NäringAI",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = EmeraldPrimary,
                    strokeWidth = 2.5.dp
                )

                Text(
                    text = "Initierar Firebase och säkerhetstjänster...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = onRetryFirebase,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Försök igen", fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var isCheckingAuth by remember { mutableStateOf(currentUser == null) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
            if (auth.currentUser != null) {
                isCheckingAuth = false
            }
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    // Auto sign-in attempt on startup if currentUser is not yet initialized
    LaunchedEffect(Unit) {
        val cachedUser = Firebase.auth.currentUser
        if (cachedUser != null) {
            currentUser = cachedUser
            isCheckingAuth = false
            return@LaunchedEffect
        }

        val credentialManager = CredentialManager.create(context)
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
                isCheckingAuth = false
            },
            onUnauthenticated = {
                isCheckingAuth = false
            },
            scope = coroutineScope
        )
    }

    if (isCheckingAuth) {
        // High-polish startup splash screen while restoring user session
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(EmeraldPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = "NäringAI",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = EmeraldPrimary,
                    strokeWidth = 2.5.dp
                )

                Text(
                    text = "Ansluter till ditt konto...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else if (currentUser == null) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        val aiNutritionService = remember { AiNutritionService() }
        AppNavigation(
            currentUserId = currentUser!!.uid,
            aiNutritionService = aiNutritionService
        )
    }
}
