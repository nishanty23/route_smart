package com.nishantyadav.routesmart

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.nishantyadav.routesmart.firebase.FirebaseHelper
import com.nishantyadav.routesmart.model.Route

class MapActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        val btnFetch = findViewById<Button>(R.id.btnFetchRoute)

        btnFetch.setOnClickListener {

            val route = Route(
                source = "Delhi",
                destination = "Noida",
                distance = "15 km",
                duration = "30 min"
            )

            FirebaseHelper.saveRoute(route)
        }
    }
}