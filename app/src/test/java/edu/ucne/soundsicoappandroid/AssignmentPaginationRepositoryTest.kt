package edu.ucne.soundsicoappandroid

import edu.ucne.soundsicoappandroid.data.repository.SupabaseHomeRepository
import edu.ucne.soundsicoappandroid.domain.model.*
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class AssignmentPaginationRepositoryTest {
    @Test
    fun sendsSearchFilterAndOffsetToTheRightServerFunctionForEachRole() = runTest {
        val requests = mutableListOf<Pair<String, JsonObject>>()
        val transport = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            val body = Buffer().also { requireNotNull(request.body).writeTo(it) }.readUtf8()
            requests += request.url.encodedPath to Json.parseToJsonElement(body).jsonObject
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .header("Content-Type", "application/json")
                .body("""{"items":[],"has_more":false,"next_offset":null}""".toResponseBody()).build()
        }.build()
        val client = createSupabaseClient("https://soundisco.test", "public-test-key") {
            install(Postgrest)
            osInformation = null
            httpEngine = OkHttp.create { preconfigured = transport }
        }
        try {
            val repository = SupabaseHomeRepository(client)
            for (admin in listOf(false, true)) {
                for (filter in AssignmentFeedFilter.entries.filterNot { it == AssignmentFeedFilter.All }) {
                    repository.loadAssignmentPage("viewer", admin, 10, filter, "hotel", null)
                    val (path, params) = requests.last()
                    assertEquals("/rest/v1/rpc/" + if (admin) "admin_assignments_page" else "assignments_feed_page", path)
                    assertEquals(10, params.getValue("p_limit").jsonPrimitive.int)
                    assertEquals(10, params.getValue("p_offset").jsonPrimitive.int)
                    assertEquals(if (admin) filter.adminValue else filter.feedValue,
                        params.getValue(if (admin) "p_estado" else "p_status").jsonPrimitive.content)
                    assertEquals("hotel", params.getValue(if (admin) "p_busqueda" else "p_search").jsonPrimitive.content)
                    assertFalse(params.containsKey("p_day_start"))
                }
                val day = AssignmentDay.from(LocalDate.of(2026, 11, 5), ZoneId.of("America/Santo_Domingo"))
                repository.loadAssignmentPage("viewer", admin, 0, AssignmentFeedFilter.All, "", day)
                val (path, params) = requests.last()
                assertEquals("/rest/v1/rpc/assignments_feed_page", path)
                assertEquals("all", params.getValue("p_status").jsonPrimitive.content)
                assertEquals("2026-11-05T04:00:00Z", params.getValue("p_day_start").jsonPrimitive.content)
                assertEquals("2026-11-06T04:00:00Z", params.getValue("p_day_end").jsonPrimitive.content)
                assertEquals(10, params.getValue("p_limit").jsonPrimitive.int)
            }
            assertEquals(8, requests.size)
        } finally {
            client.close()
        }
    }
}
