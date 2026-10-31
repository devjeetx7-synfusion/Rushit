package com.rushit.app.ui.screens.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rushit.app.data.auth.AuthState
import com.rushit.app.ui.components.RushButton
import com.rushit.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    authState: AuthState,
    errorMessage: String?,
    onCompleteProfile: (username: String, fullName: String) -> Unit,
    onClearError: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }

    val isBusy = authState is AuthState.Authenticating || authState is AuthState.Loading

    Column(
        Modifier
            .fillMaxSize()
            .background(Cream)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Complete Your Profile", style = MaterialTheme.typography.headlineMedium, color = Ink)
        Spacer(Modifier.height(16.dp))
        Text("Please enter a username and your full name to continue.", style = MaterialTheme.typography.bodyMedium, color = Muted)

        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(32.dp))

        if (errorMessage != null) {
            Text(errorMessage, color = DeepRed, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(16.dp))
        }

        RushButton(
            text = "Complete Setup",
            onClick = { onCompleteProfile(username, fullName) },
            enabled = username.isNotBlank() && fullName.isNotBlank() && !isBusy,
            loading = isBusy
        )
    }

    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { onClearError() },
            title = { Text("Error") },
            text = { Text(errorMessage) },
            confirmButton = { TextButton(onClick = { onClearError() }) { Text("OK") } },
            containerColor = CardWhite
        )
    }
}
