package com.example.barber

data class LoginResponse(
    val success: Boolean,
    val token: String?,
    val salon_id: Int?,
    val message: String?
)