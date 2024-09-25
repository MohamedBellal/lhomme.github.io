package com.example.barber

data class BarberResponse(
    val success: Boolean,
    val barbers: List<Barber>
)

data class Barber(
    val barber_id: Int,
    val salon_id: Int,
    val barber_name: String
)