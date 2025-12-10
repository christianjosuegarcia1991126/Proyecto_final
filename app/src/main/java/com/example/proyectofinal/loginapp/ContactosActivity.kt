package com.example.proyectofinal.loginapp

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.proyectofinal.R
import com.example.proyectofinal.databinding.ActivityContactosBinding
import com.example.proyectofinal.databinding.DialogAgregarContactoBinding
import com.example.proyectofinal.loginapp.adapters.ContactosAdapter
import com.example.proyectofinal.loginapp.network.ApiService
import com.example.proyectofinal.loginapp.network.Contacto

class ContactosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityContactosBinding
    private lateinit var adapter: ContactosAdapter
    private val apiService = ApiService()

    private var usuarioId: Int = -1
    private val contactos = mutableListOf<Contacto>()

    companion object {
        private const val TAG = "ContactosActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        usuarioId = intent.getIntExtra("USUARIO_ID", -1)

        if (usuarioId == -1) {
            Toast.makeText(this, "Error: Usuario no válido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        setupRecyclerView()
        setupListeners()
        cargarContactos()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = ContactosAdapter(contactos) { contacto ->
            mostrarDialogoEliminar(contacto)
        }
        binding.rvContactos.layoutManager = LinearLayoutManager(this)
        binding.rvContactos.adapter = adapter
    }

    private fun setupListeners() {
        binding.fabAgregarContacto.setOnClickListener {
            mostrarDialogoAgregarContacto()
        }
    }

    private fun cargarContactos() {
        mostrarLoading(true)
        apiService.obtenerContactos(usuarioId) { result ->
            runOnUiThread {
                mostrarLoading(false)
                result.onSuccess { listaContactos ->
                    Log.d(TAG, "Contactos obtenidos: ${listaContactos.size}")
                    contactos.clear()
                    contactos.addAll(listaContactos)
                    adapter.notifyDataSetChanged()
                    actualizarVistaVacia()
                }.onFailure { error ->
                    Log.e(TAG, "Error al cargar contactos", error)
                    Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun mostrarDialogoAgregarContacto() {
        val dialogBinding = DialogAgregarContactoBinding.inflate(LayoutInflater.from(this))
        val dialog = AlertDialog.Builder(this).setView(dialogBinding.root).create()

        dialogBinding.btnCancelar.setOnClickListener { dialog.dismiss() }
        dialogBinding.btnGuardar.setOnClickListener {
            val nombre = dialogBinding.etNombre.text.toString().trim()
            val telefono = dialogBinding.etTelefono.text.toString().trim()
            val empresa = dialogBinding.etEmpresa.text.toString().trim()

            if (nombre.isNotEmpty() && telefono.isNotEmpty()) {
                dialog.dismiss()
                crearContacto(nombre, telefono, empresa)
            } else {
                Toast.makeText(this, "Nombre y teléfono son requeridos", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }

    private fun crearContacto(nombre: String, telefono: String, empresa: String) {
        mostrarLoading(true)
        apiService.crearContacto(nombre, telefono, empresa, usuarioId) { result ->
            runOnUiThread {
                mostrarLoading(false)
                result.onSuccess { response ->
                    // SOLUCIÓN: Acceder a la propiedad .mensaje del objeto de respuesta
                    Toast.makeText(this, response.mensaje, Toast.LENGTH_SHORT).show()
                    cargarContactos()
                }.onFailure { error ->
                    Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun mostrarDialogoEliminar(contacto: Contacto) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Contacto")
            .setMessage("¿Estás seguro de que quieres eliminar a ${contacto.nombre}?")
            .setPositiveButton("Eliminar") { _, _ ->
                // SOLUCIÓN: El ID del contacto ya es Int, no necesita .toInt()
                eliminarContacto(contacto.id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarContacto(contactoId: Int) {
        mostrarLoading(true)
        apiService.eliminarContacto(contactoId) { result ->
            runOnUiThread {
                mostrarLoading(false)
                result.onSuccess { response ->
                    // SOLUCIÓN: Acceder a la propiedad .mensaje del objeto de respuesta
                    Toast.makeText(this, response.mensaje, Toast.LENGTH_SHORT).show()
                    cargarContactos()
                }.onFailure { error ->
                    Toast.makeText(this, "Error al eliminar: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun actualizarVistaVacia() {
        binding.layoutVacio.visibility = if (contactos.isEmpty()) View.VISIBLE else View.GONE
        binding.rvContactos.visibility = if (contactos.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun mostrarLoading(mostrar: Boolean) {
        binding.progressBar.visibility = if (mostrar) View.VISIBLE else View.GONE
    }
}