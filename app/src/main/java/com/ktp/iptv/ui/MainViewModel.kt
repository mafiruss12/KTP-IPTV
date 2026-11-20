package com.ktp.iptv.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ktp.iptv.data.Channel
import com.ktp.iptv.data.FilterState
import com.ktp.iptv.data.NetworkModule
import com.ktp.iptv.data.PlaylistRepository
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
    val proxyPort: String = "8080"
)

class MainViewModel(
    private val repo: PlaylistRepository
) : ViewModel() {

    private val _filter = MutableStateFlow(FilterState())
    private val _proxyEnabled = MutableStateFlow(false)
    private val _proxyHost = MutableStateFlow("")
    private val _proxyPort = MutableStateFlow("8080")

    val uiState: StateFlow<UiState> = combine(
        repo.channelsWithFavorites,
        repo.isLoading,
        repo.error,
        _filter,
        _proxyEnabled
    ) { channels, loading, error, filter, proxyOn ->
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
            proxyPort = _proxyPort.value
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

    companion object {
        fun factory(repo: PlaylistRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(repo) as T
            }
        }
    }
}
