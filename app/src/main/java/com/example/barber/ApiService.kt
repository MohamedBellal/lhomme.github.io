import com.example.barber.AddBarbersResponse
import com.example.barber.Barber
import com.example.barber.BarberResponse
import com.example.barber.BlockedSlot
import com.example.barber.CreateSalonResponse
import com.example.barber.GenericResponse
import com.example.barber.LoginResponse
import com.example.barber.RegisterResponse
import com.example.barber.StatisticsResponse
import com.example.barber.models.Appointment
import com.example.barber.models.AppointmentRequest
import com.example.barber.models.AppointmentResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    // Récupérer les rendez-vous d'un jour spécifique
    @GET("get_appointments.php")
    suspend fun getAppointments(
        @Query("date") date: String,
        @Query("barber_id") barberId: Int
    ): Response<AppointmentResponse>

    @GET("get_appointments.php")
    suspend fun getAppointmentsForSalonAndBarber(
        @Query("salon_id") salonId: Int,
        @Query("barber_id") barberId: Int,
        @Query("date") date: String
    ): Response<AppointmentResponse>

    // Supprimer les rendez-vous
    @DELETE("delete_appointments.php")
    suspend fun deleteAppointments(
        @Query("appointment_ids") appointmentIds: String
    ): Response<Unit>

    // Créer un rendez-vous
    @POST("create_appointment.php")
    suspend fun createAppointment(@Body request: AppointmentRequest): Response<ResponseBody>

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
        @Field("date") date: String,
        @Field("time") time: String
    ): Response<ResponseBody>

    // Débloquer un créneau
    @FormUrlEncoded
    @POST("unblock_slot.php")
    suspend fun unblockSlot(
        @Field("barber_id") barberId: Int,
        @Field("date") date: String,
        @Field("time") time: String
    ): Response<ResponseBody>

    // Récupérer les créneaux bloqués
    @GET("get_blocked_slots.php")
    suspend fun getBlockedSlots(
        @Query("barber_id") barberId: Int,
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String
    ): Response<List<BlockedSlot>>

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
        @FieldMap params: Map<String, String>
    ): Response<AddBarbersResponse>

    @GET("get_barbers.php")
    suspend fun getBarbers(@Query("salon_id") salonId: Int): Response<BarberResponse>
}