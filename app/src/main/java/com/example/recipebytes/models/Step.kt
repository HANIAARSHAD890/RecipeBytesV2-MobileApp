package com.example.recipebytes.models

import java.io.Serializable

// Represents a single step in a recipe with order and description
data class Step(
    var stepId: Int = 0,
    var text: String
) : Serializable
