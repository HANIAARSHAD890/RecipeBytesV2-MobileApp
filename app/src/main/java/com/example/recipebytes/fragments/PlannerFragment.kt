package com.example.recipebytes.fragments

import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recipebytes.R
import com.example.recipebytes.adapters.MealDayAdapter
import com.example.recipebytes.models.MealDay
import com.example.recipebytes.models.MealFirebaseRepository
import com.example.recipebytes.models.RecipeRepository
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.util.Calendar

// Fragment for the weekly meal planner with a calendar grid
class PlannerFragment : Fragment() {

    private lateinit var adapter: MealDayAdapter
    private val calendar = Calendar.getInstance()
    private var selectedDateKey: String? = null
    private var selectedCategory = "breakfast"
    private var cachedDays = mutableListOf<MealDay>()

    private val breakfastMeals        = mutableListOf<String>()
    private val lunchMeals            = mutableListOf<String>()
    private val dinnerMeals           = mutableListOf<String>()
    private val dessertMeals          = mutableListOf<String>()
    private val selectedMealsInDialog = mutableListOf<String>()

    // Inflates the meal planner layout
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.activity_meal_planner_screen, container, false)

    // Initializes the recycler, month navigation, calendar, and loads meals
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<TextView>(R.id.headerTitle).text = "Meal Planner"
        setupRecyclerView(view)
        setupMonthNavigation(view)
        buildCalendarGrid(view)
        RecipeRepository.loadFromFirebase {}
        loadCurrentMonthFromFirebase(view)
    }

    // Reloads meal data when returning to the fragment
    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized) {
            view?.let { loadCurrentMonthFromFirebase(it) }
        }
    }

    // Loads saved meals for the current month from Firebase
    private fun loadCurrentMonthFromFirebase(view: View) {
        val monthKey = currentMonthKey()
        cachedDays = MealFirebaseRepository.buildDaysForMonth(monthKey)
        filterRecycler()

        MealFirebaseRepository.loadMonthMeals(
            monthKey,
            onSuccess = { savedDays ->
                if (!isAdded) return@loadMonthMeals
                savedDays.forEach { savedDay ->
                    val day = cachedDays.find { it.date == savedDay.date }
                    day?.let {
                        it.breakfast.clear(); it.breakfast.addAll(savedDay.breakfast)
                        it.lunch.clear();     it.lunch.addAll(savedDay.lunch)
                        it.dinner.clear();    it.dinner.addAll(savedDay.dinner)
                        it.dessert.clear();   it.dessert.addAll(savedDay.dessert)
                    }
                }
                Handler(Looper.getMainLooper()).post {
                    if (!isAdded) return@post
                    buildCalendarGrid(view)
                    filterRecycler()
                }
            },
            onError = { error ->
                Handler(Looper.getMainLooper()).post {
                    if (!isAdded) return@post
                    Toast.makeText(requireContext(),
                        "Could not load meals: $error", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // Sets up previous/next month navigation buttons
    private fun setupMonthNavigation(view: View) {
        view.findViewById<ImageButton>(R.id.btnPrevMonth).setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            selectedDateKey = null
            updateMonthLabel(view)
            buildCalendarGrid(view)
            loadCurrentMonthFromFirebase(view)
        }
        view.findViewById<ImageButton>(R.id.btnNextMonth).setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            selectedDateKey = null
            updateMonthLabel(view)
            buildCalendarGrid(view)
            loadCurrentMonthFromFirebase(view)
        }
        updateMonthLabel(view)
    }

    // Updates the displayed month/year label
    private fun updateMonthLabel(view: View) {
        val months = arrayOf(
            "January","February","March","April","May","June",
            "July","August","September","October","November","December"
        )
        view.findViewById<TextView>(R.id.tvMonthYear).text =
            "${months[calendar.get(Calendar.MONTH)]} ${calendar.get(Calendar.YEAR)}"
    }

    private fun currentMonthKey() =
        "%d-%02d".format(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1)

    // Builds the calendar day grid with selection, today highlight, and meal dots
    private fun buildCalendarGrid(view: View) {
        val grid  = view.findViewById<GridLayout>(R.id.calendarGrid)
        grid.removeAllViews()

        val year  = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)

        val firstDayCal    = Calendar.getInstance().apply { set(year, month, 1) }
        val firstDayOfWeek = firstDayCal.get(Calendar.DAY_OF_WEEK) - 1
        val daysInMonth    = firstDayCal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val todayCal       = Calendar.getInstance()
        val isCurrentMonth = todayCal.get(Calendar.YEAR) == year &&
                todayCal.get(Calendar.MONTH) == month
        val todayDay       = todayCal.get(Calendar.DAY_OF_MONTH)

        val screenWidth = resources.displayMetrics.widthPixels
        val cardMarginPx   = (resources.displayMetrics.density * 12).toInt() * 2
        val innerPaddingPx = (resources.displayMetrics.density * 16).toInt() * 2
        val totalPadding = cardMarginPx + innerPaddingPx
        val cellSize = (screenWidth - totalPadding) / 7

        val datesWithMeals = cachedDays.filter {
            it.breakfast.isNotEmpty() || it.lunch.isNotEmpty() ||
                    it.dinner.isNotEmpty() || it.dessert.isNotEmpty()
        }.map { it.date }.toSet()

        repeat(firstDayOfWeek) {
            val empty = View(requireContext())
            grid.addView(empty, GridLayout.LayoutParams().apply {
                width = cellSize; height = cellSize
            })
        }

        for (day in 1..daysInMonth) {
            val dateKey    = "%d-%02d-%02d".format(year, month + 1, day)
            val isToday    = isCurrentMonth && day == todayDay
            val isSelected = dateKey == selectedDateKey
            val hasMeals   = dateKey in datesWithMeals

            val container = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                gravity     = Gravity.CENTER
            }

            val tv = TextView(requireContext()).apply {
                text     = day.toString()
                gravity  = Gravity.CENTER
                textSize = 13f
                setTypeface(null, if (isToday) Typeface.BOLD else Typeface.NORMAL)
                layoutParams = LinearLayout.LayoutParams(dpToPx(32), dpToPx(32))

                when {
                    isSelected -> {
                        setBackgroundResource(R.drawable.calendar_selected_bg)
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.buttontext))
                    }
                    isToday -> {
                        setBackgroundResource(R.drawable.calendar_today_bg)
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.textcolor))
                    }
                    else -> {
                        background = null
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.textcolor))
                    }
                }

                setOnClickListener {
                    selectedDateKey = if (selectedDateKey == dateKey) null else dateKey
                    view.findViewById<TextView>(R.id.tvSelectedDay).text =
                        if (selectedDateKey != null) "📅  ${getMonthName(month)} $day"
                        else "📅  All meals this month"
                    buildCalendarGrid(view)
                    filterRecycler()
                }
            }
            container.addView(tv)

            if (hasMeals) {
                val dot = View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(dpToPx(5), dpToPx(5)).also {
                        it.topMargin = dpToPx(2)
                    }
                    setBackgroundResource(R.drawable.calendar_selected_bg)
                }
                container.addView(dot)
            }

            grid.addView(container, GridLayout.LayoutParams().apply {
                width = cellSize; height = cellSize
            })
        }
    }

    private fun dpToPx(dp: Int) =
        (dp * resources.displayMetrics.density).toInt()

    private fun getMonthName(month: Int) = arrayOf(
        "Jan","Feb","Mar","Apr","May","Jun",
        "Jul","Aug","Sep","Oct","Nov","Dec"
    )[month]

    // Sets up the meal day list with item click to open picker dialog
    private fun setupRecyclerView(view: View) {
        val rv = view.findViewById<RecyclerView>(R.id.mealPlannerRecycler)
        adapter = MealDayAdapter(mutableListOf(), currentMonthKey()) { mealDay ->
            showMealPickerDialog(mealDay)
        }
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter
    }

    // Filters meal days by selected date or shows all
    private fun filterRecycler() {
        val filtered = if (selectedDateKey != null)
            cachedDays.filter { it.date == selectedDateKey }.toMutableList()
        else
            cachedDays.toMutableList()
        adapter.updateList(filtered)
    }

    // Shows a bottom sheet dialog to pick meals per category for a day
    private fun showMealPickerDialog(mealDay: MealDay) {
        // Capture fragment view BEFORE the local 'view' variable shadows it
        val fragmentView = view

        // Clear then pre-fill with existing meals
        breakfastMeals.clear()
        lunchMeals.clear()
        dinnerMeals.clear()
        dessertMeals.clear()
        selectedMealsInDialog.clear()

        // ← KEY FIX: pre-fill existing meals so they show when dialog opens
        breakfastMeals.addAll(mealDay.breakfast)
        lunchMeals.addAll(mealDay.lunch)
        dinnerMeals.addAll(mealDay.dinner)
        dessertMeals.addAll(mealDay.dessert)

        val dialog = BottomSheetDialog(requireContext())
        val view   = layoutInflater.inflate(R.layout.fragment_meal_popup, null)
        dialog.setContentView(view)

        val tvTitle      = view.findViewById<TextView>(R.id.tvDialogDay)
        val autoComplete = view.findViewById<AutoCompleteTextView>(R.id.autoCompleteMeal)
        val chipGroup    = view.findViewById<ChipGroup>(R.id.chipGroupSelectedMeals)
        val btnSave      = view.findViewById<MaterialButton>(R.id.btnSaveMeals)
        val tvNoResult   = view.findViewById<TextView>(R.id.tvNoResult)

        val catBreakfast = view.findViewById<LinearLayout>(R.id.categoryBreakfast)
        val catLunch     = view.findViewById<LinearLayout>(R.id.categoryLunch)
        val catDinner    = view.findViewById<LinearLayout>(R.id.categoryDinner)
        val catDessert   = view.findViewById<LinearLayout>(R.id.categoryDessert)

        tvTitle.text     = mealDay.day
        selectedCategory = "breakfast"

        val categories = mapOf(
            "breakfast" to catBreakfast,
            "lunch"     to catLunch,
            "dinner"    to catDinner,
            "dessert"   to catDessert
        )

        fun updateCategoryUI() {
            categories.forEach { (key, layout) ->
                layout.setBackgroundResource(
                    if (key == selectedCategory) R.drawable.category_selected_bg
                    else R.drawable.category_unselected_bg
                )
                val label = layout.getChildAt(1) as TextView
                label.setTextColor(
                    if (key == selectedCategory)
                        ContextCompat.getColor(requireContext(), R.color.buttontext)
                    else
                        ContextCompat.getColor(requireContext(), R.color.secondary)
                )
            }
        }

        fun restoreChipsForCategory() {
            chipGroup.removeAllViews()
            val currentList = when (selectedCategory) {
                "breakfast" -> breakfastMeals
                "lunch"     -> lunchMeals
                "dinner"    -> dinnerMeals
                "dessert"   -> dessertMeals
                else        -> mutableListOf()
            }
            currentList.forEach { meal ->
                val chip = Chip(requireContext())
                chip.text = meal
                chip.isCloseIconVisible = true
                chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
                chip.setOnCloseIconClickListener {
                    currentList.remove(meal)
                    chipGroup.removeView(chip)
                }
                chipGroup.addView(chip)
            }
        }

        categories.forEach { (key, layout) ->
            layout.setOnClickListener {
                selectedCategory = key
                updateCategoryUI()
                restoreChipsForCategory()
                setupMealSelection(autoComplete, chipGroup, mealDay, tvNoResult)
            }
        }

        updateCategoryUI()
        restoreChipsForCategory()
        setupMealSelection(autoComplete, chipGroup, mealDay, tvNoResult)

        btnSave.setOnClickListener {
            val hasAnyMeal = breakfastMeals.isNotEmpty() || lunchMeals.isNotEmpty() ||
                    dinnerMeals.isNotEmpty() || dessertMeals.isNotEmpty()

            if (!hasAnyMeal) {
                Toast.makeText(requireContext(),
                    "Please select at least one meal!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Update cachedDays immediately so the calendar shows dots right away
            val cached = cachedDays.find { it.date == mealDay.date }
            cached?.let {
                it.breakfast.clear(); it.breakfast.addAll(breakfastMeals)
                it.lunch.clear();     it.lunch.addAll(lunchMeals)
                it.dinner.clear();    it.dinner.addAll(dinnerMeals)
                it.dessert.clear();   it.dessert.addAll(dessertMeals)
            }

            // Also update mealDay for the Firebase save
            mealDay.breakfast.clear(); mealDay.breakfast.addAll(breakfastMeals)
            mealDay.lunch.clear();     mealDay.lunch.addAll(lunchMeals)
            mealDay.dinner.clear();    mealDay.dinner.addAll(dinnerMeals)
            mealDay.dessert.clear();   mealDay.dessert.addAll(dessertMeals)

            dialog.dismiss()

            // Rebuild calendar and recycler immediately on the fragment view
            fragmentView?.let { v ->
                buildCalendarGrid(v)
                filterRecycler()
            }

            Toast.makeText(requireContext(),
                "✅ Meals saved for ${mealDay.day}!", Toast.LENGTH_SHORT).show()

            // Persist to Firebase in the background
            MealFirebaseRepository.saveDayMeals(mealDay,
                onError = { error ->
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(requireContext(),
                            "Failed to save: $error", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        dialog.show()
    }

    // Configures the meal search autocomplete and chip selection UI
    private fun setupMealSelection(
        autoComplete: AutoCompleteTextView,
        chipGroup: ChipGroup,
        mealDay: MealDay,
        tvNoResult: TextView
    ) {
        val filteredRecipes = RecipeRepository.getAllRecipes().filter { recipe ->
            recipe.category.lowercase() == selectedCategory.lowercase()
        }.map { it.title }

        val dropdownAdapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_dropdown_item_1line, filteredRecipes
        )
        autoComplete.setAdapter(dropdownAdapter)
        autoComplete.setText("", false)
        autoComplete.hint =
            "Search ${selectedCategory.replaceFirstChar { it.uppercase() }} recipes..."

        autoComplete.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                tvNoResult.visibility =
                    if (query.isNotEmpty() && filteredRecipes.none {
                            it.contains(query, ignoreCase = true) })
                        View.VISIBLE else View.GONE
            }
        })

        autoComplete.setOnItemClickListener { _, _, position, _ ->
            val selected = dropdownAdapter.getItem(position) ?: ""

            val currentCategoryList = when (selectedCategory) {
                "breakfast" -> breakfastMeals
                "lunch"     -> lunchMeals
                "dinner"    -> dinnerMeals
                "dessert"   -> dessertMeals
                else        -> mutableListOf()
            }

            if (!currentCategoryList.contains(selected)) {
                currentCategoryList.add(selected)
                val chip = Chip(requireContext())
                chip.text = selected
                chip.isCloseIconVisible = true
                chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
                chip.setOnCloseIconClickListener {
                    currentCategoryList.remove(selected)
                    chipGroup.removeView(chip)
                }
                chipGroup.addView(chip)
            } else {
                Toast.makeText(requireContext(),
                    "$selected already added", Toast.LENGTH_SHORT).show()
            }
            autoComplete.setText("", false)
        }
    }
}