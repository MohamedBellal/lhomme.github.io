package com.example.barber

data class GetServicesResponse(
    val services: List<Service>
)

data class AddServiceResponse(
    val service: Service
)

data class ServicesGenericResponse(
    val success: Boolean,
    val message: String
)