package com.example.recipebytes.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import com.example.recipebytes.R
import com.example.recipebytes.models.Step
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * Adapter for managing and displaying the list of preparation steps for a recipe.
 */
class StepsAdapter(
    private val list: MutableList<Step>,
    var isEditable: Boolean = true
) : RecyclerView.Adapter<StepsAdapter.ViewHolder>() {

    /**
     * ViewHolder class for individual recipe step items.
     */
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val stepNumber: TextView = view.findViewById(R.id.tvStepNumber)
        val etStepContent: TextInputEditText = view.findViewById(R.id.etStepContent)
        val tilStepContent: TextInputLayout = view.findViewById(R.id.tStepContent)
        val btnDeleteStep: ImageView = view.findViewById(R.id.btnDeleteStep)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_step, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val step = list[position]

        holder.stepNumber.text = "${position + 1}."
        holder.etStepContent.isEnabled = isEditable
        holder.btnDeleteStep.visibility = if (isEditable) View.VISIBLE else View.GONE

        if (isEditable && position == list.size - 1 && holder.etStepContent.text.isNullOrEmpty()) {
            holder.etStepContent.requestFocus()
        }

        if (!isEditable) {
            setupViewMode(holder, step)
        } else {
            setupEditMode(holder, step)
        }
    }

    /**
     * Configures the UI for read-only view mode.
     */
    private fun setupViewMode(holder: ViewHolder, step: Step) {
        holder.etStepContent.setText(step.text)
        holder.tilStepContent.boxStrokeWidth = 0
        holder.tilStepContent.boxStrokeWidthFocused = 0
        holder.etStepContent.background = null
    }

    /**
     * Configures the UI for edit mode, including delete and text change listeners.
     */
    private fun setupEditMode(holder: ViewHolder, step: Step) {
        if (holder.etStepContent.text.toString() != step.text) {
            holder.etStepContent.setText(step.text)
        }

        holder.btnDeleteStep.setOnClickListener {
            val currentPos = holder.bindingAdapterPosition
            if (currentPos != RecyclerView.NO_POSITION && list.size > currentPos) {
                list.removeAt(currentPos)
                notifyItemRemoved(currentPos)
                notifyItemRangeChanged(currentPos, list.size)
            }
        }

        holder.etStepContent.addTextChangedListener { text ->
            val currentPos = holder.bindingAdapterPosition
            if (currentPos != RecyclerView.NO_POSITION && currentPos < list.size) {
                val input = text.toString()
                validateInput(holder, input, currentPos)
            }
        }
    }

    /**
     * Validates the step content input and updates the data model "Step"
     */
    private fun validateInput(holder: ViewHolder, input: String, position: Int) {
        when {
            input.trim().isEmpty() -> {
                holder.tilStepContent.error = "Step cannot be blank"
            }
            input.length < 5 -> {
                holder.tilStepContent.error = "Description too short"
            }
            else -> {
                holder.tilStepContent.error = null
                list[position].text = input
            }
        }
    }
}
