package com.example.recipebytes.models

import java.io.Serializable

data class Nutrition(
    val calories: Int = 0,
    val protein: String = ""
) : Serializable
