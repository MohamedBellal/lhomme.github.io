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
        setContentView(R.layout.activity_main) // Assurez-vous que ce layout est bien défini

        // Initialisation des vues en utilisant les bons IDs
        Log.d("MainActivity", "Initialisation des vues")

        try {
            // Initialiser le calendrier
            Log.d("MainActivity", "Trying to initialize calendarGrid")
            calendarGrid = findViewById(R.id.calendarGrid) ?: throw NullPointerException("calendarGrid not found")
            Log.d("MainActivity", "calendarGrid initialized: $calendarGrid")

            // Initialiser le texte du mois et de l'année
            Log.d("MainActivity", "Trying to initialize monthYearText")
            monthYearText = findViewById(R.id.monthYearText) ?: throw NullPointerException("monthYearText not found")
            Log.d("MainActivity", "monthYearText initialized: $monthYearText")

            // Initialiser les boutons des coiffeurs
            Log.d("MainActivity", "Trying to initialize buttonAhmed")
            buttonAhmed = findViewById(R.id.buttonAhmed) ?: throw NullPointerException("buttonAhmed not found")
            Log.d("MainActivity", "buttonAhmed initialized: $buttonAhmed")

            Log.d("MainActivity", "Trying to initialize buttonAbdel")
            buttonAbdel = findViewById(R.id.buttonAbdel) ?: throw NullPointerException("buttonAbdel not found")
            Log.d("MainActivity", "buttonAbdel initialized: $buttonAbdel")

            Log.d("MainActivity", "Trying to initialize buttonKadir")
            buttonKadir = findViewById(R.id.buttonKadir) ?: throw NullPointerException("buttonKadir not found")
            Log.d("MainActivity", "buttonKadir initialized: $buttonKadir")

            // Initialiser les boutons de navigation
            Log.d("MainActivity", "Trying to initialize previousMonthButton")
            val previousMonthButton = findViewById<Button>(R.id.previousMonthButton) ?: throw NullPointerException("previousMonthButton not found")
            previousMonthButton.setOnClickListener {
                currentMonth.add(Calendar.MONTH, -1)
                updateCalendar()
            }
            Log.d("MainActivity", "previousMonthButton initialized: $previousMonthButton")

            Log.d("MainActivity", "Trying to initialize nextMonthButton")
            val nextMonthButton = findViewById<Button>(R.id.nextMonthButton) ?: throw NullPointerException("nextMonthButton not found")
            nextMonthButton.setOnClickListener {
                currentMonth.add(Calendar.MONTH, 1)
                updateCalendar()
            }
            Log.d("MainActivity", "nextMonthButton initialized: $nextMonthButton")

        } catch (e: NullPointerException) {
            Log.e("MainActivity", "Error in view initialization: ${e.message}")
            e.printStackTrace()
            // Gérer cette erreur comme vous le souhaitez (par exemple, afficher un message d'erreur)
        }

        // Continuez avec la configuration des écouteurs et des autres fonctionnalités
        // Exemple pour les boutons des coiffeurs
        buttonAhmed.setOnClickListener { setSelectedBarber(1) }
        buttonAbdel.setOnClickListener { setSelectedBarber(2) }
        buttonKadir.setOnClickListener { setSelectedBarber(3) }

        // Mettre à jour le calendrier avec le mois actuel
        updateCalendar()
    }

    private fun deleteSelectedAppointments() {
        deleteSelectedAppointments(selectedAppointments.toList()) {
            updateCalendar()
        }
    }

    private fun updateDeleteButtonState(dialog: Dialog) {
        val deleteButton = dialog.findViewById<Button>(R.id.deleteButton)
        val selected = selectedAppointments.isNotEmpty()
        deleteButton.isEnabled = selected
        deleteButton.setBackgroundColor(if (selected) Color.RED else Color.GRAY)
    }

    private fun deleteSelectedAppointments(appointments: List<Appointment>, onComplete: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                appointments.forEach { appointment ->
                    val response = RetrofitClient.apiService.deleteAppointment(appointment.appointment_id)
                    if (!response.isSuccessful) {
                        Log.e("DeleteAppointments", "Erreur HTTP: ${response.errorBody()?.string()}")
                    }
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Rendez-vous supprimés", Toast.LENGTH_SHORT).show()
                    onComplete()
                }
            } catch (e: JsonSyntaxException) { // Remplacez MalformedJsonException par JsonSyntaxException
                Log.e("DeleteAppointments", "Malformed JSON error", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Erreur JSON malformé lors de la suppression des rendez-vous", Toast.LENGTH_SHORT).show()
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

        appointmentsRecyclerView.layoutManager = LinearLayoutManager(this)
        appointmentsRecyclerView.setHasFixedSize(true)

        closeModalButton.setOnClickListener {
            dialog.dismiss()
        }

        fetchAppointments(date) { appointments ->
            val adapter = AppointmentsAdapter(
                appointments,
                onAppointmentLongClick = { appointment ->
                    updateDeleteButtonState(dialog)
                },
                onAppointmentClick = { appointment ->
                    updateDeleteButtonState(dialog)
                }
            )
            appointmentsRecyclerView.adapter = adapter  // Assurez-vous que l'adaptateur est attaché ici
            Log.d("MainActivity", "Adapter attached to RecyclerView")
        }

        updateDeleteButtonState(dialog)

        val window = dialog.window
        val layoutParams = window?.attributes
        layoutParams?.height = (resources.displayMetrics.heightPixels * 0.8).toInt()
        window?.attributes = layoutParams

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
    }
}