package com.example.coordinategrabber.viewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.coordinategrabber.data.export.CoordinateExportDTO
import com.example.coordinategrabber.data.export.DataExporter
import com.example.coordinategrabber.data.model.Location
import com.example.coordinategrabber.data.model.Route
import com.example.coordinategrabber.data.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ExportState {
    data object Idle : ExportState
    data object Exporting : ExportState
    data class Success(val message: String) : ExportState
    data class Error(val message: String) : ExportState
}

sealed interface AuthState {
    data object Idle : AuthState
    data object Authenticating : AuthState
    data class Error(val message: String) : AuthState
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val darkMode = settingsRepository.darkModeFlow
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            false
        )

    val userEmail = settingsRepository.userEmailFlow
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            ""
        )

    val authToken = settingsRepository.authTokenFlow
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            ""
        )

    val isLoggedIn = settingsRepository.isLoggedInFlow
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            false
        )

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDarkMode(enabled)
        }
    }

    fun resetExportState() {
        _exportState.value = ExportState.Idle
    }

    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }

    fun logout() {
        viewModelScope.launch {
            settingsRepository.clearAuthSession()
            _exportState.value = ExportState.Idle
            _authState.value = AuthState.Idle
        }
    }

    fun performAuthenticate(context: Context) {
        viewModelScope.launch {
            _authState.value = AuthState.Authenticating
            val exporter = DataExporter(context)
            val result = exporter.authenticate()

            result.fold(
                onSuccess = { authResult ->
                    settingsRepository.saveAuthSession(authResult.userEmail, authResult.idToken)
                    _authState.value = AuthState.Idle
                },
                onFailure = { err ->
                    _authState.value = AuthState.Error(err.localizedMessage ?: "Authentication failed")
                }
            )
        }
    }

    fun performExport(
        context: Context,
        locations: List<Location>,
        routes: List<Route>
    ) {
        viewModelScope.launch {
            _exportState.value = ExportState.Exporting
            val exporter = DataExporter(context)

            val result = exporter.exportData(
                locations = locations,
                routes = routes,
                existingToken = authToken.value,
                existingUserEmail = userEmail.value
            )

            result.fold(
                onSuccess = { msg ->
                    _exportState.value = ExportState.Success(msg)
                },
                onFailure = { err ->
                    _exportState.value = ExportState.Error(err.localizedMessage ?: "Export failed")
                }
            )
        }
    }

    fun performImport(
        context: Context,
        onImportSuccess: (CoordinateExportDTO) -> Unit
    ) {
        viewModelScope.launch {
            _exportState.value = ExportState.Exporting
            val exporter = DataExporter(context)

            val result = exporter.importData(
                existingToken = authToken.value
            )

            result.fold(
                onSuccess = { dto ->
                    onImportSuccess(dto)
                    _exportState.value = ExportState.Success("Successfully imported ${dto.coordinates.size} coordinates and ${dto.routes.size} routes from server!")
                },
                onFailure = { err ->
                    _exportState.value = ExportState.Error(err.localizedMessage ?: "Import failed")
                }
            )
        }
    }

    fun performDeleteServerData(context: Context) {
        viewModelScope.launch {
            _exportState.value = ExportState.Exporting
            val exporter = DataExporter(context)

            val result = exporter.deleteServerData(
                existingToken = authToken.value
            )

            result.fold(
                onSuccess = { msg ->
                    _exportState.value = ExportState.Success(msg)
                },
                onFailure = { err ->
                    _exportState.value = ExportState.Error(err.localizedMessage ?: "Delete server data failed")
                }
            )
        }
    }

    fun performDeleteLocalData(onDeleteLocalSuccess: () -> Unit) {
        viewModelScope.launch {
            onDeleteLocalSuccess()
            _exportState.value = ExportState.Success("All local coordinates and routes have been deleted.")
        }
    }
}

class SettingsViewModelFactory(
    private val repository: SettingsRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(repository) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
