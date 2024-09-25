package com.example.barber

// Modèle de données pour un créneau bloqué
data class BlockedSlot(
    val appointment_time: String  // Ou un autre champ selon ce que l'API renvoie
)