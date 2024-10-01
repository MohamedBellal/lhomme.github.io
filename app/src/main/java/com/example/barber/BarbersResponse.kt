package com.example.barber

data class BarberResponse(
    val success: Boolean,
    val barbers: List<Barber>
)

data class Barber(
    val barber_id: Int,
    var barber_name: String
) {
    override fun toString(): String {
        return barber_name
    }
}