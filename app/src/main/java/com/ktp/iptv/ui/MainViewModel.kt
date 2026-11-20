package com.ktp.iptv.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ktp.iptv.BuildConfig
import com.ktp.iptv.data.Channel
import com.ktp.iptv.data.FilterState
import com.ktp.iptv.data.NetworkModule
import com.ktp.iptv.data.PlaylistRepository
import com.ktp.iptv.data.UpdateChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UiState(
    val channels: List<Channel> = emptyList(),
    val filtered: List<Channel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val filter: FilterState = FilterState(),
    val countries: List<String> = emptyList(),
    val categories: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val proxyEnabled: Boolean = false,
    val proxyHost: String = "",
    val proxyPort: String = "8080",
    val updateInfo: UpdateChecker.UpdateInfo? = null,
    val updateChecking: Boolean = false,
    val updateDownloading: Boolean = false,
    val updateProgress: Int = 0,
    val updateMessage: String? = null
)

class MainViewModel(
    private val repo: PlaylistRepository,
    private val app: Application
) : ViewModel() {

    private val updateChecker = UpdateChecker(app)
    private val _updateInfo = MutableStateFlow<UpdateChecker.UpdateInfo?>(null)
    private val _updateChecking = MutableStateFlow(false)
    private val _updateDownloading = MutableStateFlow(false)
    private val _updateProgress = MutableStateFlow(0)
    private val _updateMessage = MutableStateFlow<String?>(null)

    private val _filter = MutableStateFlow(FilterState())
    private val _proxyEnabled = MutableStateFlow(false)
    private val _proxyHost = MutableStateFlow("")
    private val _proxyPort = MutableStateFlow("8080")

    val uiState: StateFlow<UiState> = combine(
        repo.channelsWithFavorites,
        repo.isLoading,
        repo.error,
        _filter,
        _proxyEnabled,
        _updateInfo,
        _updateChecking,
        _updateDownloading,
        _updateProgress,
        _updateMessage
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val channels = values[0] as List<Channel>
        val loading = values[1] as Boolean
        val error = values[2] as String?
        val filter = values[3] as FilterState
        val proxyOn = values[4] as Boolean
        val updateInfo = values[5] as UpdateChecker.UpdateInfo?
        val updateChecking = values[6] as Boolean
        val updateDownloading = values[7] as Boolean
        val updateProgress = values[8] as Int
        val updateMessage = values[9] as String?
        val filtered = repo.filter(channels, filter)
        UiState(
            channels = channels,
            filtered = filtered,
            isLoading = loading,
            error = error,
            filter = filter,
            countries = repo.availableCountries(channels),
            categories = repo.availableCategories(channels),
            languages = repo.availableLanguages(channels),
            proxyEnabled = proxyOn,
            proxyHost = _proxyHost.value,
            proxyPort = _proxyPort.value,
            updateInfo = updateInfo,
            updateChecking = updateChecking,
            updateDownloading = updateDownloading,
            updateProgress = updateProgress,
            updateMessage = updateMessage
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState())

    fun loadDefault() {
        viewModelScope.launch { repo.loadDefaultIptvOrg() }
    }

    fun loadM3u(url: String) {
        viewModelScope.launch { repo.loadM3uFromUrl(url) }
    }

    fun loadXtream(server: String, user: String, pass: String) {
        viewModelScope.launch { repo.loadXtream(server, user, pass) }
    }

    fun setCountry(country: String?) {
        _filter.value = _filter.value.copy(country = country)
    }

    fun setCategory(cat: String?) {
        _filter.value = _filter.value.copy(category = cat)
    }

    fun setLanguage(lang: String?) {
        _filter.value = _filter.value.copy(language = lang)
    }

    fun setQuery(q: String) {
        _filter.value = _filter.value.copy(query = q)
    }

    fun toggleFavorite(channel: Channel) {
        viewModelScope.launch { repo.toggleFavorite(channel) }
    }

    fun updateProxy(enabled: Boolean, host: String, port: String) {
        _proxyEnabled.value = enabled
        _proxyHost.value = host
        _proxyPort.value = port
        val client = if (enabled && host.isNotBlank()) {
            NetworkModule.createClient(host, port.toIntOrNull() ?: 8080)
        } else {
            NetworkModule.createClient()
        }
        repo.updateHttpClient(client)
    }

    fun checkUpdate() {
        viewModelScope.launch {
            _updateChecking.value = true
            _updateMessage.value = null
            try {
                val info = updateChecker.checkForUpdate(BuildConfig.VERSION_CODE.toLong())
                _updateInfo.value = info
                if (info == null) {
                    _updateMessage.value = "Vous avez déjà la dernière version"
                }
            } catch (e: Exception) {
                _updateMessage.value = "Erreur vérification : ${e.message}"
            } finally {
                _updateChecking.value = false
            }
        }
    }

    fun downloadAndInstallUpdate() {
        val info = _updateInfo.value ?: return
        viewModelScope.launch {
            _updateDownloading.value = true
            _updateProgress.value = 0
            _updateMessage.value = "Téléchargement…"
            try {
                val file = updateChecker.downloadApk(info.apkUrl) { p ->
                    _updateProgress.value = p
                }
                if (file != null) {
                    _updateMessage.value = "Installation…"
                    updateChecker.installApk(file)
                } else {
                    _updateMessage.value = "Échec du téléchargement"
                }
            } catch (e: Exception) {
                _updateMessage.value = "Erreur : ${e.message}"
            } finally {
                _updateDownloading.value = false
            }
        }
    }

    companion object {
        fun factory(repo: PlaylistRepository, app: Application) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MainViewModel(repo, app) as T
                }
            }
    }
}
