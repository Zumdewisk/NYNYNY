package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MintAccent
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.security.MessageDigest

private const val TAG = "AuthScreen"

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun resolveDefaultWebClientId(context: Context): String? {
    val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
    if (resId == 0) return null
    return try {
        context.getString(resId).takeIf { it.isNotBlank() }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to read default_web_client_id", e)
        null
    }
}

@Suppress("DEPRECATION")
private fun getApkSigningSha1(context: Context): String? {
    return try {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val info = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNING_CERTIFICATES
            )
            val signingInfo = info.signingInfo ?: return null
            if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners
            } else {
                signingInfo.signingCertificateHistory
            }
        } else {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNATURES
            ).signatures
        }
        val certBytes = signatures?.firstOrNull()?.toByteArray() ?: return null
        val digest = MessageDigest.getInstance("SHA-1").digest(certBytes)
        digest.joinToString(":") { byte -> "%02X".format(byte) }
    } catch (e: Exception) {
        Log.w(TAG, "Could not inspect APK signing SHA-1: ${e.message}")
        null
    }
}

private fun formatConfigError(context: Context, prefix: String): String {
    val sha1 = getApkSigningSha1(context)
    val shaLine = if (sha1 != null) "\n\nAPK-signatur (SHA-1): $sha1" else ""
    return "$prefix$shaLine"
}

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    // 1. If Firebase already holds an authenticated user session, proceed immediately!
    val currentAuthUser = Firebase.auth.currentUser
    if (currentAuthUser != null) {
        Log.d(TAG, "Current user already authenticated: ${currentAuthUser.uid}")
        onAuthSuccess()
        return
    }

    val activity = context.findActivity()
    if (activity == null) {
        Log.w(TAG, "No activity context found for auto sign-in")
        onUnauthenticated()
        return
    }

    val clientId = resolveDefaultWebClientId(context)
    if (clientId == null) {
        Log.e(TAG, "Missing default_web_client_id")
        onUnauthenticated()
        return
    }

    scope.launch {
        try {
            // Standard silent CredentialManager resolution using GetGoogleIdOption
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(true)
                .build()

            val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                Log.d(TAG, "Silent auto sign-in successful: ${Firebase.auth.currentUser?.uid}")
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (e: Exception) {
            Log.d(TAG, "Silent auto sign-in attempt skipped: ${e.message}")
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val activity = context.findActivity()
    if (activity == null) {
        onAuthError("Kunde inte hitta appfönstret för Google-inloggning.")
        return
    }

    val clientId = resolveDefaultWebClientId(context)
    if (clientId == null) {
        onAuthError("Konfigurationen för Google-inloggning saknas.")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

    scope.launch {
        val startTimeMs = SystemClock.elapsedRealtime()
        try {
            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("Oväntad inloggningstyp. Försök igen.")
            }
        } catch (e: GetCredentialCancellationException) {
            val elapsedMs = SystemClock.elapsedRealtime() - startTimeMs
            val rawMsg = e.message.orEmpty()
            Log.w(TAG, "Google Sign-In avbröts eller nekades (${elapsedMs}ms): $rawMsg", e)
            val looksLikeConfigAbort = elapsedMs < 800L ||
                rawMsg.contains("2844", ignoreCase = true) ||
                rawMsg.contains("developer", ignoreCase = true) ||
                rawMsg.contains("caller", ignoreCase = true) ||
                rawMsg.contains("signature", ignoreCase = true)
            if (looksLikeConfigAbort) {
                onAuthError(
                    formatConfigError(
                        context,
                        "Google-inloggningen avvisades direkt av Google Play-tjänster. Kontrollera att APK:ns signeringsnyckel (SHA-1) är registrerad i Firebase / google-services.json och att ett Google-konto finns på enheten."
                    )
                )
            } else {
                onAuthCancelled()
            }
        } catch (e: NoCredentialException) {
            Log.w(TAG, "Inga Google-inloggningsuppgifter tillgängliga: ${e.message}", e)
            onAuthError(
                formatConfigError(
                    context,
                    "Inget Google-konto kunde användas för inloggning. Kontrollera att du är inloggad med ett Google-konto på telefonen, att inloggningsfrågor är aktiverade under Google-kontoinställningar och att APK:ns SHA-1 matchar Firebase."
                )
            )
        } catch (e: GetCredentialProviderConfigurationException) {
            Log.e(TAG, "Credential Manager provider saknas", e)
            onAuthError("Google Play-tjänster för inloggning saknas eller behöver uppdateras på enheten.")
        } catch (e: GetCredentialUnsupportedException) {
            Log.e(TAG, "Credential Manager stöds inte på enheten", e)
            onAuthError("Enheten stöder inte Google Credential Manager. Uppdatera Google Play-tjänster och försök igen.")
        } catch (e: GetCredentialInterruptedException) {
            Log.w(TAG, "Inloggningen avbröts tillfälligt", e)
            onAuthError("Inloggningen avbröts tillfälligt. Tryck på Fortsätt med Google för att försöka igen.")
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Google Credential Manager misslyckades (${e.type})", e)
            val msg = e.localizedMessage ?: e.message ?: "Inloggningen misslyckades."
            if (msg.contains("2844") ||
                msg.contains("Developer console", ignoreCase = true) ||
                msg.contains("10:") ||
                msg.contains("12500")
            ) {
                onAuthError(
                    formatConfigError(
                        context,
                        "Google-inloggningen blockerades på grund av felaktig app-signatur eller OAuth-konfiguration i Firebase (kod 2844/10). Kontrollera att GitHub-bygget använde rätt debug.keystore.base64 och google-services.json."
                    )
                )
            } else {
                onAuthError("Google-inloggning misslyckades: $msg")
            }
        } catch (e: FirebaseAuthException) {
            Log.e(TAG, "Firebase Auth misslyckades (${e.errorCode})", e)
            onAuthError("Firebase-autentisering misslyckades (${e.errorCode}): ${e.localizedMessage ?: "Försök igen."}")
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In misslyckades", e)
            val msg = e.localizedMessage ?: "Inloggningen misslyckades."
            if (msg.contains("2844") || msg.contains("Developer console", ignoreCase = true)) {
                onAuthError(
                    formatConfigError(
                        context,
                        "Google-inloggningen blockerades på grund av felaktig app-signatur eller OAuth-konfiguration i Firebase."
                    )
                )
            } else {
                onAuthError(msg)
            }
        }
    }
}

fun signOutUser(
    context: Context,
    credentialManager: CredentialManager,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        .edit()
        .putBoolean("was_signed_in", false)
        .apply()
    scope.launch {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(TAG, "Kunde inte rensa inloggningsstatus: ${e.message}")
            } finally {
                Firebase.auth.signOut()
                onSignOutComplete()
            }
        }
    }
}

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .systemBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Brand Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(EmeraldPrimary, MintAccent, EmeraldDark)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "NäringAI-logotyp",
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "NäringAI",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "Din smarta AI-kostcoach och matdagbok",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Feature Highlights Cards
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FeatureRow(
                    icon = Icons.Default.CameraAlt,
                    title = "Fotografera din mat",
                    description = "AI analyserar och beräknar kalorier och makron på sekunder."
                )
                FeatureRow(
                    icon = Icons.Default.PieChart,
                    title = "Komplett matdagbok",
                    description = "Följ frukost, lunch, middag och mellanmål med precision."
                )
                FeatureRow(
                    icon = Icons.Default.WaterDrop,
                    title = "Vatten- och hälsoöversikt",
                    description = "Smarta personliga näringsmål anpassade för din kropp."
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bottom Sign In Area
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (errorMessage != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Inloggningsfel",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            SelectionContainer {
                                Text(
                                    text = errorMessage ?: "",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onAuthError = { err ->
                                isLoading = false
                                errorMessage = err
                            },
                            scope = coroutineScope,
                            onAuthCancelled = {
                                isLoading = false
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("google_signin_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = Color.Black
                    ),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.Black,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Fortsätt med Google",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Dina måltider sparas tryggt och synkroniseras i molnet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(EmeraldPrimary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
