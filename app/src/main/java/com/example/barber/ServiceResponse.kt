package com.example.barber

data class Service(
    val service_id: Int,
    val salon_id: Int,
    var service_name: String,
    var service_price: Int
)

data class ServiceResponse(
    val success: Boolean,
    val services: List<Service>
)
