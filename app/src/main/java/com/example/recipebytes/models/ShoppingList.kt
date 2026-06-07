package com.example.recipebytes.models

import java.io.Serializable

// Represents a single item in a shopping list with checked state
data class ShoppingListItem(
    var name: String = "",
    var quantity: String = "",
    var isChecked: Boolean = false,
    var recipeTitle: String = ""
) : Serializable

// Represents a complete shopping list containing multiple items
data class ShoppingList(
    var id: String = "",
    var recipeTitle: String = "",
    var createdAt: Long = System.currentTimeMillis(),
    var items: List<ShoppingListItem> = emptyList()
) : Serializable