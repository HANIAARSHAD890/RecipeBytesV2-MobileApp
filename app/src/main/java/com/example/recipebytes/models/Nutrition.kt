package com.example.recipebytes.models

import java.io.Serializable

data class Nutrition(
    var calories: Int = 0,
    var protein: Float = 0f,
    var carbs: Float = 0f,
    var fat: Float = 0f,
    var netCarbs: Float = 0f
) : Serializable