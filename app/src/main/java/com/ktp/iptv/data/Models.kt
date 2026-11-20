package com.ktp.iptv.data

import androidx.room.Entity
import androidx.room.PrimaryKey

data class Channel(
    val id: String,
    val name: String,
    val url: String,
    val logo: String? = null,
    val group: String? = null,          // catégorie
    val country: String? = null,
    val language: String? = null,
    val tvgId: String? = null,
    val isFavorite: Boolean = false
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val channelId: String,
    val name: String,
    val url: String,
    val logo: String?,
    val group: String?,
    val country: String?,
    val language: String?,
    val addedAt: Long = System.currentTimeMillis()
)

data class PlaylistSource(
    val id: String,
    val name: String,
    val type: SourceType,
    val url: String? = null,            // M3U
    val server: String? = null,         // Xtream
    val username: String? = null,
    val password: String? = null
)

enum class SourceType { M3U, XTREAM }

data class FilterState(
    val country: String? = null,
    val category: String? = null,
    val language: String? = null,
    val query: String = ""
)

/** Endpoints publics iptv-org (analysés depuis https://github.com/iptv-org/iptv) */
object IptvOrg {
    const val INDEX = "https://iptv-org.github.io/iptv/index.m3u"
    const val BY_COUNTRY = "https://iptv-org.github.io/iptv/index.country.m3u"
    const val BY_CATEGORY = "https://iptv-org.github.io/iptv/index.category.m3u"
    const val BY_LANGUAGE = "https://iptv-org.github.io/iptv/index.language.m3u"

    fun country(code: String) = "https://iptv-org.github.io/iptv/countries/${code.lowercase()}.m3u"
    fun category(name: String) = "https://iptv-org.github.io/iptv/categories/${name.lowercase()}.m3u"
    fun language(code: String) = "https://iptv-org.github.io/iptv/languages/${code.lowercase()}.m3u"
}
