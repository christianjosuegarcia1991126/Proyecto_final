package com.example.proyectofinal.loginapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectofinal.databinding.ItemContactoBinding
import com.example.proyectofinal.loginapp.network.Contacto

/**
 * Adapter para la lista de contactos, ahora usando el objeto Contacto.
 */
// 1. CAMBIO: El constructor ahora espera una lista de objetos Contacto
class ContactosAdapter(
    private val contactos: List<Contacto>,
    private val onEliminarClick: (Contacto) -> Unit
) : RecyclerView.Adapter<ContactosAdapter.ContactoViewHolder>() {

    inner class ContactoViewHolder(private val binding: ItemContactoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        // 2. CAMBIO: La función bind ahora recibe un objeto Contacto
        fun bind(contacto: Contacto) {
            // 3. CAMBIO: Accedemos a las propiedades directamente, de forma segura
            val nombre = contacto.nombre
            val empresa = if (contacto.empresa.isNullOrEmpty()) "Sin empresa" else contacto.empresa

            // Mostrar inicial del nombre
            val inicial = nombre.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            binding.tvInicial.text = inicial

            // Mostrar información
            binding.tvNombre.text = nombre
            binding.tvEmpresa.text = empresa

            // Listener para eliminar (ahora pasa el objeto Contacto)
            binding.btnEliminar.setOnClickListener {
                onEliminarClick(contacto)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactoViewHolder {
        val binding = ItemContactoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ContactoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactoViewHolder, position: Int) {
        holder.bind(contactos[position])
    }

    override fun getItemCount(): Int = contactos.size
}
