package com.example.barber.models

data class AppointmentResponse(
    val success: Boolean,
    val appointments: List<Appointment>
)
