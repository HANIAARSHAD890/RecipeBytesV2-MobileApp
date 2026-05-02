package com.example.recipebytes.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import com.example.recipebytes.models.Ingredient
import com.example.recipebytes.R
import com.google.android.material.textfield.TextInputLayout

/**
 * Adapter for managing and displaying a list of ingredients.
 */
class IngredientAdapter(
    val list: MutableList<Ingredient>,
    var isEditMode: Boolean = false
) : RecyclerView.Adapter<IngredientAdapter.ViewHolder>() {

    /**
     * ViewHolder class for individual ingredient items.
     */
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name = view.findViewById<EditText>(R.id.etIngredientName)
        val qty = view.findViewById<EditText>(R.id.etQuantity)
        val tilName = view.findViewById<TextInputLayout>(R.id.tilIngredientName)
        val tilQty = view.findViewById<TextInputLayout>(R.id.tilQuantity)
        val delete = view.findViewById<ImageView>(R.id.btnDeleteIngredient)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ingredient, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]

        holder.name.setText(item.name)
        holder.qty.setText(item.quantity)
        holder.name.isEnabled = isEditMode
        holder.qty.isEnabled = isEditMode

        holder.delete.visibility = if (isEditMode) View.VISIBLE else View.GONE

        if (isEditMode) {
            setupEditModeListeners(holder, item)
        }
    }

    /**
     * Sets up text change and click listeners for editing and deleting ingredients.
     */
    private fun setupEditModeListeners(holder: ViewHolder, item: Ingredient) {
        holder.name.addTextChangedListener { text ->
            val input = text.toString().trim()
            when {
                input.isEmpty() -> holder.tilName.error = "Blank spaces not allowed"
                input.length < 2 -> holder.tilName.error = "Name too short"
                else -> {
                    holder.tilName.error = null
                    item.name = input
                }
            }
        }
        
        holder.qty.addTextChangedListener { text ->
            val input = text.toString().trim()
            if (input.isEmpty()) {
                holder.tilQty.error = "Required"
            } else {
                holder.tilQty.error = null
                item.quantity = input
            }
        }

        holder.delete.setOnClickListener {
            val currentPos = holder.bindingAdapterPosition
            if (currentPos != RecyclerView.NO_POSITION) {
                list.removeAt(currentPos)
                notifyItemRemoved(currentPos)
            }
        }
    }
}
