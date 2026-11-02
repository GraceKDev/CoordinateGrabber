package com.example.coordinategrabber.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.coordinategrabber.components.ScaffoldTemplate
import com.example.coordinategrabber.viewModel.SettingsViewModel
import kotlinx.coroutines.flow.StateFlow


@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel
) {
    ScaffoldTemplate("Settings") {
        SettingScreenContent(modifier,settingsViewModel)
    }
}

@Composable
fun SettingScreenContent(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel
) {
    val darkMode by viewModel.darkMode.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SwitchSettingsState(
            name = "Dark Mode",
            value = darkMode,
            onValueChange = { enabled ->
                viewModel.setDarkMode(enabled)
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