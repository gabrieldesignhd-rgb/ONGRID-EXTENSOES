package eu.kanade.tachiyomi.extension.pt.nexusmangas

import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Serializable
class WorkDto(
    @SerialName("slug") val id: String,
    @SerialName("title") val title: String,
    @SerialName("cover_url") val cover: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("author") val author: String? = null,
    @SerialName("artist") val artist: String? = null,
    @SerialName("status") val status: String? = null
) {
    fun toSManga() = SManga.create().apply {
        title = this@WorkDto.title
        url = id
        thumbnail_url = cover
        author = this@WorkDto.author?.takeIf { it.isNotBlank() && it != "-" }
        artist = this@WorkDto.artist?.takeIf { it.isNotBlank() && it != "-" }
        description = this@WorkDto.description
        status = parseStatus(this@WorkDto.status)
        initialized = true
    }
}

@Serializable
class WorkChaptersDto(
    @SerialName("chapters") val chapters: List<ChapterDto>? = null,
)

@Serializable
class ChapterDto(
    val id: String,
    @SerialName("number") val number: Double,
    @SerialName("title") val title: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
) {
    fun toSChapter() = SChapter.create().apply {
        url = id
        val num = if (number % 1.0 == 0.0) number.toInt().toString() else number.toString()
        name = "Capítulo $num" + (title?.takeIf { it.isNotBlank() }?.let { " - $it" }.orEmpty())
        chapter_number = number.toFloat()
        date_upload = parseDate(createdAt)
    }
}

@Serializable
class ChapterPagesDto(
    @SerialName("pages") val pages: List<String>? = null,
)

private fun parseStatus(status: String?) = when (status?.lowercase()) {
    "releasing", "ongoing", "em andamento", "andamento", "em lançamento" -> SManga.ONGOING
    "completed", "concluído", "concluido", "finalizado", "completo" -> SManga.COMPLETED
    "hiatus", "hiato", "em hiato" -> SManga.ON_HIATUS
    "cancelled", "canceled", "cancelado" -> SManga.CANCELLED
    else -> SManga.UNKNOWN
}

private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT).apply {
    timeZone = TimeZone.getTimeZone("UTC")
}

private fun parseDate(date: String?): Long {
    if (date.isNullOrBlank()) return 0L
    return try {
        dateFormat.parse(date.take(19))?.time ?: 0L
    } catch (_: Exception) {
        0L
    }
}
