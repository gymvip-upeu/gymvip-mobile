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
import com.gymvip.mobile.di.AppContainer
import com.gymvip.mobile.telemetry.SafeCrashLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Servicio FCM para recepción segura de avisos operativos y recordatorios de membresía.
 * Cumple con VISIBILITY_PRIVATE para ocultar información sensible en la pantalla de bloqueo.
 */
class GymVipFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val CHANNEL_ID = "gymvip_member_notices"
        const val CHANNEL_NAME = "Avisos y Membresía GYM VIP"
        const val NOTIFICATION_ID_BASE = 5000

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notificaciones de vigencia de plan, horarios y comunicados del club"
                    lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
                    enableVibration(true)
                }
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.createNotificationChannel(channel)
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        SafeCrashLogger.logBreadcrumb("FCM", "Nuevo token de registro FCM generado")
        // Enviar al backend si hay sesión autenticada activa
        serviceScope.launch {
            try {
                if (AppContainer.isInitialized) {
                    val secureStore = AppContainer.secureTokenStore
                    val currentJwt = secureStore.getAccessToken()
                    if (!currentJwt.isNullOrBlank()) {
                        syncPushRegistration()
                    }
                }
            } catch (e: Throwable) {
                SafeCrashLogger.recordException(e, mapOf("stage" to "onNewToken"))
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "GYM VIP"
        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["message"]
            ?: "Tiene un nuevo comunicado del gimnasio disponible."

        showPrivateNotification(title, body)
    }

    private fun showPrivateNotification(title: String, body: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        // En pantalla bloqueada solo se expone el encabezado público sin detalles confidenciales
        val publicNotification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("GYM VIP")
            .setContentText("Nuevo aviso del club disponible")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(SafeCrashLogger.sanitize(title))
            .setContentText(SafeCrashLogger.sanitize(body))
            .setStyle(NotificationCompat.BigTextStyle().bigText(SafeCrashLogger.sanitize(body)))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicNotification)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(this).notify((NOTIFICATION_ID_BASE..NOTIFICATION_ID_BASE + 999).random(), notification)
        } catch (_: SecurityException) {
            // Permiso POST_NOTIFICATIONS denegado en Android 13+
        }
    }
}
