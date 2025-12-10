package com.example.proyectofinal.loginapp.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.proyectofinal.R
import com.example.proyectofinal.loginapp.HomeActivity
import java.util.*

/**
 * Clase helper para gestionar notificaciones y alarmas
 */
class NotificationHelper(private val context: Context) {

    companion object {
        private const val TAG = "NotificationHelper"
        private const val CHANNEL_ID = "recordatorios_channel"
        private const val CHANNEL_NAME = "Recordatorios"
        private const val CHANNEL_DESCRIPTION = "Notificaciones de recordatorios"
    }

    init {
        crearCanalNotificacion()
    }

    /**
     * Crea el canal de notificaciones (requerido para Android 8.0+)
     */
    private fun crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

            Log.d(TAG, "Canal de notificación creado")
        }
    }

    /**
     * Programa una alarma/notificación para un recordatorio
     */
    fun scheduleNotification(
        timeInMillis: Long,
        title: String,
        contactoNombre: String,  // ✅ CAMBIO: Ahora recibe contactoNombre
        requisiciones: String,
        notificationId: Int
    ) {
        try {
            // Validar que la fecha sea futura
            val calendar = Calendar.getInstance()
            if (timeInMillis <= System.currentTimeMillis()) {
                Log.w(TAG, "La fecha/hora del recordatorio ya pasó o es inmediata")
                // Si es muy reciente (menos de 1 minuto), mostrar inmediatamente
                if (System.currentTimeMillis() - timeInMillis < 60000) {
                    mostrarNotificacion(notificationId, title, contactoNombre, requisiciones)
                }
                return
            }

            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = "com.example.proyectofinal.NOTIFICATION_ACTION"
                putExtra("recordatorio_id", notificationId)
                putExtra("titulo", title)
                putExtra("contacto_nombre", contactoNombre)  // ✅ CORREGIDO
                putExtra("requisiciones", requisiciones)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // Verificar y programar según la versión de Android
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    // Android 12+
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            timeInMillis,
                            pendingIntent
                        )
                        Log.d(TAG, "✅ Alarma EXACTA programada para: ${Date(timeInMillis)} (ID: $notificationId)")
                    } else {
                        Log.w(TAG, "⚠️ No se tienen permisos para alarmas exactas. Programando alarma aproximada...")
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            timeInMillis,
                            pendingIntent
                        )
                        Log.d(TAG, "✅ Alarma APROXIMADA programada para: ${Date(timeInMillis)} (ID: $notificationId)")
                    }
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    // Android 6.0 a 11
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        timeInMillis,
                        pendingIntent
                    )
                    Log.d(TAG, "✅ Alarma programada para: ${Date(timeInMillis)} (ID: $notificationId)")
                }
                else -> {
                    // Android 5.x o inferior
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        timeInMillis,
                        pendingIntent
                    )
                    Log.d(TAG, "✅ Alarma programada para: ${Date(timeInMillis)} (ID: $notificationId)")
                }
            }

        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Error de permisos al programar alarma", e)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al programar notificación", e)
        }
    }

    /**
     * Cancela una notificación programada
     */
    fun cancelNotification(recordatorioId: Int) {
        try {
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = "com.example.proyectofinal.NOTIFICATION_ACTION"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                recordatorioId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()

            Log.d(TAG, "✅ Notificación cancelada ID: $recordatorioId")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al cancelar notificación", e)
        }
    }

    /**
     * Muestra una notificación inmediatamente
     */
    fun mostrarNotificacion(
        recordatorioId: Int,
        titulo: String,
        contactoNombre: String,
        requisiciones: String
    ) {
        try {
            if (!tienePermisosNotificacion()) {
                Log.w(TAG, "⚠️ No se tienen permisos de notificación")
                return
            }

            val intent = Intent(context, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)  // Ícono del sistema como respaldo
                .setContentTitle("📅 $titulo")
                .setContentText("👤 $contactoNombre")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("👤 Contacto: $contactoNombre\n\n📝 Notas:\n$requisiciones")
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(longArrayOf(0, 500, 200, 500))
                .setDefaults(NotificationCompat.DEFAULT_SOUND)

            with(NotificationManagerCompat.from(context)) {
                notify(recordatorioId, builder.build())
            }

            Log.d(TAG, "✅ Notificación mostrada - ID: $recordatorioId, Título: $titulo")

        } catch (e: SecurityException) {
            Log.e(TAG, "❌ No se tienen permisos de notificación", e)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al mostrar notificación", e)
        }
    }

    /**
     * Verifica si se tienen permisos de notificación
     */
    fun tienePermisosNotificacion(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        } else {
            true
        }
    }

    /**
     * Verifica si se pueden programar alarmas exactas (Android 12+)
     */
    fun puedeUsarAlarmasExactas(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }
}