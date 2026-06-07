package com.example.recipebytes.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.recipebytes.R

class SuggestResultAdapter(
    private val results: MutableList<SuggestResult>,
    private val onClick: (SuggestResult) -> Unit
) : RecyclerView.Adapter<SuggestResultAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView       = view.findViewById(R.id.tvSuggestTitle)
        val tvLabel: TextView       = view.findViewById(R.id.tvSuggestLabel)
        val tvHaveCount: TextView   = view.findViewById(R.id.tvHaveCount)
        val tvMissingCount: TextView= view.findViewById(R.id.tvMissingCount)
        val tvCookTime: TextView    = view.findViewById(R.id.tvCookTime)
        val progressMatch: ProgressBar = view.findViewById(R.id.progressMatch)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_suggest_result, parent, false)
        )

    override fun getItemCount() = results.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val result  = results[position]
        val missing = result.totalCount - result.matchCount
        val percent = ((result.matchCount.toFloat() / result.totalCount) * 100).toInt()

        holder.tvTitle.text        = result.recipe.title
        holder.tvLabel.text        = result.label
        holder.tvHaveCount.text    = "✅ Have ${result.matchCount}"
        holder.tvMissingCount.text = "Missing $missing"
        holder.tvCookTime.text     = "⏱ ${result.recipe.cookingTime} min"
        holder.progressMatch.progress = percent

        val (bgColor, textColor) = when (result.label) {
            "Can Make Now"  -> "#4CAF50" to "#FFFFFF"
            "Almost There"  -> "#FF9800" to "#FFFFFF"
            else            -> "#F44336" to "#FFFFFF"
        }
        holder.tvLabel.setBackgroundColor(Color.parseColor(bgColor))
        holder.tvLabel.setTextColor(Color.parseColor(textColor))

        holder.itemView.setOnClickListener { onClick(result) }
    }

    fun updateResults(newResults: List<SuggestResult>) {
        results.clear()
        results.addAll(newResults)
        notifyDataSetChanged()
    }
}