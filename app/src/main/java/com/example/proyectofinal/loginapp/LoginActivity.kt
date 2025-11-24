package com.example.proyectofinal.loginapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.proyectofinal.R
import com.example.proyectofinal.databinding.ActivityLoginBinding
import com.example.proyectofinal.loginapp.network.ApiService
import com.example.proyectofinal.loginapp.network.LoginResponse

/**
 * Actividad principal de Login
 * Maneja la autenticación de usuarios con validaciones completas
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val apiService = ApiService()

    companion object {
        private const val TAG = "LoginActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = ActivityLoginBinding.inflate(layoutInflater)
            setContentView(binding.root)

            Log.d(TAG, "LoginActivity iniciada correctamente")
            setupListeners()

        } catch (e: Exception) {
            Log.e(TAG, "Error al inicializar LoginActivity", e)
            Toast.makeText(this, "Error al iniciar: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupListeners() {
        // Botón de login
        binding.btnLogin.setOnClickListener {
            Log.d(TAG, "Botón login presionado")
            if (validarCampos()) {
                realizarLogin()
            }
        }

        // Botón de registro (opcional)
        binding.tvRegistro.setOnClickListener {
            Toast.makeText(this, R.string.registro_proximamente, Toast.LENGTH_SHORT).show()
        }

        // Limpiar error al escribir
        binding.etCorreo.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.tilCorreo.error = null
            }
        }

        binding.etPassword.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.tilPassword.error = null
            }
        }
    }

    /**
     * Valida el formato del correo y que la contraseña no esté vacía
     */
    private fun validarCampos(): Boolean {
        val correo = binding.etCorreo.text.toString().trim()
        val password = binding.etPassword.text.toString()

        var isValid = true

        // Validar correo
        if (correo.isEmpty()) {
            binding.tilCorreo.error = getString(R.string.error_correo_vacio)
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            binding.tilCorreo.error = getString(R.string.error_correo_invalido)
            isValid = false
        } else {
            binding.tilCorreo.error = null
        }

        // Validar contraseña
        if (password.isEmpty()) {
            binding.tilPassword.error = getString(R.string.error_password_vacio)
            isValid = false
        } else if (password.length < 6) {
            binding.tilPassword.error = getString(R.string.error_password_corto)
            isValid = false
        } else {
            binding.tilPassword.error = null
        }

        return isValid
    }

    /**
     * Realiza el proceso de login
     */
    private fun realizarLogin() {
        val correo = binding.etCorreo.text.toString().trim()
        val password = binding.etPassword.text.toString()

        Log.d(TAG, "Intentando login con correo: $correo")

        // Mostrar loading
        mostrarLoading(true)

        try {
            apiService.login(correo, password) { result ->
                // Ejecutar en el hilo principal
                runOnUiThread {
                    mostrarLoading(false)

                    result.onSuccess { response ->
                        Log.d(TAG, "Respuesta recibida - Success: ${response.success}")

                        if (response.success) {
                            // Login exitoso
                            val nombreUsuario = response.usuario?.nombre ?: getString(R.string.usuario_default)

                            Toast.makeText(
                                this,
                                getString(R.string.bienvenido, nombreUsuario),
                                Toast.LENGTH_SHORT
                            ).show()

                            // Navegar a la siguiente pantalla
                            val intent = Intent(this, HomeActivity::class.java)
                            intent.putExtra("NOMBRE_USUARIO", nombreUsuario)
                            intent.putExtra("CORREO_USUARIO", response.usuario?.correo ?: "")
                            startActivity(intent)
                            finish()
                        } else {
                            // Login fallido
                            Log.w(TAG, "Login fallido: ${response.mensaje}")
                            manejarErrorLogin(response)
                        }
                    }.onFailure { error ->
                        Log.e(TAG, "Error en login", error)
                        Toast.makeText(
                            this,
                            getString(R.string.error_conexion, error.message),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción al realizar login", e)
            mostrarLoading(false)
            Toast.makeText(
                this,
                getString(R.string.error_conexion, e.message),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Maneja los diferentes tipos de error en el login
     */
    private fun manejarErrorLogin(response: LoginResponse) {
        when {
            response.bloqueado -> {
                // Usuario bloqueado
                val mensajeCompleto = getString(R.string.usuario_bloqueado, response.mensaje)

                Toast.makeText(
                    this,
                    response.mensaje,
                    Toast.LENGTH_LONG
                ).show()

                // Deshabilitar campos temporalmente
                binding.btnLogin.isEnabled = false
                binding.etCorreo.isEnabled = false
                binding.etPassword.isEnabled = false

                binding.tvMensajeError.visibility = View.VISIBLE
                binding.tvMensajeError.text = mensajeCompleto
            }
            response.mensaje.contains("Correo inválido", ignoreCase = true) -> {
                // Correo no existe
                binding.tilCorreo.error = getString(R.string.error_correo_no_registrado)
                Toast.makeText(this, R.string.correo_invalido, Toast.LENGTH_SHORT).show()
            }
            response.intentosRestantes != null -> {
                // Contraseña incorrecta con intentos restantes
                binding.tilPassword.error = getString(R.string.error_password_incorrecta)

                val mensaje = if (response.intentosRestantes > 0) {
                    getString(R.string.intentos_restantes, response.intentosRestantes)
                } else {
                    response.mensaje
                }

                binding.tvMensajeError.visibility = View.VISIBLE
                binding.tvMensajeError.text = mensaje

                Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(this, response.mensaje, Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Muestra u oculta el indicador de carga
     */
    private fun mostrarLoading(mostrar: Boolean) {
        binding.progressBar.visibility = if (mostrar) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !mostrar
        binding.etCorreo.isEnabled = !mostrar
        binding.etPassword.isEnabled = !mostrar
    }
}