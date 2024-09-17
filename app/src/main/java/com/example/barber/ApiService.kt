import com.example.barber.models.Appointment
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @GET("get_appointments.php")
    suspend fun getAppointments(
        @Query("date") date: String,
        @Query("barber_id") barberId: Int
    ): Response<List<Appointment>>

    @DELETE("get_appointments.php/{id}")
    suspend fun deleteAppointment(@Path("id") appointmentId: Int): Response<Unit>
}