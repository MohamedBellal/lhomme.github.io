package com.example.barber.manage

import BarberAdapter
import ServiceAdapter
import android.app.Dialog
import android.content.Context
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.barber.Barber
import com.example.barber.MainActivity
import com.example.barber.R
import com.example.barber.Service
import com.example.barber.network.RetrofitClient
import kotlinx.coroutines.launch

class ManageSalon {

    private lateinit var adapter: BarberAdapter  // Declare adapter as a class variable
    private lateinit var serviceAdapter: ServiceAdapter

    fun handleSalon(context: Context, onBarberAdded: () -> Unit) {
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.dialog_manage_salon)

        handleBarbers(context, dialog, onBarberAdded)

        val services = mutableListOf<Service>()
        serviceAdapter = ServiceAdapter(services,
            onEditService = { service -> editService(context, service, getSalonId(context)) },
            onDeleteService = { service -> deleteService(context, service, getSalonId(context)) }
        )

        handleServices(context, dialog, serviceAdapter)

        // Trouver le bouton "Ajouter Service" et lui associer l'ouverture de la boîte de dialogue
        val addServiceButton = dialog.findViewById<Button>(R.id.addServiceButton)
        if (addServiceButton != null) {
            Log.e("ManageSalon", "Bouton 'Ajouter Service' trouvé et listener configuré.")
            addServiceButton.setOnClickListener {
                openAddServiceDialog(context, getSalonId(context), serviceAdapter)
            }
        } else {
            Log.e("ManageSalon", "Bouton 'Ajouter Service' non trouvé dans l'interface.")
        }

        dialog.show()
    }

    private fun getSalonId(context: Context): Int {
        val sharedPreferences = context.getSharedPreferences("MyApp", Context.MODE_PRIVATE)
        return sharedPreferences.getInt("salon_id", 0)
    }

    fun handleServices(context: Context, dialog: Dialog, adapter: ServiceAdapter) {
        val salonId = getSalonId(context)

        val servicesRecyclerView = dialog.findViewById<RecyclerView>(R.id.servicesRecyclerView)
        servicesRecyclerView.layoutManager = LinearLayoutManager(context)
        servicesRecyclerView.adapter = adapter  // Attacher l'adaptateur

        // Récupérer les services dès l'ouverture de la fenêtre
        (context as? MainActivity)?.lifecycleScope?.launch {
            try {
                val response = RetrofitClient.apiService.getServices(salonId)
                if (response.isSuccessful) {
                    val fetchedServices = response.body()?.services ?: emptyList()
                    adapter.services.clear()  // Assurez-vous que c'est bien mutable
                    adapter.services.addAll(fetchedServices)
                    adapter.notifyDataSetChanged()
                } else {
                    Log.e("HandleServices", "Erreur lors de la récupération des services")
                }
            } catch (e: Exception) {
                Log.e("HandleServices", "Erreur réseau: ${e.message}")
            }
        }
    }

    private fun openAddServiceDialog(context: Context, salonId: Int, adapter: ServiceAdapter) {
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.dialog_add_service)

        val serviceNameEditText = dialog.findViewById<EditText>(R.id.serviceNameEditText)
        val servicePriceEditText = dialog.findViewById<EditText>(R.id.servicePriceEditText)
        val addServiceButton = dialog.findViewById<Button>(R.id.addServiceButton)

        addServiceButton.setOnClickListener {
            Log.e("AddService", "Bouton ajouter service cliqué")
            addServiceButton.isEnabled = false

            // Récupération des données, sans supprimer les caractères accentués
            val serviceName = serviceNameEditText.text.toString().trim()
            val servicePriceString = servicePriceEditText.text.toString().trim().replace("[^\\d]".toRegex(), "")

            // Vérification que le nom et le prix ne sont pas vides
            if (serviceName.isEmpty()) {
                Log.e("AddService", "Le nom du service est vide.")
                Toast.makeText(context, "Veuillez entrer un nom de service.", Toast.LENGTH_SHORT).show()
                addServiceButton.isEnabled = true
                return@setOnClickListener
            }

            if (servicePriceString.isEmpty()) {
                Log.e("AddService", "Le prix du service est vide ou incorrect.")
                Toast.makeText(context, "Veuillez entrer un prix valide.", Toast.LENGTH_SHORT).show()
                addServiceButton.isEnabled = true
                return@setOnClickListener
            }

            // Convertir le prix en entier (en centimes)
            val servicePrice = servicePriceString.toDoubleOrNull()?.times(100)?.toInt()

            // Log des valeurs
            Log.e("AddService", "Nom du service: $serviceName, Prix du service: $servicePrice")

            if (servicePrice != null) {
                Log.e("AddService", "Les informations du service sont valides")
                (context as? MainActivity)?.lifecycleScope?.launch {
                    try {
                        Log.e("AddService", "Envoi de la requête API pour ajouter le service")
                        Log.e("API Request", "Salon ID: $salonId, Service Name: $serviceName, Service Price: $servicePrice")
                        val response = RetrofitClient.apiService.addService(salonId, serviceName, servicePrice)

                        // Ajoute ceci pour loguer la réponse brute
                        val rawResponse = response.errorBody()?.string() ?: response.body().toString()
                        Log.e("AddService", "Réponse brute : $rawResponse")

                        if (response.isSuccessful) {
                            val newService = response.body()?.service
                            Log.e("AddService", "Réponse de l'API reçue: $newService")
                            if (newService != null) {
                                adapter.addService(newService)  // Ajout du service
                                Log.e("AddService", "Service ajouté à l'adaptateur")
                            }
                            Toast.makeText(context, "Service ajouté", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        } else {
                            Log.e("AddService", "Erreur lors de l'ajout du service : ${response.errorBody()?.string()}")
                        }
                    } catch (e: Exception) {
                        Log.e("AddService", "Erreur réseau: ${e.message}")
                    } finally {
                        addServiceButton.isEnabled = true
                    }
                }
            } else {
                Log.e("AddService", "Le prix du service est incorrect.")
                Toast.makeText(context, "Veuillez entrer un prix valide.", Toast.LENGTH_SHORT).show()
                addServiceButton.isEnabled = true
            }
        }

        dialog.show()
    }

    private fun deleteService(context: Context, service: Service, salonId: Int) {
        // Boîte de dialogue de confirmation pour la suppression du service
        val confirmationDialog = Dialog(context)
        confirmationDialog.setContentView(R.layout.dialog_confirmation_service)

        // Affichage des informations sur le service à supprimer
        val confirmationTextView = confirmationDialog.findViewById<TextView>(R.id.confirmationTextView)
        confirmationTextView.text = "Supprimer \"${service.service_name}\" ?"

        // Bouton pour confirmer la suppression
        val confirmButton = confirmationDialog.findViewById<Button>(R.id.confirmButton)
        confirmButton.setOnClickListener {
            (context as? MainActivity)?.lifecycleScope?.launch {
                try {
                    Log.e("ManageSalon", "Suppression du service : ${service.service_id}")
                    val response = RetrofitClient.apiService.deleteService(service.service_id, salonId)

                    // Log de la réponse brute pour voir ce que le serveur renvoie
                    val rawResponse = response.errorBody()?.string() ?: response.body().toString()
                    Log.e("ManageSalon", "Réponse brute de suppression : $rawResponse")

                    if (response.isSuccessful) {
                        Toast.makeText(context, "Service supprimé", Toast.LENGTH_SHORT).show()

                        // Supprimer le service de l'adaptateur
                        serviceAdapter.services.remove(service)
                        serviceAdapter.notifyDataSetChanged()  // Notifie l'adaptateur

                        // Actualiser la liste des services
                        handleServices(context, confirmationDialog, serviceAdapter)
                    } else {
                        Log.e("ManageSalon", "Erreur lors de la suppression du service : ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    Log.e("ManageSalon", "Erreur réseau: ${e.message}")
                }
            }
            confirmationDialog.dismiss() // Ferme la boîte de dialogue après la suppression
        }

        // Bouton pour annuler la suppression
        val cancelButton = confirmationDialog.findViewById<Button>(R.id.cancelButton)
        cancelButton.setOnClickListener {
            confirmationDialog.dismiss() // Ferme la boîte de dialogue sans supprimer
        }

        confirmationDialog.show() // Affiche la boîte de dialogue de confirmation
    }

    private fun editService(context: Context, service: Service, salonId: Int) {
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.dialog_edit_service)

        val serviceNameEditText = dialog.findViewById<EditText>(R.id.serviceNameEditText)
        val servicePriceEditText = dialog.findViewById<EditText>(R.id.servicePriceEditText)
        val editServiceButton = dialog.findViewById<Button>(R.id.editServiceButton)

        serviceNameEditText.setText(service.service_name)
        servicePriceEditText.setText(String.format("%.2f", service.service_price / 100.0))

        editServiceButton.setOnClickListener {
            val newName = serviceNameEditText.text.toString()
            val newPriceString = servicePriceEditText.text.toString().replace("[^\\d]".toRegex(), "")
            val newPrice = newPriceString.toDoubleOrNull()?.times(100)?.toInt()

            if (newName.isNotEmpty() && newPrice != null) {
                (context as? MainActivity)?.lifecycleScope?.launch {
                    try {
                        val response = RetrofitClient.apiService.updateService(service.service_id, salonId, newName, newPrice)
                        if (response.isSuccessful) {
                            service.service_name = newName
                            service.service_price = newPrice
                            Toast.makeText(context, "Service modifié", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        } else {
                            Log.e("ManageSalon", "Erreur lors de la modification du service")
                        }
                    } catch (e: Exception) {
                        Log.e("ManageSalon", "Erreur réseau: ${e.message}")
                    }
                }
            }
        }

        dialog.show()
    }

    private fun addService(context: Context, onServiceAdded: () -> Unit) {
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.dialog_add_service)

        val serviceNameEditText = dialog.findViewById<EditText>(R.id.serviceNameEditText)
        val servicePriceEditText = dialog.findViewById<EditText>(R.id.servicePriceEditText)
        val addButton = dialog.findViewById<Button>(R.id.addServiceButton)

        addButton.setOnClickListener {
            val serviceName = serviceNameEditText.text.toString()
            val servicePriceString = servicePriceEditText.text.toString().replace("[^\\d]".toRegex(), "")
            val servicePrice = servicePriceString.toDoubleOrNull()?.times(100)?.toInt()

            if (serviceName.isNotEmpty() && servicePrice != null) {
                val sharedPreferences = context.getSharedPreferences("MyApp", Context.MODE_PRIVATE)
                val salonId = sharedPreferences.getInt("salon_id", 0)

                (context as? MainActivity)?.lifecycleScope?.launch {
                    try {
                        val response = RetrofitClient.apiService.addService(salonId, serviceName, servicePrice)
                        if (response.isSuccessful) {
                            val newService = response.body()?.service
                            if (newService != null) {
                                // Ajoute le nouveau service à la liste et notifie l'adaptateur
                                serviceAdapter.addService(newService)
                                serviceAdapter.notifyItemInserted(serviceAdapter.itemCount - 1)

                                // Actualiser la liste des services dans ManageSalon
                                handleServices(context, dialog, serviceAdapter)

                                // Appelle le callback pour actualiser l'interface utilisateur
                                onServiceAdded()
                            }
                            Toast.makeText(context, "Service ajouté", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        } else {
                            Log.e("ManageSalon", "Erreur lors de l'ajout du service")
                        }
                    } catch (e: Exception) {
                        Log.e("ManageSalon", "Erreur réseau: ${e.message}")
                    }
                }
            } else {
                Toast.makeText(context, "Veuillez entrer un nom et un prix valides", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }

    private fun handleBarbers(context: Context, dialog: Dialog, onBarberAdded: () -> Unit) {
        val sharedPreferences = context.getSharedPreferences("MyApp", Context.MODE_PRIVATE)
        val salonId = sharedPreferences.getInt("salon_id", 0)

        (context as? MainActivity)?.lifecycleScope?.launch {
            try {
                val response = RetrofitClient.apiService.getBarbers(salonId.toString())
                if (response.isSuccessful) {
                    val barbers = response.body()?.barbers?.toMutableList() ?: mutableListOf()

                    val barbersRecyclerView = dialog.findViewById<RecyclerView>(R.id.barbersRecyclerView)
                    adapter = BarberAdapter(barbers,
                        onEditBarber = { barber -> editBarber(context, barber, onBarberAdded) },
                        onDeleteBarber = { barber -> deleteBarber(context, barber, onBarberAdded) }
                    )
                    barbersRecyclerView.layoutManager = LinearLayoutManager(context)
                    barbersRecyclerView.adapter = adapter

                    val addBarberButton = dialog.findViewById<Button>(R.id.addBarberButton)
                    addBarberButton.setOnClickListener {
                        addBarber(context, onBarberAdded)  // Passe l'adaptateur ici
                    }

                } else {
                    Log.e("ManageSalon", "Erreur lors de la récupération des barbiers")
                }
            } catch (e: Exception) {
                Log.e("ManageSalon", "Erreur réseau: ${e.message}")
            }
        }
    }

    private fun addBarber(context: Context, onBarberAdded: () -> Unit) {
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.dialog_add_barber)

        val barberNameEditText = dialog.findViewById<EditText>(R.id.barberNameEditText)
        val addButton = dialog.findViewById<Button>(R.id.addBarberButton)

        addButton.setOnClickListener {
            val barberName = barberNameEditText.text.toString()
            if (barberName.isNotEmpty()) {
                val sharedPreferences = context.getSharedPreferences("MyApp", Context.MODE_PRIVATE)
                val salonId = sharedPreferences.getInt("salon_id", 0)

                (context as? MainActivity)?.lifecycleScope?.launch {
                    try {
                        val response = RetrofitClient.apiService.addBarbers(salonId.toString(), barberName)
                        if (response.isSuccessful) {
                            val newBarber = response.body()?.barber
                            if (newBarber != null) {
                                // Ajoute le nouveau barbier à la liste et notifie l'adaptateur
                                adapter.addBarberToList(newBarber)
                                adapter.notifyItemInserted(adapter.itemCount - 1)

                                // Actualise la liste dans ManageSalon
                                handleBarbers(context, dialog, onBarberAdded)

                                // Appelle le callback pour rafraîchir les boutons dans MainActivity
                                onBarberAdded()
                            }
                            Toast.makeText(context, "Barbier ajouté", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        } else {
                            Log.e("ManageSalon", "Erreur lors de l'ajout du barbier")
                        }
                    } catch (e: Exception) {
                        Log.e("ManageSalon", "Erreur réseau: ${e.message}")
                    }
                }
            }
        }
        dialog.show()
    }

    private fun editBarber(context: Context, barber: Barber, onBarberAdded: () -> Unit) {
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.dialog_edit_barber)

        val barberNameEditText = dialog.findViewById<EditText>(R.id.barberNameEditText)
        barberNameEditText.setText(barber.barber_name)

        val editButton = dialog.findViewById<Button>(R.id.editBarberButton)
        editButton.setOnClickListener {
            val newName = barberNameEditText.text.toString()
            if (newName.isNotEmpty()) {
                val sharedPreferences = context.getSharedPreferences("MyApp", Context.MODE_PRIVATE)
                val salonId = sharedPreferences.getInt("salon_id", 0)

                (context as? MainActivity)?.lifecycleScope?.launch {
                    try {
                        val response = RetrofitClient.apiService.updateBarberName(barber.barber_id, newName, salonId)
                        if (response.isSuccessful) {
                            barber.barber_name = newName
                            adapter.notifyDataSetChanged()
                            Toast.makeText(context, "Barbier modifié", Toast.LENGTH_SHORT).show()

                            // Met à jour la liste dans MainActivity
                            onBarberAdded()
                            dialog.dismiss()
                        } else {
                            Log.e("ManageSalon", "Erreur lors de la modification du barbier")
                        }
                    } catch (e: Exception) {
                        Log.e("ManageSalon", "Erreur réseau: ${e.message}")
                    }
                }
            }
        }
        dialog.show()
    }

    private fun deleteBarber(context: Context, barber: Barber, onBarberAdded: () -> Unit) {
        // Boîte de dialogue de confirmation
        val confirmationDialog = Dialog(context)
        confirmationDialog.setContentView(R.layout.dialog_confirmation)

        val confirmationTextView = confirmationDialog.findViewById<TextView>(R.id.confirmationTextView)
        confirmationTextView.text = "Supprimer le barbier ?"

        val confirmButton = confirmationDialog.findViewById<Button>(R.id.confirmButton)
        confirmButton.setOnClickListener {
            val sharedPreferences = context.getSharedPreferences("MyApp", Context.MODE_PRIVATE)
            val salonId = sharedPreferences.getInt("salon_id", 0) // Récupère salon_id

            (context as? MainActivity)?.lifecycleScope?.launch {
                try {
                    val response = RetrofitClient.apiService.deleteBarber(barber.barber_id, salonId)  // Passe salonId
                    if (response.isSuccessful) {
                        adapter.removeBarberFromList(barber)  // Supprime le barbier de la liste
                        adapter.notifyDataSetChanged()  // Notifie l'adaptateur
                        Toast.makeText(context, "Barbier supprimé", Toast.LENGTH_SHORT).show()

                        // Actualise la liste des barbiers dans MainActivity
                        onBarberAdded()
                    } else {
                        Log.e("ManageSalon", "Erreur lors de la suppression du barbier")
                    }
                } catch (e: Exception) {
                    Log.e("ManageSalon", "Erreur réseau: ${e.message}")
                }
            }
            confirmationDialog.dismiss()  // Ferme la boîte de dialogue de confirmation
        }

        val cancelButton = confirmationDialog.findViewById<Button>(R.id.cancelButton)
        cancelButton.setOnClickListener {
            confirmationDialog.dismiss()  // Annule la suppression
        }

        confirmationDialog.show()  // Affiche la boîte de dialogue
    }
}
