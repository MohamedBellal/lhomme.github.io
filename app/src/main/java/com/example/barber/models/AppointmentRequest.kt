package com.example.barber.models

data class AppointmentRequest(
    val client_name: String,
    val barber_id: Int,
    val service_id: Int,
    val appointment_date: String,
    val appointment_time: String,
    val payment_method: String,
    val salon_id: Int // Ajout du salon_id ici
)