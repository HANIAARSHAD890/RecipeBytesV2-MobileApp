package com.example.recipebytes.adapters

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.recipebytes.R
import com.example.recipebytes.activities.RecipeViewDetailsScreen
import com.example.recipebytes.models.Recipe
import com.example.recipebytes.models.RecipeRepository

class RecipeAdapter(
    private val recipes: MutableList<Recipe>,
    private val currentUserId: String,
    private val userNameMap: Map<String, String>,
    private val favoriteIds: Set<String>,
    private val onDelete: (Recipe) -> Unit,
    private val onTogglePublic: (Recipe, Boolean) -> Unit,
    private val onToggleFavorite: (recipeId: String, isFav: Boolean) -> Unit
) : RecyclerView.Adapter<RecipeAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profileCircle: TextView = view.findViewById(R.id.profileCircle)
        val textUsername: TextView = view.findViewById(R.id.textUsername)
        val switchPublic: Switch = view.findViewById(R.id.switchPublic)
        val title: TextView = view.findViewById(R.id.textTitle)
        val desc: TextView = view.findViewById(R.id.textDescription)
        val delete: ImageView = view.findViewById(R.id.iconDelete)
        val image: ImageView = view.findViewById(R.id.imageRecipe)
        val iconFavorite: TextView = view.findViewById(R.id.iconFavorite)
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

        setupProfileSection(holder, recipe)
        setupToggle(holder, recipe)
        setupFavorite(holder, recipe)
        setupClickListeners(holder, recipe)
        loadImage(holder, recipe)
    }

    private fun setupProfileSection(holder: ViewHolder, recipe: Recipe) {
        val displayName: String
        if (recipe.userId == currentUserId) {
            displayName = "You"
        } else {
            val email = userNameMap[recipe.userId] ?: "User"
            displayName = email.substringBefore("@")
        }
        holder.textUsername.text = displayName
        holder.profileCircle.text = displayName.take(1).uppercase()
    }

    private fun setupToggle(holder: ViewHolder, recipe: Recipe) {
        holder.switchPublic.setOnCheckedChangeListener(null)
        holder.switchPublic.isChecked = recipe.isPublic
        holder.switchPublic.setOnCheckedChangeListener { _, isChecked ->
            recipe.isPublic = isChecked
            onTogglePublic(recipe, isChecked)
        }
    }

    private fun setupFavorite(holder: ViewHolder, recipe: Recipe) {
        val isFav = favoriteIds.contains(recipe.recipeId)
        holder.iconFavorite.text = if (isFav) "\u2764" else "\u2661"
        holder.iconFavorite.setTextColor(if (isFav) Color.parseColor("#E91E63") else Color.GRAY)
        holder.iconFavorite.setOnClickListener {
            val newFav = !favoriteIds.contains(recipe.recipeId)
            onToggleFavorite(recipe.recipeId, newFav)
        }
    }

    private fun setupClickListeners(holder: ViewHolder, recipe: Recipe) {
        holder.delete.setOnClickListener { onDelete(recipe) }

        // ── Delete ────────────────────────────────────────────────────────────
        holder.delete.setOnClickListener { onDelete(recipe) }

        // ── Open details ──────────────────────────────────────────────────────
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, RecipeViewDetailsScreen::class.java)
            intent.putExtra("recipe", recipe)
            holder.itemView.context.startActivity(intent)
        }

    private fun loadImage(holder: ViewHolder, recipe: Recipe) {
        if (!recipe.imageUri.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(recipe.imageUri)
                .placeholder(R.drawable.recipe_default)
                .error(R.drawable.recipe_default)
                .into(holder.image)
        } else {
            holder.image.setImageResource(R.drawable.recipe_default)
        }
    }

    fun refresh(newList: List<Recipe>) {
        recipes.clear()
        recipes.addAll(newList)
        notifyDataSetChanged()
    }
}