package com.example.barber

import com.example.barber.adapters.AppointmentsAdapter
import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.GridLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.barber.adapters.SelectedAppointmentsAdapter
import com.example.barber.models.Appointment
import com.example.barber.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.*
import com.google.gson.JsonSyntaxException

private var appointments = mutableListOf<Appointment>()

class MainActivity : AppCompatActivity() {

    private lateinit var calendarGrid: GridLayout
    private lateinit var monthYearText: TextView
    private lateinit var buttonAhmed: Button
    private lateinit var buttonAbdel: Button
    private lateinit var buttonKadir: Button
    private var currentMonth = Calendar.getInstance()
    private var selectedBarberId = 1
    private var selectedAppointments = mutableSetOf<Appointment>()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialisation des vues
        calendarGrid = findViewById(R.id.calendarGrid)
        monthYearText = findViewById(R.id.monthYearText)
        buttonAhmed = findViewById(R.id.buttonAhmed)
        buttonAbdel = findViewById(R.id.buttonAbdel)
        buttonKadir = findViewById(R.id.buttonKadir)

        val previousMonthButton = findViewById<Button>(R.id.previousMonthButton)
        previousMonthButton.setOnClickListener {
            currentMonth.add(Calendar.MONTH, -1)
            updateCalendar()
        }

        val nextMonthButton = findViewById<Button>(R.id.nextMonthButton)
        nextMonthButton.setOnClickListener {
            currentMonth.add(Calendar.MONTH, 1)
            updateCalendar()
        }

        // Configuration des boutons des coiffeurs
        buttonAhmed.setOnClickListener { setSelectedBarber(1) }
        buttonAbdel.setOnClickListener { setSelectedBarber(2) }
        buttonKadir.setOnClickListener { setSelectedBarber(3) }

        updateCalendar()
    }

    private fun updateDeleteButtonState(dialog: Dialog) {
        val deleteButton = dialog.findViewById<Button>(R.id.deleteButton)
        val selected = selectedAppointments.isNotEmpty()
        deleteButton.isEnabled = selected
        deleteButton.setBackgroundColor(if (selected) Color.RED else Color.GRAY)
    }

    private fun deleteSelectedAppointments(appointments: List<Appointment>, onComplete: () -> Unit) {
        val appointmentIds = appointments.map { it.appointment_id }.joinToString(",")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.deleteAppointments(appointmentIds)
                if (response.isSuccessful) {
                    selectedAppointments.clear()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Rendez-vous supprimés", Toast.LENGTH_SHORT).show()
                        onComplete()
                    }
                } else {
                    Log.e("DeleteAppointments", "Erreur HTTP: ${response.errorBody()?.string()}")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Erreur lors de la suppression", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("DeleteAppointments", "Erreur lors de la suppression des rendez-vous", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Erreur lors de la suppression", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setSelectedBarber(barberId: Int) {
        selectedBarberId = barberId
        resetButtonColors()
        when (barberId) {
            1 -> buttonAhmed.setBackgroundResource(R.drawable.button_selected)
            2 -> buttonAbdel.setBackgroundResource(R.drawable.button_selected)
            3 -> buttonKadir.setBackgroundResource(R.drawable.button_selected)
        }
        updateCalendarForSelectedBarber(barberId)
    }

    private fun resetButtonColors() {
        buttonAhmed.setBackgroundResource(R.drawable.button_unselected)
        buttonAbdel.setBackgroundResource(R.drawable.button_unselected)
        buttonKadir.setBackgroundResource(R.drawable.button_unselected)
    }

    private fun updateCalendar() {
        calendarGrid.removeAllViews()
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        monthYearText.text = sdf.format(currentMonth.time)
        val calendar = Calendar.getInstance()
        calendar.time = currentMonth.time
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1

        for (i in 0 until firstDayOfWeek) {
            addEmptyDay(calendarGrid)
        }

        for (day in 1..daysInMonth) {
            addDayToCalendar(calendarGrid, day)
        }

        val totalCells = firstDayOfWeek + daysInMonth
        val remainingCells = 7 - (totalCells % 7)
        if (remainingCells < 7) {
            for (i in 0 until remainingCells) {
                addEmptyDay(calendarGrid)
            }
        }
    }

    private fun addDayToCalendar(gridLayout: GridLayout, day: Int) {
        val dayLayout = RelativeLayout(this).apply {
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = 0
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(4, 4, 4, 4)
            }
        }
        val dayButton = Button(this).apply {
            text = day.toString()
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            )
            textSize = 16f
            setPadding(8, 8, 8, 8)
            setBackgroundResource(R.drawable.calendar_background)
            setTextColor(Color.BLACK)
            setOnClickListener {
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(currentMonth.apply { set(Calendar.DAY_OF_MONTH, day) }.time)
                openAppointmentDialog(date)
            }
        }
        val calendarDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(currentMonth.apply { set(Calendar.DAY_OF_MONTH, day) }.time)
        val appointmentsForDay = appointments.filter { it.appointment_date == calendarDate }
        if (appointmentsForDay.isNotEmpty()) {
            val notificationBadge = TextView(this).apply {
                text = appointmentsForDay.size.toString()
                setBackgroundResource(R.drawable.notification_badge_background)
                setTextColor(Color.WHITE)
                textSize = 10f
                setPadding(2, 2, 2, 2)
                layoutParams = RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    addRule(RelativeLayout.ALIGN_PARENT_TOP)
                    addRule(RelativeLayout.ALIGN_PARENT_END)
                    setMargins(0, 0, 8, 0)
                }
            }
            dayLayout.addView(notificationBadge)
        }
        dayLayout.addView(dayButton)
        gridLayout.addView(dayLayout)
    }

    private fun addEmptyDay(gridLayout: GridLayout) {
        val emptyButton = Button(this)
        emptyButton.layoutParams = GridLayout.LayoutParams().apply {
            width = 0
            height = 0
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            setMargins(4, 4, 4, 4)
        }
        emptyButton.setBackgroundResource(R.drawable.calendar_day_background)
        emptyButton.isEnabled = false
        gridLayout.addView(emptyButton)
    }

    private fun openAppointmentDialog(date: String) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_appointments)

        val closeModalButton = dialog.findViewById<TextView>(R.id.closeModalButton)
        val appointmentsRecyclerView = dialog.findViewById<RecyclerView>(R.id.appointmentsRecyclerView)
        val deleteButton = dialog.findViewById<Button>(R.id.deleteButton)
        deleteButton.setOnClickListener {
            if (selectedAppointments.isNotEmpty()) {
                showDeleteConfirmationDialog()
            }
        }

        appointmentsRecyclerView.layoutManager = LinearLayoutManager(this)
        appointmentsRecyclerView.setHasFixedSize(true)

        closeModalButton.setOnClickListener {
            dialog.dismiss()
        }

        fetchAppointments(date) { appointments ->
            selectedAppointments.clear()  // Clear previous selections
            val adapter = AppointmentsAdapter(
                appointments,
                selectedAppointments,
                onSelectionChanged = {
                    updateDeleteButtonState(dialog)  // Update button state on selection change
                }
            )
            appointmentsRecyclerView.adapter = adapter
            Log.d("MainActivity", "Adapter attached to RecyclerView")
        }

        updateDeleteButtonState(dialog)  // Initial update of delete button state

        dialog.show()
    }

    private fun updateCalendarForSelectedBarber(barberId: Int) {
        fetchAppointmentsForBarber(barberId) { appointments ->
            // Update the calendar with filtered appointments
        }
    }

    private fun fetchAppointmentsForBarber(barberId: Int, callback: (List<Appointment>) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("FetchAppForBarber", "Fetching appointments for barber with ID: $barberId")
                val response = RetrofitClient.apiService.getAppointments(
                    date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(currentMonth.time),
                    barberId = barberId
                )
                if (response.isSuccessful) {
                    val appointments = response.body() ?: emptyList()
                    withContext(Dispatchers.Main) {
                        callback(appointments)
                    }
                } else {
                    Log.e("FetchAppForBarber", "Error: ${response.errorBody()?.string()}")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Erreur lors de la récupération des rendez-vous", Toast.LENGTH_SHORT).show()
                        callback(emptyList())
                    }
                }
            } catch (e: JsonSyntaxException) { // Remplacez MalformedJsonException par JsonSyntaxException
                Log.e("FetchAppForBarber", "Malformed JSON error", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Erreur JSON malformé lors de la récupération des rendez-vous", Toast.LENGTH_SHORT).show()
                    callback(emptyList())
                }
            } catch (e: Exception) {
                Log.e("FetchAppForBarber", "Erreur lors de la récupération des rendez-vous", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Erreur lors de la récupération des rendez-vous", Toast.LENGTH_SHORT).show()
                    callback(emptyList())
                }
            }
        }
    }

    private fun fetchAppointments(date: String, callback: (List<Appointment>) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Correction des paramètres envoyés à l'API
                val response = RetrofitClient.apiService.getAppointments(date, selectedBarberId)
                if (response.isSuccessful) {
                    val appointments = response.body() ?: emptyList()
                    withContext(Dispatchers.Main) {
                        callback(appointments)
                    }
                } else {
                    Log.e("FetchAppointments", "Error: ${response.errorBody()?.string()}")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Erreur lors de la récupération des rendez-vous", Toast.LENGTH_SHORT).show()
                        callback(emptyList())
                    }
                }
            } catch (e: JsonSyntaxException) { // Remplacez MalformedJsonException par JsonSyntaxException
                Log.e("FetchAppointments", "Malformed JSON error", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Erreur JSON malformé lors de la récupération des rendez-vous", Toast.LENGTH_SHORT).show()
                    callback(emptyList())
                }
            } catch (e: Exception) {
                Log.e("FetchAppointments", "Erreur lors de la récupération des rendez-vous", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Erreur lors de la récupération des rendez-vous", Toast.LENGTH_SHORT).show()
                    callback(emptyList())
                }
            }
        }
        callback(listOf()) // Exemple vide pour l'instant
    }

    private fun showDeleteConfirmationDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_confirm_delete)

        val confirmationMessage = dialog.findViewById<TextView>(R.id.confirmationMessage)
        val selectedAppointmentsRecyclerView = dialog.findViewById<RecyclerView>(R.id.selectedAppointmentsRecyclerView)
        val cancelButton = dialog.findViewById<Button>(R.id.cancelButton)
        val confirmButton = dialog.findViewById<Button>(R.id.confirmButton)

        // Configure le RecyclerView pour afficher les rendez-vous sélectionnés
        selectedAppointmentsRecyclerView.layoutManager = LinearLayoutManager(this)
        selectedAppointmentsRecyclerView.adapter = SelectedAppointmentsAdapter(selectedAppointments.toList())

        cancelButton.setOnClickListener { dialog.dismiss() }

        confirmButton.setOnClickListener {
            deleteSelectedAppointments(selectedAppointments.toList()) {
                updateCalendar()  // Mets à jour le calendrier après la suppression
                dialog.dismiss()
            }
        }

        dialog.show()
    }
}