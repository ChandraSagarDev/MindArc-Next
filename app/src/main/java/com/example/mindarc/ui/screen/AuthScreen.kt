package com.example.mindarc.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.mindarc.ui.viewmodel.MindArcViewModel
import android.util.Base64
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.CustomCredential
import java.security.SecureRandom
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import com.example.mindarc.ui.components.MindArcPrimaryButton
import com.example.mindarc.ui.components.MindArcSecondaryButton

@Composable
fun AuthScreen(
    navController: NavController,
    viewModel: MindArcViewModel = hiltViewModel()
) {
    val authEmail by viewModel.authEmail.collectAsState()
    val context = LocalContext.current
    val credentialManager = remember { CredentialManager.create(context) }
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun generateSecureRandomNonce(byteLength: Int = 32): String {
        val randomBytes = ByteArray(byteLength)
        SecureRandom().nextBytes(randomBytes)
        return Base64.encodeToString(
            randomBytes,
            Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING
        )
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (authEmail != null) {
                Text(text = "Signed in as", style = MaterialTheme.typography.titleMedium)
                Text(text = authEmail ?: "", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                MindArcSecondaryButton(
                    text = "Sign out",
                    onClick = {
                        viewModel.signOut()
                        navController.popBackStack()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                TextButton(onClick = { navController.popBackStack() }) { Text("Back") }
                return@Surface
            } else {
                Text(
                    text = if (isSignUp) "Create account" else "Sign in",
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge,
            )

            error?.let {
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(18.dp))

            MindArcPrimaryButton(
                hero = true,
                text = "Continue with Google",
                onClick = {
                    error = null
                    val webClientId = context.getString(com.example.mindarc.R.string.google_web_client_id)
                    if (webClientId.isBlank() || webClientId == "YOUR_GOOGLE_WEB_CLIENT_ID") {
                        error = "Set google_web_client_id in strings.xml"
                    } else {
                        val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(webClientId)
                            .setNonce(generateSecureRandomNonce())
                            .build()

                        val request = GetCredentialRequest.Builder()
                            .addCredentialOption(signInWithGoogleOption)
                            .build()

                        scope.launch {
                            try {
                                val result: GetCredentialResponse = credentialManager.getCredential(context, request)
                                val credential = result.credential
                                if (credential is CustomCredential &&
                                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                                ) {
                                    val googleIdTokenCredential =
                                        GoogleIdTokenCredential.createFrom(credential.data)
                                    val idToken = googleIdTokenCredential.idToken
                                    viewModel.signInWithGoogleIdToken(idToken) { ok, msg ->
                                        if (ok) navController.popBackStack()
                                        else error = msg ?: "Google sign-in failed"
                                    }
                                } else {
                                    error = "Unexpected credential type"
                                }
                            } catch (e: Exception) {
                                error = e.message ?: "Google sign-in failed"
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            MindArcPrimaryButton(
                hero = true,
                onClick = {
                    error = null
                    val handler: (Boolean, String?) -> Unit = { ok, msg ->
                        if (ok) navController.popBackStack()
                        else error = msg ?: "Authentication failed"
                    }
                    if (isSignUp) viewModel.signUp(email, password, handler)
                    else viewModel.signIn(email, password, handler)
                },
                modifier = Modifier.fillMaxWidth(),
                text = if (isSignUp) "Create account" else "Sign in",
            )

            TextButton(onClick = { isSignUp = !isSignUp }) {
                Text(if (isSignUp) "Already have an account? Sign in" else "New here? Create an account")
            }
        }
    }
}

