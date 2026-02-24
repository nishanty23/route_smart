package com.nishantyadav.routesmart.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nishantyadav.routesmart.model.Route
import com.nishantyadav.routesmart.R

class RouteAdapter(private val routeList: List<Route>) :
    RecyclerView.Adapter<RouteAdapter.RouteViewHolder>() {

    class RouteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvSource: TextView = itemView.findViewById(R.id.tvSource)
        val tvDestination: TextView = itemView.findViewById(R.id.tvDestination)
        val tvDistance: TextView = itemView.findViewById(R.id.tvDistance)
        val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_route, parent, false)
        return RouteViewHolder(view)
    }

    override fun onBindViewHolder(holder: RouteViewHolder, position: Int) {
        val route = routeList[position]

        holder.tvSource.text = "From: ${route.source}"
        holder.tvDestination.text = "To: ${route.destination}"
        holder.tvDistance.text = "Distance: ${route.distance}"
        holder.tvDuration.text = "Time: ${route.duration}"
    }

    override fun getItemCount(): Int {
        return routeList.size
    }
}