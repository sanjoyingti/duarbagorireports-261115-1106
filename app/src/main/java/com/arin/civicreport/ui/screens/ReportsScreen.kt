package com.arin.civicreport.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arin.civicreport.ui.AppScaffold
import com.arin.civicreport.ui.EmptyState
import com.arin.civicreport.ui.components.StatusChip
import com.arin.civicreport.ui.components.formatDate
import com.arin.civicreport.viewmodel.ReportViewModel
import kotlinx.coroutines.launch

@Composable
fun ReportsScreen(vm: ReportViewModel, onBack: () -> Unit, onOpenReport: (String) -> Unit) {
    val reports by vm.reports.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    AppScaffold(
        screenTitle = "All complaints",
        onBack = onBack,
        snackbarHostState = snackbar
    ) { padding ->
        if (reports.isEmpty()) {
            Column(Modifier.padding(padding)) {
                EmptyState(
                    icon = Icons.AutoMirrored.Filled.Assignment,
                    title = "No complaint saved yet",
                    message = "Report a problem from the home screen and it will appear here.",
                    actionLabel = "Back to home",
                    onAction = onBack
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        "${reports.size} saved complaint(s). Tap one to see everything.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(reports) { report ->
                    Card(
                        onClick = { onOpenReport(report.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(report.title, style = MaterialTheme.typography.titleMedium)
                                StatusChip(report.status)
                            }
                            Text(
                                report.location.oneLine,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                formatDate(report.createdAt),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = {
                                    scope.launch {
                                        val ok = vm.resend(report)
                                        snackbar.showSnackbar(
                                            if (ok) "Resend prepared for ${report.authorities.size} office(s)."
                                            else "No mail or SMS app could be opened."
                                        )
                                    }
                                }) { Text("Resend") }
                                TextButton(onClick = {
                                    vm.deleteReport(report)
                                    scope.launch { snackbar.showSnackbar("Complaint deleted.") }
                                }) { Text("Delete") }
                            }
                        }
                    }
                }
            }
        }
    }
}
