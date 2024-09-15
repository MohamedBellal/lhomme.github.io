package com.example.barber.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.barber.R

import com.example.barber.models.Appointment

class AppointmentsAdapter(private val appointments: List<Appointment>) : RecyclerView.Adapter<AppointmentsAdapter.AppointmentViewHolder>() {

    inner class AppointmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val clientName: TextView = itemView.findViewById(R.id.client_name)
        val appointmentDate: TextView = itemView.findViewById(R.id.appointment_date)
        val appointmentTime: TextView = itemView.findViewById(R.id.appointment_time)
        val serviceName: TextView = itemView.findViewById(R.id.service_name)
        val servicePrice: TextView = itemView.findViewById(R.id.service_price)

        init {
            Log.d("ViewHolder", "clientName: $clientName, appointmentDate: $appointmentDate, appointmentTime: $appointmentTime, serviceName: $serviceName, servicePrice: $servicePrice")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_appointment, parent, false)
        return AppointmentViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppointmentViewHolder, position: Int) {
        val appointment = appointments[position]
        holder.clientName.text = appointment.client_name
        holder.appointmentDate.text = "Date: ${appointment.appointment_date}"
        holder.appointmentTime.text = "Heure: ${appointment.appointment_time}"
        holder.serviceName.text = "Service: ${appointment.service_name ?: "Service inconnu"}"
        holder.servicePrice.text = "Prix: ${appointment.service_price ?: 0}€"
    }

    override fun getItemCount(): Int = appointments.size
}
