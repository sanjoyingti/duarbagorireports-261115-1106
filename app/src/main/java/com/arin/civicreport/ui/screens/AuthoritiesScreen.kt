package com.arin.civicreport.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arin.civicreport.ui.AppScaffold
import com.arin.civicreport.ui.ErrorState
import com.arin.civicreport.viewmodel.ReportViewModel

@Composable
fun AuthoritiesScreen(vm: ReportViewModel, onBack: () -> Unit, onContinue: () -> Unit) {
    val draft by vm.draft.collectAsStateWithLifecycle()
    val context = LocalContext.current

    AppScaffold(screenTitle = "Who will receive it", onBack = onBack) { padding ->
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
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text("Place of the report", style = MaterialTheme.typography.titleMedium)
                            Text(
                                draft.location.oneLine,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            val jurisdiction =
                                listOf(
                                    draft.location.city,
                                    draft.location.district,
                                    draft.location.state
                                ).filter { it.isNotBlank() }
                            if (jurisdiction.isNotEmpty()) {
                                Text(
                                    "Jurisdiction: ${jurisdiction.joinToString(", ")}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                "Problems: ${draft.types.joinToString(", ") { it.shortLabel }}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (draft.searchingAuthorities) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.height(20.dp))
                            Spacer(Modifier.padding(6.dp))
                            Text("Matching the responsible offices…")
                        }
                    }
                }

                draft.loadError?.let { message ->
                    item { ErrorState(message) { vm.searchAuthorities() } }
                }

                if (draft.authorities.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${draft.selectedAuthorityIds.size} of ${draft.authorities.size} selected",
                                style = MaterialTheme.typography.labelLarge
                            )
                            TextButton(onClick = {
                                vm.selectAllAuthorities(
                                    draft.selectedAuthorityIds.size != draft.authorities.size
                                )
                            }) {
                                Text(
                                    if (draft.selectedAuthorityIds.size == draft.authorities.size) "Clear all"
                                    else "Select all"
                                )
                            }
                        }
                    }
                }

                items(draft.authorities) { authority ->
                    val checked = authority.id in draft.selectedAuthorityIds
                    OutlinedCard(
                        onClick = { vm.toggleAuthority(authority.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = checked, onCheckedChange = { vm.toggleAuthority(authority.id) })
                            Column(Modifier.weight(1f)) {
                                Text(authority.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    authority.category,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                authority.email?.let {
                                    Text(it, style = MaterialTheme.typography.bodyMedium)
                                }
                                authority.phone?.let {
                                    Text(it, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            authority.email?.let { mail ->
                                IconButton(onClick = { vm.sendHelper.openUrl("mailto:$mail") }) {
                                    Icon(Icons.Filled.Email, contentDescription = "Email")
                                }
                            }
                            authority.phone?.let { phone ->
                                IconButton(onClick = { vm.sendHelper.dial(phone) }) {
                                    Icon(Icons.Filled.Phone, contentDescription = "Call")
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        "The state Chief Minister's office and the Prime Minister's Office are always included when available.",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
            }

            Column(Modifier.padding(16.dp)) {
                Button(
                    onClick = onContinue,
                    enabled = draft.selectedAuthorityIds.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Compose and send")
                }
                TextButton(onClick = { vm.searchAuthorities() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Search again")
                }
                Text(
                    "Sending opens your mail or SMS app with everything filled in.",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                context.hashCode()
            }
        }
    }
}
