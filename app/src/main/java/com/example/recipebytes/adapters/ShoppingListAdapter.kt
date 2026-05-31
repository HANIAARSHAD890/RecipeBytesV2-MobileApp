package com.example.recipebytes.adapters

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recipebytes.R
import com.example.recipebytes.models.ShoppingList
import com.example.recipebytes.models.ShoppingListItem
import com.example.recipebytes.services.ShoppingListService

class ShoppingListAdapter(
    private val lists: MutableList<ShoppingList>,
    private val onDelete: (ShoppingList) -> Unit
) : RecyclerView.Adapter<ShoppingListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView   = view.findViewById(R.id.tvShoppingRecipeTitle)
        val rvItems: RecyclerView = view.findViewById(R.id.rvShoppingItems)
        val btnDelete: ImageView = view.findViewById(R.id.btnDeleteShoppingList)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shopping_list, parent, false))

    override fun getItemCount() = lists.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val list = lists[position]
        holder.tvTitle.text = "🛒 ${list.recipeTitle}"

        val itemsAdapter = ShoppingItemsAdapter(list.items.toMutableList(), list.id)
        holder.rvItems.layoutManager = LinearLayoutManager(holder.itemView.context)
        holder.rvItems.adapter = itemsAdapter

        holder.btnDelete.setOnClickListener { onDelete(list) }
    }

    fun updateLists(newLists: List<ShoppingList>) {
        lists.clear()
        lists.addAll(newLists)
        notifyDataSetChanged()
    }
}

class ShoppingItemsAdapter(
    private val items: MutableList<ShoppingListItem>,
    private val listId: String
) : RecyclerView.Adapter<ShoppingItemsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cb: CheckBox      = view.findViewById(R.id.cbShoppingItem)
        val tvName: TextView  = view.findViewById(R.id.tvShoppingItemName)
        val tvQty: TextView   = view.findViewById(R.id.tvShoppingItemQty)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shopping_item, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvName.text = item.name
        holder.tvQty.text  = item.quantity
        holder.cb.isChecked = item.isChecked

        // Strikethrough if checked
        if (item.isChecked) {
            holder.tvName.paintFlags = holder.tvName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            holder.tvName.paintFlags = holder.tvName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }

        holder.cb.setOnCheckedChangeListener { _, isChecked ->
            item.isChecked = isChecked
            if (isChecked) {
                holder.tvName.paintFlags = holder.tvName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                holder.tvName.paintFlags = holder.tvName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
            ShoppingListService.updateItemChecked(listId, position, isChecked)
        }
    }
}