package com.example.barber.models

data class Appointment(
    val appointment_id: Int? = null,
    val client_name: String?,
    val appointment_date: String?,
    val appointment_time: String?,
    val service_id: Int?,
    val barber_id: Int,
    val salon_id: Int,
    val appointment_status: String?,
    val payment_method: String?
) {
    override fun hashCode(): Int {
        var result = appointment_id ?: 0
        result = 31 * result + (client_name?.hashCode() ?: 0)
        result = 31 * result + (appointment_date?.hashCode() ?: 0)
        result = 31 * result + (appointment_time?.hashCode() ?: 0)
        result = 31 * result + (service_id ?: 0)
        result = 31 * result + barber_id
        result = 31 * result + salon_id
        result = 31 * result + (appointment_status?.hashCode() ?: 0)
        result = 31 * result + (payment_method?.hashCode() ?: 0)
        return result
    }
}
