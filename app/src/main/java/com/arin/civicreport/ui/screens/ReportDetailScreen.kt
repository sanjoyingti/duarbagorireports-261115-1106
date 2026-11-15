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
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arin.civicreport.ui.AppScaffold
import com.arin.civicreport.ui.EmptyState
import com.arin.civicreport.ui.components.LocationMap
import com.arin.civicreport.ui.components.PhotoThumb
import com.arin.civicreport.ui.components.StatusChip
import com.arin.civicreport.ui.components.formatDate
import com.arin.civicreport.ui.components.openMediaFile
import com.arin.civicreport.viewmodel.ReportViewModel
import kotlinx.coroutines.launch
import androidx.compose.material3.Icon

@Composable
fun ReportDetailScreen(vm: ReportViewModel, reportId: String, onBack: () -> Unit) {
    val reports by vm.reports.collectAsStateWithLifecycle()
    val report = reports.firstOrNull { it.id == reportId }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    AppScaffold(
        screenTitle = "Complaint details",
        onBack = onBack,
        snackbarHostState = snackbar
    ) { padding ->
        if (report == null) {
            Column(Modifier.padding(padding)) {
                EmptyState(
                    icon = Icons.Filled.ReportProblem,
                    title = "Complaint not found",
                    message = "It may have been deleted from the list.",
                    actionLabel = "Back",
                    onAction = onBack
                )
            }
            return@AppScaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(report.title, style = MaterialTheme.typography.headlineSmall)
                StatusChip(report.status)
            }
            Text(
                formatDate(report.createdAt),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text("Place", style = MaterialTheme.typography.titleMedium)
                    Text(report.location.oneLine, style = MaterialTheme.typography.bodyMedium)
                    if (report.location.hasFix) {
                        Text(
                            "%.6f, %.6f  ±%.0f m".format(
                                report.location.latitude,
                                report.location.longitude,
                                report.location.accuracy
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    val jurisdiction = listOf(
                        report.location.city,
                        report.location.district,
                        report.location.state,
                        report.location.postcode
                    ).filter { it.isNotBlank() }
                    if (jurisdiction.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            Text(
                                jurisdiction.joinToString(", "),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            if (report.location.hasFix) {
                LocationMap(
                    latitude = report.location.latitude,
                    longitude = report.location.longitude
                )
            }

            if (report.notes.isNotBlank()) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Details given", style = MaterialTheme.typography.titleMedium)
                        Text(report.notes, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            if (report.mediaUris.isNotEmpty() || report.videoUri != null) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Evidence", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(report.mediaUris) { path ->
                                Box {
                                    PhotoThumb(path)
                                    TextButton(onClick = { openMediaFile(context, path) }) {
                                        Text("Open")
                                    }
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
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                Icons.Filled.FiberManualRecord,
                                                contentDescription = null
                                            )
                                            TextButton(onClick = { openMediaFile(context, path) }) {
                                                Text("Play")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        "Offices notified (${report.authorities.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                    report.authorities.forEach { authority ->
                        Text("• ${authority.name}", style = MaterialTheme.typography.bodyMedium)
                        authority.email?.let {
                            Text(
                                "    $it",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        authority.phone?.let {
                            Text(
                                "    $it",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    scope.launch {
                        val ok = vm.resend(report)
                        snackbar.showSnackbar(
                            if (ok) "Resend prepared for ${report.authorities.size} office(s)."
                            else "No mail or SMS app could be opened."
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Resend this complaint") }

            TextButton(
                onClick = {
                    vm.deleteReport(report)
                    onBack()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Delete this complaint") }

            Spacer(Modifier.height(24.dp))
        }
    }
}
