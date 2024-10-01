package com.example.barber.manage

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.example.barber.Barber
import com.example.barber.R
import com.example.barber.Service
import com.example.barber.models.Appointment
import com.example.barber.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class ReserveAppointment {

    private var selectedBarberId: Int = 0 // To store the selected barber's ID

    fun openAppointmentForm(context: Context) {
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.dialog_appointment_form)

        val editTextClientName = dialog.findViewById<EditText>(R.id.editTextClientName).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
            }
            setTextIsSelectable(false)
            isFocusable = true
            isFocusableInTouchMode = true
        }
        val spinnerBarber = dialog.findViewById<Spinner>(R.id.spinnerBarber)
        val buttonSelectDate = dialog.findViewById<Button>(R.id.buttonSelectDate)
        val buttonSelectTime = dialog.findViewById<Button>(R.id.buttonSelectTime)
        val spinnerService = dialog.findViewById<Spinner>(R.id.spinnerService)
        val radioGroupPayment = dialog.findViewById<RadioGroup>(R.id.radioGroupPayment)
        val buttonReserve = dialog.findViewById<Button>(R.id.buttonValidateReservation)
        val buttonValidate = dialog.findViewById<Button>(R.id.buttonValidateReservation)

        // Fetch barbers dynamically
        fetchBarbersForSalon(context, spinnerBarber)

        // Fetch services dynamically
        fetchServicesForSalon(context, spinnerService)

        // Activate the date button after a barber is selected
        spinnerBarber.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedBarberId = (spinnerBarber.selectedItem as Barber).barber_id
                buttonSelectDate.isEnabled = true
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Open custom datepicker
        buttonSelectDate.setOnClickListener {
            showCustomDatePicker(context) { selectedDate ->
                buttonSelectDate.text = selectedDate
                buttonSelectTime.isEnabled = true
            }
        }

        // Open custom timepicker
        buttonSelectTime.setOnClickListener {
            val selectedDate = buttonSelectDate.text.toString()
            showCustomTimePicker(context, selectedDate, selectedBarberId) { selectedTime ->
                buttonSelectTime.text = selectedTime
            }
        }

        // Logic to reserve an appointment
        val reserveAppointment = {
            val clientName = editTextClientName.text.toString()
            val selectedDate = buttonSelectDate.text.toString()
            val selectedTime = buttonSelectTime.text.toString()

            // Récupérer l'objet Service sélectionné
            val selectedService = spinnerService.selectedItem as Service  // Récupère l'objet Service
            val serviceId = selectedService.service_id  // Récupère l'ID du service
            Log.e("ReserveAppointment", "Selected Service ID: $serviceId")

            val selectedPaymentMethod = when (radioGroupPayment.checkedRadioButtonId) {
                R.id.radioCash -> "Cash"
                R.id.radioCard -> "Card"
                else -> ""
            }

            if (clientName.isEmpty() || selectedPaymentMethod.isEmpty()) {
                Toast.makeText(context, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
            } else {
                saveAppointment(
                    context, clientName, selectedBarberId, selectedDate, selectedTime,
                    serviceId.toString(), selectedPaymentMethod
                ) {
                    Toast.makeText(context, "Rendez-vous réservé avec succès", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            }
        }

        buttonReserve?.setOnClickListener { reserveAppointment() }
        buttonValidate?.setOnClickListener { reserveAppointment() }

        dialog.show()
    }

    private fun fetchServicesForSalon(context: Context, spinnerService: Spinner) {
        val sharedPreferences = context.getSharedPreferences("MyApp", Context.MODE_PRIVATE)
        val salonId = sharedPreferences.getInt("salon_id", 0).toString()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.getServices(salonId.toInt())
                if (response.isSuccessful) {
                    val services = response.body()?.services ?: emptyList()

                    withContext(Dispatchers.Main) {
                        // Adapter pour afficher les services
                        val serviceAdapter = object : ArrayAdapter<Service>(
                            context, android.R.layout.simple_spinner_item, services
                        ) {
                            override fun getDropDownView(
                                position: Int, convertView: View?, parent: ViewGroup
                            ): View {
                                val view = super.getDropDownView(position, convertView, parent)
                                val textView = view as TextView
                                textView.text = getItem(position)?.service_name // Affiche le nom du service
                                return view
                            }

                            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                                val view = super.getView(position, convertView, parent)
                                val textView = view as TextView
                                textView.text = getItem(position)?.service_name // Affiche le nom du service dans le Spinner
                                return view
                            }
                        }
                        serviceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        spinnerService.adapter = serviceAdapter
                    }
                } else {
                    Log.e("ReserveAppointment", "Erreur lors de la récupération des services: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("ReserveAppointment", "Erreur réseau: ${e.message}")
            }
        }
    }

    private fun fetchBarbersForSalon(context: Context, spinnerBarber: Spinner) {
        // Get the salon_id from SharedPreferences
        val sharedPreferences = context.getSharedPreferences("MyApp", Context.MODE_PRIVATE)
        val salonId = sharedPreferences.getInt("salon_id", 0).toString()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.getBarbers(salonId)
                if (response.isSuccessful) {
                    val barbers = response.body()?.barbers ?: emptyList()
                    withContext(Dispatchers.Main) {
                        val barberAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, barbers)
                        barberAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        spinnerBarber.adapter = barberAdapter
                    }
                } else {
                    Log.e("ReserveAppointment", "Erreur lors de la récupération des barbiers: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("ReserveAppointment", "Erreur réseau: ${e.message}")
            }
        }
    }

    private fun saveAppointment(
        context: Context,
        clientName: String,
        barberId: Int,
        date: String,
        time: String,
        serviceId: String,  // Assure-toi que c'est bien le service ID réel
        paymentMethod: String,
        onComplete: () -> Unit
    ) {
        // Récupérer le salon_id des SharedPreferences
        val sharedPreferences = context.getSharedPreferences("MyApp", Context.MODE_PRIVATE)
        val salonId = sharedPreferences.getInt("salon_id", 0)

        // Créer l'objet Appointment directement avec l'ID du service récupéré
        val appointment = Appointment(
            appointment_id = null,
            client_name = clientName,
            appointment_date = date,
            appointment_time = time,
            service_id = serviceId.toInt(),
            barber_id = barberId,
            salon_id = salonId,
            appointment_status = "COMPLETED",
            payment_method = paymentMethod
        )

        Log.e("SaveAppointment", "Service ID envoyé: $serviceId")
        Log.e("SaveAppointment", "Requête envoyée: $appointment")

        // Envoyer la requête à l'API
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.createAppointment(appointment)
                if (response.isSuccessful) {
                    withContext(Dispatchers.Main) { onComplete() }
                } else {
                    // Log l'erreur de l'API si la requête échoue
                    val errorBody = response.errorBody()?.string()
                    Log.e("API Error", "Erreur lors de la réservation: $errorBody")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Erreur lors de la réservation: $errorBody", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                // Log l'exception
                Log.e("API Exception", "Exception lors de la réservation: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Erreur lors de la réservation: ${e.message}", Toast.LENGTH_SHORT).show()
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

        // Define available times based on the day of the week
        val times = when (dayOfWeek) {
            Calendar.SATURDAY -> generateTimesForDay(9, 0, 19, 0) // 9:00 to 19:00
            Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY -> generateTimesForDay(
                9, 30, 18, 30
            ) // 9:30 to 18:30
            else -> emptyList() // No available times for other days
        }

        // Fetch appointments and blocked slots for the selected barber and salon
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Fetch the salon_id from SharedPreferences
                val sharedPreferences = context.getSharedPreferences("MyApp", Context.MODE_PRIVATE)
                val salonId = sharedPreferences.getInt("salon_id", 0).toString()

                // Retrieve appointments
                val appointmentsResponse = RetrofitClient.apiService.getAppointmentsForBarberAndSalon(
                    selectedDate, barberId, salonId
                )

                // Retrieve blocked slots
                val blockedSlotsResponse = RetrofitClient.apiService.getBlockedSlotsForBarberAndSalon(
                    barberId, selectedDate, selectedDate, salonId
                )

                if (appointmentsResponse.isSuccessful && blockedSlotsResponse.isSuccessful) {
                    val appointments = appointmentsResponse.body()?.appointments ?: emptyList()
                    val blockedSlots = blockedSlotsResponse.body() ?: emptyList() // Directly use the response as a list

                    // Combine taken times from appointments and blocked slots
                    val takenTimes = appointments.mapNotNull { it.appointment_time?.substring(0, 5) } +
                            blockedSlots.mapNotNull { it.appointment_time?.substring(0, 5) }

                    withContext(Dispatchers.Main) {
                        gridView.removeAllViews()

                        // Display time slots and disable taken times
                        times.forEach { time ->
                            val timeButton = Button(context).apply {
                                text = time
                                layoutParams = GridLayout.LayoutParams().apply {
                                    width = 0
                                    height = GridLayout.LayoutParams.WRAP_CONTENT
                                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                                    setMargins(8, 8, 8, 8) // Set button margins
                                }

                                // Disable button and strike through text if the time is taken
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

        confirmButton.setOnClickListener {
            dialog.dismiss()
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
                val dayOfWeek =
                    currentMonth.apply { set(Calendar.DAY_OF_MONTH, day) }.get(Calendar.DAY_OF_WEEK)
                isEnabled =
                    day >= currentDay && dayOfWeek != Calendar.SUNDAY && dayOfWeek != Calendar.MONDAY

                setOnClickListener {
                    val formattedDate = SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.getDefault()
                    ).format(currentMonth.apply {
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
