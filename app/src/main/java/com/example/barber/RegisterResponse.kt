package com.example.barber

data class RegisterResponse(
    val success: Boolean,
    val userId: Int?,  // Assurez-vous que le backend renvoie bien cet ID
    val message: String
)