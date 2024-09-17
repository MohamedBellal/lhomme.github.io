package com.example.barber.adapters

import android.animation.ObjectAnimator
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
    private val onAppointmentLongClick: (Appointment) -> Unit,
    private val onAppointmentClick: (Appointment) -> Unit
) : RecyclerView.Adapter<AppointmentsAdapter.ViewHolder>() {

    private val selectedAppointments = mutableSetOf<Appointment>()

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val clientName: TextView = view.findViewById(R.id.client_name) ?: throw NullPointerException("TextView client_name not found")
        val appointmentDate: TextView = view.findViewById(R.id.appointment_date) ?: throw NullPointerException("TextView appointment_date not found")
        val appointmentTime: TextView = view.findViewById(R.id.appointment_time) ?: throw NullPointerException("TextView appointment_time not found")

        init {
            // Vérification des vues avec des logs
            Log.d("AppointmentsAdapter", "clientName view: $clientName")
            Log.d("AppointmentsAdapter", "appointmentDate view: $appointmentDate")
            Log.d("AppointmentsAdapter", "appointmentTime view: $appointmentTime")

            view.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val appointment = appointments[position]
                    if (selectedAppointments.contains(appointment)) {
                        selectedAppointments.remove(appointment)
                        view.setBackgroundColor(Color.WHITE)
                    } else {
                        selectedAppointments.add(appointment)
                        view.setBackgroundColor(Color.LTGRAY)
                    }
                    onAppointmentClick(appointment)
                    notifyDataSetChanged()
                }
            }

            view.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val appointment = appointments[position]
                    if (!selectedAppointments.contains(appointment)) {
                        selectedAppointments.add(appointment)
                        animateLift(view)
                        view.setBackgroundColor(Color.LTGRAY)
                        onAppointmentLongClick(appointment)
                    }
                    notifyDataSetChanged()
                    true
                } else {
                    false
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        Log.d("AppointmentsAdapter", "Inflating view for item_appointment")
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_appointment, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appointment = appointments[position]
        holder.clientName.text = appointment.client_name
        holder.appointmentDate.text = appointment.appointment_date
        holder.appointmentTime.text = appointment.appointment_time

        holder.itemView.setBackgroundColor(
            if (selectedAppointments.contains(appointment)) Color.LTGRAY else Color.WHITE
        )
    }

    override fun getItemCount(): Int = appointments.size

    private fun animateLift(view: View) {
        val animator = ObjectAnimator.ofFloat(view, "translationZ", 0f, 15f)
        animator.duration = 300
        animator.start()
    }

    fun getSelectedAppointments(): List<Appointment> = selectedAppointments.toList()
}// test pour github ssupprime mtn