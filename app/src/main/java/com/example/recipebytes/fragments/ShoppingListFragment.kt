package com.example.recipebytes.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recipebytes.R
import com.example.recipebytes.adapters.ShoppingListAdapter
import com.example.recipebytes.models.ShoppingList
import com.example.recipebytes.services.ShoppingListService

class ShoppingListFragment : Fragment(R.layout.fragment_shopping_list) {

    private lateinit var adapter: ShoppingListAdapter
    private val lists = mutableListOf<ShoppingList>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val headerTitle    = view.findViewById<TextView>(R.id.headerTitle)
        headerTitle.text   = "Shopping Lists"

        val rv             = view.findViewById<RecyclerView>(R.id.rvShoppingLists)
        val layoutEmpty    = view.findViewById<LinearLayout>(R.id.layoutEmptyShopping)

        adapter = ShoppingListAdapter(lists) { list ->
            ShoppingListService.deleteShoppingList(list.id)
            lists.remove(list)
            adapter.notifyDataSetChanged()
            if (lists.isEmpty()) {
                layoutEmpty.visibility = View.VISIBLE
                rv.visibility          = View.GONE
            }
            Toast.makeText(requireContext(), "Deleted", Toast.LENGTH_SHORT).show()
        }

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        // Load from Firebase
        ShoppingListService.getShoppingLists { fetchedLists ->
            activity?.runOnUiThread {
                lists.clear()
                lists.addAll(fetchedLists)
                adapter.notifyDataSetChanged()
                if (lists.isEmpty()) {
                    layoutEmpty.visibility = View.VISIBLE
                    rv.visibility          = View.GONE
                } else {
                    layoutEmpty.visibility = View.GONE
                    rv.visibility          = View.VISIBLE
                }
            }
        }
    }
}