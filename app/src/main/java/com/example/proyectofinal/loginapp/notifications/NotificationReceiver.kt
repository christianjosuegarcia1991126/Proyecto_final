package com.example.proyectofinal.loginapp.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * BroadcastReceiver que recibe las alarmas programadas
 * y muestra las notificaciones de recordatorios
 */
class NotificationReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NotificationReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "🔔 onReceive() llamado - Action: ${intent.action}")

        try {
            // Obtener datos del recordatorio
            val recordatorioId = intent.getIntExtra("recordatorio_id", -1)
            val titulo = intent.getStringExtra("titulo") ?: "Recordatorio"
            val contactoNombre = intent.getStringExtra("contacto_nombre") ?: "Sin contacto"
            val requisiciones = intent.getStringExtra("requisiciones") ?: "Sin notas"

            Log.d(TAG, "📋 Datos recibidos:")
            Log.d(TAG, "   - ID: $recordatorioId")
            Log.d(TAG, "   - Título: $titulo")
            Log.d(TAG, "   - Contacto: $contactoNombre")
            Log.d(TAG, "   - Requisiciones: $requisiciones")

            if (recordatorioId == -1) {
                Log.e(TAG, "❌ ID de recordatorio inválido")
                return
            }

            // Crear helper y mostrar notificación
            val notificationHelper = NotificationHelper(context)
            notificationHelper.mostrarNotificacion(
                recordatorioId,
                titulo,
                contactoNombre,
                requisiciones
            )

            Log.d(TAG, "✅ Notificación procesada exitosamente")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al procesar notificación", e)
        }
    }
}