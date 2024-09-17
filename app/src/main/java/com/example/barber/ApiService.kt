import com.example.barber.models.Appointment
import com.example.barber.models.AppointmentRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @GET("get_appointments.php")
    suspend fun getAppointments(
        @Query("date") date: String,
        @Query("barber_id") barberId: Int
    ): Response<List<Appointment>>

    @DELETE("delete_appointments.php")
    suspend fun deleteAppointments(
        @Query("appointment_ids") appointmentIds: String // Change to String for comma-separated values
    ): Response<Unit>

    @POST("create_appointment.php")
    suspend fun createAppointment(@Body request: AppointmentRequest): Response<ResponseBody>
}