package com.example.proyectofinal.loginapp.network

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Servicio para comunicación con la API REST
 * Maneja todas las peticiones HTTP de autenticación
 */
class ApiService {

    companion object {
        private const val TAG = "ApiService"

        // Usar configuración centralizada
        private val BASE_URL = NetworkConfig.BASE_URL

        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Realiza el login del usuario
     */
    fun login(correo: String, password: String, callback: (Result<LoginResponse>) -> Unit) {
        Log.d(TAG, "Iniciando login para: $correo")
        Log.d(TAG, "URL: $BASE_URL/auth/login")

        val json = JSONObject().apply {
            put("correo", correo)
            put("password", password)
        }

        Log.d(TAG, "JSON Request: $json")

        val body = json.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url("$BASE_URL/auth/login")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Error de conexión", e)
                Log.e(TAG, "URL intentada: ${call.request().url}")
                callback(Result.failure(Exception("Error de conexión: ${e.message}\n\nVerifica:\n1. Servidor corriendo\n2. IP correcta: $BASE_URL\n3. Misma red WiFi")))
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseBody = response.body?.string() ?: ""
                    Log.d(TAG, "Código de respuesta: ${response.code}")
                    Log.d(TAG, "Respuesta del servidor: $responseBody")

                    val jsonResponse = JSONObject(responseBody)

                    when (response.code) {
                        200 -> {
                            Log.d(TAG, "Login exitoso")
                            // Login exitoso
                            val usuarioJson = jsonResponse.getJSONObject("usuario")
                            val loginResponse = LoginResponse(
                                success = true,
                                mensaje = jsonResponse.getString("mensaje"),
                                usuario = Usuario(
                                    id = usuarioJson.getInt("id"),
                                    nombre = usuarioJson.getString("nombre"),
                                    correo = usuarioJson.getString("correo")
                                )
                            )
                            callback(Result.success(loginResponse))
                        }
                        401 -> {
                            Log.w(TAG, "Credenciales incorrectas")
                            // Credenciales incorrectas
                            val error = jsonResponse.getString("error")
                            val intentosRestantes = if (jsonResponse.has("intentos_restantes")) {
                                jsonResponse.getInt("intentos_restantes")
                            } else null

                            callback(Result.success(LoginResponse(
                                success = false,
                                mensaje = error,
                                intentosRestantes = intentosRestantes
                            )))
                        }
                        403 -> {
                            Log.w(TAG, "Usuario bloqueado")
                            // Usuario bloqueado
                            val mensaje = jsonResponse.getString("mensaje")
                            callback(Result.success(LoginResponse(
                                success = false,
                                mensaje = mensaje,
                                bloqueado = true
                            )))
                        }
                        else -> {
                            Log.e(TAG, "Error desconocido: ${response.code}")
                            val error = jsonResponse.optString("error", "Error desconocido")
                            callback(Result.failure(Exception(error)))
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error al procesar respuesta", e)
                    callback(Result.failure(Exception("Error al procesar respuesta: ${e.message}")))
                }
            }
        })
    }

    /**
     * Registra un nuevo usuario
     */
    fun registro(nombre: String, correo: String, password: String,
                 callback: (Result<RegistroResponse>) -> Unit) {
        Log.d(TAG, "Iniciando registro para: $correo")

        val json = JSONObject().apply {
            put("nombre", nombre)
            put("correo", correo)
            put("password", password)
        }

        val body = json.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url("$BASE_URL/auth/registro")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Error de conexión en registro", e)
                callback(Result.failure(Exception("Error de conexión: ${e.message}")))
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseBody = response.body?.string() ?: ""
                    Log.d(TAG, "Respuesta registro: $responseBody")
                    val jsonResponse = JSONObject(responseBody)

                    when (response.code) {
                        201 -> {
                            Log.d(TAG, "Registro exitoso")
                            val usuarioJson = jsonResponse.getJSONObject("usuario")
                            callback(Result.success(RegistroResponse(
                                success = true,
                                mensaje = jsonResponse.getString("mensaje"),
                                usuario = Usuario(
                                    id = usuarioJson.getInt("id"),
                                    nombre = usuarioJson.getString("nombre"),
                                    correo = usuarioJson.getString("correo")
                                )
                            )))
                        }
                        409 -> {
                            Log.w(TAG, "Usuario ya existe")
                            callback(Result.success(RegistroResponse(
                                success = false,
                                mensaje = jsonResponse.getString("error")
                            )))
                        }
                        else -> {
                            Log.e(TAG, "Error en registro: ${response.code}")
                            callback(Result.failure(Exception(
                                jsonResponse.optString("error", "Error desconocido")
                            )))
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error al procesar respuesta de registro", e)
                    callback(Result.failure(Exception("Error al procesar respuesta: ${e.message}")))
                }
            }
        })
    }
}

// 📦 Modelos de datos
data class Usuario(
    val id: Int,
    val nombre: String,
    val correo: String
)

data class LoginResponse(
    val success: Boolean,
    val mensaje: String,
    val usuario: Usuario? = null,
    val intentosRestantes: Int? = null,
    val bloqueado: Boolean = false
)

data class RegistroResponse(
    val success: Boolean,
    val mensaje: String,
    val usuario: Usuario? = null
)