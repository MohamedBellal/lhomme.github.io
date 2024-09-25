package com.example.barber

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AddBarbersActivity : AppCompatActivity() {

    private lateinit var buttonAddBarber: Button
    private lateinit var barberListLayout: LinearLayout
    private lateinit var textViewPageTitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_barbers)

        // Initialize views
        textViewPageTitle = findViewById(R.id.textViewPageTitle)
        buttonAddBarber = findViewById(R.id.buttonAddBarber)
        barberListLayout = findViewById(R.id.barberListLayout)

        textViewPageTitle.text = "Création du salon: 2/2"

        buttonAddBarber.setOnClickListener {
            addBarberInputField()
        }
    }

    private fun addBarberInputField() {
        val barberInput = EditText(this).apply {
            hint = "Nom du coiffeur"
        }
        barberListLayout.addView(barberInput)
    }
}
