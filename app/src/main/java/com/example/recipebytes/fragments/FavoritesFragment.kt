package com.example.recipebytes.fragments

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recipebytes.R
import com.example.recipebytes.adapters.RecipeAdapter
import com.example.recipebytes.models.RecipeRepository

class FavoritesFragment : Fragment(R.layout.fragment_favorites) {

    private lateinit var adapter: RecipeAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.headerTitle).text = "Favourites"

        val recycler = view.findViewById<RecyclerView>(R.id.favoritesRecycler)
        val tvEmpty  = view.findViewById<TextView>(R.id.tvNoFavorites)

        val favorites = RecipeRepository.getFavorites().toMutableList()

        if (favorites.isEmpty()) {
            tvEmpty.visibility  = View.VISIBLE
            recycler.visibility = View.GONE
        } else {
            tvEmpty.visibility  = View.GONE
            recycler.visibility = View.VISIBLE
        }

        adapter = RecipeAdapter(
            favorites,
            onDelete = {},   // no delete from favorites screen
            onEdit   = {}
        )
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized) {
            val favorites = RecipeRepository.getFavorites().toMutableList()
            adapter.refresh(favorites)
        }
    }
}