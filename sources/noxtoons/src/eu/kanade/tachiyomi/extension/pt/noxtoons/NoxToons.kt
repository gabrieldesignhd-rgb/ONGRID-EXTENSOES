package eu.kanade.tachiyomi.extension.pt.noxtoons

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
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.net.URLEncoder
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * NoxToons roda na plataforma ReMangas. As páginas ficam atrás do Cloudflare,
 * mas os dados vêm de uma API REST (xodneo.site) que assina cada requisição com
 * HMAC-SHA256. A chave e o site-id estão embutidos no site (JS público), então a
 * extensão replica a assinatura e fala direto com a API, ignorando o Cloudflare.
 */
@Source
abstract class NoxToons : HttpSource() {

    override val supportsLatest = true

    override val client = network.client.newBuilder()
        .addInterceptor(SignatureInterceptor())
        .rateLimit(3)
        .build()

    // =====================Popular=====================

    override fun popularMangaRequest(page: Int): Request = comicsRequest(page, "sort=popular")

    override fun popularMangaParse(response: Response): MangasPage = response.parseAs<ComicsDto>().toMangasPage()

    // =====================Latest=====================

    override fun latestUpdatesRequest(page: Int): Request = comicsRequest(page, "sort=latest")

    override fun latestUpdatesParse(response: Response): MangasPage = response.parseAs<ComicsDto>().toMangasPage()

    // =====================Search=====================

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        if (query.isBlank()) return popularMangaRequest(page)
        return comicsRequest(page, "search=" + URLEncoder.encode(query, "UTF-8"))
    }

    override fun searchMangaParse(response: Response): MangasPage = response.parseAs<ComicsDto>().toMangasPage()

    private fun comicsRequest(page: Int, filter: String): Request = GET("$API_URL/comics?$filter&page=$page&per_page=$PER_PAGE", headers)

    // =====================Details=====================

    override fun getMangaUrl(manga: SManga): String = "$baseUrl/manga/${manga.url}"

    override fun mangaDetailsRequest(manga: SManga): Request = GET("$API_URL/comics/slug/${manga.url}", headers)

    override fun mangaDetailsParse(response: Response): SManga = response.parseAs<ComicDto>().toSManga()

    // =====================Chapters=====================

    override fun chapterListRequest(manga: SManga): Request = GET("$API_URL/comics/slug/${manga.url}/chapters?page=1&per_page=2000", headers)

    override fun chapterListParse(response: Response): List<SChapter> = response.parseAs<ChaptersDto>().chapters.map { it.toSChapter() }

    // =====================Pages=====================

    override fun pageListRequest(chapter: SChapter): Request = GET("$API_URL/chapters/${chapter.url}", headers)

    override fun pageListParse(response: Response): List<Page> = response.parseAs<ChapterPagesDto>().pages.mapIndexed { index, page ->
        Page(index, imageUrl = page.imageUrl)
    }

    override fun imageUrlParse(response: Response): String = throw UnsupportedOperationException()

    /** Assina as requisições à API (xodneo.site) com HMAC-SHA256, como o site faz. */
    private class SignatureInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            if (request.url.host != API_HOST) return chain.proceed(request)

            val timestamp = (System.currentTimeMillis() / 1000).toString()
            val message = timestamp + request.method + request.url.encodedPath
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(SECRET.toByteArray(), "HmacSHA256"))
            val signature = mac.doFinal(message.toByteArray()).joinToString("") { "%02x".format(it) }

            val signed = request.newBuilder()
                .header("X-Signature", signature)
                .header("X-Timestamp", timestamp)
                .header("X-Site-ID", SITE_ID)
                .build()
            return chain.proceed(signed)
        }
    }

    companion object {
        private const val API_HOST = "xodneo.site"
        private const val API_URL = "https://xodneo.site/api/v1"
        private const val SECRET = "fe7f9e20851be60eb720015918784c68b4216fb05eb8ca4f20bec58ef2d3fffb"
        private const val SITE_ID = "00000000-0000-0000-0000-000000000003"
        private const val PER_PAGE = 24
    }
}
