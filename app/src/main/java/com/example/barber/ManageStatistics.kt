package com.example.barber

import android.content.Context
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.example.barber.network.RetrofitClient
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.utils.ColorTemplate
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ManageStatistics {

    fun handleStatistics(context: Context) {
        showStatisticsDialog(context)
    }

    private fun showStatisticsDialog(context: Context) {
        val inflater = android.view.LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_statistics_with_spinner, null)

        // Initialisation des spinners
        val durationSpinnerPie = view.findViewById<Spinner>(R.id.durationSpinnerPie)
        val serviceSpinner = view.findViewById<Spinner>(R.id.serviceSpinner)
        val durationSpinnerLine = view.findViewById<Spinner>(R.id.durationSpinnerLine)
        val barberSpinner = view.findViewById<Spinner>(R.id.barberSpinner)

        // Initialisation des graphiques
        val pieChart = view.findViewById<PieChart>(R.id.pieChart)
        val lineChart = view.findViewById<LineChart>(R.id.lineChart)

        // Adapter pour le spinner des durées
        val durations = arrayOf("1 mois", "3 mois", "6 mois", "1 an", "2 ans", "Tout")
        val durationAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, durations)
        durationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        durationSpinnerPie.adapter = durationAdapter
        durationSpinnerLine.adapter = durationAdapter

        // Adapter pour les services
        val services = arrayOf("Tout", "Coupe Homme", "Coupe Femme", "Coupe Enfant")
        val serviceAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, services)
        serviceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        serviceSpinner.adapter = serviceAdapter

        // Adapter pour les coiffeurs
        val barbers = arrayOf("Tous", "Ahmed", "Abdel", "Kadir")
        val barberAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, barbers)
        barberAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        barberSpinner.adapter = barberAdapter

        // Charger les statistiques initiales
        loadStatistics(context, "2024-01-01", "2024-12-31", pieChart, lineChart, serviceSpinner.selectedItem.toString(), barberSpinner.selectedItem.toString())

        // Gestion du changement de période pour le PieChart
        durationSpinnerPie.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val selectedDuration = durations[position]
                val endDate = Calendar.getInstance().time
                val startDate = calculateStartDate(selectedDuration)
                loadPieChart(context, startDate, SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(endDate), pieChart, serviceSpinner.selectedItem.toString())
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })

// Gestion du changement de période pour le LineChart
        durationSpinnerLine.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val selectedDuration = durations[position]
                val endDate = Calendar.getInstance().time
                val startDate = calculateStartDate(selectedDuration)
                loadLineChart(context, startDate, SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(endDate), lineChart, barberSpinner.selectedItem.toString())
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })

// Gestion du filtre par service pour le PieChart
        serviceSpinner.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val selectedService = services[position]
                loadPieChart(context, "2024-01-01", "2024-12-31", pieChart, selectedService)
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })

// Gestion du filtre par coiffeur pour le LineChart
        barberSpinner.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val selectedBarber = barbers[position]
                loadLineChart(context, "2024-01-01", "2024-12-31", lineChart, selectedBarber)
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })

        // Création et affichage de la boîte de dialogue
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Gestion des statistiques")
        builder.setView(view)
        builder.setPositiveButton("Fermer") { dialog, _ -> dialog.dismiss() }

        val dialog = builder.create()
        dialog.show()

        // Définir la taille de la boîte de dialogue à 90% de la largeur et de la hauteur de l'écran
        val displayMetrics = context.resources.displayMetrics
        val width = (displayMetrics.widthPixels * 0.9).toInt() // 90% de la largeur
        val height = (displayMetrics.heightPixels * 0.9).toInt() // 90% de la hauteur

        dialog.window?.setLayout(width, height)
    }

    // Fonction pour calculer la date de début basée sur la durée sélectionnée
    private fun calculateStartDate(duration: String): String {
        val calendar = Calendar.getInstance()
        when (duration) {
            "1 mois" -> calendar.add(Calendar.MONTH, -1)
            "3 mois" -> calendar.add(Calendar.MONTH, -3)
            "6 mois" -> calendar.add(Calendar.MONTH, -6)
            "1 an" -> calendar.add(Calendar.YEAR, -1)
            "2 ans" -> calendar.add(Calendar.YEAR, -2)
            "Tout" -> calendar.set(2000, 0, 1) // Début arbitraire si "Tout"
        }
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
    }

    // Fonction pour charger les statistiques via l'API
    private fun loadStatistics(
        context: Context,
        startDate: String,
        endDate: String,
        pieChart: PieChart,
        lineChart: LineChart,
        selectedService: String,
        selectedBarber: String
    ) {
        val barberId = if (selectedBarber == "Tous") 0 else getBarberIdByName(selectedBarber)
        (context as? MainActivity)?.lifecycleScope?.launch {
            try {
                // Envoie du barberId dans l'appel API
                val response = RetrofitClient.apiService.getStatistics(startDate, endDate, barberId)
                if (response.isSuccessful) {
                    val statistics = response.body()
                    statistics?.let {
                        // Mise à jour des graphiques
                        setupPieChart(context, pieChart, it, selectedService)
                        setupLineChart(context, lineChart, it, selectedBarber)
                    }
                } else {
                    Toast.makeText(context, "Erreur lors du chargement des statistiques", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Erreur réseau: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Fonction pour obtenir l'ID d'un coiffeur à partir de son nom
    private fun getBarberIdByName(barberName: String): Int {
        // Ajoute la logique ici pour correspondre le nom du coiffeur à son ID
        // Exemple simple :
        return when (barberName) {
            "Ahmed" -> 1
            "Abdel" -> 2
            "Kadir" -> 3
            else -> 0 // Tous les coiffeurs
        }
    }

    // Fonction pour configurer le graphique circulaire (PieChart)
    // Fonction pour configurer le graphique circulaire (PieChart)
    // Fonction pour configurer le graphique circulaire (PieChart)
    private fun setupPieChart(context: Context, pieChart: PieChart, statistics: StatisticsResponse, selectedService: String) {
        val pieEntries = ArrayList<PieEntry>()
        pieChart.setDrawEntryLabels(false) // Désactive les étiquettes sur le graphique

        val filteredStats = if (selectedService == "Tout") {
            statistics.appointmentsPerService
        } else {
            statistics.appointmentsPerService?.filter { it.serviceName == selectedService }
        }

        filteredStats?.forEach { serviceStat ->
            pieEntries.add(PieEntry(serviceStat.totalAppointments.toFloat(), serviceStat.serviceName))
        }

        val pieDataSet = PieDataSet(pieEntries, "Répartition des rendez-vous par service")
        pieDataSet.colors = ColorTemplate.COLORFUL_COLORS.asList()
        pieChart.data = PieData(pieDataSet)

        // Configurer la légende pour afficher chaque élément sur une nouvelle ligne en dessous du graphique
        val legend = pieChart.legend
        legend.isWordWrapEnabled = true  // Activer le retour à la ligne
        legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM // Position verticale en bas
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER // Centrer la légende horizontalement
        legend.orientation = Legend.LegendOrientation.HORIZONTAL // Affichage en ligne horizontale
        legend.setDrawInside(false) // Placer la légende à l'extérieur du graphique
        legend.formSize = 12f // Taille des icônes dans la légende
        legend.textSize = 12f // Taille du texte de la légende

        pieChart.invalidate() // Rafraîchir le graphique
    }

    // Fonction pour configurer le graphique linéaire (LineChart)
    private fun setupLineChart(context: Context, lineChart: LineChart, statistics: StatisticsResponse, selectedBarber: String) {
        val lineEntries = ArrayList<Entry>()
        val filteredStats = if (selectedBarber == "Tous") {
            statistics.appointmentsPerBarber
        } else {
            statistics.appointmentsPerBarber?.filter { it.barberName == selectedBarber }
        }

        filteredStats?.forEachIndexed { index, barberStat ->
            lineEntries.add(Entry(index.toFloat(), barberStat.totalAppointments.toFloat()))
        }

        val lineDataSet = LineDataSet(lineEntries, "Évolution des rendez-vous")
        lineChart.data = LineData(lineDataSet)
        lineChart.invalidate()
    }




    // Fonction pour charger les données du PieChart uniquement
    private fun loadPieChart(
        context: Context,
        startDate: String,
        endDate: String,
        pieChart: PieChart,
        selectedService: String
    ) {
        (context as? MainActivity)?.lifecycleScope?.launch {
            try {
                val response = RetrofitClient.apiService.getStatistics(startDate, endDate, 0)
                if (response.isSuccessful) {
                    val statistics = response.body()
                    statistics?.let {
                        setupPieChart(context, pieChart, it, selectedService)
                    }
                } else {
                    Toast.makeText(context, "Erreur lors du chargement des statistiques", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Erreur réseau: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Fonction pour charger les données du LineChart uniquement
    private fun loadLineChart(
        context: Context,
        startDate: String,
        endDate: String,
        lineChart: LineChart,
        selectedBarber: String
    ) {
        val barberId = if (selectedBarber == "Tous") 0 else getBarberIdByName(selectedBarber)
        (context as? MainActivity)?.lifecycleScope?.launch {
            try {
                val response = RetrofitClient.apiService.getStatistics(startDate, endDate, barberId)
                if (response.isSuccessful) {
                    val statistics = response.body()
                    statistics?.let {
                        setupLineChart(context, lineChart, it, selectedBarber)
                    }
                } else {
                    Toast.makeText(context, "Erreur lors du chargement des statistiques", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Erreur réseau: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

}
