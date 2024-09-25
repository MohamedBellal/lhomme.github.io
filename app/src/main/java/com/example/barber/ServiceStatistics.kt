package com.example.barber
import com.google.gson.annotations.SerializedName

data class ServiceStatistics(
    @SerializedName("service_name") val serviceName: String,
    val totalAppointments: Int
)