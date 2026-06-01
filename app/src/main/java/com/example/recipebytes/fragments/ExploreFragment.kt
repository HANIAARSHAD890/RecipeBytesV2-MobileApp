package com.example.recipebytes.fragments
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.recipebytes.R
import com.example.recipebytes.activities.AddRecipeActivity
import com.example.recipebytes.adapters.LikersAdapter
import com.example.recipebytes.adapters.RecipeAdapter
import com.example.recipebytes.models.RecipeRepository
import com.example.recipebytes.models.User
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
    private lateinit var layoutLoading: LinearLayout
    private var isAscending = true
    private var isGridView = false
    private var currentUserId = ""
    private var userNameMap = mutableMapOf<String, String>()
    private var favoriteIds = mutableSetOf<String>()
    private var likedIds = mutableSetOf<String>()
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

        // Check if we should open favorites tab
        if (arguments?.getBoolean("show_favorites", false) == true) {
            showFavoritesOnly = true
        }

        initializeViews(view)
        setupCategoryDropdown(view)
        setupRecipeAdapter()
        setupEventListeners(view)

        loadAndDisplay()
    }

    private fun loadAndDisplay() {
        layoutLoading.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        tvNoRecipes.visibility = View.GONE
        try {
            RecipeRepository.loadFromFirebase {
                refreshFromFirebase()
            }
        } catch (e: Exception) {
            android.util.Log.e("ExploreFragment", "loadAndDisplay failed", e)
            layoutLoading.visibility = View.GONE
            tvNoRecipes.visibility = View.VISIBLE
        }
    }

    private fun refreshFromFirebase() {
        if (!isAdded) {
            layoutLoading.visibility = View.GONE
            return
        }
        buildUserMapAndRefresh()
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
            firebaseService.getFavoriteIds(currentUserId) { favIds ->
                favoriteIds.clear()
                favoriteIds.addAll(favIds)
                firebaseService.getLikedIds(currentUserId) { liked ->
                    likedIds.clear()
                    likedIds.addAll(liked)
                    performFilterAndSort(etSearch.text.toString())
                }
            }
        } else {
            likedIds.clear()
            performFilterAndSort(etSearch.text.toString())
        }
    }

    private fun initializeViews(view: View) {
        recyclerView = view.findViewById(R.id.recipeRecyclerView)
        tvNoRecipes = view.findViewById(R.id.tvNoRecipes)
        layoutLoading = view.findViewById(R.id.layoutLoading)
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
            likedIds = likedIds,
            showToggle = false,
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
            },
            onToggleLike = { recipeId, isLiked ->
                if (currentUserId.isNotEmpty()) {
                    val alreadyLiked = likedIds.contains(recipeId)
                    if (isLiked != alreadyLiked) {
                        if (isLiked) likedIds.add(recipeId) else likedIds.remove(recipeId)

                        RecipeRepository.updateLikesCountLocally(recipeId, if (isLiked) 1 else -1)

                        if (isLiked) firebaseService.addLike(currentUserId, recipeId)
                        else firebaseService.removeLike(currentUserId, recipeId)

                        performFilterAndSort(etSearch.text.toString())
                    }
                }
            },
            onShowLikers = { recipeId ->
                showLikersDialog(recipeId)
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

    private fun performFilterAndSort(query: String) {
        if (!isAdded) return
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

        layoutLoading.visibility = View.GONE
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
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val btnDeleteConfirm = dialog.findViewById<Button>(R.id.btnDeleteConfirm)
        val ivClose = dialog.findViewById<ImageView>(R.id.ivCloseDialog)
        val tvMessage = dialog.findViewById<TextView>(R.id.tvDeleteMessage)

        tvMessage.text = "Are you sure you want to delete \"${recipe.title}\"?"

        btnDeleteConfirm.setOnClickListener {
            RecipeRepository.deleteRecipe(context, recipe.title)
            performFilterAndSort(etSearch.text.toString())
            dialog.dismiss()
        }

        ivClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showLikersDialog(recipeId: String) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_likers)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val ivClose = dialog.findViewById<ImageView>(R.id.ivCloseDialog)
        val tvHeader = dialog.findViewById<TextView>(R.id.tvLikersHeader)
        val rvLikers = dialog.findViewById<RecyclerView>(R.id.rvLikers)

        rvLikers.layoutManager = LinearLayoutManager(requireContext())

        firebaseService.getLikedByUsers(recipeId) { likedByMap ->
            tvHeader.text = "All ${likedByMap.size}"
            val adapter = LikersAdapter(requireContext(), likedByMap)
            rvLikers.adapter = adapter
        }

        ivClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
}
