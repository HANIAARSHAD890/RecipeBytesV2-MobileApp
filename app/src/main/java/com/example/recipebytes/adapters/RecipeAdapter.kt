package com.example.recipebytes.adapters

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.recipebytes.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.recipebytes.activities.RecipeViewDetailsScreen
import com.example.recipebytes.models.Recipe
import com.example.recipebytes.models.RecipeRepository

class RecipeAdapter(
    private val recipes: MutableList<Recipe>,
    private val currentUserId: String,
    private val userNameMap: Map<String, String>,
    private val favoriteIds: Set<String>,
    private val likedIds: Set<String>,
    private val userProfileMap: Map<String, String> = emptyMap(),
    private val onDelete: (Recipe) -> Unit,
    private val onTogglePublic: (Recipe, Boolean) -> Unit,
    private val onToggleFavorite: (recipeId: String, isFav: Boolean) -> Unit,
    private val onToggleLike: (recipeId: String, isLiked: Boolean) -> Unit,
    private val onShowLikers: (recipeId: String) -> Unit,
    private val showToggle: Boolean = true
) : RecyclerView.Adapter<RecipeAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profileCircle: TextView = view.findViewById(R.id.profileCircle)
        val profileImage: ImageView? = view.findViewById(R.id.profileImage)
        val textUsername: TextView = view.findViewById(R.id.textUsername)
        val switchPublic: SwitchCompat = view.findViewById(R.id.switchPublic)
        val title: TextView = view.findViewById(R.id.textTitle)
        val desc: TextView = view.findViewById(R.id.textDescription)
        val delete: ImageView = view.findViewById(R.id.iconDelete)
        val image: ImageView = view.findViewById(R.id.imageRecipe)
        val iconFavorite: TextView = view.findViewById(R.id.iconFavorite)
        val iconLike: ImageView = view.findViewById(R.id.iconLike)
        val textLikesCount: TextView = view.findViewById(R.id.textLikesCount)
        val textLikersCount: TextView = view.findViewById(R.id.textLikersCount)
        val textCreatedAt: TextView = view.findViewById(R.id.textCreatedAt)
        val iconShare: ImageView = view.findViewById(R.id.iconShare)
        val iconCopy: ImageView = view.findViewById(R.id.iconCopy)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = recipes.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        try {
            val recipe = recipes[position]

            holder.title.text = recipe.title
            holder.desc.text = recipe.description

            holder.switchPublic.visibility = if (showToggle && recipe.userId == currentUserId) View.VISIBLE else View.GONE

            setupProfileSection(holder, recipe)
            setupToggle(holder, recipe)
            setupFavorite(holder, recipe)
            setupLike(holder, recipe)
            setupClickListeners(holder, recipe)
            loadImage(holder, recipe)

            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            holder.textCreatedAt.text = dateFormat.format(Date(recipe.createdAt))
            holder.textCreatedAt.visibility = View.VISIBLE
        } catch (e: Exception) {
            android.util.Log.e("RecipeAdapter", "bind error at $position", e)
        }
    }

    private fun setupProfileSection(holder: ViewHolder, recipe: Recipe) {
        val displayName = if (recipe.userId == currentUserId) {
            "You"
        } else {
            userNameMap[recipe.userId] ?: "User"
        }
        holder.textUsername.text = displayName

        val profileImageUrl = userProfileMap[recipe.userId]
        if (!profileImageUrl.isNullOrEmpty()) {
            holder.profileImage?.visibility = View.VISIBLE
            holder.profileImage?.let {
                Glide.with(holder.itemView.context)
                    .load(profileImageUrl)
                    .circleCrop()
                    .placeholder(R.drawable.circle_bg)
                    .error(R.drawable.circle_bg)
                    .into(it)
            }
            holder.profileCircle.text = ""
            holder.profileCircle.background = null
        } else {
            holder.profileImage?.visibility = View.GONE
            holder.profileCircle.text = displayName.take(1).uppercase()
        }
    }

    private fun setupToggle(holder: ViewHolder, recipe: Recipe) {
        holder.switchPublic.setOnCheckedChangeListener(null)
        holder.switchPublic.isChecked = recipe.isPublic
        holder.switchPublic.setOnCheckedChangeListener { _, isChecked ->
            val context = holder.itemView.context
            val dialog = Dialog(context)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.dialog_recipe_delete)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            val title = dialog.findViewById<TextView>(R.id.tvDeleteMessage)
            title.text = if (isChecked) "Are you sure you want to make this recipe public?" else "Are you sure you want to make this recipe private?"

            val confirmBtn = dialog.findViewById<Button>(R.id.btnDeleteConfirm)
            confirmBtn.text = if (isChecked) "Public" else "Private"
            confirmBtn.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, if (isChecked) R.color.primary else R.color.red)
            )

            dialog.findViewById<ImageView>(R.id.ivCloseDialog).setOnClickListener {
                holder.switchPublic.isChecked = !isChecked
                dialog.dismiss()
            }

            confirmBtn.setOnClickListener {
                recipe.isPublic = isChecked
                onTogglePublic(recipe, isChecked)
                dialog.dismiss()
            }

            dialog.setOnCancelListener {
                holder.switchPublic.isChecked = !isChecked
            }

            dialog.show()
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

    private fun setupLike(holder: ViewHolder, recipe: Recipe) {
        val isLiked = likedIds.contains(recipe.recipeId)
        holder.iconLike.setImageResource(
            if (isLiked) R.drawable.ic_thumb_up else R.drawable.ic_thumb_up
        )
        holder.iconLike.imageTintList = ContextCompat.getColorStateList(
            holder.itemView.context,
            if (isLiked) R.color.primary else R.color.gray
        )
        holder.textLikesCount.text = recipe.likesCount.toString()
        holder.textLikesCount.setTextColor(
            ContextCompat.getColor(holder.itemView.context,
                if (isLiked) R.color.primary else R.color.gray)
        )
        holder.textLikersCount.text = "${recipe.likesCount} likes"
        holder.textLikersCount.setOnClickListener {
            onShowLikers(recipe.recipeId)
        }
        holder.iconLike.setOnClickListener {
            val newLiked = !likedIds.contains(recipe.recipeId)
            onToggleLike(recipe.recipeId, newLiked)
        }
    }

    private fun setupClickListeners(holder: ViewHolder, recipe: Recipe) {
        holder.delete.visibility = View.GONE

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, RecipeViewDetailsScreen::class.java)
            intent.putExtra("recipe", recipe)
            intent.putExtra("allow_edit", false)
            holder.itemView.context.startActivity(intent)
        }

        holder.iconShare.setOnClickListener {
            val shareText = buildFullRecipeText(recipe)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            holder.itemView.context.startActivity(
                Intent.createChooser(intent, "Share Recipe")
            )
        }

        holder.iconCopy.setOnClickListener {
            val shareText = buildFullRecipeText(recipe)
            val clipboard = holder.itemView.context
                .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Recipe", shareText))
            Toast.makeText(holder.itemView.context, "Recipe copied to clipboard", Toast.LENGTH_SHORT).show()
        }
    }

    private fun buildFullRecipeText(recipe: Recipe): String {
        val sb = StringBuilder()
        sb.appendLine("🍽 ${recipe.title}")
        sb.appendLine()
        if (recipe.description.isNotBlank()) {
            sb.appendLine("📖 ${recipe.description}")
            sb.appendLine()
        }
        sb.appendLine("⏱ Cooking Time: ${recipe.cookingTime} mins")
        sb.appendLine("📂 Category: ${recipe.category}")
        sb.appendLine()
        if (recipe.ingredients.isNotEmpty()) {
            sb.appendLine("🥗 Ingredients:")
            recipe.ingredients.forEachIndexed { i, ing ->
                sb.appendLine("  ${i + 1}. ${ing.quantity} ${ing.name}")
            }
            sb.appendLine()
        }
        if (recipe.steps.isNotEmpty()) {
            sb.appendLine("👨‍🍳 Steps:")
            recipe.steps.forEachIndexed { i, step ->
                sb.appendLine("  ${i + 1}. ${step.text}")
            }
            sb.appendLine()
        }
        recipe.nutrition?.let { n ->
            if (n.calories > 0 || n.carbs > 0f || n.fat > 0f || n.protein > 0f) {
                sb.appendLine("📊 Nutrition:")
                if (n.calories > 0) sb.appendLine("  Calories: ${n.calories} kcal")
                if (n.carbs > 0f) sb.appendLine("  Carbs: ${n.carbs}g")
                if (n.fat > 0f) sb.appendLine("  Fat: ${n.fat}g")
                if (n.protein > 0f) sb.appendLine("  Protein: ${n.protein}g")
                sb.appendLine()
            }
        }
        if (!recipe.imageUri.isNullOrEmpty()) {
            sb.appendLine("📸 ${recipe.imageUri}")
        }
        sb.appendLine()
        sb.append("— Shared from RecipeBytes")
        return sb.toString()
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
