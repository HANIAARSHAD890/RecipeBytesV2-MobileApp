package com.example.recipebytes.adapters

import android.app.Dialog
import androidx.appcompat.widget.SwitchCompat
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
        holder.iconDelete.visibility = View.GONE

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
