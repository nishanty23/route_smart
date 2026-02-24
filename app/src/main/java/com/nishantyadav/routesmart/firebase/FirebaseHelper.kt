package com.nishantyadav.routesmart.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.nishantyadav.routesmart.model.Route

object FirebaseHelper {

    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    fun saveRoute(route: Route) {
        val userId = auth.currentUser?.uid ?: return
        database.child("users")
            .child(userId)
            .child("routes")
            .push()
            .setValue(route)
    }
}