package com.example.recipebytes.fragments

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recipebytes.R
import com.example.recipebytes.activities.AddRecipeActivity
import com.example.recipebytes.adapters.StepsAdapter
import com.example.recipebytes.models.Step
import java.util.Collections

// Fragment for step 3 of adding a recipe: cooking steps with drag-to-reorder
class AddRecipeFragment3 : Fragment(R.layout.activity_add_recipe_fragment3) {

    private val stepsList = mutableListOf<Step>()
    private lateinit var adapter: StepsAdapter
    private lateinit var touchHelper: ItemTouchHelper  // ← declare here

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recycler = view.findViewById<RecyclerView>(R.id.stepsRecycler)
        val addBtn   = view.findViewById<Button>(R.id.btnAddStep)
        val nextBtn  = view.findViewById<Button>(R.id.btnNext3)

        setupRecyclerView(recycler)
        setupTouchHelper(recycler)   // ← call here inside onViewCreated

        addBtn.setOnClickListener { addNewStep() }
        nextBtn.setOnClickListener { validateAndProceed(recycler) }
    }

    private fun setupRecyclerView(recycler: RecyclerView) {
        if (stepsList.isEmpty()) {
            val aiSteps = (activity as? AddRecipeActivity)?.getAiSteps()
            if (!aiSteps.isNullOrEmpty()) {
                stepsList.addAll(aiSteps)
            } else {
                stepsList.add(Step(text = ""))
            }
        }
        adapter = StepsAdapter(stepsList)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter
    }

    private fun setupTouchHelper(recycler: RecyclerView) {
        touchHelper = ItemTouchHelper(
            object : ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
            ) {
                override fun onMove(
                    rv: RecyclerView,
                    vh: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    val from = vh.bindingAdapterPosition
                    val to   = target.bindingAdapterPosition
                    Collections.swap(stepsList, from, to)
                    adapter.notifyItemMoved(from, to)
                    adapter.notifyItemRangeChanged(minOf(from, to), maxOf(from, to) - minOf(from, to) + 1)
                    return true
                }
                override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {}
            }
        )
        touchHelper.attachToRecyclerView(recycler)  // ← attach here
    }

    private fun addNewStep() {
        stepsList.add(Step(text = ""))
        adapter.notifyItemInserted(stepsList.size - 1)
    }

    private fun validateAndProceed(recycler: RecyclerView) {
        var hasError = false
        for (i in 0 until stepsList.size) {
            if (stepsList[i].text.trim().isEmpty()) {
                hasError = true
                val holder = recycler.findViewHolderForAdapterPosition(i)
                        as? StepsAdapter.ViewHolder
                holder?.tilStepContent?.error = "Step description required"
            }
        }
        if (!hasError && stepsList.isNotEmpty()) {
            (activity as? AddRecipeActivity)?.goToStep4(stepsList)
        }
    }
}