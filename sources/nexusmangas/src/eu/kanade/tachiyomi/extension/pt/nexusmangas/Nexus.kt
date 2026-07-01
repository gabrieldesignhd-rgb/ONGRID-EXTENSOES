package eu.kanade.tachiyomi.extension.pt.nexusmangas

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import keiyoushi.annotation.Source
import keiyoushi.network.rateLimit
import keiyoushi.utils.parseAs
import okhttp3.Request
import okhttp3.Response
import java.net.URLEncoder

/**
 * Gerado pelo Mihon Source Studio. O site tem um backend Supabase (PostgREST)
 * acessível com a chave pública (anon), então a extensão fala direto com ele,
 * ignorando qualquer proteção (Cloudflare) que exista na frente do site.
 */
@Source
abstract class Nexus : HttpSource() {

    override val supportsLatest = true

    override val client = network.client.newBuilder()
        .rateLimit(3)
        .build()

    override fun headersBuilder() = super.headersBuilder()
        .add("apikey", API_KEY)
        .add("Authorization", "Bearer $API_KEY")

    override fun popularMangaRequest(page: Int): Request = worksRequest(page, "order=updated_at.desc.nullslast")

    override fun popularMangaParse(response: Response): MangasPage = worksParse(response)

    override fun latestUpdatesRequest(page: Int): Request = worksRequest(page, "order=updated_at.desc.nullslast")

    override fun latestUpdatesParse(response: Response): MangasPage = worksParse(response)

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        if (query.isBlank()) return popularMangaRequest(page)
        val term = URLEncoder.encode("%$query%", "UTF-8")
        return worksRequest(page, "title=ilike.$term")
    }

    override fun searchMangaParse(response: Response): MangasPage = worksParse(response)

    private fun worksRequest(page: Int, filter: String): Request {
        val offset = (page - 1) * PAGE_SIZE
        val query = if (filter.isEmpty()) "" else "&$filter"
        return GET("$API_URL/works?select=$WORK_FIELDS$query&limit=$PAGE_SIZE&offset=$offset", headers)
    }

    private fun worksParse(response: Response): MangasPage {
        val entries = response.parseAs<List<WorkDto>>().map { it.toSManga() }
        return MangasPage(entries, entries.size == PAGE_SIZE)
    }

    override fun getMangaUrl(manga: SManga): String = baseUrl

    override fun mangaDetailsRequest(manga: SManga): Request = GET("$API_URL/works?slug=eq.${manga.url}&select=*", headers)

    override fun mangaDetailsParse(response: Response): SManga = response.parseAs<List<WorkDto>>().first().toSManga()

    override fun chapterListRequest(manga: SManga): Request = GET("$API_URL/works?slug=eq.${manga.url}&select=$CHAPTER_SELECT", headers)

    override fun chapterListParse(response: Response): List<SChapter> {
        val work = response.parseAs<List<WorkChaptersDto>>().firstOrNull() ?: return emptyList()
        return work.chapters.orEmpty().map { it.toSChapter() }
    }

    override fun pageListRequest(chapter: SChapter): Request = GET("$API_URL/chapters?id=eq.${chapter.url}&select=pages", headers)

    override fun pageListParse(response: Response): List<Page> {
        val chapter = response.parseAs<List<ChapterPagesDto>>().firstOrNull()
        return chapter?.pages.orEmpty().mapIndexed { index, imageUrl -> Page(index, imageUrl = imageUrl) }
    }

    override fun imageUrlParse(response: Response): String = throw UnsupportedOperationException()

    companion object {
        private const val API_URL = "https://odjelfwddlpwuvviltqy.supabase.co/rest/v1"
        private const val API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im9" +
            "kamVsZndkZGxwd3V2dmlsdHF5Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjU0MDU0NzQsImV" +
            "4cCI6MjA4MDk4MTQ3NH0.hv7jX5tB0Og2O8S03krE7Rj7BrUSSRLpGYta1doYGXc"
        private const val PAGE_SIZE = 24
        private const val WORK_FIELDS = "slug,title,cover_url,author,artist,status"
        private const val CHAPTER_SELECT = "slug,chapters(id,number,title,created_at)&chapters.order=number.desc&chapters.limit=5000"
    }
}
