package com.example.barber

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

class RegisterActivity : AppCompatActivity() {

    private lateinit var editTextName: EditText
    private lateinit var editTextEmail: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var buttonRegister: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        editTextName = findViewById(R.id.editTextName)
        editTextEmail = findViewById(R.id.editTextEmail)
        editTextPassword = findViewById(R.id.editTextPassword)
        buttonRegister = findViewById(R.id.buttonRegister)

        buttonRegister.setOnClickListener {
            val name = editTextName.text.toString().trim()
            val email = editTextEmail.text.toString().trim()
            val password = editTextPassword.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
            } else {
                performRegistration(name, email, password)
            }
        }
    }

    private fun performRegistration(name: String, email: String, password: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Crée un Map pour les paramètres
                val params = mapOf(
                    "name" to name,
                    "email" to email,
                    "password" to password
                )

                // Appel de l'API avec le Map
                val response = RetrofitClient.apiService.register(params)

                if (response.isSuccessful && response.body()?.success == true) {
                    withContext(Dispatchers.Main) {
                        val userId = response.body()?.userId // Récupérer l'ID de l'utilisateur
                        if (userId != null) {
                            // Sauvegarder le user_id dans SharedPreferences
                            val sharedPreferences = getSharedPreferences("MyApp", MODE_PRIVATE)
                            val editor = sharedPreferences.edit()
                            editor.putInt("user_id", userId)
                            editor.apply()

                            // Redirection vers CreateSalonActivity
                            val intent = Intent(this@RegisterActivity, CreateSalonActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this@RegisterActivity, "Erreur lors de l'inscription", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@RegisterActivity, "Erreur lors de l'inscription", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@RegisterActivity, "Erreur réseau : ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
