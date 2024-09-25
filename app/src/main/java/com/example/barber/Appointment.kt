package com.example.barber.models

data class Appointment(
    val appointment_id: Int,
    val client_name: String,
    val appointment_date: String,
    val appointment_time: String,
    val service_id: Int?,
    val barber_id: Int,
    val salon_id: Int
)