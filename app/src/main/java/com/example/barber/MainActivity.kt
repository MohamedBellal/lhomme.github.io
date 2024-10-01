package com.example.barber

import com.example.barber.manage.ManageAvailability
import com.example.barber.adapters.AppointmentsAdapter
import com.example.barber.adapters.SelectedAppointmentsAdapter
import com.example.barber.models.Appointment
import com.example.barber.network.RetrofitClient
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.barber.manage.ManageDeleted
import com.example.barber.manage.ManageFinances
import com.example.barber.manage.ManageSalon
import com.example.barber.manage.ManageStatistics
import com.example.barber.manage.ReserveAppointment
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
    private var currentMonth = Calendar.getInstance()
    private var selectedBarberId = 1
    private var selectedAppointments = mutableSetOf<Appointment>()

    private lateinit var reservationHandler: ReserveAppointment

    private var salonId: Int = 0

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Récupérer le salon_id de l'utilisateur connecté
        val sharedPreferences = getSharedPreferences("MyApp", Context.MODE_PRIVATE)
        salonId = sharedPreferences.getInt("salon_id", 0)

        if (salonId == 0) {
            // Si aucun salon_id n'est trouvé, renvoyer l'utilisateur vers la page de connexion
            startActivity(Intent(this, LoginActivity::class.java))
            finish()  // Empêche l'utilisateur de revenir à MainActivity sans être connecté
            return
        }

        // Appel pour récupérer les barbiers et afficher les boutons
        fetchBarbersForSalon(salonId)

        // Reste de l'initialisation de l'activité
        reservationHandler = ReserveAppointment()

        // Si l'utilisateur est connecté, continuer avec le reste de l'application
        setContentView(R.layout.activity_main)

        reservationHandler = ReserveAppointment()

        val buttonReserveAppointment = findViewById<ImageButton>(R.id.buttonReserveAppointment)
        buttonReserveAppointment.setOnClickListener {
            reservationHandler.openAppointmentForm(this)
        }

        val buttonManageFinances = findViewById<ImageButton>(R.id.buttonManageFinances)
        buttonManageFinances.setOnClickListener {
            ManageFinances().handleFinances(this)
        }

        val buttonManageAvailability = findViewById<ImageButton>(R.id.buttonManageAvailability)
        buttonManageAvailability.setOnClickListener {
            ManageAvailability().handleAvailability(this)
        }

        val buttonManageSalon = findViewById<ImageButton>(R.id.buttonManageServices)
        buttonManageSalon.setOnClickListener {
            ManageSalon().handleSalon(this) {
                // Actualiser les boutons après l'ajout d'un barbier
                fetchBarbersForSalon(salonId)
            }
        }

        val buttonManageStatistics = findViewById<ImageButton>(R.id.buttonManageStatistics)
        buttonManageStatistics.setOnClickListener {
            ManageStatistics().handleStatistics(this)
        }

        val buttonManageDeleted = findViewById<ImageButton>(R.id.buttonManageDeleted)
        buttonManageDeleted.setOnClickListener {
            ManageDeleted().handleDeleted(this)
        }

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

        // Définir le coiffeur par défaut à barber_id = 1
        setSelectedBarber(1)

        updateCalendar()
    }

    private fun setSelectedBarber(barberId: Int) {
        selectedBarberId = barberId
        updateCalendarForSelectedBarber(barberId)
    }

    fun fetchBarbersForSalon(salonId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.e("API Request", "Fetching barbers for salon ID: $salonId")
                val response = RetrofitClient.apiService.getBarbers(salonId.toString())
                if (response.isSuccessful) {
                    val barberResponse = response.body()  // Adapter à la structure de BarberResponse

                    // Ajoutez ce log pour voir la réponse brute
                    Log.e("API Response", "Barber Response: ${response.body()}")

                    if (barberResponse?.success == true) {
                        val barbers = barberResponse.barbers
                        withContext(Dispatchers.Main) {
                            if (barbers.isNotEmpty()) {
                                Log.e("MyTag", "Appel à displayBarberButtons")
                                displayBarberButtons(barbers)  // Affiche dynamiquement les boutons des barbiers
                            } else {
                                Log.e("MainActivity", "Aucun barbier trouvé pour le salon")
                            }
                        }
                    } else {
                        Log.e("MainActivity", "Erreur lors de la récupération des barbiers")
                    }
                } else {
                    Log.e("MainActivity", "Erreur lors de la récupération des barbiers: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Erreur lors de la récupération des barbiers", e)
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

    private fun displayBarberButtons(barbers: List<Barber>) {
        val barberButtonLayout = findViewById<LinearLayout>(R.id.barberButtonLayout)
        barberButtonLayout.removeAllViews()  // Supprime les anciens boutons

        Log.e("MyTag", "Nombre de barbiers reçus: ${barbers.size}")

        // Sélectionner automatiquement le premier barbier s'il n'y a pas de sélection actuelle
        if (selectedBarberId == 1 && barbers.isNotEmpty()) {
            selectedBarberId = barbers[0].barber_id
            Log.e("MyTag", "Barbier par défaut sélectionné: ${barbers[0].barber_name}")
            updateCalendarForSelectedBarber(selectedBarberId)  // Met à jour le calendrier pour le premier barbier
        }

        barbers.forEach { barber ->
            val button = Button(this)
            button.text = barber.barber_name
            button.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(8, 0, 8, 0)
            }

            // Appliquer la couleur en fonction de si le barbier est sélectionné ou non
            if (barber.barber_id == selectedBarberId) {
                button.setBackgroundColor(Color.BLUE)  // Couleur du bouton sélectionné
            } else {
                button.setBackgroundColor(Color.parseColor("#FF69B4"))  // Couleur rose pour les autres boutons
            }

            // Lorsqu'on clique sur un bouton de barbier, on change l'ID du barbier sélectionné et on met à jour l'affichage
            button.setOnClickListener {
                setSelectedBarber(barber.barber_id)
                displayBarberButtons(barbers)  // Réinitialise les boutons avec les nouvelles couleurs
            }

            barberButtonLayout.addView(button)  // Ajoute dynamiquement le bouton au layout
        }
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

        // Ajouter des jours vides avant le début du mois
        for (i in 0 until firstDayOfWeek) {
            addEmptyDay(calendarGrid)
        }

        // Ajouter les jours du mois avec les rendez-vous
        for (day in 1..daysInMonth) {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(currentMonth.apply { set(Calendar.DAY_OF_MONTH, day) }.time)

            // Filtrer les rendez-vous pour ce jour spécifique
            val appointmentsForDay = appointments.filter { it.appointment_date == date }

            // Appeler addDayToCalendar en passant la liste filtrée
            addDayToCalendar(calendarGrid, day, appointmentsForDay)
        }

        // Remplir les cellules restantes pour compléter la grille
        val totalCells = firstDayOfWeek + daysInMonth
        val remainingCells = 7 - (totalCells % 7)
        if (remainingCells < 7) {
            for (i in 0 until remainingCells) {
                addEmptyDay(calendarGrid)
            }
        }
    }

    private fun addDayToCalendar(gridLayout: GridLayout, day: Int, appointmentsForDay: List<Appointment>) {
        val dayLayout = FrameLayout(this).apply {
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = 0
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(4, 4, 4, 4)  // Marges autour de chaque case
            }
        }

        // Bouton du jour
        val dayButton = Button(this).apply {
            text = day.toString()
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
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

        dayLayout.addView(dayButton)  // Ajouter le bouton du jour

        // Ajouter l'icône de notification s'il y a des rendez-vous pour ce jour
        // Ajouter l'icône de notification s'il y a des rendez-vous pour ce jour
        if (appointmentsForDay.isNotEmpty()) {
            val notificationBadge = TextView(this).apply {
                text = appointmentsForDay.size.toString()  // Affiche le nombre de rendez-vous
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.END or Gravity.TOP  // Positionner dans le coin supérieur droit
                    setMargins(0, 0, 10, 10)  // Ajuster les marges pour bien positionner le badge
                }
                setTextColor(Color.WHITE)
                setBackgroundResource(R.drawable.notification_background)  // Fond rouge pour le badge
                setPadding(10, 6, 10, 6)  // Ajuster le padding pour que ce soit circulaire
                textSize = 12f
                gravity = Gravity.CENTER  // Centrer le texte dans le badge
            }

            dayLayout.addView(notificationBadge)  // Ajouter le badge au layout du jour
        }

        gridLayout.addView(dayLayout)  // Ajouter la case du jour à la grille
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

        closeModalButton.setOnClickListener {
            dialog.dismiss()
        }

        // Mise à jour de l'interface avec les rendez-vous récupérés
        fetchAppointments(date) { appointments ->
            selectedAppointments.clear()
            val adapter = AppointmentsAdapter(
                appointments,
                selectedAppointments,
                onSelectionChanged = {
                    updateDeleteButtonState(dialog)  // Met à jour l'état du bouton de suppression
                    Log.d("Appointments", "Selected appointments: $selectedAppointments")  // Log pour vérifier la sélection
                }
            )
            appointmentsRecyclerView.layoutManager = LinearLayoutManager(this)
            appointmentsRecyclerView.adapter = adapter

            // Met à jour l'état initial du bouton de suppression
            updateDeleteButtonState(dialog)
        }

        // Action du bouton de suppression
        deleteButton.setOnClickListener {
            if (selectedAppointments.isNotEmpty()) {
                showDeleteConfirmationDialog(date, appointmentsRecyclerView, dialog)  // Appel de la nouvelle méthode avec RecyclerView et dialog
            } else {
                Toast.makeText(this, "Veuillez sélectionner un rendez-vous à supprimer", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun updateCalendarForSelectedBarber(barberId: Int) {
        fetchAppointmentsForBarber(barberId) { fetchedAppointments ->
            val filteredAppointments = fetchedAppointments.filter { it.salon_id == salonId }
            appointments.clear()  // Vide la liste actuelle
            appointments.addAll(filteredAppointments)  // Ajoute les rendez-vous filtrés
            updateCalendar()  // Mets à jour l'affichage du calendrier
        }
    }

    private fun fetchAppointmentsForBarber(barberId: Int, callback: (List<Appointment>) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val salonIdString = salonId.toString()

                // Ajout de la requête pour récupérer les rendez-vous par barber et salon
                val response = RetrofitClient.apiService.getAppointmentsForBarberAndSalon(salonIdString, barberId, currentDate)

                if (response.isSuccessful) {
                    val appointmentResponse = response.body()
                    val appointments = appointmentResponse?.appointments ?: emptyList()

                    withContext(Dispatchers.Main) {
                        callback(appointments)
                    }
                } else {
                    Log.e("FetchAppForBarber", "Erreur: ${response.errorBody()?.string()}")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Erreur lors de la récupération des rendez-vous", Toast.LENGTH_SHORT).show()
                        callback(emptyList())
                    }
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
                // Ajoute ce log pour afficher les paramètres de l'API
                Log.d("API Response", "Appointments: ${appointments}")
                Log.d("API Request", "Date: $date, Barber ID: $selectedBarberId")

                val response = RetrofitClient.apiService.getAppointmentsForBarberAndSalon(date, selectedBarberId, salonId.toString()
                )

                if (response.isSuccessful) {
                    val appointmentResponse = response.body()
                    val appointments = appointmentResponse?.appointments ?: emptyList()

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
            } catch (e: JsonSyntaxException) {
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

    private fun showDeleteConfirmationDialog(date: String, recyclerView: RecyclerView, dialog: Dialog) {
        val confirmDialog = Dialog(this)
        confirmDialog.setContentView(R.layout.dialog_confirm_delete)

        val confirmationMessage = confirmDialog.findViewById<TextView>(R.id.confirmationMessage)
        val selectedAppointmentsRecyclerView = confirmDialog.findViewById<RecyclerView>(R.id.selectedAppointmentsRecyclerView)
        val cancelButton = confirmDialog.findViewById<Button>(R.id.cancelButton)
        val confirmButton = confirmDialog.findViewById<Button>(R.id.confirmButton)

        // Configure le RecyclerView pour afficher les rendez-vous sélectionnés
        selectedAppointmentsRecyclerView.layoutManager = LinearLayoutManager(this)
        selectedAppointmentsRecyclerView.adapter = SelectedAppointmentsAdapter(selectedAppointments.toList())

        cancelButton.setOnClickListener { confirmDialog.dismiss() }

        confirmButton.setOnClickListener {
            deleteSelectedAppointments(
                selectedAppointments.toList()
            ) {
                // Mets à jour les rendez-vous après la suppression
                fetchAppointments(date) { updatedAppointments ->
                    val adapter = AppointmentsAdapter(
                        updatedAppointments,
                        selectedAppointments,
                        onSelectionChanged = {
                            updateDeleteButtonState(dialog)  // Met à jour l'état du bouton de suppression
                        }
                    )
                    recyclerView.adapter = adapter  // Mise à jour de l'adaptateur du RecyclerView avec les nouveaux rendez-vous
                }
                confirmDialog.dismiss()
            }
        }

        confirmDialog.show()
    }
}