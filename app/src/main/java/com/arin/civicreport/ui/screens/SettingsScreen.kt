package com.arin.civicreport.ui.screens

import android.util.Patterns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.arin.civicreport.model.SendChannel
import com.arin.civicreport.ui.AppScaffold
import com.arin.civicreport.viewmodel.ReportViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(vm: ReportViewModel, onBack: () -> Unit) {
    val current = vm.settings.value
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var name by remember { mutableStateOf(current.senderName) }
    var email by remember { mutableStateOf(current.senderEmail) }
    var phone by remember { mutableStateOf(current.senderPhone) }
    var channel by remember { mutableStateOf(current.defaultChannel) }
    var notifications by remember { mutableStateOf(current.notificationsEnabled) }
    var highAccuracy by remember { mutableStateOf(current.highAccuracy) }
    var national by remember { mutableStateOf(current.includeNationalAuthorities) }

    val emailValid = email.isBlank() || Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    val phoneValid = phone.isBlank() || phone.count { it.isDigit() } in 8..15

    AppScaffold(
        screenTitle = "Settings",
        onBack = onBack,
        snackbarHostState = snackbar
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text("Your details", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Added at the bottom of every complaint so offices can reply to you.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Your name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Your email") },
                        singleLine = true,
                        isError = !emailValid,
                        supportingText = if (!emailValid) {
                            { Text("This does not look like an email address") }
                        } else null,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Your phone") },
                        singleLine = true,
                        isError = !phoneValid,
                        supportingText = if (!phoneValid) {
                            { Text("Use 8 to 15 digits") }
                        } else null,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text("Default way to send", style = MaterialTheme.typography.titleMedium)
                    SendChannel.entries.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = channel == option,
                                    onClick = { channel = option }
                                )
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = channel == option,
                                onClick = { channel = option }
                            )
                            Text(option.label, modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Notify me when a complaint is prepared")
                        Switch(checked = notifications, onCheckedChange = { notifications = it })
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Use the most precise GPS fix")
                        Switch(checked = highAccuracy, onCheckedChange = { highAccuracy = it })
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Always copy the PMO and CPCB")
                        Switch(checked = national, onCheckedChange = { national = it })
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Complaints are saved inside this phone. Nothing is uploaded until you press send.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(14.dp)
                )
            }

            Button(
                onClick = {
                    if (!emailValid || !phoneValid) {
                        scope.launch { snackbar.showSnackbar("Check the email and phone fields.") }
                        return@Button
                    }
                    vm.saveSettings(
                        current.copy(
                            senderName = name.trim(),
                            senderEmail = email.trim(),
                            senderPhone = phone.trim(),
                            defaultChannel = channel,
                            notificationsEnabled = notifications,
                            highAccuracy = highAccuracy,
                            includeNationalAuthorities = national
                        )
                    )
                    scope.launch {
                        snackbar.showSnackbar("Settings saved.")
                        onBack()
                    }
                },
                enabled = emailValid && phoneValid,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save settings") }

            Spacer(Modifier.height(24.dp))
        }
    }
}
