package com.example.recipebytes.activities

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.recipebytes.fragments.AddRecipeFragment1
import com.example.recipebytes.fragments.AddRecipeFragment2
import com.example.recipebytes.fragments.AddRecipeFragment3
import com.example.recipebytes.fragments.AddRecipeFragment4
import com.example.recipebytes.models.Ingredient
import com.example.recipebytes.R
import com.example.recipebytes.models.Step

class AddRecipeActivity : AppCompatActivity() {


    private var title = ""
    private var desc = ""
    private var category = ""
    private var ingredients = ArrayList<Ingredient>()
    private var stepsList = ArrayList<Step>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_recipe)
        val headerTitle = findViewById<TextView>(R.id.headerTitle)
        headerTitle.text = "Add Recipe"
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, AddRecipeFragment1())
                .commit()
        }
    }
 ///functions to navigate between fragments
 // STEP 1 → STEP 2
    fun goToStep2(titler: String, descr: String, categoryr: String) {
        title= titler
        desc = descr
        category = categoryr
        val fragment = AddRecipeFragment2()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }
    // STEP 2 → STEP 3
    fun goToStep3(ingredientsList: List<Ingredient>) {
        ingredients = ArrayList(ingredientsList)
        val fragment = AddRecipeFragment3()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    // STEP 3 → STEP 4
    fun goToStep4(steps: List<Step>) {
        stepsList = ArrayList(steps)
        val bundle = Bundle()
        bundle.putString("title", title)
        bundle.putString("desc", desc)
        bundle.putString("category", category)
        bundle.putSerializable("ingredients", ingredients)
        bundle.putSerializable("steps", ArrayList(steps))
        val fragment = AddRecipeFragment4()
        fragment.arguments = bundle
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }
}