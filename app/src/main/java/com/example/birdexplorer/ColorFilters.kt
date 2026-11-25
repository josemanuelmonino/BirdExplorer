package com.example.birdexplorer
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix

object ColorFilters {

    // Daltonization filter based on Chromium's
    // deuteranomaly correction algorithm (severity = 1.0)
    val deuteranopiaFilter = ColorFilter.colorMatrix(
        ColorMatrix(
            floatArrayOf(
                1.42620f, -0.61786f, 0.19153f, 0f, 0f,
                0.00000f,  1.00000f, 0.00000f, 0f, 0f,
                -0.18180f,  0.31294f, 0.92673f, 0f, 0f,
                0.00000f,  0.00000f, 0.00000f, 1f, 0f
            )
        )
    )
}

