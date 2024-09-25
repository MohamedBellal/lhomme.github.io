package com.example.barber

import com.google.gson.annotations.SerializedName

data class StatisticsResponse(
    val totalAppointments: Int,
    val totalRevenue: String,
    val appointmentsPerService: List<ServiceStatistics>,
    val appointmentsPerBarber: List<BarberStatistics>
)