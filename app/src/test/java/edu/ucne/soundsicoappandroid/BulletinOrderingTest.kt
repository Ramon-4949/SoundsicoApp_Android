package edu.ucne.soundsicoappandroid

import edu.ucne.soundsicoappandroid.data.repository.SupabaseHomeRepository
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test

class BulletinOrderingTest {
    @Test
    fun homeQueriesMessagesByPublicationDateWithStablePaginationOrder() = runTest {
        var messageOrder: String? = null
        val transport = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            val response = when (request.url.encodedPath) {
                "/rest/v1/comunicados" -> {
                    messageOrder = request.url.queryParameter("order")
                    """[
                        {"id":"a-new","asunto":"Último publicado","mensaje":"Aviso nuevo","fecha_publicacion":"2026-10-12T12:00:00-04:00"},
                        {"id":"z-old","asunto":"Anterior","mensaje":"Aviso anterior","fecha_publicacion":"2026-10-12T15:59:00Z"}
                    ]"""
                }
                else -> error("Unexpected request")
            }
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .header("Content-Type", "application/json")
                .body(response.toResponseBody()).build()
        }.build()
        val client = createSupabaseClient("https://soundisco.test", "public-test-key") {
            install(Postgrest)
            osInformation = null
            httpEngine = OkHttp.create { preconfigured = transport }
        }
        try {
            val content = SupabaseHomeRepository(client).load("employee", false)
            assertEquals("fecha_publicacion.desc.nullslast,id.desc.nullslast", messageOrder)
            assertEquals(listOf("a-new", "z-old"), content.bulletins.map { it.id })
            assertEquals("2026-10-12T12:00:00-04:00", content.bulletins.first().publishedAt)
        } finally {
            client.close()
        }
    }
}
