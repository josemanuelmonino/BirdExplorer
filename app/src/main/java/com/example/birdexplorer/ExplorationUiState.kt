package com.example.birdexplorer

data class ExplorationUiState(
    val elapsedSeconds: Long = 0L,
    val steps: Int = 0,
    val isRunning: Boolean = false
)
