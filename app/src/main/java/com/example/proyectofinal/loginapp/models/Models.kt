package com.example.proyectofinal.loginapp.models

import java.io.Serializable

/**
 * Modelo de Contacto
 */
data class Contacto(
    val id: Int,
    val nombre: String,
    val empresa: String?,
    val fecha_creacion: String
) : Serializable

/**
 * Modelo de Recordatorio
 */
data class Recordatorio(
    val id: Int,
    val nombre: String,
    val contacto_id: Int,
    val contacto_nombre: String?,
    val contacto_empresa: String?,
    val requisiciones: String?,
    val fecha: String,  // DD/MM/YYYY
    val hora: String,   // HH:MM
    val notificado: Boolean,
    val fecha_creacion: String
) : Serializable

/**
 * Request para crear contacto
 */
data class ContactoRequest(
    val nombre: String,
    val telefono: String,
    val empresa: String?,
    val usuario_id: Int
)

/**
 * Request para crear recordatorio
 */
data class RecordatorioRequest(
    val nombre: String,
    val contacto_id: Int,
    val requisiciones: String?,
    val fecha: String,  // DD/MM/YYYY
    val hora: String,   // HH:MM
    val usuario_id: Int
)

/**
 * Response genérico
 */
data class ApiResponse<T>(
    val mensaje: String?,
    val error: String?,
    val data: T?
)