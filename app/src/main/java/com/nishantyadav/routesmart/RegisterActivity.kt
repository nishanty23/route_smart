package com.nishantyadav.routesmart

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.nishantyadav.routesmart.firebase.FirebaseHelper

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val email = findViewById<EditText>(R.id.etEmail)
        val password = findViewById<EditText>(R.id.etPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        btnRegister.setOnClickListener {
            FirebaseHelper.auth.createUserWithEmailAndPassword(
                email.text.toString(),
                password.text.toString()
            ).addOnSuccessListener {
                startActivity(Intent(this, DashboardActivity::class.java))
                finish()
            }
        }
    }
}