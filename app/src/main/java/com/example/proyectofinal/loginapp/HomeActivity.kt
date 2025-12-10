package com.example.proyectofinal.loginapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.proyectofinal.R
import com.example.proyectofinal.databinding.ActivityHomeBinding

/**
 * Pantalla de bienvenida después del login exitoso
 */
class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    // Declarar las variables como propiedades de la clase para que sean accesibles en toda la actividad
    private var usuarioId: Int = -1 // Asumiendo que el ID es un entero. Usamos -1 como valor por defecto.
    private lateinit var nombreUsuario: String

    companion object {
        private const val TAG = "HomeActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = ActivityHomeBinding.inflate(layoutInflater)
            setContentView(binding.root)

            Log.d(TAG, "HomeActivity iniciada correctamente")

            // Obtener datos del usuario del Intent y asignarlos a las propiedades de la clase
            usuarioId = intent.getIntExtra("USUARIO_ID", -1) // Es importante que LoginActivity envíe este dato
            nombreUsuario = intent.getStringExtra("NOMBRE_USUARIO")
                ?: getString(R.string.usuario_default)
            val correoUsuario = intent.getStringExtra("CORREO_USUARIO") ?: ""

            // Mostrar mensaje de bienvenida usando recursos
            binding.tvBienvenida.text = getString(R.string.bienvenida_home, nombreUsuario)
            binding.tvCorreo.text = correoUsuario

            Log.d(TAG, "ID: $usuarioId - Usuario: $nombreUsuario - Correo: $correoUsuario")

            setupListeners()

            // Deshabilitar el gesto de retroceso
            onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // No hacer nada para deshabilitar el botón de "Atrás"
                    Log.d(TAG, "Botón atrás deshabilitado")
                }
            })

        } catch (e: Exception) {
            Log.e(TAG, "Error al inicializar HomeActivity", e)
        }
    }

    private fun setupListeners() {
        // Botón de Contactos
        binding.btnContactos.setOnClickListener {
            Log.d(TAG, "Navegando a Contactos")
            val intent = Intent(this, ContactosActivity::class.java)
            // Ahora 'usuarioId' y 'nombreUsuario' son accesibles aquí
            intent.putExtra("USUARIO_ID", usuarioId)
            intent.putExtra("NOMBRE_USUARIO", nombreUsuario)
            startActivity(intent)
        }

        // Botón de Recordatorios
        binding.btnRecordatorios.setOnClickListener {
            Log.d(TAG, "Navegando a Recordatorios")
            val intent = Intent(this, RecordatoriosActivity::class.java)
            // También son accesibles aquí
            intent.putExtra("USUARIO_ID", usuarioId)
            intent.putExtra("NOMBRE_USUARIO", nombreUsuario)
            startActivity(intent)
        }

        // Botón de cerrar sesión
        binding.btnCerrarSesion.setOnClickListener {
            Log.d(TAG, "Cerrando sesión")
            cerrarSesion()
        }

        // Botón de perfil (ejemplo)
        binding.btnPerfil.setOnClickListener {
            Log.d(TAG, "Botón perfil presionado")
            // Aquí puedes navegar a la pantalla de perfil
            // o mostrar más información del usuario
        }
    }

    /**
     * Cierra la sesión y regresa al login
     */
    private fun cerrarSesion() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}