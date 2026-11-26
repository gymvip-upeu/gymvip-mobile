package com.gymvip.mobile.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.gymvip.mobile.MainActivity
import kotlinx.coroutines.*

class GymVipFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { syncPushRegistration() }
    }
    override fun onMessageReceived(message: RemoteMessage) {
        // Finish local validation before the FCM callback ends; do not fetch private content here.
        runBlocking(Dispatchers.IO) { display(this@GymVipFirebaseMessagingService, message.data) }
    }
    companion object {
        const val CHANNEL_ID = "gymvip_member_ops"
        const val CHANNEL_PROMO_ID = "gymvip_member_promos"
        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(NotificationManager::class.java)
                manager.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Avisos operativos y seguridad", NotificationManager.IMPORTANCE_HIGH).apply {
                        description = "Accesos QR, vencimientos, pedidos y alertas operativas de cuenta"
                        lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
                    }
                )
                manager.createNotificationChannel(
                    NotificationChannel(CHANNEL_PROMO_ID, "Promociones y novedades (Opt-in)", NotificationManager.IMPORTANCE_DEFAULT).apply {
                        description = "Beneficios grupales y campañas comerciales autorizadas por el socio"
                        lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
                    }
                )
            }
        }
        internal suspend fun display(context: Context, data: Map<String, String>): Boolean {
            val orders = data["type"] == "GYMVIP_ORDERS"
            val promo = data["type"] == "GYMVIP_PROMO"
            if (!orders && !promo && data["type"] != "GYMVIP_INBOX") return false
            val targetChannel = if (promo) CHANNEL_PROMO_ID else CHANNEL_ID
            val account = data["accountId"] ?: return false
            val item = data["itemId"] ?: return false
            val tag = data["sessionTag"] ?: return false
            if (!PushDeviceRuntime.owns(account, tag)) return false
            if (!orders && !promo) PushNavigation.changed()
            if (orders && item != "ORDERS") return false
            if (!orders && !Regex("[0-9a-fA-F-]{36}").matches(item)) return false
            val manager = NotificationManagerCompat.from(context)
            if (!manager.areNotificationsEnabled()) return false
            createNotificationChannel(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                context.getSystemService(NotificationManager::class.java).getNotificationChannel(targetChannel)?.importance == NotificationManager.IMPORTANCE_NONE) return false
            val seen = context.getSharedPreferences("push_seen", Context.MODE_PRIVATE)
            val key = "$account:$tag:$item"
            if (seen.contains(key)) return false
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                // Distinct PendingIntent identity prevents opening the last notice for every notification.
                action = "com.gymvip.mobile.OPEN_NOTICE.$key"
                putExtra("pushAccountId", account)
                putExtra("pushItemId", item)
                putExtra("pushSessionTag", tag)
                putExtra("pushType", data["type"])
            }
            val pending = PendingIntent.getActivity(context, key.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val notification = NotificationCompat.Builder(context, targetChannel)
                .setSmallIcon(com.gymvip.mobile.R.drawable.ic_stat_notice)
                .setContentTitle(if (promo) "GYM VIP · Novedad" else "GYM VIP")
                .setContentText(
                    when {
                        orders -> "Hay una actualización en tus pedidos."
                        promo -> "Nueva promoción disponible en tu gimnasio."
                        else -> "Tienes un nuevo aviso operativo. Ábrelo en tu bandeja."
                    }
                )
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setContentIntent(pending).setAutoCancel(true).setOnlyAlertOnce(true).build()
            return synchronized(PushDeviceRuntime.displayLock) { try {
                if (!PushDeviceRuntime.matchesBinding(account, tag)) return@synchronized false
                if (seen.contains(key)) return@synchronized false
                manager.notify(key, 0, notification)
                val editor = seen.edit()
                if (seen.all.size >= 200) editor.clear()
                editor.putBoolean(key, true).commit()
                true
            } catch (_: SecurityException) { false } }
        }
    }
}
