package com.example.barber.manage

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.barber.BlockedSlot
import com.example.barber.MainActivity
import com.example.barber.R
import com.example.barber.network.RetrofitClient
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ManageAvailability {

    private var currentWeekStart: Calendar = Calendar.getInstance()
    private var barberId: Int = 1  // ID du coiffeur par défaut

    fun handleAvailability(context: Context) {
        println("handleAvailability: Ouverture du dialogue")
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.dialog_manage_availability)

        val gridView = dialog.findViewById<GridLayout>(R.id.availabilityGrid)
        val weekTextView = dialog.findViewById<TextView>(R.id.weekTextView)
        val prevButton = dialog.findViewById<ImageButton>(R.id.prevWeekButton)
        val nextButton = dialog.findViewById<ImageButton>(R.id.nextWeekButton)

        val barberButtonLayout = dialog.findViewById<LinearLayout>(R.id.barberButtonLayout)
        barberButtonLayout?.removeAllViews()

// Récupérer les barbiers pour le salon
        val sharedPreferences = context.getSharedPreferences("MyApp", Context.MODE_PRIVATE)
        val salonId = sharedPreferences.getInt("salon_id", 0)

        (context as? MainActivity)?.lifecycleScope?.launch {
            try {
                val response = RetrofitClient.apiService.getBarbers(salonId.toString())
                if (response.isSuccessful) {
                    val barbers = response.body()?.barbers ?: emptyList()

                    barbers.forEach { barber ->
                        val barberButton = Button(context)
                        barberButton.text = barber.barber_name

                        barberButton.setOnClickListener {
                            setSelectedBarber(barber.barber_id, context, gridView)
                        }

                        barberButtonLayout.addView(barberButton)
                    }

                    // Définir le coiffeur par défaut (le premier récupéré depuis l'API)
                    if (barbers.isNotEmpty()) {
                        // Select the first barber and load its slots
                        setSelectedBarber(barbers[0].barber_id, context, gridView)
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Erreur lors de la récupération des barbiers", Toast.LENGTH_SHORT).show()
            }
        }

        // Mettre à jour le texte de la semaine affichée
        updateWeekText(weekTextView)

        val daysOfWeek = listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim")
        val hours = generateTimesForDay(9, 0, 19, 0)

        // Afficher les horaires pour la semaine en cours
        println("Initialisation de la grille de disponibilité")
        setupAvailabilityGrid(context, gridView, daysOfWeek, hours)

        // Navigation entre les semaines
        prevButton.setOnClickListener {
            println("Semaine précédente sélectionnée")
            changeWeek(-1, weekTextView, context, gridView, daysOfWeek, hours)
        }

        nextButton.setOnClickListener {
            println("Semaine suivante sélectionnée")
            changeWeek(1, weekTextView, context, gridView, daysOfWeek, hours)
        }

        dialog.show()
    }

    private fun setSelectedBarber(selectedBarberId: Int, context: Context, gridView: GridLayout) {
        println("setSelectedBarber: Sélection du coiffeur avec ID $selectedBarberId")

        // Update the barberId variable to the selected one
        barberId = selectedBarberId

        // Clear the existing blocked slots in the grid
        clearBlockedSlots(gridView)

        val daysOfWeek = listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim")
        val hours = generateTimesForDay(9, 0, 19, 0)

        // Reload the blocked slots for the newly selected barber
        loadBlockedSlotsAndApplyColors(context, gridView, daysOfWeek, hours)
    }

    private fun clearBlockedSlots(gridView: GridLayout) {
        for (i in 0 until gridView.childCount) {
            val button = gridView.getChildAt(i) as? Button
            button?.let {
                if (it.tag == "blocked") {
                    applyBorderWithBackground(it, Color.WHITE, Color.parseColor("#D3D3D3"), 2)
                    it.tag = null
                }
            }
        }
    }

    private fun loadBlockedSlotsAndApplyColors(
        context: Context,
        gridView: GridLayout,
        daysOfWeek: List<String>,
        hours: List<String>
    ) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val weekStart = getMonday(currentWeekStart)
        val weekEnd = getSunday(currentWeekStart)

        println("Appel à l'API pour les créneaux bloqués avec barberId = $barberId, start_date = ${dateFormat.format(weekStart.time)}, end_date = ${dateFormat.format(weekEnd.time)}")

        (context as? MainActivity)?.lifecycleScope?.launch {
            try {
                val sharedPreferences = context.getSharedPreferences("MyApp", Context.MODE_PRIVATE)
                val salonId = sharedPreferences.getInt("salon_id", 0).toString()
                val response = RetrofitClient.apiService.getBlockedSlotsForBarberAndSalon(
                    barberId,
                    dateFormat.format(weekStart.time),
                    dateFormat.format(weekEnd.time),
                    salonId
                )

                if (response.isSuccessful) {
                    val blockedSlots: List<BlockedSlot> = response.body() ?: emptyList()

                    // Add this log to check the retrieved data
                    Log.e("BlockedSlots", "Barber ID: $barberId, Salon ID: $salonId, Blocked Slots: $blockedSlots")

                    blockedSlots.forEach { slot: BlockedSlot ->
                        applyBlockedSlotColor(
                            gridView, daysOfWeek, hours, slot.appointment_date, slot.appointment_time, context
                        )
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(context, "Erreur: $errorBody", Toast.LENGTH_SHORT).show()
                    println("Erreur lors de la récupération des créneaux bloqués pour barberId $barberId: $errorBody")
                }
            } catch (e: Exception) {
                println("Erreur réseau lors de la récupération des créneaux bloqués : ${e.message}")
            }
        }
    }

    private fun applyBlockedSlotColor(gridView: GridLayout, daysOfWeek: List<String>, hours: List<String>, appointmentDate: String, appointmentTime: String, context: Context) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val slotDate = dateFormat.parse(appointmentDate) ?: return

        val calendar = Calendar.getInstance()
        calendar.time = slotDate

        val dayOfWeekIndex = calendar.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY
        val timeIndex = hours.indexOf(appointmentTime.substring(0, 5))

        if (dayOfWeekIndex in 0 until daysOfWeek.size && timeIndex != -1) {
            val buttonIndex = (timeIndex + 1) * gridView.columnCount + (dayOfWeekIndex + 1)
            val button = gridView.getChildAt(buttonIndex) as? Button
            button?.let {
                println("Application de la couleur rouge pour l'élément à la position $buttonIndex (Date: $appointmentDate, Time: $appointmentTime)")
                applyBorderWithBackground(it, Color.RED, Color.RED, 2)
                it.tag = "blocked"
            }
        } else {
            println("Erreur: Créneau bloqué non trouvé dans la grille (Date: $appointmentDate, Time: $appointmentTime)")
        }
    }

    private fun setupAvailabilityGrid(context: Context, gridView: GridLayout, daysOfWeek: List<String>, hours: List<String>) {
        gridView.removeAllViews()

        gridView.columnCount = daysOfWeek.size + 1
        gridView.rowCount = hours.size + 1

        val cellWidth = 120
        val cellHeight = 40

        val borderColor = Color.parseColor("#D3D3D3")
        val white = Color.parseColor("#FFFFFF")

        val emptyCell = TextView(context).apply {
            layoutParams = GridLayout.LayoutParams().apply {
                width = cellWidth
                height = cellHeight
            }
        }
        applyBorderWithBackground(emptyCell, white, borderColor, 2)
        gridView.addView(emptyCell)

        daysOfWeek.forEachIndexed { index, day ->
            val dayLabel = TextView(context).apply {
                text = day
                gravity = Gravity.CENTER
                setTextColor(Color.BLACK)
                layoutParams = GridLayout.LayoutParams().apply {
                    width = cellWidth
                    height = cellHeight
                    columnSpec = GridLayout.spec(index + 1)
                    rowSpec = GridLayout.spec(0)
                }
            }
            applyBorderWithBackground(dayLabel, white, borderColor, 2)
            gridView.addView(dayLabel)
        }

        hours.forEachIndexed { index, hour ->
            val hourLabel = TextView(context).apply {
                text = hour
                gravity = Gravity.CENTER
                setTextColor(Color.BLACK)
                layoutParams = GridLayout.LayoutParams().apply {
                    width = cellWidth
                    height = cellHeight
                    columnSpec = GridLayout.spec(0)
                    rowSpec = GridLayout.spec(index + 1)
                }
            }
            applyBorderWithBackground(hourLabel, white, borderColor, 2)
            gridView.addView(hourLabel)

            daysOfWeek.forEachIndexed { dayIndex, _ ->
                val timeButton = Button(context).apply {
                    text = ""
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = cellWidth
                        height = cellHeight
                        columnSpec = GridLayout.spec(dayIndex + 1)
                        rowSpec = GridLayout.spec(index + 1)
                    }
                    applyBorderWithBackground(this, white, borderColor, 2)
                    setOnClickListener {
                        handleTimeSlotClick(dayIndex, hour, this, context)
                    }
                }
                gridView.addView(timeButton)
            }
        }
    }

    private fun handleTimeSlotClick(dayIndex: Int, hour: String, button: Button, context: Context) {
        // Calculer la date exacte du créneau sélectionné en fonction de la semaine actuelle et du jour
        val monday = getMonday(currentWeekStart)  // Récupérer le lundi de la semaine actuelle
        val selectedDate = monday.clone() as Calendar
        selectedDate.add(Calendar.DAY_OF_YEAR, dayIndex)  // Ajouter le jour sélectionné (dayIndex)

        // Formater la date pour la passer à l'API
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val selectedDateString = dateFormat.format(selectedDate.time)

        // Si le créneau est bloqué (rouge), on le débloque (blanc)
        if (button.tag == "blocked") {
            unblockSlotInDatabase(selectedDateString, hour, barberId, button, context)
            println("Déblocage du créneau : Date=$selectedDateString, Heure=$hour, BarberID=$barberId")
        }
        // Si le créneau est libre (blanc), on le bloque (rouge)
        else {
            blockSlotInDatabase(selectedDateString, hour, barberId, button, context)
            println("Blocage du créneau : Date=$selectedDateString, Heure=$hour, BarberID=$barberId")
        }
    }

    private fun generateTimesForDay(startHour: Int, startMinute: Int, endHour: Int, endMinute: Int): List<String> {
        val times = mutableListOf<String>()
        var currentHour = startHour
        var currentMinute = startMinute

        while (currentHour < endHour || (currentHour == endHour && currentMinute <= endMinute)) {
            times.add(String.format("%02d:%02d", currentHour, currentMinute))
            currentMinute += 30
            if (currentMinute >= 60) {
                currentMinute = 0
                currentHour++
            }
        }

        return times
    }

    private fun updateWeekText(weekTextView: TextView) {
        val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
        val weekStart = getMonday(currentWeekStart)
        val weekEnd = getSunday(weekStart)

        val weekRangeText = "${dateFormat.format(weekStart.time)} - ${dateFormat.format(weekEnd.time)}"
        weekTextView.text = weekRangeText
    }

    private fun getMonday(currentWeek: Calendar): Calendar {
        val monday = currentWeek.clone() as Calendar
        monday.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        monday.set(Calendar.HOUR_OF_DAY, 0)
        monday.set(Calendar.MINUTE, 0)
        monday.set(Calendar.SECOND, 0)
        monday.set(Calendar.MILLISECOND, 0)
        return monday
    }

    private fun getSunday(currentWeek: Calendar): Calendar {
        val sunday = currentWeek.clone() as Calendar
        sunday.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        sunday.add(Calendar.DATE, 7) // S'assure que c'est le dernier jour de la semaine
        sunday.set(Calendar.HOUR_OF_DAY, 23)
        sunday.set(Calendar.MINUTE, 59)
        sunday.set(Calendar.SECOND, 59)
        sunday.set(Calendar.MILLISECOND, 999)
        return sunday
    }

    private fun changeWeek(offset: Int, weekTextView: TextView, context: Context, gridView: GridLayout, daysOfWeek: List<String>, hours: List<String>) {
        currentWeekStart.add(Calendar.WEEK_OF_YEAR, offset)
        updateWeekText(weekTextView)
        setupAvailabilityGrid(context, gridView, daysOfWeek, hours)
        loadBlockedSlotsAndApplyColors(context, gridView, daysOfWeek, hours) // Recharge les créneaux bloqués pour la nouvelle semaine
    }

    private fun blockSlotInDatabase(date: String, time: String, barberId: Int, button: Button, context: Context) {
        (context as? MainActivity)?.lifecycleScope?.launch {
            try {
                val sharedPreferences = context.getSharedPreferences("MyApp", Context.MODE_PRIVATE)
                val salonId = sharedPreferences.getInt("salon_id", 0) // Retrieve salon_id

                val response = RetrofitClient.apiService.blockSlot(barberId, salonId, date, time) // Pass salon_id
                if (response.isSuccessful) {
                    button.tag = "blocked"
                    applyBorderWithBackground(button, Color.RED, Color.RED, 2)
                    Toast.makeText(context, "Créneau bloqué", Toast.LENGTH_SHORT).show()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(context, "Erreur: $errorBody", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Erreur de réseau", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun unblockSlotInDatabase(date: String, time: String, barberId: Int, button: Button, context: Context) {
        (context as? MainActivity)?.lifecycleScope?.launch {
            try {
                val sharedPreferences = context.getSharedPreferences("MyApp", Context.MODE_PRIVATE)
                val salonId = sharedPreferences.getInt("salon_id", 0)  // Retrieve salon_id

                val response = RetrofitClient.apiService.unblockSlot(barberId, salonId, date, time) // Pass salon_id
                if (response.isSuccessful) {
                    button.tag = "unblocked"
                    applyBorderWithBackground(button, Color.WHITE, Color.WHITE, 2)
                    Toast.makeText(context, "Créneau débloqué", Toast.LENGTH_SHORT).show()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(context, "Erreur: $errorBody", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Erreur de réseau", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun applyBorderWithBackground(view: View, backgroundColor: Int, borderColor: Int, borderWidth: Int) {
        val shape = GradientDrawable()
        shape.setColor(backgroundColor)
        shape.setStroke(borderWidth, borderColor)
        view.background = shape
    }
}