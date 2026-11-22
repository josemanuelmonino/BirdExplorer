package com.example.birdexplorer

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.birdexplorer.data.BirdRepository
import com.example.birdexplorer.data.BirdSighting
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import retrofit2.HttpException

// Translates the exception into a message the user can understand
private fun errorMessage(e: Exception): String = when (e) {
    is IOException -> "No internet connection. Check your network and try again."
    is HttpException -> "Server error (${e.code()}). Try again later."
    else -> "Something went wrong while loading the data."
}

private const val TAG = "BirdViewModel"

class BirdViewModel(
    private val repository: BirdRepository = BirdRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(BirdUiState())
    val uiState: StateFlow<BirdUiState> = _uiState.asStateFlow()

    // we store the coroutines if we want to cancel them
    private var loadJob: Job? = null
    private var searchJob: Job? = null

    // --- Eventos que llama la UI ---

    fun loadNearMe(lat: Double, lon: Double, radiusKm: Int = 10) =
        load { repository.nearMe(lat, lon, radiusKm) }

    fun loadByArea(neLat: Double, neLng: Double, swLat: Double, swLng: Double) =
        load { repository.byArea(neLat, neLng, swLat, swLng) }

    fun loadBySpecies(taxonId: Int) =
        load { repository.bySpecies(taxonId) }

    fun searchSpecies(query: String) {
        val q = query.trim()
        searchJob?.cancel()
        if (q.length < 2) {
            _uiState.update { it.copy(speciesResults = emptyList()) }
            return
        }
        searchJob = viewModelScope.launch {
            try {
                val result = repository.searchSpecies(q)
                _uiState.update { it.copy(speciesResults = result) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "searchSpecies failed", e)
                _uiState.update { it.copy(error = errorMessage(e)) }
            }
        }
    }
    fun clearError() = _uiState.update { it.copy(error = null) } // deletes errors from de uiState

    // --- common logic  ---

    private fun load(request: suspend () -> List<BirdSighting>) {
        loadJob?.cancel()                         // we do not want the same load function running twice
        loadJob = viewModelScope.launch {// we use a corroutine from viewModel to
                                         // delete itself if the viewModel is destroyed
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val result = request()
                _uiState.update { it.copy(isLoading = false, sightings = result) }
            } catch (e: CancellationException) {
                throw e                           // cancelled
            } catch (e: Exception) {
                Log.e(TAG, "load failed: ${e::class.simpleName}: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false, error = errorMessage(e)) }
            }
        }
    }

}