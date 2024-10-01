import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.barber.Barber
import com.example.barber.R

class BarberAdapter(
    private val barbers: MutableList<Barber>,  // Make this a mutable list
    private val onEditBarber: (Barber) -> Unit,
    private val onDeleteBarber: (Barber) -> Unit
) : RecyclerView.Adapter<BarberAdapter.BarberViewHolder>() {

    inner class BarberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.barberNameTextView)
        val editButton: Button = itemView.findViewById(R.id.editBarberButton)
        val deleteButton: Button = itemView.findViewById(R.id.deleteBarberButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BarberViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_barber, parent, false)
        return BarberViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: BarberViewHolder, position: Int) {
        val barber = barbers[position]
        holder.nameTextView.text = barber.barber_name
        holder.editButton.setOnClickListener { onEditBarber(barber) }
        holder.deleteButton.setOnClickListener { onDeleteBarber(barber) }
    }

    override fun getItemCount() = barbers.size

    fun addBarberToList(barber: Barber) {
        barbers.add(barber)  // Now you can add to the mutable list
    }

    fun removeBarberFromList(barber: Barber) {
        barbers.remove(barber)  // Now you can remove from the mutable list
    }
}