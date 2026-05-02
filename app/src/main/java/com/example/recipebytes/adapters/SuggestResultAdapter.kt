package com.example.recipebytes.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.recipebytes.R
import com.example.recipebytes.models.Recipe

/**
 * Data class representing the result of a recipe suggestion based on matching ingredients.
 */
data class SuggestResult(
    val recipe: Recipe,
    val matchLabel: String,   // "Best Match", "Better Match", "Less Likely"
    val matchCount: Int,
    val totalIngredients: Int
)

/**
 * Adapter for displaying recipe suggestions and their match quality.
 */
class SuggestResultAdapter(
    private val results: MutableList<SuggestResult>
) : RecyclerView.Adapter<SuggestResultAdapter.ViewHolder>() {

    /**
     * ViewHolder for suggestion result list items.
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvSuggestTitle)
        val tvMatchLabel: TextView = view.findViewById(R.id.tvMatchLabel)
        val tvMatchCount: TextView = view.findViewById(R.id.tvMatchCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_suggest_result, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = results.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val result = results[position]
        
        holder.tvTitle.text = result.recipe.title
        holder.tvMatchCount.text =
            "Matching ingredients: ${result.matchCount} / ${result.totalIngredients}"

        setupMatchLabel(holder, result.matchLabel)
    }

    /**
     * Configures the match label's text and styling based on match quality.
     */
    private fun setupMatchLabel(holder: ViewHolder, label: String) {
        holder.tvMatchLabel.text = label
        
        val color = when (label) {
            "Best Match" -> R.color.primary
            "Better Match" -> R.color.secondary
            else -> R.color.blackapp
        }
        
        holder.tvMatchLabel.setBackgroundResource(R.drawable.dot_active)
        holder.tvMatchLabel.setTextColor(
            holder.itemView.context.getColor(android.R.color.white)
        )
    }

    /**
     * Updates the adapter with a new list of suggestion results.
     */
    fun updateResults(newResults: List<SuggestResult>) {
        results.clear()
        results.addAll(newResults)
        notifyDataSetChanged()
    }
}
