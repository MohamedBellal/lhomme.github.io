import com.example.barber.models.Appointment
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("get_appointments.php")
    suspend fun getAppointments(
        @Query("date") date: String,
        @Query("barber_id") barberId: Int
    ): List<Appointment>
}
