package com.example.barber.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.barber.R
import com.example.barber.models.Appointment

class SelectedAppointmentsAdapter(
    private val appointments: List<Appointment>
) : RecyclerView.Adapter<SelectedAppointmentsAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val clientName: TextView = view.findViewById(R.id.client_name)
        val appointmentTime: TextView = view.findViewById(R.id.appointment_time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_selected_appointment, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appointment = appointments[position]
        holder.clientName.text = appointment.client_name
        holder.appointmentTime.text = appointment.appointment_time
    }

    override fun getItemCount(): Int = appointments.size
}
