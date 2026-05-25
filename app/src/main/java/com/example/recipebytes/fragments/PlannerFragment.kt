package com.example.recipebytes.fragments

import android.graphics.Typeface
import android.os.Bundle
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
import com.example.recipebytes.models.MealRepository
import com.example.recipebytes.models.RecipeRepository
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.util.Calendar

class PlannerFragment : Fragment() {

    private lateinit var adapter: MealDayAdapter
    private val calendar = Calendar.getInstance()
    private var selectedDateKey: String? = null
    private var selectedCategory = "breakfast"  // ← ADDED

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.activity_meal_planner_screen, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<TextView>(R.id.headerTitle).text = "Meal Planner"
        setupRecyclerView(view)
        setupMonthNavigation(view)
        buildCalendarGrid(view)
        loadCurrentMonth()
    }

    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized) loadCurrentMonth()
    }

    // ── month navigation ──────────────────────────────────────────────────────

    private fun setupMonthNavigation(view: View) {
        view.findViewById<ImageButton>(R.id.btnPrevMonth).setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            selectedDateKey = null
            updateMonthLabel(view)
            buildCalendarGrid(view)
            loadCurrentMonth()
        }
        view.findViewById<ImageButton>(R.id.btnNextMonth).setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            selectedDateKey = null
            updateMonthLabel(view)
            buildCalendarGrid(view)
            loadCurrentMonth()
        }
        updateMonthLabel(view)
    }

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

    // ── calendar grid ─────────────────────────────────────────────────────────

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

        val screenWidth    = resources.displayMetrics.widthPixels
        val cellSize       = (screenWidth - dpToPx(24)) / 7

        // Days that have meals — for dot indicator
        val monthPlan = MealRepository.getMealPlanForMonth(requireContext(), currentMonthKey())
        val datesWithMeals = monthPlan.filter {
            it.breakfast.isNotEmpty() || it.lunch.isNotEmpty() ||
                    it.dinner.isNotEmpty()    || it.dessert.isNotEmpty()
        }.map { it.date }.toSet()

        // Empty offset cells before day 1
        repeat(firstDayOfWeek) {
            val empty = View(requireContext())
            grid.addView(empty, GridLayout.LayoutParams().apply {
                width = cellSize; height = cellSize
            })
        }

        // Day cells
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
                text      = day.toString()
                gravity   = Gravity.CENTER
                textSize  = 13f
                setTypeface(null, if (isToday) Typeface.BOLD else Typeface.NORMAL)
                layoutParams = LinearLayout.LayoutParams(dpToPx(32), dpToPx(32))

                when {
                    isSelected -> {
                        setBackgroundResource(R.drawable.calendar_selected_bg)
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.buttontext))
                    }
                    isToday -> {
                        setBackgroundResource(R.drawable.calendar_today_bg)
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.secondary))
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

            // Dot indicator if day has meals
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

    // ── RecyclerView ──────────────────────────────────────────────────────────

    private fun setupRecyclerView(view: View) {
        val rv = view.findViewById<RecyclerView>(R.id.mealPlannerRecycler)
        adapter = MealDayAdapter(mutableListOf(), currentMonthKey()) { mealDay ->
            showMealPickerDialog(mealDay)
        }
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter
    }

    private fun loadCurrentMonth() {
        val monthKey = currentMonthKey()
        val days = MealRepository.getMealPlanForMonth(requireContext(), monthKey)
        adapter = MealDayAdapter(days, monthKey) { mealDay ->
            showMealPickerDialog(mealDay)
        }
        requireView().findViewById<RecyclerView>(R.id.mealPlannerRecycler).adapter = adapter
        filterRecycler()
    }

    private fun filterRecycler() {
        val allDays = MealRepository.getMealPlanForMonth(requireContext(), currentMonthKey())
        val filtered = if (selectedDateKey != null)
            allDays.filter { it.date == selectedDateKey }.toMutableList()
        else
            allDays.toMutableList()
        adapter.updateList(filtered)
    }

    // ── meal picker dialog ────────────────────────────────────────────────────

    private val selectedMealsInDialog = mutableListOf<String>()

    private fun showMealPickerDialog(mealDay: MealDay) {
        val dialog = BottomSheetDialog(requireContext())
        val view   = layoutInflater.inflate(R.layout.fragment_meal_popup, null)
        dialog.setContentView(view)

        val tvTitle      = view.findViewById<TextView>(R.id.tvDialogDay)
        val autoComplete = view.findViewById<AutoCompleteTextView>(R.id.autoCompleteMeal)
        val chipGroup    = view.findViewById<ChipGroup>(R.id.chipGroupSelectedMeals)
        val btnSave      = view.findViewById<MaterialButton>(R.id.btnSaveMeals)
        val tvNoResult   = view.findViewById<TextView>(R.id.tvNoResult)

        // Category layouts
        val catBreakfast = view.findViewById<LinearLayout>(R.id.categoryBreakfast)
        val catLunch     = view.findViewById<LinearLayout>(R.id.categoryLunch)
        val catDinner    = view.findViewById<LinearLayout>(R.id.categoryDinner)
        val catDessert   = view.findViewById<LinearLayout>(R.id.categoryDessert)

        tvTitle.text  = mealDay.day
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

        categories.forEach { (key, layout) ->
            layout.setOnClickListener {
                selectedCategory = key
                selectedMealsInDialog.clear()
                chipGroup.removeAllViews()
                updateCategoryUI()
            }
        }

        updateCategoryUI()
        setupMealSelection(autoComplete, chipGroup, mealDay, tvNoResult)
        btnSave.setOnClickListener { handleSaveMeals(dialog, selectedMealsInDialog, mealDay) }
        dialog.show()
    }

    private fun setupMealSelection(
        autoComplete: AutoCompleteTextView,
        chipGroup: ChipGroup,
        mealDay: MealDay,
        tvNoResult: TextView
    ) {
        selectedMealsInDialog.clear()
        val recipeNames = RecipeRepository.getAllRecipes().map { it.title }
        val dropdownAdapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_dropdown_item_1line, recipeNames
        )
        autoComplete.setAdapter(dropdownAdapter)

        autoComplete.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                tvNoResult.visibility =
                    if (query.isNotEmpty() && recipeNames.none { it.contains(query, ignoreCase = true) })
                        View.VISIBLE else View.GONE
            }
        })

        autoComplete.setOnItemClickListener { _, _, position, _ ->
            val selected = dropdownAdapter.getItem(position) ?: ""
            if (!selectedMealsInDialog.contains(selected)) {
                selectedMealsInDialog.add(selected)
                addChipToGroup(selected, chipGroup, mealDay)
            } else {
                Toast.makeText(requireContext(), "$selected already added", Toast.LENGTH_SHORT).show()
            }
            autoComplete.setText("", false)
        }
    }

    private fun addChipToGroup(text: String, chipGroup: ChipGroup, mealDay: MealDay) {
        val chip = Chip(requireContext())
        chip.text = text
        chip.isCloseIconVisible = true
        chip.setOnCloseIconClickListener {
            selectedMealsInDialog.remove(text)
            chipGroup.removeView(chip)
        }
        chipGroup.addView(chip)
    }

    private fun handleSaveMeals(
        dialog: BottomSheetDialog,
        selectedMeals: List<String>,
        mealDay: MealDay
    ) {
        if (selectedMeals.isEmpty()) {
            Toast.makeText(requireContext(), "Please select at least one meal!", Toast.LENGTH_SHORT).show()
            return
        }

        val targetList = when (selectedCategory) {
            "breakfast" -> mealDay.breakfast
            "lunch"     -> mealDay.lunch
            "dinner"    -> mealDay.dinner
            "dessert"   -> mealDay.dessert
            else        -> mealDay.meals
        }

        selectedMeals.forEach { if (!targetList.contains(it)) targetList.add(it) }
        MealRepository.saveMealPlan(requireContext(), currentMonthKey())
        adapter.notifyDataSetChanged()
        Toast.makeText(
            requireContext(),
            "${selectedCategory.replaceFirstChar { it.uppercase() }} saved for ${mealDay.day}!",
            Toast.LENGTH_SHORT
        ).show()
        dialog.dismiss()
    }
}