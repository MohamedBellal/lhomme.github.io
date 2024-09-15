package com.example.barber

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
import com.example.barber.adapters.AppointmentsAdapter
import com.example.barber.models.Appointment
import com.example.barber.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.*
private var appointments = mutableListOf<Appointment>() // Define this at the class level


class MainActivity : AppCompatActivity() {

    private lateinit var calendarGrid: GridLayout
    private lateinit var monthYearText: TextView
    private lateinit var buttonAhmed: Button
    private lateinit var buttonAbdel: Button
    private lateinit var buttonKadir: Button
    private var currentMonth = Calendar.getInstance()
    private var selectedBarberId = 1 // Coiffeur par défaut

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialisation des vues en utilisant les bons IDs
        calendarGrid = findViewById(R.id.calendarGrid)
        monthYearText = findViewById(R.id.monthYearText)
        buttonAhmed = findViewById(R.id.buttonAhmed)
        buttonAbdel = findViewById(R.id.buttonAbdel)
        buttonKadir = findViewById(R.id.buttonKadir)

        // Initialisation du coiffeur par défaut
        setSelectedBarber(selectedBarberId)

        // Gestion des clics sur les boutons
        buttonAhmed.setOnClickListener { setSelectedBarber(1) }
        buttonAbdel.setOnClickListener { setSelectedBarber(2) }
        buttonKadir.setOnClickListener { setSelectedBarber(3) }

        findViewById<Button>(R.id.previousMonthButton).setOnClickListener {
            currentMonth.add(Calendar.MONTH, -1)
            updateCalendar()
        }

        findViewById<Button>(R.id.nextMonthButton).setOnClickListener {
            currentMonth.add(Calendar.MONTH, 1)
            updateCalendar()
        }

        updateCalendar()  // Initialiser l'affichage du calendrier avec le mois actuel
    }

    private fun setSelectedBarber(barberId: Int) {
        selectedBarberId = barberId
        resetButtonColors()
        when (barberId) {
            1 -> {
                buttonAhmed.setBackgroundResource(R.drawable.button_selected)
                buttonAhmed.isSelected = true
            }
            2 -> {
                buttonAbdel.setBackgroundResource(R.drawable.button_selected)
                buttonAbdel.isSelected = true
            }
            3 -> {
                buttonKadir.setBackgroundResource(R.drawable.button_selected)
                buttonKadir.isSelected = true
            }
        }

        // Met à jour les données du calendrier pour le coiffeur sélectionné
        updateCalendarForSelectedBarber(barberId)
    }

    private fun resetButtonColors() {
        buttonAhmed.setBackgroundResource(R.drawable.button_unselected)
        buttonAhmed.isSelected = false
        buttonAbdel.setBackgroundResource(R.drawable.button_unselected)
        buttonAbdel.isSelected = false
        buttonKadir.setBackgroundResource(R.drawable.button_unselected)
        buttonKadir.isSelected = false
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

    @SuppressLint("MissingInflatedId")
    private fun addDayToCalendar(gridLayout: GridLayout, day: Int) {
        // Create a RelativeLayout for each day to hold the button and the notification badge
        val dayLayout = RelativeLayout(this)
        dayLayout.layoutParams = GridLayout.LayoutParams().apply {
            width = 0
            height = 0
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            setMargins(4, 4, 4, 4)
        }

        // Create the button for the day
        val dayButton = Button(this)
        dayButton.text = day.toString()
        dayButton.layoutParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT
        )
        dayButton.textSize = 16f
        dayButton.setPadding(8, 8, 8, 8) // Adjust padding for better fitting
        dayButton.setBackgroundResource(R.drawable.calendar_background)
        dayButton.setTextColor(Color.BLACK)
        dayButton.setOnClickListener {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(currentMonth.apply { set(Calendar.DAY_OF_MONTH, day) }.time)
            openAppointmentDialog(date)
        }

        // Fetch the appointments for the current date
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(currentMonth.apply { set(Calendar.DAY_OF_MONTH, day) }.time)
        val appointmentsForDay = appointments.filter { it.appointment_date == date }

        // Add the notification badge if there are appointments
        if (appointmentsForDay.isNotEmpty()) {
            val notificationBadge = TextView(this)
            notificationBadge.text = appointmentsForDay.size.toString()
            notificationBadge.setBackgroundResource(R.drawable.notification_badge_background)
            notificationBadge.setTextColor(Color.WHITE)
            notificationBadge.textSize = 12f
            notificationBadge.setPadding(4, 2, 4, 2)

            // Position the badge in the top-right corner
            val badgeParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            )
            badgeParams.addRule(RelativeLayout.ALIGN_PARENT_TOP)
            badgeParams.addRule(RelativeLayout.ALIGN_PARENT_END)
            badgeParams.setMargins(0, 4, 4, 0) // Adjust the margin for positioning
            notificationBadge.layoutParams = badgeParams

            // Add the badge to the day layout
            dayLayout.addView(notificationBadge)
        }

        // Add the day button to the day layout
        dayLayout.addView(dayButton)

        // Add the day layout to the GridLayout
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

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        fetchAppointments(date) { appointments ->
            val adapter = AppointmentsAdapter(appointments)
            appointmentsRecyclerView.adapter = adapter
        }

        dialog.show()
    }

    private fun updateCalendarForSelectedBarber(barberId: Int) {
        fetchAppointmentsForBarber(barberId) { appointments ->
            // Mettre à jour le calendrier avec les rendez-vous filtrés
        }
    }

    private fun fetchAppointmentsForBarber(barberId: Int, callback: (List<Appointment>) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("FetchAppForBarber", "Fetching appointments for barber with ID: $barberId")
                // Ensure the correct method and parameters
                val response = RetrofitClient.apiService.getAppointments(
                    date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(currentMonth.time),
                    barberId = barberId
                )
                Log.d("FetchAppForBarber", "Response: $response")

                withContext(Dispatchers.Main) {
                    callback(response)
                }
            } catch (e: HttpException) {
                Log.e("FetchAppForBarber", "HTTP error code: ${e.code()}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Erreur HTTP: ${e.code()} lors de la récupération des rendez-vous", Toast.LENGTH_SHORT).show()
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
                val response = RetrofitClient.apiService.getAppointments(date, selectedBarberId) // Ensure apiService is used correctly
                withContext(Dispatchers.Main) {
                    callback(response)
                }
            } catch (e: Exception) {
                Log.e("FetchAppointments", "Erreur lors de la récupération des rendez-vous", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Erreur lors de la récupération des rendez-vous", Toast.LENGTH_SHORT).show()
                    callback(emptyList()) // Return an empty list in case of error
                }
            }
        }
    }
}
