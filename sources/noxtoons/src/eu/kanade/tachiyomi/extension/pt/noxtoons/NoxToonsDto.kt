package eu.kanade.tachiyomi.extension.pt.noxtoons

import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Serializable
class ComicsDto(
    val comics: List<ComicDto> = emptyList(),
    val page: Int = 1,
    @SerialName("total_pages") val totalPages: Int = 1,
) {
    fun toMangasPage() = MangasPage(comics.map { it.toSManga() }, page < totalPages)
}

@Serializable
class ComicDto(
    val slug: String,
    val title: String,
    val synopsis: String? = null,
    val cover: String? = null,
    val type: String? = null,
    val status: String? = null,
    val genres: List<GenreDto>? = null,
) {
    fun toSManga() = SManga.create().apply {
        title = this@ComicDto.title
        url = slug
        thumbnail_url = cover
        description = synopsis
        genre = (listOfNotNull(typeLabel(type)) + genres.orEmpty().map { it.name })
            .joinToString()
            .takeIf { it.isNotBlank() }
        status = parseStatus(this@ComicDto.status)
        initialized = true
    }
}

@Serializable
class GenreDto(
    val name: String,
)

@Serializable
class ChaptersDto(
    val chapters: List<ChapterDto> = emptyList(),
)

@Serializable
class ChapterDto(
    val id: String,
    val number: Double,
    val title: String? = null,
    @SerialName("published_at") val publishedAt: String? = null,
) {
    fun toSChapter() = SChapter.create().apply {
        url = id
        val num = if (number % 1.0 == 0.0) number.toInt().toString() else number.toString()
        name = "Capítulo $num" + (title?.takeIf { it.isNotBlank() }?.let { " - $it" }.orEmpty())
        chapter_number = number.toFloat()
        date_upload = parseDate(publishedAt)
    }
}

@Serializable
class ChapterPagesDto(
    val pages: List<PageDto> = emptyList(),
)

@Serializable
class PageDto(
    @SerialName("image_url") val imageUrl: String,
)

private fun typeLabel(type: String?) = when (type?.lowercase()) {
    "manga" -> "Mangá"
    "manhwa" -> "Manhwa"
    "manhua" -> "Manhua"
    "webtoon" -> "Webtoon"
    "novel" -> "Novel"
    else -> type
}

private fun parseStatus(status: String?) = when (status?.lowercase()) {
    "ongoing", "releasing" -> SManga.ONGOING
    "completed" -> SManga.COMPLETED
    "hiatus" -> SManga.ON_HIATUS
    "cancelled", "canceled" -> SManga.CANCELLED
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
