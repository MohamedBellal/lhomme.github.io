package com.example.barber

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.barber.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreateBarbersActivity : AppCompatActivity() {

    private lateinit var buttonAddMoreBarbers: Button
    private lateinit var buttonValidate: Button
    private lateinit var barbersContainer: LinearLayout
    private val barberNames = mutableListOf<EditText>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_barbers)

        barbersContainer = findViewById(R.id.barbersContainer)
        buttonAddMoreBarbers = findViewById(R.id.buttonAddMoreBarbers)
        buttonValidate = findViewById(R.id.buttonValidate)

        // Ajouter le premier champ barber au démarrage
        addBarberField()

        buttonAddMoreBarbers.setOnClickListener {
            addBarberField()  // Ajouter un champ supplémentaire pour un barber
        }

        buttonValidate.setOnClickListener {
            saveBarbers()  // Enregistrer tous les barbers dans la base de données
        }
    }

    private fun addBarberField() {
        val barberEditText = EditText(this)
        barberEditText.hint = "Nom du barber"
        barberEditText.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        barbersContainer.addView(barberEditText)
        barberNames.add(barberEditText)
    }

    private fun saveBarbers() {
        val barbersList = barberNames.map { it.text.toString().trim() }.filter { it.isNotEmpty() }

        if (barbersList.isEmpty()) {
            Toast.makeText(this, "Veuillez entrer au moins un nom de barber", Toast.LENGTH_SHORT).show()
            return
        }

        // Récupérer l'ID du salon depuis SharedPreferences
        val sharedPreferences = getSharedPreferences("MyApp", Context.MODE_PRIVATE)
        val salonId = sharedPreferences.getInt("salon_id", 0)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Créer un map pour envoyer les barbers et le salon_id comme paramètre de l'API
                val params = mutableMapOf<String, String>()
                params["salon_id"] = salonId.toString()
                params["barber_names"] = barbersList.joinToString(",")  // Représente la liste comme une chaîne

                val response = RetrofitClient.apiService.addBarbers(params)
                if (response.isSuccessful && response.body()?.success == true) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@CreateBarbersActivity, "Barbers ajoutés avec succès", Toast.LENGTH_SHORT).show()

                        // Redirection vers MainActivity après l'ajout
                        val intent = Intent(this@CreateBarbersActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@CreateBarbersActivity, "Erreur lors de l'ajout des barbers", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CreateBarbersActivity, "Erreur réseau : ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
