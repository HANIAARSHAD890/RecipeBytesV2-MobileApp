package com.example.recipebytes.models

import java.io.Serializable

data class ShoppingListItem(
    var name: String = "",
    var quantity: String = "",
    var isChecked: Boolean = false,
    var recipeTitle: String = ""
) : Serializable

data class ShoppingList(
    var id: String = "",
    var recipeTitle: String = "",
    var createdAt: Long = System.currentTimeMillis(),
    var items: List<ShoppingListItem> = emptyList()
) : Serializable