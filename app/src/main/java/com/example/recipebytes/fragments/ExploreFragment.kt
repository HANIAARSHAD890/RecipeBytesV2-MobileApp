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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.recipebytes.R
import com.example.recipebytes.activities.AddRecipeActivity
import com.example.recipebytes.adapters.LikersAdapter
import com.example.recipebytes.adapters.RecipeAdapter
import com.example.recipebytes.models.Recipe
import com.example.recipebytes.models.RecipeRepository
import com.example.recipebytes.models.User
import com.example.recipebytes.services.FirebaseAuthService
import com.example.recipebytes.services.FirebaseRecipeService
import com.google.android.material.textfield.TextInputEditText
import java.util.Locale
import com.example.recipebytes.fragments.AddRecipeMethodBottomSheet
import android.widget.ImageView
import com.google.android.material.chip.Chip

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
    private var userProfileMap = mutableMapOf<String, String>()
    private var favoriteIds = mutableSetOf<String>()
    private var likedIds = mutableSetOf<String>()
    private var showFavoritesOnly = false
    private var selectedTimeFilter = "all"
    private var selectedRecencyFilter = ""
    private val firebaseService = FirebaseRecipeService()
    private var hasLoadedOnce = false

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
        setupFilterChips(view)
        setupRecipeAdapter()
        setupEventListeners(view)

        loadAndDisplay()
    }

    override fun onResume() {
        super.onResume()
        if (hasLoadedOnce) {
            loadAndDisplay()
        }
        hasLoadedOnce = true
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
                userProfileMap.clear()
                for (user in users) {
                    userNameMap[user.uid] = user.username.ifEmpty { user.email.substringBefore("@") }
                    if (user.profileImage.isNotEmpty()) {
                        userProfileMap[user.uid] = user.profileImage
                    }
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
        val adapterCategory = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, Recipe.CATEGORIES_WITH_ALL)
        categoryDropdown.setAdapter(adapterCategory)
        categoryDropdown.setText("All", false)
        categoryDropdown.setOnItemClickListener { _, _, _, _ ->
            performFilterAndSort(etSearch.text.toString())
        }
    }

    private fun setupFilterChips(view: View) {
        // Time filter chips
        val chipAll = view.findViewById<com.google.android.material.chip.Chip>(R.id.chipAll)
        val chipUnder30 = view.findViewById<com.google.android.material.chip.Chip>(R.id.chipUnder30)
        val chip30to60 = view.findViewById<com.google.android.material.chip.Chip>(R.id.chip30to60)
        val chipOver60 = view.findViewById<com.google.android.material.chip.Chip>(R.id.chipOver60)
        // Recency filter chips
        val chipNewest = view.findViewById<com.google.android.material.chip.Chip>(R.id.chipNewest)
        val chipWeekly = view.findViewById<com.google.android.material.chip.Chip>(R.id.chipWeekly)
        val chipMonthly = view.findViewById<com.google.android.material.chip.Chip>(R.id.chipMonthly)

        val timeChips = listOf(chipAll, chipUnder30, chip30to60, chipOver60)
        val recencyChips = listOf(chipNewest, chipWeekly, chipMonthly)
        val allChips = timeChips + recencyChips

        val primaryColor = ContextCompat.getColor(requireContext(), R.color.primary)
        val whiteColor = ContextCompat.getColor(requireContext(), R.color.buttontext)
        val textColor = ContextCompat.getColor(requireContext(), R.color.textcolor)

        // Style chips: blue bg + white text when checked; transparent + gray border when unchecked
        fun styleChip(chip: com.google.android.material.chip.Chip, checked: Boolean) {
            if (checked) {
                chip.setChipBackgroundColorResource(R.color.primary)
                chip.setTextColor(whiteColor)
                chip.chipStrokeColor = android.content.res.ColorStateList.valueOf(primaryColor)
            } else {
                chip.setChipBackgroundColorResource(R.color.background)
                chip.setTextColor(textColor)
                chip.chipStrokeColor = android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.gray)
                )
            }
        }

        // Apply initial style to all chips
        for (chip in allChips) {
            styleChip(chip, chip.isChecked)
        }

        // Time chip click: mutual exclusion within time group
        for (chip in timeChips) {
            chip.setOnCheckedChangeListener { buttonView, isChecked ->
                styleChip(buttonView as Chip, isChecked)
                if (isChecked) {
                    selectedTimeFilter = when (buttonView.id) {
                        R.id.chipUnder30 -> "under30"
                        R.id.chip30to60 -> "30to60"
                        R.id.chipOver60 -> "over60"
                        else -> "all"
                    }
                    // Uncheck other time chips
                    for (other in timeChips) {
                        if (other != buttonView) {
                            other.isChecked = false
                            styleChip(other, false)
                        }
                    }
                    performFilterAndSort(etSearch.text.toString())
                } else if (!timeChips.any { it.isChecked }) {
                    // Ensure at least one time chip is always selected
                    chipAll.isChecked = true
                    styleChip(chipAll, true)
                }
            }
        }

        // Recency chip click: mutual exclusion within recency group
        for (chip in recencyChips) {
            chip.setOnCheckedChangeListener { buttonView, isChecked ->
                styleChip(buttonView as Chip, isChecked)
                if (isChecked) {
                    selectedRecencyFilter = when (buttonView.id) {
                        R.id.chipWeekly -> "weekly"
                        R.id.chipMonthly -> "monthly"
                        else -> "newest"
                    }
                    // Uncheck other recency chips
                    for (other in recencyChips) {
                        if (other != buttonView) {
                            other.isChecked = false
                            styleChip(other, false)
                        }
                    }
                    performFilterAndSort(etSearch.text.toString())
                } else if (!recencyChips.any { it.isChecked }) {
                    selectedRecencyFilter = ""
                }
            }
        }
    }

    private fun setupRecipeAdapter() {
        adapter = RecipeAdapter(
            mutableListOf(),
            currentUserId = currentUserId,
            userNameMap = userNameMap,
            favoriteIds = favoriteIds,
            likedIds = likedIds,
            userProfileMap = userProfileMap,
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

        view.findViewById<ImageView>(R.id.btnAdd).setOnClickListener {
            AddRecipeMethodBottomSheet()
                .show(parentFragmentManager, "AddRecipeMethodBottomSheet")
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
        val now = System.currentTimeMillis()
        val dayMs = 86400000L

        val filteredList = allRecipes.filter { recipe ->
            val canView = recipe.isPublic || recipe.userId == currentUserId
            if (!canView) return@filter false

            val matchesCategory = if (selectedCategory == "All") true
            else recipe.category.equals(selectedCategory, ignoreCase = true)

            val matchesSearch = if (lowerQuery.isEmpty()) true
            else recipe.title.lowercase(Locale.ROOT).contains(lowerQuery) ||
                    recipe.description.lowercase(Locale.ROOT).contains(lowerQuery)

            val matchesFavorites = !showFavoritesOnly || recipe.recipeId in favoriteIds

            // Time filter
            val matchesTime = when (selectedTimeFilter) {
                "under30" -> recipe.cookingTime in 1..30
                "30to60" -> recipe.cookingTime in 31..60
                "over60" -> recipe.cookingTime >= 61
                else -> true // "all"
            }

            // Recency filter (date range)
            val matchesRecency = when (selectedRecencyFilter) {
                "weekly" -> (now - recipe.createdAt) <= 7 * dayMs
                "monthly" -> (now - recipe.createdAt) <= 30 * dayMs
                else -> true
            }

            matchesCategory && matchesSearch && matchesFavorites && matchesTime && matchesRecency
        }

        val sortedList = when {
            selectedRecencyFilter == "newest" -> filteredList.sortedByDescending { it.createdAt }
            isAscending -> filteredList.sortedBy { it.title.lowercase() }
            else -> filteredList.sortedByDescending { it.title.lowercase() }
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
        val rvLikers = dialog.findViewById<RecyclerView>(R.id.rvLikers)

        rvLikers.layoutManager = LinearLayoutManager(requireContext())

        firebaseService.getLikedByUsers(recipeId) { likedByMap ->
            val adapter = LikersAdapter(requireContext(), likedByMap)
            rvLikers.adapter = adapter
        }

        ivClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
}
