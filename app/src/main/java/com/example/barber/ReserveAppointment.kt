package com.example.barber

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*
import com.example.barber.models.AppointmentRequest
import com.example.barber.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ReserveAppointment {

    fun openAppointmentForm(context: Context) {
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.dialog_appointment_form)

        val editTextClientName = dialog.findViewById<EditText>(R.id.editTextClientName).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setAutofillHints("")
            }
            setTextIsSelectable(false)
            isFocusable = true
            isFocusableInTouchMode = true
            inputType = android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        }
        val spinnerBarber = dialog.findViewById<Spinner>(R.id.spinnerBarber)
        val buttonSelectDate = dialog.findViewById<Button>(R.id.buttonSelectDate)
        val buttonSelectTime = dialog.findViewById<Button>(R.id.buttonSelectTime)
        val spinnerService = dialog.findViewById<Spinner>(R.id.spinnerService)
        val radioGroupPayment = dialog.findViewById<RadioGroup>(R.id.radioGroupPayment)
        val buttonReserve = dialog.findViewById<ImageButton>(R.id.buttonReserveAppointment)
        val buttonValidate = dialog.findViewById<Button>(R.id.buttonValidateReservation)

        // Initialisation des données pour les coiffeurs
        val barbers = arrayOf("Ahmed", "Abdel", "Kadir")
        val barberAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, barbers)
        barberAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerBarber.adapter = barberAdapter

        // Activation du bouton de date après sélection du coiffeur
        spinnerBarber.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                buttonSelectDate.isEnabled = true
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Ouvrir le datepicker personnalisé
        buttonSelectDate.setOnClickListener {
            showCustomDatePicker(context) { selectedDate ->
                buttonSelectDate.text = selectedDate
                buttonSelectTime.isEnabled = true
            }
        }

        // Ouvrir le timepicker personnalisé
        buttonSelectTime.setOnClickListener {
            val selectedDate = buttonSelectDate.text.toString() // Récupère la date sélectionnée
            val selectedBarber = spinnerBarber.selectedItem.toString() // Récupère le nom du coiffeur

            // Mapper le nom du coiffeur à un ID
            val barberId = when (selectedBarber) {
                "Ahmed" -> 1
                "Abdel" -> 2
                "Kadir" -> 3
                else -> 0
            }

            // Appelle la fonction avec les paramètres requis
            showCustomTimePicker(context, selectedDate, barberId) { selectedTime ->
                buttonSelectTime.text = selectedTime
            }
        }

        // Initialisation des services
        val services = arrayOf("Coupe Homme", "Coupe ado", "Coupe enfant", "FORFAIT coupe + barbe")
        val serviceAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, services)
        serviceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerService.adapter = serviceAdapter

        // Logique commune pour réserver un rendez-vous
        val reserveAppointment = {
            val clientName = editTextClientName.text.toString()
            val selectedBarber = spinnerBarber.selectedItem.toString()
            val selectedDate = buttonSelectDate.text.toString()
            val selectedTime = buttonSelectTime.text.toString()
            val selectedService = spinnerService.selectedItem.toString()
            val selectedPaymentMethod = when (radioGroupPayment.checkedRadioButtonId) {
                R.id.radioCash -> "Cash"
                R.id.radioCard -> "Card"
                else -> ""
            }

            if (clientName.isEmpty() || selectedPaymentMethod.isEmpty()) {
                Toast.makeText(context, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
            } else {
                saveAppointment(
                    context,
                    clientName,
                    selectedBarber,
                    selectedDate,
                    selectedTime,
                    selectedService,
                    selectedPaymentMethod
                ) {
                    Toast.makeText(context, "Rendez-vous réservé avec succès", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            }
        }

        buttonReserve?.setOnClickListener {
            reserveAppointment()
        }

        buttonValidate?.setOnClickListener {
            reserveAppointment()
        }

        dialog.show()
    }

    private fun saveAppointment(
        context: Context,
        clientName: String,
        barber: String,
        date: String,
        time: String,
        service: String,
        paymentMethod: String,
        onComplete: () -> Unit
    ) {
        val appointmentRequest = AppointmentRequest(
            client_name = clientName,
            barber_id = when (barber) {
                "Ahmed" -> 1
                "Abdel" -> 2
                "Kadir" -> 3
                else -> 0
            },
            service_id = when (service) {
                "Coupe Homme" -> 1
                "Coupe ado" -> 2
                "Coupe enfant" -> 3
                "FORFAIT coupe + barbe" -> 4
                else -> 0
            },
            appointment_date = date,
            appointment_time = time,
            payment_method = paymentMethod
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.createAppointment(appointmentRequest)
                if (response.isSuccessful) {
                    val responseBody = response.body()?.string()

                    if (responseBody?.contains("success", ignoreCase = true) == true) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Rendez-vous réservé avec succès", Toast.LENGTH_SHORT).show()
                            onComplete()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Erreur lors de la réservation, réponse inattendue du serveur", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Erreur lors de la réservation", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Erreur lors de la réservation", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showCustomTimePicker(
        context: Context,
        selectedDate: String,
        barberId: Int,
        onTimeSelected: (String) -> Unit
    ) {
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.custom_time_picker_dialog)

        val gridView = dialog.findViewById<GridLayout>(R.id.timePickerGrid)
        val confirmButton = dialog.findViewById<Button>(R.id.buttonConfirmTime)

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val selectedCalendar = Calendar.getInstance()
        selectedCalendar.time = sdf.parse(selectedDate)!!

        val dayOfWeek = selectedCalendar.get(Calendar.DAY_OF_WEEK)

        // Déterminer les créneaux horaires en fonction du jour de la semaine
        val times = when (dayOfWeek) {
            Calendar.SATURDAY -> generateTimesForDay(9, 0, 19, 0) // De 9:00 à 19:00
            Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY -> generateTimesForDay(9, 30, 18, 30) // De 8:30 à 18:30
            else -> emptyList() // Autres jours, pas d'horaires disponibles
        }

        // Récupération des rendez-vous et créneaux bloqués
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appointmentsResponse = RetrofitClient.apiService.getAppointments(selectedDate, barberId)
                val blockedSlotsResponse = RetrofitClient.apiService.getBlockedSlots(
                    barberId,
                    selectedDate, // start_date
                    selectedDate  // end_date (même jour)
                )

                if (appointmentsResponse.isSuccessful && blockedSlotsResponse.isSuccessful) {
                    val appointments = appointmentsResponse.body()?.appointments ?: emptyList()
                    val blockedSlots = blockedSlotsResponse.body()?.blocked_slots ?: emptyList()

                    // Liste des heures déjà prises (format "HH:mm")
                    val takenTimes = appointments.map { it.appointment_time.substring(0, 5) } +
                            blockedSlots.map { it.appointment_time.substring(0, 5) }

                    withContext(Dispatchers.Main) {
                        gridView.removeAllViews()

                        times.forEach { time ->
                            val timeButton = Button(context).apply {
                                text = time
                                layoutParams = GridLayout.LayoutParams().apply {
                                    width = 0
                                    height = GridLayout.LayoutParams.WRAP_CONTENT
                                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                                    setMargins(8, 8, 8, 8)
                                }

                                if (takenTimes.contains(time)) {
                                    paintFlags = paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                                    isEnabled = false
                                }

                                setOnClickListener {
                                    onTimeSelected(time)
                                    dialog.dismiss()
                                }
                            }
                            gridView.addView(timeButton)
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Erreur lors de la récupération des créneaux", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Erreur réseau : ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        dialog.show()
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

    fun showCustomDatePicker(context: Context, onDateSelected: (String) -> Unit) {
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.custom_datepicker_dialog)

        val gridView = dialog.findViewById<GridLayout>(R.id.datePickerGrid)
        val confirmButton = dialog.findViewById<Button>(R.id.buttonConfirmDate)
        val currentMonth = Calendar.getInstance()

        // Boutons pour naviguer dans les mois
        val previousMonthButton = dialog.findViewById<Button>(R.id.buttonPreviousMonth)
        Log.d("ReserveAppointment", "Previous Month Button: $previousMonthButton")

        val nextMonthButton = dialog.findViewById<Button>(R.id.buttonNextMonth)
        Log.d("ReserveAppointment", "Next Month Button: $nextMonthButton")


        previousMonthButton.setOnClickListener {
            currentMonth.add(Calendar.MONTH, -1)
            updateCalendarGrid(context, gridView, currentMonth, onDateSelected, dialog)
        }

        nextMonthButton.setOnClickListener {
            currentMonth.add(Calendar.MONTH, 1)
            updateCalendarGrid(context, gridView, currentMonth, onDateSelected, dialog)
        }

        gridView.removeAllViews()

        // Ajout des jours de la semaine
        val daysOfWeek = listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim")
        daysOfWeek.forEach { day ->
            val dayLabel = TextView(context).apply {
                text = day
                gravity = Gravity.CENTER
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                }
            }
            gridView.addView(dayLabel)
        }

        // Génération des numéros de jours pour le mois actuel
        val currentDay = currentMonth.get(Calendar.DAY_OF_MONTH)
        val daysInMonth = currentMonth.getActualMaximum(Calendar.DAY_OF_MONTH)

        for (day in 1..daysInMonth) {
            val dayButton = Button(context).apply {
                text = day.toString()
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(8, 8, 8, 8) // Espacement entre les boutons
                }

                // Désactive les jours passés et le Lundi et Dimanche
                val dayOfWeek = currentMonth.apply { set(Calendar.DAY_OF_MONTH, day) }.get(Calendar.DAY_OF_WEEK)
                isEnabled = day >= currentDay && dayOfWeek != Calendar.SUNDAY && dayOfWeek != Calendar.MONDAY

                setOnClickListener {
                    val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(currentMonth.apply {
                        set(Calendar.DAY_OF_MONTH, day)
                    }.time)
                    onDateSelected(formattedDate)
                    dialog.dismiss()
                }
            }
            gridView.addView(dayButton)
        }

        confirmButton.setOnClickListener {
            // Confirmation supplémentaire si nécessaire
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun generateTimesForPicker(): List<String> {
        val times = mutableListOf<String>()
        for (hour in 0..23) {
            for (minute in listOf(0, 15, 30, 45)) {
                times.add(String.format("%02d:%02d:00", hour, minute))
            }
        }
        return times
    }

    private fun generateDatesForCurrentMonth(): List<String> {
        val dates = mutableListOf<String>()
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        for (day in 1..daysInMonth) {
            dates.add(day.toString())
        }

        return dates
    }

    private fun updateCalendarGrid(
        context: Context,
        gridView: GridLayout,
        calendar: Calendar,
        onDateSelected: (String) -> Unit,
        dialog: Dialog
    ) {
        gridView.removeAllViews()

        // Obtenir le TextView pour afficher le mois et l'année
        val monthYearTextView = dialog.findViewById<TextView>(R.id.textViewMonthYear)
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        monthYearTextView.text = sdf.format(calendar.time)

        // Ajout des jours de la semaine
        val daysOfWeek = listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim")
        daysOfWeek.forEach { day ->
            val dayLabel = TextView(context).apply {
                text = day
                gravity = Gravity.CENTER
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                }
            }
            gridView.addView(dayLabel)
        }

        // Calculer le jour de la semaine du premier jour du mois
        val firstDayOfMonth = calendar.clone() as Calendar
        firstDayOfMonth.set(Calendar.DAY_OF_MONTH, 1)
        var firstDayOfWeek = firstDayOfMonth.get(Calendar.DAY_OF_WEEK)
        firstDayOfWeek = if (firstDayOfWeek == Calendar.SUNDAY) 7 else firstDayOfWeek - 1 // Ajuster pour que lundi = 1 et dimanche = 7

        // Ajouter des jours vides avant le début du mois
        for (i in 1 until firstDayOfWeek) {
            val emptyButton = Button(context).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(8, 8, 8, 8)
                }
                isEnabled = false
            }
            gridView.addView(emptyButton)
        }

        // Récupérer le mois et l'année actuels
        val today = Calendar.getInstance()
        val isCurrentMonth = today.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
                today.get(Calendar.MONTH) == calendar.get(Calendar.MONTH)

        val currentDay = if (isCurrentMonth) today.get(Calendar.DAY_OF_MONTH) else 1
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        for (day in 1..daysInMonth) {
            val dayButton = Button(context).apply {
                text = day.toString()
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(8, 8, 8, 8)
                }

                // Ajuster le calendrier pour le jour spécifique
                val dayOfMonth = calendar.clone() as Calendar
                dayOfMonth.set(Calendar.DAY_OF_MONTH, day)
                val dayOfWeek = dayOfMonth.get(Calendar.DAY_OF_WEEK)

                // Désactiver les jours passés et bloquer le lundi et le dimanche
                isEnabled = (day >= currentDay || !isCurrentMonth) &&
                        dayOfWeek != Calendar.SUNDAY &&
                        dayOfWeek != Calendar.MONDAY

                setOnClickListener {
                    val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(dayOfMonth.time)
                    onDateSelected(formattedDate)
                    dialog.dismiss()
                }
            }
            gridView.addView(dayButton)
        }
    }
}