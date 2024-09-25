package com.example.barber

data class CreateSalonResponse(
    val success: Boolean,
    val salon_id: Int?,
    val error: String?
)