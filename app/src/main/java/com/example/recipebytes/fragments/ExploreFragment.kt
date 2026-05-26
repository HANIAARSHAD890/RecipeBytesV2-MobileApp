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
import android.widget.Toast
import com.example.recipebytes.R
import com.example.recipebytes.activities.AddRecipeActivity
import com.example.recipebytes.adapters.RecipeAdapter
import com.example.recipebytes.models.RecipeRepository
import com.example.recipebytes.services.FirebaseAuthService
import com.example.recipebytes.services.FirebaseRecipeService
import com.google.android.material.textfield.TextInputEditText
import java.util.Locale

class ExploreFragment : Fragment() {

    private lateinit var adapter: RecipeAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var categoryDropdown: AutoCompleteTextView
    private lateinit var etSearch: TextInputEditText
    private lateinit var tvNoRecipes: TextView
    private var isAscending = true
    private var isGridView = false
    private var currentUserId = ""
    private var userNameMap = mutableMapOf<String, String>()
    private var favoriteIds = mutableSetOf<String>()
    private var showFavoritesOnly = false
    private val firebaseService = FirebaseRecipeService()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_recepies_view_screen, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentUserId = FirebaseAuthService().getCurrentUserId() ?: ""

        initializeViews(view)
        setupCategoryDropdown(view)
        setupRecipeAdapter()
        setupEventListeners(view)

        loadAndDisplay()
    }

    private fun loadAndDisplay() {
        RecipeRepository.loadFromFirebase {
           RecipeRepository.getAllRecipes()
                           buildUserMapAndRefresh()
        }
    }

    private fun buildUserMapAndRefresh() {
        val authService = FirebaseAuthService()
        authService.getAllUsers(
            onSuccess = { users ->
                userNameMap.clear()
                for (user in users) {
                    userNameMap[user.uid] = user.email
                }
                loadFavoritesAndRefresh()
            },
            onError = {
                loadFavoritesAndRefresh()
            }
        )
    }

    private fun loadFavoritesAndRefresh() {
        if (currentUserId.isNotEmpty()) {
            firebaseService.getFavoriteIds(currentUserId) { ids ->
                favoriteIds.clear()
                favoriteIds.addAll(ids)
                performFilterAndSort(etSearch.text.toString())
            }
        } else {
            performFilterAndSort(etSearch.text.toString())
        }
    }

    private fun initializeViews(view: View) {
        recyclerView = view.findViewById(R.id.recipeRecyclerView)
        tvNoRecipes = view.findViewById(R.id.tvNoRecipes)
        etSearch = view.findViewById(R.id.etSearch)
        view.findViewById<TextView>(R.id.headerTitle).text = "Recipes to Explore"
    }

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

    private fun setupRecipeAdapter() {
        adapter = RecipeAdapter(
            mutableListOf(),
            currentUserId = currentUserId,
            userNameMap = userNameMap,
            favoriteIds = favoriteIds,
            onDelete = { recipe ->
                showDeleteConfirmationDialog(requireContext(), recipe)
            },
            onTogglePublic = { recipe, isChecked ->
                RecipeRepository.updateRecipe(requireContext(), recipe.title, recipe)
                val msg = if (isChecked) "Recipe is now public" else "Recipe is now private"
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            },
            onToggleFavorite = { recipeId, isFav ->
                if (currentUserId.isNotEmpty()) {
                    if (isFav) {
                        firebaseService.addFavorite(currentUserId, recipeId)
                        Toast.makeText(requireContext(), "Recipe saved to favorites", Toast.LENGTH_SHORT).show()
                    } else {
                        firebaseService.removeFavorite(currentUserId, recipeId)
                        Toast.makeText(requireContext(), "Removed from favorites", Toast.LENGTH_SHORT).show()
                    }
                    if (isFav) favoriteIds.add(recipeId) else favoriteIds.remove(recipeId)
                    adapter.notifyDataSetChanged()
                }
            }
        )
        updateLayoutManager()
        recyclerView.adapter = adapter
    }

    private fun setupEventListeners(view: View) {
        val layoutToggleBtn = view.findViewById<ImageView>(R.id.ivLayoutToggle)
        val sortBtn = view.findViewById<ImageView>(R.id.ivSort)
        val addBtn = view.findViewById<ImageView>(R.id.btnAdd)
        val btnAllRecipes = view.findViewById<TextView>(R.id.btnAllRecipes)
        val btnFavorites = view.findViewById<TextView>(R.id.btnFavorites)

        updateSegmentStyle(btnAllRecipes, btnFavorites)

        btnAllRecipes.setOnClickListener {
            if (showFavoritesOnly) {
                showFavoritesOnly = false
                updateSegmentStyle(btnAllRecipes, btnFavorites)
                performFilterAndSort(etSearch.text.toString())
            }
        }

        btnFavorites.setOnClickListener {
            if (!showFavoritesOnly) {
                showFavoritesOnly = true
                updateSegmentStyle(btnAllRecipes, btnFavorites)
                performFilterAndSort(etSearch.text.toString())
            }
        }

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

    private fun updateSegmentStyle(btnAll: TextView, btnFav: TextView) {
        if (showFavoritesOnly) {
            btnAll.background = resources.getDrawable(R.drawable.segment_left_unselected, null)
            btnAll.setTextColor(resources.getColor(R.color.primary, null))
            btnFav.background = resources.getDrawable(R.drawable.segment_right, null)
            btnFav.setTextColor(resources.getColor(R.color.buttontext, null))
        } else {
            btnAll.background = resources.getDrawable(R.drawable.segment_left, null)
            btnAll.setTextColor(resources.getColor(R.color.buttontext, null))
            btnFav.background = resources.getDrawable(R.drawable.segment_right_unselected, null)
            btnFav.setTextColor(resources.getColor(R.color.primary, null))
        }
    }

    override fun onResume() {
        super.onResume()
        loadAndDisplay()
    }

    private fun performFilterAndSort(query: String) {
        val allRecipes = RecipeRepository.getAllRecipes()
        val selectedCategory = categoryDropdown.text.toString()
        val lowerQuery = query.lowercase(Locale.ROOT).trim()

        val filteredList = allRecipes.filter { recipe ->
            val canView = recipe.isPublic || recipe.userId == currentUserId
            if (!canView) return@filter false

            val matchesCategory = if (selectedCategory == "All") true
            else recipe.category.equals(selectedCategory, ignoreCase = true)

            val matchesSearch = if (lowerQuery.isEmpty()) true
            else recipe.title.lowercase(Locale.ROOT).contains(lowerQuery) ||
                    recipe.description.lowercase(Locale.ROOT).contains(lowerQuery)

            val matchesFavorites = !showFavoritesOnly || recipe.recipeId in favoriteIds

            matchesCategory && matchesSearch && matchesFavorites
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

    private fun updateLayoutManager() {
        val spanCount = if (isGridView) 2 else 1
        recyclerView.layoutManager = GridLayoutManager(requireContext(), spanCount)
    }

    private fun showDeleteConfirmationDialog(context: Context, recipe: com.example.recipebytes.models.Recipe) {
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
