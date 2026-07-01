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
 * O nexusmangas.com fica atrás do Cloudflare, mas os dados vêm de um backend
 * Supabase (PostgREST) que não passa pelo Cloudflare. A extensão fala direto
 * com o Supabase usando a chave pública (anon) embutida no site.
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
        .add("Referer", "$baseUrl/")

    // =====================Popular=====================

    override fun popularMangaRequest(page: Int): Request = worksRequest(page, "order=rating_count.desc.nullslast")

    override fun popularMangaParse(response: Response): MangasPage = worksParse(response)

    // =====================Latest=====================

    override fun latestUpdatesRequest(page: Int): Request = worksRequest(page, "order=updated_at.desc.nullslast")

    override fun latestUpdatesParse(response: Response): MangasPage = worksParse(response)

    // =====================Search=====================

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        if (query.isBlank()) return latestUpdatesRequest(page)
        val term = URLEncoder.encode("%$query%", "UTF-8")
        return worksRequest(page, "title=ilike.$term&order=rating_count.desc.nullslast")
    }

    override fun searchMangaParse(response: Response): MangasPage = worksParse(response)

    private fun worksRequest(page: Int, filter: String): Request {
        val offset = (page - 1) * PAGE_SIZE
        val url = "$API_URL/works?select=$WORK_LIST_FIELDS&$filter&limit=$PAGE_SIZE&offset=$offset"
        return GET(url, headers)
    }

    private fun worksParse(response: Response): MangasPage {
        val works = response.parseAs<List<WorkDto>>()
        val entries = works.map { it.toSManga() }
        return MangasPage(entries, entries.size == PAGE_SIZE)
    }

    // =====================Details=====================

    private fun SManga.slug(): String = url.substringAfterLast("/")

    override fun getMangaUrl(manga: SManga): String = "$baseUrl${manga.url}"

    override fun mangaDetailsRequest(manga: SManga): Request = GET("$API_URL/works?slug=eq.${manga.slug()}&select=*", headers)

    override fun mangaDetailsParse(response: Response): SManga = response.parseAs<List<WorkDto>>().first().toSManga()

    // =====================Chapters=====================

    override fun chapterListRequest(manga: SManga): Request {
        val select = "slug,chapters(id,number,title,created_at)" +
            "&chapters.order=number.desc&chapters.limit=5000"
        return GET("$API_URL/works?slug=eq.${manga.slug()}&select=$select", headers)
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        val work = response.parseAs<List<WorkChaptersDto>>().firstOrNull() ?: return emptyList()
        return work.chapters.orEmpty().map { it.toSChapter() }
    }

    // =====================Pages=====================

    override fun pageListRequest(chapter: SChapter): Request = GET("$API_URL/chapters?id=eq.${chapter.url}&select=pages", headers)

    override fun pageListParse(response: Response): List<Page> {
        val chapter = response.parseAs<List<ChapterPagesDto>>().firstOrNull()
        return chapter?.pages.orEmpty().mapIndexed { index, imageUrl ->
            Page(index, imageUrl = imageUrl)
        }
    }

    override fun imageUrlParse(response: Response): String = throw UnsupportedOperationException()

    companion object {
        private const val API_URL = "https://odjelfwddlpwuvviltqy.supabase.co/rest/v1"
        private const val API_KEY =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im9k" +
                "amVsZndkZGxwd3V2dmlsdHF5Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjU0MDU0NzQsImV4" +
                "cCI6MjA4MDk4MTQ3NH0.hv7jX5tB0Og2O8S03krE7Rj7BrUSSRLpGYta1doYGXc"
        private const val PAGE_SIZE = 24
        private const val WORK_LIST_FIELDS = "title,slug,cover_url,author,artist,status,type,demographic"
    }
}
