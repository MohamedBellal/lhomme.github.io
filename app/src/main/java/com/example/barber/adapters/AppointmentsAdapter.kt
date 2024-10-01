package com.example.barber.adapters

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.barber.models.Appointment
import com.example.barber.R

class AppointmentsAdapter(
    private val appointments: List<Appointment>,
    private val selectedAppointments: MutableSet<Appointment>,
    private val onSelectionChanged: () -> Unit
) : RecyclerView.Adapter<AppointmentsAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val clientName: TextView = view.findViewById(R.id.client_name)
        val appointmentDate: TextView = view.findViewById(R.id.appointment_date)
        val appointmentTime: TextView = view.findViewById(R.id.appointment_time)

        init {
            view.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val appointment = appointments[position]

                    // Ajouter des logs pour inspecter les valeurs de l'appointment
                    Log.e("AppointmentCheck", "Appointment ID: ${appointment.appointment_id}, Client: ${appointment.client_name}")

                    // Code pour sélectionner l'appointment
                    if (selectedAppointments.contains(appointment)) {
                        selectedAppointments.remove(appointment)
                        view.setBackgroundColor(Color.WHITE)
                    } else {
                        selectedAppointments.add(appointment)
                        view.setBackgroundColor(Color.LTGRAY)
                    }
                    onSelectionChanged()
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_appointment, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appointment = appointments[position]

        if (appointment.client_name != null && appointment.appointment_time != null) {
            holder.clientName.text = appointment.client_name
            holder.appointmentDate.text = appointment.appointment_date
            holder.appointmentTime.text = appointment.appointment_time

            holder.itemView.setBackgroundColor(
                if (selectedAppointments.contains(appointment)) Color.LTGRAY else Color.WHITE
            )
        } else {
            // Log an error or provide fallback behavior
            Log.e("AppointmentsAdapter", "Appointment at position $position has null values")
        }
    }

    override fun getItemCount(): Int = appointments.size
}