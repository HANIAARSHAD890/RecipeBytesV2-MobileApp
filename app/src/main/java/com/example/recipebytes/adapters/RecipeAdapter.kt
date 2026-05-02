package com.example.recipebytes.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.recipebytes.R
import com.example.recipebytes.activities.RecipeViewDetailsScreen
import com.example.recipebytes.models.Recipe

/**
 * Adapter for displaying a list of recipes in a RecyclerView.
 */
class RecipeAdapter(
    private val recipes: MutableList<Recipe>,
    private val onDelete: (Recipe) -> Unit,
    private val onEdit: (Recipe) -> Unit
) : RecyclerView.Adapter<RecipeAdapter.ViewHolder>() {

    /**
     * ViewHolder for recipe list items.
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title = view.findViewById<TextView>(R.id.textTitle)
        val desc = view.findViewById<TextView>(R.id.textDescription)
        val delete = view.findViewById<ImageView>(R.id.iconDelete)
        val image = view.findViewById<ImageView>(R.id.imageRecipe)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = recipes.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val recipe = recipes[position]

        holder.title.text = recipe.title
        holder.desc.text = recipe.description

        setupClickListeners(holder, recipe)
        loadImage(holder, recipe)
    }

    /**
     * Configures click listeners for the delete action and opening details.
     */
    private fun setupClickListeners(holder: ViewHolder, recipe: Recipe) {
        holder.delete.setOnClickListener {
            onDelete(recipe)
        }

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, RecipeViewDetailsScreen::class.java)
            intent.putExtra("recipe", recipe)
            context.startActivity(intent)
        }
    }

    /**
     * Loads the recipe image using Glide or sets a default placeholder.
     */
    private fun loadImage(holder: ViewHolder, recipe: Recipe) {
        if (!recipe.imageUri.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(recipe.imageUri)
                .placeholder(R.drawable.edit_bg)
                .error(R.drawable.ic_launcher_background)
                .into(holder.image)
        } else {
            holder.image.setImageResource(R.drawable.ic_launcher_background)
        }
    }

    /**
     * Refreshes the adapter with a new list of recipes.
     */
    fun refresh(newList: List<Recipe>) {
        recipes.clear()
        recipes.addAll(newList)
        notifyDataSetChanged()
    }
}
