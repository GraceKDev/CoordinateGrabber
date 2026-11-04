package com.example.coordinategrabber.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.coordinategrabber.components.ScaffoldTemplate
import com.example.coordinategrabber.data.export.CoordinateExportDTO
import com.example.coordinategrabber.data.model.Location
import com.example.coordinategrabber.data.model.Route
import com.example.coordinategrabber.data.network.ApiEndpoints
import com.example.coordinategrabber.viewModel.AuthState
import com.example.coordinategrabber.viewModel.ExportState
import com.example.coordinategrabber.viewModel.SettingsViewModel

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel,
    locations: List<Location> = emptyList(),
    routes: List<Route> = emptyList(),
    onDeleteLocalData: () -> Unit = {},
    onImportData: (CoordinateExportDTO) -> Unit = {}
) {
    ScaffoldTemplate("Settings") {
        SettingScreenContent(
            modifier = modifier,
            viewModel = settingsViewModel,
            locations = locations,
            routes = routes,
            onDeleteLocalData = onDeleteLocalData,
            onImportData = onImportData
        )
    }
}

@Composable
fun SettingScreenContent(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel,
    locations: List<Location>,
    routes: List<Route>,
    onDeleteLocalData: () -> Unit,
    onImportData: (CoordinateExportDTO) -> Unit
) {
    val context = LocalContext.current
    val darkMode by viewModel.darkMode.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    val exportState by viewModel.exportState.collectAsState()
    val authState by viewModel.authState.collectAsState()

    var showExportConfirmationDialog by remember { mutableStateOf(false) }
    var showImportConfirmationDialog by remember { mutableStateOf(false) }
    var showDeleteServerDialog by remember { mutableStateOf(false) }
    var showDeleteLocalDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Theme Settings Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Appearance",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                SwitchSettingsState(
                    name = "Dark Mode",
                    value = darkMode,
                    onValueChange = { enabled ->
                        viewModel.setDarkMode(enabled)
                    }
                )
            }
        }

        // Account Session Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Account Session",
                    style = MaterialTheme.typography.titleMedium
                )

                if (isLoggedIn) {
                    Text(
                        text = if (userEmail.isNotBlank()) "Logged in as $userEmail" else "Logged in with Google",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedButton(
                        onClick = { viewModel.logout() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sign Out")
                    }
                } else {
                    Text(
                        text = "Not signed in. Sign in to enable server sync.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = { viewModel.performAuthenticate(context) },
                        enabled = authState !is AuthState.Authenticating,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (authState is AuthState.Authenticating) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Text("Signing in...")
                            }
                        } else {
                            Text("Sign In with Google")
                        }
                    }

                    if (authState is AuthState.Error) {
                        Text(
                            text = (authState as AuthState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // Server Data Synchronization & Export Card (Visible when logged in)
        if (isLoggedIn) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Server Data Sync",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = "${locations.size} saved coordinates & ${routes.size} routes locally. Backend: ${ApiEndpoints.DEFAULT_BASE_URL}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Button(
                        onClick = { showExportConfirmationDialog = true },
                        enabled = exportState !is ExportState.Exporting,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (exportState is ExportState.Exporting) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Text("Syncing with server...")
                            }
                        } else {
                            Text("Export Data to Server")
                        }
                    }

                    OutlinedButton(
                        onClick = { showImportConfirmationDialog = true },
                        enabled = exportState !is ExportState.Exporting,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Import Data from Server")
                    }

                    Button(
                        onClick = { showDeleteServerDialog = true },
                        enabled = exportState !is ExportState.Exporting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Delete Server Data")
                    }

                    when (val state = exportState) {
                        is ExportState.Success -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = state.message.ifBlank { "Operation successful!" },
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { viewModel.resetExportState() }) {
                                    Text("Dismiss")
                                }
                            }
                        }
                        is ExportState.Error -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = state.message,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { viewModel.resetExportState() }) {
                                    Text("Dismiss")
                                }
                            }
                        }
                        else -> { }
                    }
                }
            }
        }

        // Local Storage Management Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Local Storage",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "Manage coordinates and route data stored on this device.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Button(
                    onClick = { showDeleteLocalDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete Local Data")
                }
            }
        }
    }

    // Dialog 1: Export Confirmation
    if (showExportConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showExportConfirmationDialog = false },
            title = { Text("Export Data") },
            text = {
                Text(
                    "Upload ${locations.size} coordinates and ${routes.size} routes to your backend at:\n\n${ApiEndpoints.DEFAULT_BASE_URL}"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExportConfirmationDialog = false
                        viewModel.performExport(
                            context = context,
                            locations = locations,
                            routes = routes
                        )
                    }
                ) {
                    Text("Upload Data")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showExportConfirmationDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog 2: Import Confirmation
    if (showImportConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showImportConfirmationDialog = false },
            title = { Text("Import Data from Server") },
            text = {
                Text(
                    "Pull coordinates and routes from the backend server (${ApiEndpoints.DEFAULT_BASE_URL}) and merge them with your local database?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showImportConfirmationDialog = false
                        viewModel.performImport(
                            context = context,
                            onImportSuccess = { importedDto ->
                                onImportData(importedDto)
                            }
                        )
                    }
                ) {
                    Text("Import & Merge")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showImportConfirmationDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog 3: Delete Server Data Confirmation
    if (showDeleteServerDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteServerDialog = false },
            title = { Text("Delete Server Data") },
            text = {
                Text(
                    "Are you sure you want to PERMANENTLY DELETE all your coordinates and routes stored on the backend server?\n\nThis action cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteServerDialog = false
                        viewModel.performDeleteServerData(context = context)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Delete Server Data")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteServerDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog 4: Delete Local Data Confirmation
    if (showDeleteLocalDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteLocalDialog = false },
            title = { Text("Delete Local Data") },
            text = {
                Text(
                    "Are you sure you want to PERMANENTLY DELETE all local coordinates and routes stored on this device?\n\nThis action cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteLocalDialog = false
                        viewModel.performDeleteLocalData {
                            onDeleteLocalData()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Delete Local Data")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteLocalDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SwitchSettingsState(
    modifier: Modifier = Modifier,
    value: Boolean,
    name: String,
    onValueChange: (Boolean) -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name)

        Switch(
            checked = value,
            onCheckedChange = onValueChange
        )
    }
}
