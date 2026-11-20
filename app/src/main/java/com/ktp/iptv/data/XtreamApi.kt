package com.ktp.iptv.data

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * Client minimal pour l'API Xtream Codes (player_api.php).
 * Documentation publique : https://github.com/iptv-org (et forums Xtream)
 */
interface XtreamApi {
    @GET
    suspend fun getLiveCategories(
        @Url url: String
    ): List<XtreamCategory>

    @GET
    suspend fun getLiveStreams(
        @Url url: String
    ): List<XtreamStream>
}

data class XtreamCategory(
    @SerializedName("category_id") val categoryId: String?,
    @SerializedName("category_name") val categoryName: String?,
    @SerializedName("parent_id") val parentId: Int? = 0
)

data class XtreamStream(
    @SerializedName("num") val num: Int?,
    @SerializedName("name") val name: String?,
    @SerializedName("stream_type") val streamType: String?,
    @SerializedName("stream_id") val streamId: Int?,
    @SerializedName("stream_icon") val streamIcon: String?,
    @SerializedName("epg_channel_id") val epgChannelId: String?,
    @SerializedName("category_id") val categoryId: String?,
    @SerializedName("tv_archive") val tvArchive: Int? = 0
)

object XtreamUrlBuilder {
    fun playerApi(server: String, username: String, password: String, action: String): String {
        val base = server.trimEnd('/')
        return "$base/player_api.php?username=$username&password=$password&action=$action"
    }

    fun liveStream(server: String, username: String, password: String, streamId: Int, ext: String = "ts"): String {
        val base = server.trimEnd('/')
        return "$base/live/$username/$password/$streamId.$ext"
    }
}
