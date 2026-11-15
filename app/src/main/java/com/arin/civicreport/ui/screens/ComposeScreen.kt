package com.arin.civicreport.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arin.civicreport.model.Report
import com.arin.civicreport.model.ReportStatus
import com.arin.civicreport.model.SendChannel
import com.arin.civicreport.ui.AppScaffold
import com.arin.civicreport.ui.EmptyState
import com.arin.civicreport.ui.components.PhotoThumb
import com.arin.civicreport.ui.components.openMediaFile
import com.arin.civicreport.viewmodel.ReportViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ComposeScreen(vm: ReportViewModel, onBack: () -> Unit, onFinished: () -> Unit) {
    val report: Report? = remember { vm.buildReport() }
    val settings by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    if (report == null) {
        AppScaffold(screenTitle = "Review and send", onBack = onBack) { padding ->
            Column(Modifier.padding(padding)) {
                EmptyState(
                    icon = Icons.Filled.ReportProblem,
                    title = "The draft is incomplete",
                    message = "A problem type and an exact location are needed before sending.",
                    actionLabel = "Go back",
                    onAction = onBack
                )
            }
        }
        return
    }

    var subject by remember {
        mutableStateOf(
            "Complaint: ${report.title}" +
                if (report.location.city.isNotBlank()) " at ${report.location.city}" else ""
        )
    }
    var body by remember { mutableStateOf(vm.sendHelper.buildBody(report)) }
    var sendEmail by remember { mutableStateOf(settings.defaultChannel != SendChannel.SMS) }
    var sendSms by remember { mutableStateOf(settings.defaultChannel != SendChannel.EMAIL) }
    var sending by remember { mutableStateOf(false) }

    fun currentChannel(): SendChannel = when {
        sendEmail && sendSms -> SendChannel.BOTH
        sendSms -> SendChannel.SMS
        else -> SendChannel.EMAIL
    }

    fun snapshot(status: ReportStatus): Report = report.copy(
        sendEmail = sendEmail,
        sendSms = sendSms,
        status = status,
        lastSentAt = if (status == ReportStatus.SENT) System.currentTimeMillis() else report.lastSentAt,
        sentChannels = when (currentChannel()) {
            SendChannel.BOTH -> listOf("email", "sms")
            SendChannel.EMAIL -> listOf("email")
            SendChannel.SMS -> listOf("sms")
        }
    )

    val canSend = !sending &&
        report.authorities.isNotEmpty() &&
        subject.isNotBlank() &&
        body.isNotBlank() &&
        (sendEmail || sendSms)

    AppScaffold(
        screenTitle = "Review and send",
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
                    Text(
                        "Recipients (${report.authorities.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (report.authorities.isEmpty()) {
                        Text(
                            "No office selected. Go back and tick at least one.",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    report.authorities.forEach { authority ->
                        Text("• ${authority.name}", style = MaterialTheme.typography.bodyMedium)
                        authority.email?.let { mail ->
                            Text(
                                "    $mail",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        authority.phone?.let { phone ->
                            Text(
                                "    $phone",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (report.mediaUris.isNotEmpty() || report.videoUri != null) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Attached evidence", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(report.mediaUris) { path ->
                                Box {
                                    PhotoThumb(path)
                                }
                            }
                            report.videoUri?.let { path ->
                                item {
                                    Box(
                                        modifier = Modifier
                                            .size(84.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.secondaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.FiberManualRecord,
                                            contentDescription = null
                                        )
                                    }
                                }
                            }
                        }
                        TextButton(onClick = {
                            report.videoUri?.let { openMediaFile(context, it) }
                                ?: report.mediaUris.firstOrNull()?.let { openMediaFile(context, it) }
                        }) { Text("Open the first attachment") }
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text("Place", style = MaterialTheme.typography.titleMedium)
                    Text(report.location.oneLine, style = MaterialTheme.typography.bodyMedium)
                    if (report.location.hasFix) {
                        Text(
                            "%.6f, %.6f".format(report.location.latitude, report.location.longitude),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                label = { Text("Subject") },
                singleLine = true,
                isError = subject.isBlank(),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text("Message") },
                minLines = 8,
                isError = body.isBlank(),
                modifier = Modifier.fillMaxWidth()
            )

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("How to send", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Email the selected offices")
                        Switch(checked = sendEmail, onCheckedChange = { sendEmail = it })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Text message to the selected offices")
                        Switch(checked = sendSms, onCheckedChange = { sendSms = it })
                    }
                    if (!sendEmail && !sendSms) {
                        Text(
                            "Turn on at least one way to send.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Button(
                onClick = {
                    sending = true
                    val channel = currentChannel()
                    scope.launch {
                        val ok = vm.sendCurrent(report, subject, body, channel)
                        vm.persist(snapshot(if (ok) ReportStatus.SENT else ReportStatus.DRAFT))
                        delay(150)
                        sending = false
                        if (ok) {
                            snackbar.showSnackbar("Complaint handed to the mail or SMS app.")
                            delay(400)
                            onFinished()
                        } else {
                            snackbar.showSnackbar(
                                "No mail or SMS app opened. The complaint was saved as a draft."
                            )
                        }
                    }
                },
                enabled = canSend,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (sending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.size(8.dp))
                    Text("Preparing…")
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Send the complaint")
                }
            }

            TextButton(
                onClick = {
                    scope.launch {
                        vm.persist(snapshot(ReportStatus.DRAFT))
                        snackbar.showSnackbar("Saved as a draft. You can resend it from the list.")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save without sending") }

            Spacer(Modifier.height(24.dp))
        }
    }
}
