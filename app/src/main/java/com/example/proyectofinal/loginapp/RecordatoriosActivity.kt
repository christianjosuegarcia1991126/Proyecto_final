package com.example.proyectofinal.loginapp

import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.proyectofinal.databinding.ActivityRecordatoriosBinding
import com.example.proyectofinal.databinding.DialogAgregarRecordatorioBinding
import com.example.proyectofinal.loginapp.adapters.RecordatoriosAdapter
import com.example.proyectofinal.loginapp.network.ApiService
import com.example.proyectofinal.loginapp.network.Contacto
import com.example.proyectofinal.loginapp.network.Recordatorio // Importamos el modelo correcto
import com.example.proyectofinal.loginapp.notifications.NotificationHelper
import java.text.SimpleDateFormat
import java.util.*

class RecordatoriosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecordatoriosBinding
    private lateinit var adapter: RecordatoriosAdapter
    private lateinit var notificationHelper: NotificationHelper
    private val apiService = ApiService()

    private var usuarioId: Int = -1
    private val recordatorios = mutableListOf<Recordatorio>()
    private val contactos = mutableListOf<Contacto>()

    companion object {
        private const val TAG = "RecordatoriosActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecordatoriosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        notificationHelper = NotificationHelper(this)
        solicitarPermisosNotificacion()

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
        cargarRecordatorios()
    }

    private fun solicitarPermisosNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                    if (!isGranted) Toast.makeText(this, "Sin permisos, no se mostrarán notificaciones", Toast.LENGTH_LONG).show()
                }.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = RecordatoriosAdapter(recordatorios) { recordatorio ->
            mostrarDialogoEliminar(recordatorio)
        }
        binding.rvRecordatorios.layoutManager = LinearLayoutManager(this)
        binding.rvRecordatorios.adapter = adapter
    }

    private fun setupListeners() {
        binding.fabAgregarRecordatorio.setOnClickListener {
            if (contactos.isEmpty()) {
                Toast.makeText(this, "Debes tener contactos para crear un recordatorio", Toast.LENGTH_LONG).show()
            } else {
                mostrarDialogoAgregarRecordatorio()
            }
        }
    }

    private fun cargarContactos() {
        apiService.obtenerContactos(usuarioId) { result ->
            runOnUiThread {
                result.onSuccess { listaContactos ->
                    contactos.clear()
                    contactos.addAll(listaContactos)
                }.onFailure { Log.e(TAG, "Error al cargar contactos: ${it.message}") }
            }
        }
    }

    private fun cargarRecordatorios() {
        mostrarLoading(true)
        apiService.obtenerRecordatorios(usuarioId) { result ->
            runOnUiThread {
                mostrarLoading(false)
                result.onSuccess { listaRecordatorios ->
                    recordatorios.clear()
                    recordatorios.addAll(listaRecordatorios)
                    adapter.notifyDataSetChanged()
                    actualizarVistaVacia()
                }.onFailure { error ->
                    Toast.makeText(this, "Error al cargar recordatorios: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun mostrarDialogoAgregarRecordatorio() {
        val dialogBinding = DialogAgregarRecordatorioBinding.inflate(LayoutInflater.from(this))
        val nombresContactos = contactos.map { "${it.nombre} - ${it.empresa ?: ""}" }
        val adapterContactos = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, nombresContactos)
        dialogBinding.actvContacto.setAdapter(adapterContactos)

        var contactoSeleccionadoId: Int? = null
        dialogBinding.actvContacto.setOnItemClickListener { _, _, position, _ ->
            contactoSeleccionadoId = contactos[position].id
        }

        val calendar = Calendar.getInstance()

        dialogBinding.etFecha.setOnClickListener {
            DatePickerDialog(this, { _, year, month, day ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, day)
                dialogBinding.etFecha.setText(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time))
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        dialogBinding.etHora.setOnClickListener {
            TimePickerDialog(this, { _, hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                dialogBinding.etHora.setText(SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendar.time))
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }

        AlertDialog.Builder(this).setView(dialogBinding.root).create().apply {
            dialogBinding.btnCancelar.setOnClickListener { dismiss() }
            dialogBinding.btnGuardar.setOnClickListener {
                val nombre = dialogBinding.etNombre.text.toString().trim()
                val requisiciones = dialogBinding.etRequisiciones.text.toString().trim()
                val fecha = dialogBinding.etFecha.text.toString().trim()
                val hora = dialogBinding.etHora.text.toString().trim()

                if (nombre.isNotEmpty() && contactoSeleccionadoId != null && fecha.isNotEmpty() && hora.isNotEmpty()) {
                    dismiss()
                    crearRecordatorio(nombre, contactoSeleccionadoId!!, requisiciones, fecha, hora, calendar.timeInMillis)
                } else {
                    Toast.makeText(context, "Nombre, contacto, fecha y hora son requeridos", Toast.LENGTH_SHORT).show()
                }
            }
        }.show()
    }


    private fun crearRecordatorio(nombre: String, contactoId: Int, requisiciones: String, fecha: String, hora: String, timeInMillis: Long) {
        mostrarLoading(true)
        apiService.crearRecordatorio(nombre, contactoId, requisiciones, fecha, hora, usuarioId) { result ->
            runOnUiThread {
                mostrarLoading(false)
                result.onSuccess { nuevoRecordatorio ->
                    Toast.makeText(this, "Recordatorio creado exitosamente", Toast.LENGTH_SHORT).show()

                    // ✅ CORREGIDO: Pasar el nombre del contacto
                    val contactoNombre = contactos.find { it.id == contactoId }?.nombre ?: "Sin contacto"

                    Log.d(TAG, "Programando notificación para: ${Date(timeInMillis)}")
                    Log.d(TAG, "Recordatorio ID: ${nuevoRecordatorio.id}, Título: $nombre, Contacto: $contactoNombre")

                    // Programar notificación con los datos correctos
                    notificationHelper.scheduleNotification(
                        timeInMillis = timeInMillis,
                        title = nombre,
                        contactoNombre = contactoNombre,  // ✅ CORREGIDO
                        requisiciones = requisiciones.ifEmpty { "Sin notas adicionales" },
                        notificationId = nuevoRecordatorio.id
                    )

                    cargarRecordatorios()
                }.onFailure { error ->
                    Log.e(TAG, "Error al crear recordatorio", error)
                    Toast.makeText(this, "Error al crear: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun mostrarDialogoEliminar(recordatorio: Recordatorio) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Recordatorio")
            .setMessage("¿Seguro que quieres eliminar el recordatorio '${recordatorio.nombre}'?")
            .setPositiveButton("Eliminar") { _, _ -> eliminarRecordatorio(recordatorio) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarRecordatorio(recordatorio: Recordatorio) {
        mostrarLoading(true)
        apiService.eliminarRecordatorio(recordatorio.id) { result ->
            runOnUiThread {
                mostrarLoading(false)
                result.onSuccess { response ->
                    Toast.makeText(this, response.mensaje ?: "Recordatorio eliminado", Toast.LENGTH_SHORT).show()
                    notificationHelper.cancelNotification(recordatorio.id)
                    cargarRecordatorios()
                }.onFailure { error ->
                    Toast.makeText(this, "Error al eliminar: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun actualizarVistaVacia() {
        binding.layoutVacio.visibility = if (recordatorios.isEmpty()) View.VISIBLE else View.GONE
        binding.rvRecordatorios.visibility = if (recordatorios.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun mostrarLoading(mostrar: Boolean) {
        binding.progressBar.visibility = if (mostrar) View.VISIBLE else View.GONE
    }
}