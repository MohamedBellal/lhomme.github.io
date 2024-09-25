package com.example.barber

data class AddBarbersResponse(
    val success: Boolean,
    val barber_id: Int?,
    val error: String?
)