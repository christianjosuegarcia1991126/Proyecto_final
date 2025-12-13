package com.example.proyectofinal.loginapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.proyectofinal.R
import com.example.proyectofinal.databinding.ActivityHomeBinding

/**
 * Pantalla de bienvenida después del login exitoso
 */
class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    private var usuarioId: Int = -1
    private lateinit var nombreUsuario: String
    private lateinit var correoUsuario: String

    companion object {
        private const val TAG = "HomeActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = ActivityHomeBinding.inflate(layoutInflater)
            setContentView(binding.root)

            Log.d(TAG, "HomeActivity iniciada correctamente")

            // ✅ VALIDACIÓN: Verificar que se recibieron los datos del usuario
            usuarioId = intent.getIntExtra("USUARIO_ID", -1)
            nombreUsuario = intent.getStringExtra("NOMBRE_USUARIO") ?: ""
            correoUsuario = intent.getStringExtra("CORREO_USUARIO") ?: ""

            // Si no hay datos válidos, regresar al login
            if (usuarioId == -1 || nombreUsuario.isEmpty()) {
                Log.e(TAG, "❌ Datos de usuario inválidos. Regresando a login.")
                Toast.makeText(
                    this,
                    "Error: Sesión no válida. Por favor, inicia sesión nuevamente.",
                    Toast.LENGTH_LONG
                ).show()
                regresarALogin()
                return
            }

            // Mostrar información del usuario
            binding.tvBienvenida.text = getString(R.string.bienvenida_home, nombreUsuario)
            binding.tvCorreo.text = correoUsuario

            Log.d(TAG, "✅ Usuario cargado - ID: $usuarioId, Nombre: $nombreUsuario, Correo: $correoUsuario")

            setupListeners()

            // Deshabilitar el botón de retroceso
            onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    Log.d(TAG, "Botón atrás deshabilitado")
                }
            })

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al inicializar HomeActivity", e)
            Toast.makeText(this, "Error al cargar la pantalla: ${e.message}", Toast.LENGTH_LONG).show()
            regresarALogin()
        }
    }

    private fun setupListeners() {
        // Botón de Contactos
        binding.btnContactos.setOnClickListener {
            Log.d(TAG, "Navegando a Contactos")
            val intent = Intent(this, ContactosActivity::class.java).apply {
                putExtra("USUARIO_ID", usuarioId)
                putExtra("NOMBRE_USUARIO", nombreUsuario)
            }
            startActivity(intent)
        }

        // Botón de Recordatorios
        binding.btnRecordatorios.setOnClickListener {
            Log.d(TAG, "Navegando a Recordatorios")
            val intent = Intent(this, RecordatoriosActivity::class.java).apply {
                putExtra("USUARIO_ID", usuarioId)
                putExtra("NOMBRE_USUARIO", nombreUsuario)
            }
            startActivity(intent)
        }

        // Botón de cerrar sesión
        binding.btnCerrarSesion.setOnClickListener {
            Log.d(TAG, "Cerrando sesión")
            cerrarSesion()
        }

        // Botón de perfil
        binding.btnPerfil.setOnClickListener {
            Log.d(TAG, "Botón perfil presionado")
            Toast.makeText(this, "Función de perfil próximamente", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Cierra la sesión y regresa al login
     */
    private fun cerrarSesion() {
        regresarALogin()
    }

    /**
     * Regresa a la pantalla de login y limpia el stack
     */
    private fun regresarALogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}