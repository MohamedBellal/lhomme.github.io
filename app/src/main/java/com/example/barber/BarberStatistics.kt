package com.example.barber
import com.google.gson.annotations.SerializedName

data class BarberStatistics(
    @SerializedName("barber_name") val barberName: String,
    val totalAppointments: Int
)