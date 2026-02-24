package com.nishantyadav.routesmart

import com.nishantyadav.routesmart.R
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.nishantyadav.routesmart.firebase.FirebaseHelper

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val email = findViewById<EditText>(R.id.etEmail)
        val password = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        btnLogin.setOnClickListener {
            FirebaseHelper.auth.signInWithEmailAndPassword(
                email.text.toString(),
                password.text.toString()
            ).addOnSuccessListener {
                startActivity(Intent(this, DashboardActivity::class.java))
                finish()
            }
        }
    }
}