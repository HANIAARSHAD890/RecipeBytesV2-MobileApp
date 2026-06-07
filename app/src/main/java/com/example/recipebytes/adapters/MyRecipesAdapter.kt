package com.example.recipebytes.adapters

import android.app.Dialog
import androidx.appcompat.widget.SwitchCompat
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
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.recipebytes.R
import com.example.recipebytes.activities.RecipeViewDetailsScreen
import com.example.recipebytes.models.Recipe
import com.example.recipebytes.models.RecipeRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyRecipesAdapter(
    private val recipes: MutableList<Recipe>,
    private val currentUserId: String
) : RecyclerView.Adapter<MyRecipesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profileCircle: TextView = view.findViewById(R.id.profileCircle)
        val textUsername: TextView = view.findViewById(R.id.textUsername)
        val title: TextView = view.findViewById(R.id.textTitle)
        val desc: TextView = view.findViewById(R.id.textDescription)
        val image: ImageView = view.findViewById(R.id.imageRecipe)
        val textCreatedAt: TextView = view.findViewById(R.id.textCreatedAt)
        val switchPublic: SwitchCompat = view.findViewById(R.id.switchPublic)
        val iconDelete: ImageView = view.findViewById(R.id.iconDelete)
        val iconFavorite: TextView = view.findViewById(R.id.iconFavorite)
        val iconLike: ImageView = view.findViewById(R.id.iconLike)
        val textLikesCount: TextView = view.findViewById(R.id.textLikesCount)
        val textLikersCount: TextView = view.findViewById(R.id.textLikersCount)
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
        val recipe = recipes[position]

        holder.title.text = recipe.title
        holder.desc.text = recipe.description
        holder.textUsername.text = "You"
        holder.profileCircle.text = "You".take(1).uppercase()

        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        holder.textCreatedAt.text = "${dateFormat.format(Date(recipe.createdAt))}"
        holder.textCreatedAt.visibility = View.VISIBLE

        holder.switchPublic.isChecked = recipe.isPublic
        holder.switchPublic.setOnCheckedChangeListener(null)
        holder.switchPublic.setOnCheckedChangeListener { _, isChecked ->
            val context = holder.itemView.context
            val dialog = Dialog(context)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.dialog_recipe_delete)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            val titleView = dialog.findViewById<TextView>(R.id.tvDeleteMessage)
            titleView.text = if (isChecked) "Are you sure you want to make this recipe public?" else "Are you sure you want to make this recipe private?"

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
                RecipeRepository.updateRecipe(context, recipe.title, recipe)
                dialog.dismiss()
            }

            dialog.setOnCancelListener {
                holder.switchPublic.isChecked = !isChecked
            }

            dialog.show()
        }

        // Like
        holder.iconLike.alpha = 0.5f
        holder.iconLike.isEnabled = false
        holder.textLikesCount.text = recipe.likesCount.toString()
        holder.textLikersCount.text = "${recipe.likesCount} likes"
        holder.textLikersCount.setOnClickListener {
            Toast.makeText(holder.itemView.context, "View likes in Explore", Toast.LENGTH_SHORT).show()
        }

        // Favorite
        holder.iconFavorite.alpha = 0.5f
        holder.iconFavorite.isEnabled = false
        holder.iconFavorite.setOnClickListener(null)

        // Share
        holder.iconShare.setOnClickListener {
            val shareText = "${recipe.title}\n\n${recipe.description}"
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            holder.itemView.context.startActivity(
                Intent.createChooser(intent, "Share Recipe")
            )
        }

        // Copy
        holder.iconCopy.setOnClickListener {
            val shareText = "${recipe.title}\n\n${recipe.description}"
            val clipboard = holder.itemView.context
                .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Recipe", shareText))
            Toast.makeText(holder.itemView.context, "Recipe copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        // Delete
        holder.iconDelete.setOnClickListener {
            val context = holder.itemView.context
            val dialog = Dialog(context)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.dialog_recipe_delete)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            val tvMessage = dialog.findViewById<TextView>(R.id.tvDeleteMessage)
            tvMessage.text = "Are you sure you want to delete \"${recipe.title}\"?"

            dialog.findViewById<ImageView>(R.id.ivCloseDialog).setOnClickListener { dialog.dismiss() }
            dialog.findViewById<Button>(R.id.btnDeleteConfirm).setOnClickListener {
                val pos = holder.bindingAdapterPosition
                if (pos >= 0) {
                    RecipeRepository.deleteRecipe(context, recipe.title)
                    recipes.removeAt(pos)
                    notifyItemRemoved(pos)
                }
                dialog.dismiss()
            }
            dialog.show()
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, RecipeViewDetailsScreen::class.java)
            intent.putExtra("recipe", recipe)
            holder.itemView.context.startActivity(intent)
        }

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