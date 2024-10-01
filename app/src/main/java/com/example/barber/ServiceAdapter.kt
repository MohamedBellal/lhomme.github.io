import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.barber.AddServiceResponse
import com.example.barber.R
import com.example.barber.Service

class ServiceAdapter(
    val services: MutableList<Service>,  // Déclaration de la liste comme MutableList
    private val onEditService: (Service) -> Unit,
    private val onDeleteService: (Service) -> Unit
) : RecyclerView.Adapter<ServiceAdapter.ServiceViewHolder>() {

    class ServiceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val serviceName: TextView = view.findViewById(R.id.serviceName)
        val servicePrice: TextView = view.findViewById(R.id.servicePrice)
        val editButton: Button = view.findViewById(R.id.editServiceButton)
        val deleteButton: Button = view.findViewById(R.id.deleteServiceButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_service, parent, false)
        return ServiceViewHolder(view)
    }

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {
        val service = services[position]
        holder.serviceName.text = service.service_name
        holder.servicePrice.text = String.format("%.2f €", service.service_price / 100.0)

        // Gérer l'action de modification
        holder.editButton.setOnClickListener {
            onEditService(service)
        }

        // Gérer l'action de suppression
        holder.deleteButton.setOnClickListener {
            onDeleteService(service)  // Appelle la fonction de suppression avec le service à supprimer
        }
    }

    override fun getItemCount() = services.size

    // Méthode pour ajouter un service
    fun addService(service: Service) {
        services.add(service)
        notifyItemInserted(services.size - 1)
    }
}
