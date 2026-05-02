package com.example.recipebytes.models

import java.io.Serializable

/**
 * Data class representing a single step in a recipe's preparation instructions.
 *
 * @property stepcontent The textual content describing the preparation action for this step.
 */
data class Step(
    var stepcontent: String
) : Serializable
