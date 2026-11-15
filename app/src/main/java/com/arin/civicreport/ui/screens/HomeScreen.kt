package com.arin.civicreport.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arin.civicreport.R
import com.arin.civicreport.model.IncidentType
import com.arin.civicreport.ui.AppScaffold
import com.arin.civicreport.ui.components.StatusChip
import com.arin.civicreport.ui.components.formatDate
import com.arin.civicreport.viewmodel.ReportViewModel
import kotlinx.coroutines.delay
import androidx.compose.ui.res.stringResource

@Composable
fun HomeScreen(
    vm: ReportViewModel,
    onNewReport: (IncidentType?) -> Unit,
    onReports: () -> Unit,
    onSettings: () -> Unit,
    onOpenReport: (String) -> Unit
) {
    val reports by vm.reports.collectAsStateWithLifecycle()
    var suggestions by remember { mutableStateOf(emptyList<IncidentType>()) }

    LaunchedEffect(Unit) {
        vm.detector.start()
        delay(1600)
        suggestions = vm.detectActivitySuggestion()
    }
    DisposableEffect(Unit) { onDispose { vm.detector.stop() } }

    AppScaffold(
        screenTitle = stringResource(R.string.app_tagline),
        actions = {
            IconButton(onClick = onReports) {
                Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = "My reports")
            }
            IconButton(onClick = onSettings) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = stringResource(R.string.app_tagline),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { onNewReport(null) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text("Report an issue")
                        }
                    }
                }
            }

            if (suggestions.isNotEmpty()) {
                item {
                    Column {
                        Text(
                            text = "Suggested for what you are doing now",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            suggestions.take(3).forEach { type ->
                                AssistChip(
                                    onClick = { onNewReport(type) },
                                    label = { Text(type.shortLabel) },
                                    leadingIcon = { Text(type.emoji) }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text("Every activity you can report", style = MaterialTheme.typography.titleMedium)
            }

            items(IncidentType.entries.toList()) { type ->
                Card(
                    onClick = { onNewReport(type) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(type.emoji, style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.size(12.dp))
                        Column {
                            Text(type.label, style = MaterialTheme.typography.titleMedium)
                            Text(
                                type.blurb,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recent complaints", style = MaterialTheme.typography.titleMedium)
                    if (reports.size > 3) {
                        TextButton(onClick = onReports) { Text("See all") }
                    }
                }
            }

            if (reports.isEmpty()) {
                item {
                    Text(
                        "No complaint saved yet. Start with the report button above.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(reports.take(3)) { report ->
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
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
