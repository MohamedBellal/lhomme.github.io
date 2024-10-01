package com.example.barber

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.barber.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var editTextEmail: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var buttonLogin: Button
    private lateinit var buttonLoginAdmin: Button  // Bouton pour connexion admin

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        editTextEmail = findViewById(R.id.editTextEmail)
        editTextPassword = findViewById(R.id.editTextPassword)
        buttonLogin = findViewById(R.id.buttonLogin)
        buttonLoginAdmin = findViewById(R.id.buttonLoginAdmin)  // Associe le nouveau bouton

        buttonLogin.setOnClickListener {
            val email = editTextEmail.text.toString().trim()
            val password = editTextPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Veuillez entrer vos identifiants", Toast.LENGTH_SHORT).show()
            } else {
                performLogin(email, password)
            }
        }

        // Ajoute le comportement pour le bouton admin
        buttonLoginAdmin.setOnClickListener {
            // Remplit les champs avec "admin" et tente une connexion
            editTextEmail.setText("admin")
            editTextPassword.setText("admin")
            performLogin("admin", "admin")
        }
    }

    private fun performLogin(email: String, password: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.login(email, password)
                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    if (loginResponse?.success == true) {
                        val token = loginResponse.token
                        val salonId = loginResponse.salon_id

                        if (!token.isNullOrEmpty() && salonId != null) {
                            withContext(Dispatchers.Main) {
                                val sharedPreferences = getSharedPreferences("MyApp", Context.MODE_PRIVATE)
                                val editor = sharedPreferences.edit()
                                editor.putString("token", token)
                                editor.putInt("salon_id", salonId)
                                editor.apply()

                                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@LoginActivity, "Erreur: token ou salon_id manquant", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@LoginActivity, "Erreur lors de la connexion: ${loginResponse?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@LoginActivity, "Erreur lors de la connexion : ${response.message()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LoginActivity, "Erreur réseau : ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
