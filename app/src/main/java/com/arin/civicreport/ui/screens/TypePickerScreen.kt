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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arin.civicreport.model.IncidentType
import com.arin.civicreport.ui.AppScaffold
import com.arin.civicreport.viewmodel.ReportViewModel

@Composable
fun TypePickerScreen(vm: ReportViewModel, onBack: () -> Unit, onContinue: () -> Unit) {
    val draft by vm.draft.collectAsStateWithLifecycle()

    AppScaffold(screenTitle = "What are you reporting?", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth()
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        "Tick every problem you see here. The app finds the offices that must be told.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(IncidentType.entries.toList()) { type ->
                    val selected = type in draft.types
                    if (selected) {
                        Card(
                            onClick = { vm.toggleType(type) },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TypeRow(type, true)
                        }
                    } else {
                        OutlinedCard(
                            onClick = { vm.toggleType(type) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TypeRow(type, false)
                        }
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }

            Column(Modifier.padding(16.dp)) {
                Text(
                    text = if (draft.types.isEmpty()) "Nothing ticked yet" else
                        "${draft.types.size} problem(s) ticked",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onContinue,
                    enabled = draft.types.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Continue to photo and location")
                }
            }
        }
    }
}

@Composable
private fun TypeRow(type: IncidentType, selected: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(type.emoji, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(type.label, style = MaterialTheme.typography.titleMedium)
            Text(
                type.blurb,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (selected) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
