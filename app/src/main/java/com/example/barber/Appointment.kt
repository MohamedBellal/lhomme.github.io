package com.example.barber.models

data class Appointment(
    val appointment_id: Int,
    val client_name: String,
    val appointment_date: String,
    val appointment_time: String,
    val service_name: String?,
    val service_price: Int?,
    val barber_name: String?,
    val additional_name: String?,
    val additional_price: Int,
    val barber_id: Int?
)
