package edu.ucne.soundsicoappandroid

import androidx.test.platform.app.InstrumentationRegistry
import edu.ucne.soundsicoappandroid.core.notifications.PushDeliveryStore
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class PushDeliveryStoreTest {
    @Test
    fun duplicateDeliveryIsSuppressedAfterReopeningTheDatabase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = "delivery-test-${UUID.randomUUID()}.db"
        var posts = 0
        try {
            PushDeliveryStore(context, database).use {
                assertTrue(it.postOnce("employee", "notice") { posts++ })
            }
            PushDeliveryStore(context, database).use {
                assertFalse(it.postOnce("employee", "notice") { posts++ })
                assertTrue(it.postOnce("employee", "another-notice") { posts++ })
                assertTrue(it.postOnce("another-employee", "notice") { posts++ })
            }
            assertEquals(3, posts)
        } finally {
            context.deleteDatabase(database)
        }
    }

    @Test
    fun unsuccessfulPostingDoesNotPreventARetry() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = "delivery-test-${UUID.randomUUID()}.db"
        try {
            PushDeliveryStore(context, database).use { deliveries ->
                assertThrows(IllegalStateException::class.java) {
                    deliveries.postOnce("employee", "notice") { error("Posting failed") }
                }
                assertTrue(deliveries.postOnce("employee", "notice") {})
                assertFalse(deliveries.postOnce("employee", "notice") {})
            }
        } finally {
            context.deleteDatabase(database)
        }
    }
}
