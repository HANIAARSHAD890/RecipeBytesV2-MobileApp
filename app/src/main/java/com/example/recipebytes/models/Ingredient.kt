package com.example.recipebytes.models

import java.io.Serializable

/**
 * Data class representing an ingredient used in a recipe.
 * 
 * @property name The name of the ingredient (e.g., "Sugar").
 * @property quantity The amount required for the recipe (e.g., "200g").
 */
data class Ingredient(
    var name: String,
    var quantity: String
) : Serializable
