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
import com.example.recipebytes.models.RecipeRepository

class RecipeAdapter(
    private val recipes: MutableList<Recipe>,
    private val onDelete: (Recipe) -> Unit,
    private val onEdit: (Recipe) -> Unit
) : RecyclerView.Adapter<RecipeAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView     = view.findViewById(R.id.textTitle)
        val desc: TextView      = view.findViewById(R.id.textDescription)
        val delete: ImageView   = view.findViewById(R.id.iconDelete)
        val image: ImageView    = view.findViewById(R.id.imageRecipe)
        val favorite: ImageView = view.findViewById(R.id.iconFavorite)
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
        holder.desc.text  = recipe.description

        // ── Favorite star ─────────────────────────────────────────────────────
        // Read fresh from repository so state is always accurate
        val isFav = RecipeRepository.getAllRecipes()
            .find { it.title == recipe.title }?.isFavorite ?: false

        holder.favorite.setImageResource(
            if (isFav) android.R.drawable.btn_star_big_on
            else       android.R.drawable.btn_star_big_off
        )

        holder.favorite.setOnClickListener {
            RecipeRepository.toggleFavorite(holder.itemView.context, recipe.title)
            notifyItemChanged(position)  // re-bind this item to refresh star
        }

        // ── Delete ────────────────────────────────────────────────────────────
        holder.delete.setOnClickListener { onDelete(recipe) }

        // ── Open details ──────────────────────────────────────────────────────
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, RecipeViewDetailsScreen::class.java)
            intent.putExtra("recipe", recipe)
            holder.itemView.context.startActivity(intent)
        }

        // ── Image ─────────────────────────────────────────────────────────────
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

    fun refresh(newList: List<Recipe>) {
        recipes.clear()
        recipes.addAll(newList)
        notifyDataSetChanged()
    }
}