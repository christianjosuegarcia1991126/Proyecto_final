package com.example.proyectofinal.loginapp.network

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiService {

    companion object {
        private const val TAG = "ApiService"
        private val BASE_URL = NetworkConfig.BASE_URL
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    // --- Métodos de la API ---

    fun login(correo: String, password: String, callback: (Result<LoginResponse>) -> Unit) {
        val request = Request.Builder()
            .url("$BASE_URL/auth/login")
            .post(JSONObject().apply {
                put("correo", correo)
                put("password", password)
            }.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        // Usar callback específico para login que SIEMPRE parsea como LoginResponse
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Error de conexión en login", e)
                callback(Result.failure(Exception("Error de conexión: ${e.message}")))
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { // Esto cierra automáticamente el response body
                    try {
                        val responseBody = response.body?.string() ?: ""
                        Log.d(TAG, "Respuesta del servidor (código ${response.code}): $responseBody")

                        if (response.isSuccessful) {
                            val loginResponse = gson.fromJson(responseBody, LoginResponse::class.java)
                            callback(Result.success(loginResponse))
                        } else {
                            // Si el servidor devuelve un error HTTP, intentar parsear como LoginResponse
                            try {
                                val loginResponse = gson.fromJson(responseBody, LoginResponse::class.java)
                                callback(Result.success(loginResponse))
                            } catch (e: Exception) {
                                Log.e(TAG, "Error al parsear respuesta de error", e)
                                callback(Result.failure(Exception("Error del servidor (${response.code})")))
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al procesar respuesta de login", e)
                        callback(Result.failure(Exception("No se pudo procesar la respuesta del servidor: ${e.message}")))
                    }
                }
            }
        })
    }

    fun obtenerContactos(usuarioId: Int, callback: (Result<List<Contacto>>) -> Unit) {
        val request = Request.Builder()
            .url("$BASE_URL/contactos?usuario_id=$usuarioId")
            .get()
            .build()
        client.newCall(request).enqueue(genericCallback(callback))
    }

    fun crearContacto(nombre: String, telefono: String, empresa: String, usuarioId: Int, callback: (Result<GenericResponse>) -> Unit) {
        val request = Request.Builder()
            .url("$BASE_URL/contactos")
            .post(JSONObject().apply {
                put("nombre", nombre)
                put("telefono", telefono)
                put("empresa", empresa)
                put("usuario_id", usuarioId)
            }.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()
        client.newCall(request).enqueue(genericCallback(callback))
    }

    fun eliminarContacto(contactoId: Int, callback: (Result<GenericResponse>) -> Unit) {
        val request = Request.Builder()
            .url("$BASE_URL/contactos/$contactoId")
            .delete()
            .build()
        client.newCall(request).enqueue(genericCallback(callback))
    }

    fun obtenerRecordatorios(usuarioId: Int, callback: (Result<List<Recordatorio>>) -> Unit) {
        val request = Request.Builder()
            .url("$BASE_URL/recordatorios?usuario_id=$usuarioId")
            .get()
            .build()
        client.newCall(request).enqueue(genericCallback(callback))
    }

    fun crearRecordatorio(nombre: String, contactoId: Int, requisiciones: String, fecha: String, hora: String, usuarioId: Int, callback: (Result<Recordatorio>) -> Unit) {
        val request = Request.Builder()
            .url("$BASE_URL/recordatorios")
            .post(JSONObject().apply {
                put("nombre", nombre)
                put("contacto_id", contactoId)
                put("requisiciones", requisiciones)
                put("fecha", fecha)
                put("hora", hora)
                put("usuario_id", usuarioId)
            }.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()
        client.newCall(request).enqueue(genericCallback(callback))
    }

    fun eliminarRecordatorio(recordatorioId: Int, callback: (Result<GenericResponse>) -> Unit) {
        val request = Request.Builder()
            .url("$BASE_URL/recordatorios/$recordatorioId")
            .delete()
            .build()
        client.newCall(request).enqueue(genericCallback(callback))
    }

    // --- Manejador genérico de respuestas ---
    private inline fun <reified T> genericCallback(crossinline callback: (Result<T>) -> Unit): Callback {
        return object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Error de conexión", e)
                callback(Result.failure(Exception("Error de conexión: ${e.message}")))
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { // ✅ ESTO CIERRA AUTOMÁTICAMENTE EL RESPONSE
                    try {
                        val responseBody = response.body?.string() ?: ""
                        Log.d(TAG, "Respuesta genérica (código ${response.code}): $responseBody")

                        if (response.isSuccessful) {
                            val typeToken = object : TypeToken<T>() {}.type
                            val successResponse = gson.fromJson<T>(responseBody, typeToken)
                            callback(Result.success(successResponse))
                        } else {
                            val errorResponse = gson.fromJson(responseBody, GenericResponse::class.java)
                            callback(Result.failure(Exception(errorResponse.error ?: errorResponse.mensaje ?: "Error desconocido (${response.code})")))
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al procesar respuesta", e)
                        callback(Result.failure(Exception("No se pudo procesar la respuesta del servidor: ${e.message}")))
                    }
                }
            }
        }
    }
}

// --- Data Classes ---
data class Usuario(val id: Int, val nombre: String, val correo: String)

data class LoginResponse(
    val success: Boolean? = null,  // Ahora es nullable
    val mensaje: String,
    val usuario: Usuario? = null,
    val intentosRestantes: Int? = null,
    val bloqueado: Boolean = false
) {
    // Propiedad computada: si existe usuario, fue exitoso
    val isSuccess: Boolean
        get() = success ?: (usuario != null)
}

data class Contacto(val id: Int, val nombre: String, val telefono: String?, val empresa: String?)

data class Recordatorio(
    val id: Int,
    val nombre: String,
    val fecha: String,
    val hora: String,
    val requisiciones: String?,
    val contacto_nombre: String
)

data class GenericResponse(val mensaje: String?, val error: String?)