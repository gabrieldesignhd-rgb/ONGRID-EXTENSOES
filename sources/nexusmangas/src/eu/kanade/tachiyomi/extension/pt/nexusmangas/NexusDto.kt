package eu.kanade.tachiyomi.extension.pt.nexusmangas

import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Serializable
class WorkDto(
    val title: String,
    val slug: String,
    val cover_url: String? = null,
    val description: String? = null,
    val author: String? = null,
    val artist: String? = null,
    val status: String? = null,
    val type: String? = null,
    val demographic: String? = null,
) {
    fun toSManga() = SManga.create().apply {
        title = this@WorkDto.title
        url = "/obra/$slug"
        thumbnail_url = cover_url
        author = this@WorkDto.author?.takeIf { it.isNotBlank() && it != "-" }
        artist = this@WorkDto.artist?.takeIf { it.isNotBlank() && it != "-" }
        description = this@WorkDto.description
        genre = listOfNotNull(
            type?.let { typeLabels[it] ?: it.lowercase().replaceFirstChar(Char::uppercase) },
            demographic?.takeIf { it.isNotBlank() },
        ).joinToString()
        status = parseStatus(this@WorkDto.status)
        initialized = true
    }
}

@Serializable
class WorkChaptersDto(
    val chapters: List<ChapterDto>? = null,
)

@Serializable
class ChapterDto(
    val id: String,
    val number: Double,
    val title: String? = null,
    val created_at: String? = null,
) {
    fun toSChapter() = SChapter.create().apply {
        url = id
        val num = if (number % 1.0 == 0.0) number.toInt().toString() else number.toString()
        name = "Capítulo $num" + (title?.takeIf { it.isNotBlank() }?.let { " - $it" }.orEmpty())
        chapter_number = number.toFloat()
        date_upload = parseDate(created_at)
    }
}

@Serializable
class ChapterPagesDto(
    val pages: List<String>? = null,
)

private val typeLabels = mapOf(
    "MANGA" to "Mangá",
    "MANHWA" to "Manhwa",
    "MANHUA" to "Manhua",
    "WEBTOON" to "Webtoon",
    "NOVEL" to "Novel",
)

private fun parseStatus(status: String?) = when (status?.uppercase()) {
    "RELEASING" -> SManga.ONGOING
    "COMPLETED" -> SManga.COMPLETED
    "HIATUS" -> SManga.ON_HIATUS
    "CANCELLED" -> SManga.CANCELLED
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
