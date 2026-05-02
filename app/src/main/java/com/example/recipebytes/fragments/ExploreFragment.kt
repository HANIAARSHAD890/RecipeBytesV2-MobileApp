package com.example.recipebytes.fragments

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recipebytes.R
import com.example.recipebytes.activities.AddRecipeActivity
import com.example.recipebytes.adapters.RecipeAdapter
import com.example.recipebytes.models.Recipe
import com.example.recipebytes.models.RecipeRepository
import com.google.android.material.textfield.TextInputEditText
import java.util.Locale

/**
 * Fragment that allows users to explore, search, filter, and sort all available recipes.
 */
class ExploreFragment : Fragment() {

    private lateinit var adapter: RecipeAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var categoryDropdown: AutoCompleteTextView
    private lateinit var etSearch: TextInputEditText
    private lateinit var tvNoRecipes: TextView
    private var isAscending = true
    private var isGridView = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_recepies_view_screen, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupCategoryDropdown(view)
        setupRecipeAdapter()
        setupEventListeners(view)

        performFilterAndSort("")
    }

    /**
     * Initializes UI references and sets default header text.
     */
    private fun initializeViews(view: View) {
        recyclerView = view.findViewById(R.id.recipeRecyclerView)
        tvNoRecipes = view.findViewById(R.id.tvNoRecipes)
        etSearch = view.findViewById(R.id.etSearch)
        view.findViewById<TextView>(R.id.headerTitle).text = "Recipes to Explore"
    }

    /**
     * Sets up the category filter dropdown with predefined recipe categories.
     */
    private fun setupCategoryDropdown(view: View) {
        categoryDropdown = view.findViewById(R.id.autoCompleteCategory)
        val categories = arrayOf("All", "Breakfast", "Lunch", "Dinner", "Dessert", "Baked Goods")
        val adapterCategory = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, categories)
        categoryDropdown.setAdapter(adapterCategory)
        categoryDropdown.setText("All", false)
        categoryDropdown.setOnItemClickListener { _, _, _, _ ->
            performFilterAndSort(etSearch.text.toString())
        }
    }

    /**
     * Configures the recipe adapter with delete and edit callbacks.
     */
    private fun setupRecipeAdapter() {
        adapter = RecipeAdapter(
            mutableListOf(),
            onDelete = { recipe ->
                showDeleteConfirmationDialog(requireContext(), recipe)
            },
            onEdit = {
                performFilterAndSort(etSearch.text.toString())
            }
        )
        updateLayoutManager()
        recyclerView.adapter = adapter
    }

    /**
     * Sets up listeners for layout toggling, searching, sorting, and adding recipes.
     */
    private fun setupEventListeners(view: View) {
        val layoutToggleBtn = view.findViewById<ImageView>(R.id.ivLayoutToggle)
        val sortBtn = view.findViewById<ImageView>(R.id.ivSort)
        val addBtn = view.findViewById<ImageView>(R.id.btnAdd)

        layoutToggleBtn.setOnClickListener {
            isGridView = !isGridView
            layoutToggleBtn.alpha = if (isGridView) 0.5f else 1.0f
            updateLayoutManager()
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                performFilterAndSort(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        sortBtn.setOnClickListener {
            isAscending = !isAscending
            sortBtn.alpha = if (isAscending) 0.5f else 1.0f
            performFilterAndSort(etSearch.text.toString())
        }

        addBtn.setOnClickListener {
            startActivity(Intent(requireContext(), AddRecipeActivity::class.java))
        }
    }

    /**
     * Refreshes data when the fragment becomes visible again.
     */
    override fun onResume() {
        super.onResume()
        performFilterAndSort(etSearch.text.toString())
    }

    /**
     * Filters and sorts the recipe list based on category, search query, and sort order.
     */
    private fun performFilterAndSort(query: String) {
        val allRecipes = RecipeRepository.getAllRecipes()
        val selectedCategory = categoryDropdown.text.toString()
        val lowerQuery = query.lowercase(Locale.ROOT).trim()

        val filteredList = allRecipes.filter { recipe ->
            val matchesCategory = if (selectedCategory == "All") true
            else recipe.category?.equals(selectedCategory, ignoreCase = true) == true

            val matchesSearch = if (lowerQuery.isEmpty()) true
            else recipe.title.lowercase(Locale.ROOT).contains(lowerQuery) ||
                    recipe.description.lowercase(Locale.ROOT).contains(lowerQuery)

            matchesCategory && matchesSearch
        }

        val sortedList = if (isAscending) {
            filteredList.sortedBy { it.title.lowercase() }
        } else {
            filteredList.sortedByDescending { it.title.lowercase() }
        }

        tvNoRecipes.visibility = if (sortedList.isEmpty()) View.VISIBLE else View.GONE
        recyclerView.visibility = if (sortedList.isEmpty()) View.GONE else View.VISIBLE
        adapter.refresh(sortedList)
    }

    /**
     * Switches between list and grid layout managers.
     */
    private fun updateLayoutManager() {
        val spanCount = if (isGridView) 2 else 1
        recyclerView.layoutManager = GridLayoutManager(requireContext(), spanCount)
    }

    /**
     * Displays a confirmation dialog before deleting a recipe.
     */
    private fun showDeleteConfirmationDialog(context: Context, recipe: Recipe) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_recipe_delete)
        dialog.window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))

        val btnConfirmDelete = dialog.findViewById<Button>(R.id.btnDeleteConfirm)
        val ivClose = dialog.findViewById<ImageView>(R.id.ivCloseDialog)
        val tvMessage = dialog.findViewById<TextView>(R.id.tvDeleteMessage)

        tvMessage.text = "Are you sure you want to delete \"${recipe.title}\"?"

        btnConfirmDelete.setOnClickListener {
            RecipeRepository.deleteRecipe(context, recipe.title)
            performFilterAndSort(etSearch.text.toString())
            dialog.dismiss()
        }

        ivClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
}
