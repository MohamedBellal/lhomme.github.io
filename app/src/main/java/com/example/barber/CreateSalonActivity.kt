package com.example.barber

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.barber.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreateSalonActivity : AppCompatActivity() {

    private lateinit var editTextSalonName: EditText
    private lateinit var editTextSalonAddress: EditText
    private lateinit var editTextSalonPhone: EditText
    private lateinit var editTextSalonEmail: EditText
    private lateinit var buttonNext: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_salon)

        // Initialize views
        editTextSalonName = findViewById(R.id.editTextSalonName)
        editTextSalonAddress = findViewById(R.id.editTextSalonAddress)
        editTextSalonPhone = findViewById(R.id.editTextSalonPhone)
        editTextSalonEmail = findViewById(R.id.editTextSalonEmail)
        buttonNext = findViewById(R.id.buttonNext)

        buttonNext.setOnClickListener {
            val salonName = editTextSalonName.text.toString().trim()
            val salonAddress = editTextSalonAddress.text.toString().trim()
            val salonPhone = editTextSalonPhone.text.toString().trim()
            val salonEmail = editTextSalonEmail.text.toString().trim()

            Log.e("CreateSalonActivity", "Salon Name: $salonName")
            Log.e("CreateSalonActivity", "Salon Address: $salonAddress")
            Log.e("CreateSalonActivity", "Salon Phone: $salonPhone")
            Log.e("CreateSalonActivity", "Salon Email: $salonEmail")

            if (salonName.isEmpty() || salonAddress.isEmpty() || salonPhone.isEmpty() || salonEmail.isEmpty()) {
                Toast.makeText(this, "Veuillez remplir toutes les informations", Toast.LENGTH_SHORT).show()
            } else {
                // Logique pour sauvegarder les informations du salon
                saveSalonInfo(salonName, salonAddress, salonPhone, salonEmail)
            }
        }
    }

    private fun saveSalonInfo(name: String, address: String, phone: String, email: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Récupérer l'ID de l'utilisateur depuis SharedPreferences
                val sharedPreferences = getSharedPreferences("MyApp", MODE_PRIVATE)
                val userId = sharedPreferences.getInt("user_id", 0) // Default 0 if not found

                // Appel à l'API pour créer le salon
                val params = mapOf(
                    "salon_name" to name,
                    "salon_address" to address,
                    "salon_phone" to phone,
                    "salon_email" to email,
                    "user_id" to userId.toString()  // Passer l'ID utilisateur ici
                )

                val response = RetrofitClient.apiService.createSalon(params)
                if (response.isSuccessful) {
                    val salonId = response.body()?.salon_id // Récupérer le salon_id
                    withContext(Dispatchers.Main) {
                        if (salonId != null) {
                            // Sauvegarder le salon_id dans SharedPreferences
                            val editor = sharedPreferences.edit()
                            editor.putInt("salon_id", salonId)
                            editor.apply()

                            // Redirection vers CreateBarbersActivity
                            val intent = Intent(this@CreateSalonActivity, CreateBarbersActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this@CreateSalonActivity, "Erreur lors de la récupération du salon ID", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@CreateSalonActivity, "Erreur lors de la création du salon", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CreateSalonActivity, "Erreur réseau : ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
