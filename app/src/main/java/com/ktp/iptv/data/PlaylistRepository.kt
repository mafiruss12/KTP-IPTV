package com.ktp.iptv.data

import com.ktp.iptv.data.db.FavoriteDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class PlaylistRepository(
    private val favoriteDao: FavoriteDao,
    private var httpClient: OkHttpClient = NetworkModule.createClient()
) {
    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    val channels: StateFlow<List<Channel>> = _channels.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val favorites: Flow<List<FavoriteEntity>> = favoriteDao.getAll()

    val channelsWithFavorites: Flow<List<Channel>> = combine(
        channels,
        favoriteDao.getAllIds()
    ) { list, favIds ->
        val set = favIds.toSet()
        list.map { it.copy(isFavorite = it.id in set) }
    }

    fun updateHttpClient(client: OkHttpClient) {
        httpClient = client
    }

    suspend fun loadM3uFromUrl(url: String) = withContext(Dispatchers.IO) {
        _isLoading.value = true
        _error.value = null
        try {
            val request = Request.Builder().url(url).build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("HTTP ${response.code}: ${response.message}")
                }
                val body = response.body?.string()
                    ?: throw Exception("Réponse vide")
                val parsed = M3uParser.parse(body)
                _channels.value = parsed
            }
        } catch (e: Exception) {
            _error.value = e.message ?: "Erreur inconnue"
            _channels.value = emptyList()
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun loadDefaultIptvOrg() {
        loadM3uFromUrl(IptvOrg.INDEX)
    }

    suspend fun loadXtream(server: String, username: String, password: String) =
        withContext(Dispatchers.IO) {
            _isLoading.value = true
            _error.value = null
            try {
                val retrofit = Retrofit.Builder()
                    .baseUrl(if (server.endsWith("/")) server else "$server/")
                    .client(httpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                val api = retrofit.create(XtreamApi::class.java)

                val catsUrl = XtreamUrlBuilder.playerApi(server, username, password, "get_live_categories")
                val streamsUrl = XtreamUrlBuilder.playerApi(server, username, password, "get_live_streams")

                val categories = api.getLiveCategories(catsUrl).associateBy { it.categoryId }
                val streams = api.getLiveStreams(streamsUrl)

                val list = streams.mapNotNull { s ->
                    val id = s.streamId ?: return@mapNotNull null
                    val name = s.name ?: "Stream $id"
                    val url = XtreamUrlBuilder.liveStream(server, username, password, id)
                    Channel(
                        id = "xtream_$id",
                        name = name,
                        url = url,
                        logo = s.streamIcon,
                        group = categories[s.categoryId]?.categoryName,
                        tvgId = s.epgChannelId
                    )
                }
                _channels.value = list
            } catch (e: Exception) {
                _error.value = e.message ?: "Erreur Xtream"
                _channels.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }

    suspend fun toggleFavorite(channel: Channel) {
        if (favoriteDao.isFavorite(channel.id)) {
            favoriteDao.delete(channel.id)
        } else {
            favoriteDao.insert(
                FavoriteEntity(
                    channelId = channel.id,
                    name = channel.name,
                    url = channel.url,
                    logo = channel.logo,
                    group = channel.group,
                    country = channel.country,
                    language = channel.language
                )
            )
        }
    }

    fun filter(channels: List<Channel>, state: FilterState): List<Channel> {
        return channels.filter { ch ->
            (state.country == null || ch.country.equals(state.country, ignoreCase = true)) &&
            (state.category == null || ch.group.equals(state.category, ignoreCase = true)) &&
            (state.language == null || ch.language.equals(state.language, ignoreCase = true)) &&
            (state.query.isBlank() || ch.name.contains(state.query, ignoreCase = true) ||
                    (ch.group?.contains(state.query, ignoreCase = true) == true))
        }
    }

    fun availableCountries(channels: List<Channel>): List<String> =
        channels.mapNotNull { it.country }.distinct().sorted()

    fun availableCategories(channels: List<Channel>): List<String> =
        channels.mapNotNull { it.group }.distinct().sorted()

    fun availableLanguages(channels: List<Channel>): List<String> =
        channels.mapNotNull { it.language }.distinct().sorted()
}
