
package com.example.birdexplorer

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.birdexplorer.data.StepSensorManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ExplorationViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ExplorationUiState())
    val uiState: StateFlow<ExplorationUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    // Gestor del sensor de pasos
    private val stepSensorManager = StepSensorManager(
        getApplication()
    ) { steps ->
        _uiState.update { currentState ->
            currentState.copy(steps = steps)
        }
    }

    // Iniciar la exploración
    fun startExploration() {
        if (_uiState.value.isRunning) return

        _uiState.update { currentState ->
            currentState.copy(
                elapsedSeconds = 0L,
                steps = 0,
                isRunning = true
            )
        }

        stepSensorManager.start()

        val startTime = SystemClock.elapsedRealtime()

        timerJob = viewModelScope.launch {
            while (true) {
                val seconds =
                    (SystemClock.elapsedRealtime() - startTime) / 1000

                _uiState.update { currentState ->
                    currentState.copy(elapsedSeconds = seconds)
                }

                delay(1000)
            }
        }
    }

    // Finalizar la exploración
    fun finishExploration() {
        if (!_uiState.value.isRunning) return

        timerJob?.cancel()
        stepSensorManager.stop()

        _uiState.update { currentState ->
            currentState.copy(
                elapsedSeconds = 0L,
                steps = 0,
                isRunning = false)
        }
    }

    override fun onCleared() {
        stepSensorManager.stop()
    }
}

