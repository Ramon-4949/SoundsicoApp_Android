package edu.ucne.soundsicoappandroid.core.notifications

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class PushDeliveryStore(
    context: Context,
    databaseName: String = "soundisco_push_deliveries.db"
) : SQLiteOpenHelper(context.applicationContext, databaseName, null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE delivered (recipient_id TEXT NOT NULL, notification_id TEXT NOT NULL, PRIMARY KEY (recipient_id, notification_id))")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun postOnce(recipientId: String, notificationId: String, post: () -> Unit): Boolean = synchronized(deliveryLock) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val values = ContentValues().apply {
                put("recipient_id", recipientId)
                put("notification_id", notificationId)
            }
            if (db.insertWithOnConflict("delivered", null, values, SQLiteDatabase.CONFLICT_IGNORE) == -1L) {
                return@synchronized false
            }
            post()
            db.setTransactionSuccessful()
            true
        } finally {
            db.endTransaction()
        }
    }

    companion object {
        private val deliveryLock = Any()
    }
}
