package edu.ucne.soundsicoappandroid.core.notifications

import android.app.NotificationManager
import android.content.Context
import androidx.work.*
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import edu.ucne.soundsicoappandroid.core.network.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID

class PushRegistration(private val context: Context) {
    private val preferences = context.getSharedPreferences("soundisco_push", Context.MODE_PRIVATE)
    val configured: Boolean get() = FirebaseApp.getApps(context).isNotEmpty()
    val failure: String? get() = preferences.getString("failure", null)

    fun installation(): String = preferences.getString("installation", null) ?: UUID.randomUUID().toString().also {
        preferences.edit().putString("installation", it).commit()
    }

    fun connect(userId: String) {
        preferences.edit().putString("owner", userId).commit()
        if (configured) enqueue(context)
    }

    suspend fun disconnect() = registrationLock.withLock {
        WorkManager.getInstance(context).cancelUniqueWork("push-registration")
        if (preferences.getBoolean("registered", false)) {
            SupabaseProvider.client.postgrest.rpc("unregister_android_push_device", buildJsonObject {
                put("p_installation", installation())
            })
        }
        clear()
    }

    fun clear() {
        preferences.edit().remove("owner").remove("failure").putBoolean("registered", false).commit()
        context.getSystemService(NotificationManager::class.java).cancelAll()
    }

    companion object {
        val registrationLock = Mutex()
        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<PushRegistrationWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork("push-registration", ExistingWorkPolicy.REPLACE, request)
        }
    }
}

class PushRegistrationWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = PushRegistration.registrationLock.withLock {
        val client = SupabaseProvider.client
        val preferences = applicationContext.getSharedPreferences("soundisco_push", Context.MODE_PRIVATE)
        try {
            client.auth.awaitInitialization()
            val userId = client.auth.currentUserOrNull()?.id ?: return@withLock Result.success()
            if (preferences.getString("owner", null) != userId) return@withLock Result.success()
            if (FirebaseApp.getApps(applicationContext).isEmpty()) return@withLock Result.success()
            val token = FirebaseMessaging.getInstance().token.await()
            if (preferences.getString("owner", null) != userId || client.auth.currentUserOrNull()?.id != userId) return@withLock Result.success()
            client.postgrest.rpc("register_android_push_device", buildJsonObject {
                put("p_installation", PushRegistration(applicationContext).installation())
                put("p_token", token)
            })
            preferences.edit().putBoolean("registered", true).remove("failure").commit()
            Result.success()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            preferences.edit().putString("failure", "No se pudo registrar el dispositivo para avisos. Se reintentará al conectar.").commit()
            if (runAttemptCount < 8) Result.retry() else Result.failure()
        }
    }
}
