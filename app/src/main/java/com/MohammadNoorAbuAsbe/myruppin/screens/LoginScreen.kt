package com.MohammadNoorAbuAsbe.myruppin.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.MohammadNoorAbuAsbe.myruppin.data.TokenManager
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

@Composable
fun LoginScreen(navController: NavController, modifier: Modifier = Modifier) {
    var id by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var responseText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var initialAutoLoginDone by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val scope = rememberCoroutineScope()
    val client = remember { OkHttpClient() }

    val savedId by tokenManager.studentId.collectAsState(initial = null)
    val savedPassword by tokenManager.password.collectAsState(initial = null)

    val tryLogin = { studentId: String, pwd: String ->
        isLoading = true
        scope.launch {
            try {
                val jsonObject = JSONObject().apply {
                    put("loginType", "student")
                    put("password", pwd)
                    put("zht", studentId)
                }

                val requestBody = jsonObject.toString()
                    .toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("https://ruppinet.ruppin.ac.il/Portals/api/Login/Login")
                    .post(requestBody)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        scope.launch {
                            responseText = "Error: ${e.message}"
                            isLoading = false
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val responseBody = response.body?.string() ?: ""
                        scope.launch {
                            try {
                                val jsonResponse = JSONObject(responseBody)
                                val success = jsonResponse.getBoolean("success")
                                if (success) {
                                    val newToken = jsonResponse.getString("token")
                                    tokenManager.saveCredentials(newToken, studentId, pwd)
                                    responseText = "Login successful!"
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                } else {
                                    responseText = "Login failed"
                                }
                            } catch (e: Exception) {
                                responseText = "Error parsing response: ${e.message}"
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                })
            } catch (e: Exception) {
                responseText = "Error: ${e.message}"
                isLoading = false
            }
        }
    }

    LaunchedEffect(savedId, savedPassword) {
        savedId?.let { id = it }
        savedPassword?.let { password = it }
    }

    LaunchedEffect(id, password) {
        if (!initialAutoLoginDone && savedId != null && savedPassword != null) {
            id = savedId!!
            password = savedPassword!!
            tryLogin(savedId!!, savedPassword!!)
            initialAutoLoginDone = true
        }
    }

    val idFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextField(
                value = id,
                onValueChange = { id = it },
                label = { Text("Student ID") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(idFocusRequester),
                enabled = !isLoading,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Next
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onNext = { passwordFocusRequester.requestFocus() }
                )
            )

            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(passwordFocusRequester),
                enabled = !isLoading,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onDone = { tryLogin(id, password) }
                )
            )

            Button(
                onClick = { tryLogin(id, password) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && id.isNotEmpty() && password.isNotEmpty()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Login")
                }
            }

            if (responseText.isNotEmpty()) {
                Text(
                    text = responseText,
                    color = if (responseText.contains("successful")) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }
        }
    }
}