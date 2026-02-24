package com.nishantyadav.routesmart

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class DashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val btnStart = findViewById<Button>(R.id.btnStart)

        btnStart.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }
    }
}