package com.example.barber

// Modèle de données pour un créneau bloqué
data class BlockedSlot(
    val appointment_date: String,
    val appointment_time: String
)