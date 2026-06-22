package com.nishantyadav.routesmart.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nishantyadav.routesmart.databinding.ItemPlaceSuggestionBinding
import com.nishantyadav.routesmart.model.NearbyPlace

/**
 * PlaceSuggestionsAdapter — shows smart route suggestions in a horizontal RecyclerView.
 * Uses ListAdapter for efficient diffing (no full dataset reloads).
 */
class PlaceSuggestionsAdapter(
    private val onItemClick: (NearbyPlace) -> Unit
) : ListAdapter<NearbyPlace, PlaceSuggestionsAdapter.ViewHolder>(DiffCallback) {

    // ── ViewHolder ──────────────────────────────────
    inner class ViewHolder(
        private val binding: ItemPlaceSuggestionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(place: NearbyPlace) {
            binding.tvEmoji.text       = place.emoji()
            binding.tvName.text        = place.name
            binding.tvType.text        = place.type.replaceFirstChar { it.uppercase() }
            binding.tvDistance.text    = place.distanceLabel()

            binding.root.setOnClickListener { onItemClick(place) }
        }
    }

    // ── Inflation ───────────────────────────────────
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPlaceSuggestionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // ── DiffCallback ────────────────────────────────
    companion object DiffCallback : DiffUtil.ItemCallback<NearbyPlace>() {
        override fun areItemsTheSame(old: NearbyPlace, new: NearbyPlace) =
            old.name == new.name && old.lat == new.lat

        override fun areContentsTheSame(old: NearbyPlace, new: NearbyPlace) =
            old == new
    }
}
