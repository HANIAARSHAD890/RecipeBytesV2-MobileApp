package com.example.recipebytes.adapters

import com.example.recipebytes.models.Recipe

data class SuggestResult(
    val recipe: Recipe,
    val label: String,
    val matchCount: Int,
    val totalCount: Int
)