package com.ktp.iptv.data

import java.util.UUID

/**
 * Parser M3U robuste pour les playlists iptv-org et classiques.
 * Gère les attributs : tvg-id, tvg-name, tvg-logo, tvg-country, tvg-language, group-title
 */
object M3uParser {

    private val attrRegex = Regex("""([\w-]+)="([^"]*)"""")

    fun parse(content: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        val lines = content.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()

        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            if (line.startsWith("#EXTINF", ignoreCase = true)) {
                val info = line.substringAfter(":", missingDelimiterValue = line)
                val attrs = parseAttributes(line)
                val name = info.substringAfterLast(",").trim().ifBlank {
                    attrs["tvg-name"] ?: "Chaîne ${channels.size + 1}"
                }
                // URL est généralement la ligne suivante (non commentée)
                var url: String? = null
                var j = i + 1
                while (j < lines.size) {
                    val candidate = lines[j]
                    if (!candidate.startsWith("#")) {
                        url = candidate
                        break
                    }
                    j++
                }
                if (url != null && (url.startsWith("http") || url.startsWith("rtsp") || url.startsWith("udp"))) {
                    val id = attrs["tvg-id"]?.takeIf { it.isNotBlank() }
                        ?: UUID.nameUUIDFromBytes((name + url).toByteArray()).toString()
                    channels += Channel(
                        id = id,
                        name = name,
                        url = url,
                        logo = attrs["tvg-logo"],
                        group = attrs["group-title"],
                        country = attrs["tvg-country"] ?: attrs["country"],
                        language = attrs["tvg-language"] ?: attrs["language"],
                        tvgId = attrs["tvg-id"]
                    )
                }
                i = j
            }
            i++
        }
        return channels
    }

    private fun parseAttributes(extinf: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        attrRegex.findAll(extinf).forEach { match ->
            map[match.groupValues[1].lowercase()] = match.groupValues[2]
        }
        return map
    }
}
