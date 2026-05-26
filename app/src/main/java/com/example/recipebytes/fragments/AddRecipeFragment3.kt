package com.example.recipebytes.fragments

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recipebytes.R
import com.example.recipebytes.activities.AddRecipeActivity
import com.example.recipebytes.adapters.StepsAdapter
import com.example.recipebytes.models.Step

/**
 * Third step of adding a recipe, adding preparation steps.
 */
class AddRecipeFragment3 : Fragment(R.layout.activity_add_recipe_fragment3) {
    private val stepsList = mutableListOf<Step>()
    private lateinit var adapter: StepsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recycler = view.findViewById<RecyclerView>(R.id.stepsRecycler)
        val addBtn = view.findViewById<Button>(R.id.btnAddStep)
        val nextBtn = view.findViewById<Button>(R.id.btnNext3)

        setupRecyclerView(recycler)

        addBtn.setOnClickListener {
            addNewStep()
        }

        nextBtn.setOnClickListener {
            validateAndProceed(recycler)
        }
    }

    /**
     * Configures the RecyclerView with the preparation steps adapter.
     */
    private fun setupRecyclerView(recycler: RecyclerView) {
        if (stepsList.isEmpty()) {
            stepsList.add(Step(text = ""))
        }
        adapter = StepsAdapter(stepsList)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter
    }

    /**
     * Adds a new blank step to the preparation list.
     */
    private fun addNewStep() {
        stepsList.add(Step(text = ""))
        adapter.notifyItemInserted(stepsList.size - 1)
    }

    /**
     * Validates preparation steps and proceeds to the final step if valid.
     */
    private fun validateAndProceed(recycler: RecyclerView) {
        var hasError = false

        for (i in 0 until stepsList.size) {
            if (stepsList[i].text.trim().isEmpty()) {
                hasError = true
                val holder = recycler.findViewHolderForAdapterPosition(i) as? StepsAdapter.ViewHolder
                holder?.tilStepContent?.error = "Step description required"
            }
        }

        if (!hasError && stepsList.isNotEmpty()) {
            (activity as? AddRecipeActivity)?.goToStep4(stepsList)
        }
    }
}
