package com.example.barber.models

data class AppointmentsResponse(
    val success: Boolean,
    val appointments: List<Appointment>
)
