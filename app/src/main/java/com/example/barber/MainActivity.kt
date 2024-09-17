package com.example.barber

import com.example.barber.adapters.AppointmentsAdapter
import com.example.barber.adapters.SelectedAppointmentsAdapter
import com.example.barber.models.Appointment
import com.example.barber.models.AppointmentRequest
import com.example.barber.network.RetrofitClient
import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

        // Pour les ImageButton
        val buttonReserveAppointment = findViewById<ImageButton>(R.id.buttonReserveAppointment)
        buttonReserveAppointment.setOnClickListener {
            openAppointmentForm()
        }

        val buttonManageFinances = findViewById<ImageButton>(R.id.buttonManageFinances)
        buttonManageFinances.setOnClickListener {
            // Logique pour gérer les finances
        }

        // Pour les Buttons
        buttonAhmed = findViewById<Button>(R.id.buttonAhmed)
        buttonAbdel = findViewById<Button>(R.id.buttonAbdel)
        buttonKadir = findViewById<Button>(R.id.buttonKadir)

        // Initialisation des autres vues
        calendarGrid = findViewById(R.id.calendarGrid)
        monthYearText = findViewById(R.id.monthYearText)

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

        // Définir le coiffeur par défaut à barber_id = 1
        setSelectedBarber(1)

        updateCalendar()
    }

    private fun openAppointmentForm() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_appointment_form)

        val editTextClientName = dialog.findViewById<EditText>(R.id.editTextClientName)
        val spinnerBarber = dialog.findViewById<Spinner>(R.id.spinnerBarber)
        val buttonSelectDate = dialog.findViewById<Button>(R.id.buttonSelectDate)
        val buttonSelectTime = dialog.findViewById<Button>(R.id.buttonSelectTime)
        val spinnerService = dialog.findViewById<Spinner>(R.id.spinnerService)
        val radioGroupPayment = dialog.findViewById<RadioGroup>(R.id.radioGroupPayment)
        val buttonReserve = dialog.findViewById<ImageButton>(R.id.buttonReserveAppointment)

        // Initialisation des données pour les coiffeurs
        val barbers = arrayOf("Ahmed", "Abdel", "Kadir")
        val barberAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, barbers)
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
            showCustomDatePicker { selectedDate ->
                buttonSelectDate.text = selectedDate
                buttonSelectTime.isEnabled = true
            }
        }

        // Ouvrir le timepicker personnalisé
        buttonSelectTime.setOnClickListener {
            showCustomTimePicker { selectedTime ->
                buttonSelectTime.text = selectedTime // Affiche l'heure sélectionnée sur le bouton
            }
        }

        // Initialisation des services
        val services = arrayOf("Coupe Homme", "Coupe ado", "Coupe enfant", "FORFAIT coupe + barbe")
        val serviceAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, services)
        serviceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerService.adapter = serviceAdapter

        // Réservation du rendez-vous
        buttonReserve?.setOnClickListener {
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
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Appel pour sauvegarder le rendez-vous dans la base de données
            saveAppointment(
                clientName,
                selectedBarber,
                selectedDate,
                selectedTime,
                selectedService,
                selectedPaymentMethod
            ) {
                Toast.makeText(this, "Rendez-vous réservé avec succès", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }

        dialog.show()
    }


    private fun showCustomDatePicker(onDateSelected: (String) -> Unit) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.custom_datepicker_dialog)

        val gridView = dialog.findViewById<GridLayout>(R.id.datePickerGrid)
        val confirmButton = dialog.findViewById<Button>(R.id.buttonConfirmDate)
        val currentMonth = Calendar.getInstance()

        // Assurez-vous que GridLayout est vide avant d'ajouter les jours
        gridView.removeAllViews()

        // Ajoute les jours de la semaine
        val daysOfWeek = listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim")
        daysOfWeek.forEach { day ->
            val dayLabel = TextView(this).apply {
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

        // Génère les numéros de jours pour le mois courant
        val dates = generateDatesForCurrentMonth()
        dates.forEach { day ->
            val dayButton = Button(this).apply {
                text = day
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(0, 0, 0, 0) // Éliminer les marges pour coller les cases
                }
                setOnClickListener {
                    // Formate la date sélectionnée en YY-MM-DD
                    val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(currentMonth.apply {
                        set(Calendar.DAY_OF_MONTH, day.toInt())
                    }.time)
                    onDateSelected(formattedDate)
                    dialog.dismiss()
                }
            }
            gridView.addView(dayButton)
        }

        confirmButton.setOnClickListener {
            // Gérer la confirmation si nécessaire
            dialog.dismiss()
        }

        dialog.show()
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

    private fun showCustomTimePicker(onTimeSelected: (String) -> Unit) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.custom_time_picker_dialog)

        val gridView = dialog.findViewById<GridView>(R.id.timePickerGrid)
        val confirmButton = dialog.findViewById<Button>(R.id.buttonConfirmTime)

        // Génère des intervalles de temps (ex: "09:00:00", "09:30:00", etc.)
        val times = generateTimesForPicker()
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, times)
        gridView.adapter = adapter

        // Capture l'heure immédiatement lors de la sélection
        gridView.setOnItemClickListener { _, _, position, _ ->
            val selectedTime = times[position]
            onTimeSelected(selectedTime)  // Enregistre l'heure sélectionnée
            dialog.dismiss()  // Ferme le dialogue après la sélection
        }

        // Si vous souhaitez utiliser le bouton de confirmation pour autre chose, vous pouvez ajouter un comportement ici.
        confirmButton.setOnClickListener {
            dialog.dismiss()  // Simplement fermer le dialogue
        }

        dialog.show()
    }

    private fun generateTimesForPicker(): List<String> {
        val times = mutableListOf<String>()
        for (hour in 0..23) {
            for (minute in listOf(0, 15, 30, 45)) { // incréments de 15 minutes
                times.add(String.format("%02d:%02d:00", hour, minute))
            }
        }
        return times
    }

    private fun saveAppointment(
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

        Log.d("SaveAppointment", "Sending appointment data: $appointmentRequest")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.createAppointment(appointmentRequest)
                if (response.isSuccessful) {
                    val responseBody = response.body()?.string()
                    Log.d("SaveAppointment", "Response body: $responseBody")

                    // Ajoutez ici une vérification pour valider la réponse, comme un mot-clé "success"
                    if (responseBody?.contains("success", ignoreCase = true) == true) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "Rendez-vous réservé avec succès", Toast.LENGTH_SHORT).show()
                            onComplete()
                        }
                    } else {
                        Log.e("SaveAppointment", "Erreur: Réponse inattendue du serveur: $responseBody")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "Erreur lors de la réservation, réponse inattendue du serveur", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Log.e("SaveAppointment", "Error in response: ${response.errorBody()?.string()}")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Erreur lors de la réservation", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {""
                Log.e("SaveAppointment", "Exception during reservation: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Erreur lors de la réservation", Toast.LENGTH_SHORT).show()
                }
            }
        }
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
