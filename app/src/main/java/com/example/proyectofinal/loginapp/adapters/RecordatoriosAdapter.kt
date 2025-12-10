package com.example.proyectofinal.loginapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectofinal.databinding.ItemRecordatorioBinding
import com.example.proyectofinal.loginapp.network.Recordatorio
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter para la lista de recordatorios, modernizado para usar la data class Recordatorio.
 */
class RecordatoriosAdapter(
    private val recordatorios: List<Recordatorio>,
    private val onEliminarClick: (Recordatorio) -> Unit
) : RecyclerView.Adapter<RecordatoriosAdapter.RecordatorioViewHolder>() {

    inner class RecordatorioViewHolder(private val binding: ItemRecordatorioBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(recordatorio: Recordatorio) {
            // Accedemos a las propiedades del objeto directamente
            binding.tvNombre.text = recordatorio.nombre
            binding.tvContacto.text = recordatorio.contacto_nombre
            binding.tvHora.text = recordatorio.hora

            // Formatear y mostrar fecha
            try {
                // El servidor ahora manda la fecha en formato YYYY-MM-DD
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date = sdf.parse(recordatorio.fecha)

                if (date != null) {
                    val calendar = Calendar.getInstance()
                    calendar.time = date
                    val dia = calendar.get(Calendar.DAY_OF_MONTH)
                    val mes = calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault())

                    binding.tvDia.text = dia.toString()
                    binding.tvMes.text = mes?.uppercase() ?: ""
                } else {
                    binding.tvDia.text = "-"
                    binding.tvMes.text = "ERR"
                }
            } catch (e: Exception) {
                binding.tvDia.text = "-"
                binding.tvMes.text = "ERR"
            }
            
            // Mostrar requisiciones si existen
            if (!recordatorio.requisiciones.isNullOrEmpty()) {
                binding.tvRequisiciones.visibility = View.VISIBLE
                binding.tvRequisiciones.text = "📝 ${recordatorio.requisiciones}"
            } else {
                binding.tvRequisiciones.visibility = View.GONE
            }
            
            // Ocultamos el layout de empresa que ya no se usa
            binding.layoutEmpresa.visibility = View.GONE

            // Listener para eliminar
            binding.btnEliminar.setOnClickListener {
                onEliminarClick(recordatorio)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordatorioViewHolder {
        val binding = ItemRecordatorioBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RecordatorioViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecordatorioViewHolder, position: Int) {
        holder.bind(recordatorios[position])
    }

    override fun getItemCount(): Int = recordatorios.size
}
