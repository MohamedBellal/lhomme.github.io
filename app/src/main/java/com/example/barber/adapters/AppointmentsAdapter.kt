package com.example.barber.adapters

import android.graphics.Color
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
                    // Toggle selection
                    if (selectedAppointments.contains(appointment)) {
                        selectedAppointments.remove(appointment)
                        view.setBackgroundColor(Color.WHITE)
                    } else {
                        selectedAppointments.add(appointment)
                        view.setBackgroundColor(Color.LTGRAY)
                    }
                    // Call the callback to update the button state
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
        holder.clientName.text = appointment.client_name
        holder.appointmentDate.text = appointment.appointment_date
        holder.appointmentTime.text = appointment.appointment_time

        // Update the background color based on selection state
        holder.itemView.setBackgroundColor(
            if (selectedAppointments.contains(appointment)) Color.LTGRAY else Color.WHITE
        )
    }

    override fun getItemCount(): Int = appointments.size
}
