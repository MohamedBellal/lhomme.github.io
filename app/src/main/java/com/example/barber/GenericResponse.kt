package com.example.barber

data class GenericResponse(
    val success: Boolean,
    val error: String? = null
)