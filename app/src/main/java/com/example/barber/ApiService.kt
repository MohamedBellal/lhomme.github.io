package com.example.barber

import com.example.barber.models.Appointment
import com.example.barber.models.AppointmentsResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    // Récupérer les rendez-vous d'un jour spécifique
    @GET("get_appointments.php")
    suspend fun getAppointments(
        @Query("date") date: String,
        @Query("barber_id") barberId: Int
    ): Response<AppointmentsResponse>

    // Récupérer les créneaux
    @GET("get_appointments.php")
    suspend fun getAppointmentsForBarberAndSalon(
        @Query("date") date: String,
        @Query("barber_id") barberId: Int,
        @Query("salon_id") salonId: String
    ): Response<AppointmentsResponse>

    // Récupérer les créneaux bloqués
    @GET("get_blocked_slots.php")
    suspend fun getBlockedSlotsForBarberAndSalon(
        @Query("barber_id") barberId: Int,
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String,
        @Query("salon_id") salonId: String  // Ajoutez cet argument
    ): Response<List<BlockedSlot>>

    // Supprimer les rendez-vous
    @DELETE("delete_appointments.php")
    suspend fun deleteAppointments(
        @Query("appointment_ids") appointmentIds: String
    ): Response<Unit>

    // Créer un rendez-vous
    @Headers("Content-Type: application/json")
    @POST("create_appointment.php")
    suspend fun createAppointment(@Body appointment: Appointment): Response<ResponseBody>

    // Récupérer les rendez-vous sur une plage de dates (mois entier)
    @GET("get_appointments_for_month.php")
    suspend fun getAppointmentsForMonth(
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String,
        @Query("barber_id") barberId: Int
    ): Response<List<Appointment>>

    // Bloquer un créneau
    @FormUrlEncoded
    @POST("block_slot.php")
    suspend fun blockSlot(
        @Field("barber_id") barberId: Int,
        @Field("salon_id") salonId: Int,
        @Field("date") date: String,
        @Field("time") time: String
    ): Response<ResponseBody>

    // Débloquer un créneau
    @FormUrlEncoded
    @POST("unblock_slot.php")
    suspend fun unblockSlot(
        @Field("barber_id") barberId: Int,
        @Field("salon_id") salonId: Int,
        @Field("date") date: String,
        @Field("time") time: String
    ): Response<ResponseBody>

    // Récupérer les statistiques des rendez-vous
    @GET("get_statistics.php")
    suspend fun getStatistics(
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String,
        @Query("barber_id") barberId: Int
    ): Response<StatisticsResponse>  // Ici, 'StatisticsResponse' est un objet contenant des données de statistiques

    @POST("register.php")
    suspend fun register(@Body params: Map<String, String>): retrofit2.Response<RegisterResponse>

    @FormUrlEncoded
    @POST("login.php")
    suspend fun login(
        @Field("email") email: String,
        @Field("password") password: String
    ): Response<LoginResponse>

    @FormUrlEncoded
    @POST("create_salon.php")
    suspend fun createSalon(@FieldMap salonData: Map<String, String>): retrofit2.Response<CreateSalonResponse>

    @FormUrlEncoded
    @POST("add_barbers.php")
    suspend fun addBarbers(
        @Field("salon_id") salonId: String,
        @Field("barber_name") barberName: String
    ): Response<AddBarbersResponse>

    @GET("get_barbers.php")
    suspend fun getBarbers(@Query("salon_id") salonId: String): Response<BarberResponse>

    // Mettre à jour le nom du barbier
    @FormUrlEncoded
    @POST("update_barber_name.php")
    suspend fun updateBarberName(
        @Field("barber_id") barberId: Int,
        @Field("barber_name") barberName: String,
        @Field("salon_id") salonId: Int
    ): Response<ResponseBody>

    // Supprimer un barbier
    @FormUrlEncoded
    @POST("delete_barber.php")
    suspend fun deleteBarber(
        @Field("barber_id") barberId: Int,
        @Field("salon_id") salonId: Int
    ): Response<ResponseBody>

    ////////////////////////
    //GESTION DES SERVICES//
    ////////////////////////

    @GET("get_services.php")
    suspend fun getServices(
        @Query("salon_id") salonId: Int
    ): Response<GetServicesResponse>

    @POST("add_service.php")
    suspend fun addService(
        @Query("salon_id") salonId: Int,
        @Query("service_name") serviceName: String,
        @Query("service_price") servicePrice: Int
    ): Response<AddServiceResponse>

    @DELETE("delete_service.php")
    suspend fun deleteService(
        @Query("service_id") serviceId: Int,
        @Query("salon_id") salonId: Int
    ): Response<Void>

    @POST("update_service.php")
    suspend fun updateService(
        @Query("service_id") serviceId: Int,
        @Query("salon_id") salonId: Int,
        @Query("service_name") serviceName: String,
        @Query("service_price") servicePrice: Int
    ): Response<GenericResponse>
}