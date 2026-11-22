package com.example.birdexplorer

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.birdexplorer.data.BirdRepository
import com.example.birdexplorer.data.BirdSighting
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import retrofit2.HttpException

// Translates the exception into a message the user can understand,
// connection errors (the phone) are told apart from API errors (iNaturalist)
private fun errorMessage(e: Exception): String = when (e) {
    is UnknownHostException, is ConnectException ->
        "Connection error: no internet access. Check your network and try again."
    is SocketTimeoutException ->
        "Connection error: the request took too long. Try again."
    is IOException ->
        "Connection error: the network failed. Try again."
    is HttpException -> when (e.code()) {
        429 -> "API error: too many requests. Wait a minute and try again."
        in 500..599 -> "API error: the bird service is not available (${e.code()}). Try again later."
        else -> "API error: the request was rejected (${e.code()})."
    }
    is SerializationException -> "API error: the data received is not valid."
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
        load("No sightings found near you") { repository.nearMe(lat, lon, radiusKm) }

    fun loadByArea(neLat: Double, neLng: Double, swLat: Double, swLng: Double) =
        load("No sightings found in this area") { repository.byArea(neLat, neLng, swLat, swLng) }

    fun loadBySpecies(taxonId: Int) =
        load("No sightings of this species in Spain") {
            repository.bySpecies(taxonId).also {
                // the species list is only removed once the sightings have arrived
                _uiState.update { state -> state.copy(speciesResults = emptyList()) }
            }
        }

    // The search only runs when the user asks for it, so we do not waste API requests
    fun searchSpecies(query: String) {
        val q = query.trim()
        if (q.isEmpty()) {
            _uiState.update { it.copy(info = "Type a species name first") }
            return
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, sightings = emptyList(), speciesResults = emptyList(), error = null)
            }
            try {
                val result = repository.searchSpecies(q)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        speciesResults = result,
                        info = if (result.isEmpty()) "No species found in Spain with that name" else null
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "searchSpecies failed", e)
                _uiState.update { it.copy(isLoading = false, error = errorMessage(e)) }
            }
        }
    }

    // Detail of a sighting
    fun selectSighting(sighting: BirdSighting) = _uiState.update { it.copy(selected = sighting) }
    fun dismissDetail() = _uiState.update { it.copy(selected = null) }

    // The UI calls these once the message has been shown
    fun clearError() = _uiState.update { it.copy(error = null) }
    fun clearInfo() = _uiState.update { it.copy(info = null) }

    // --- common logic  ---

    private fun load(emptyMessage: String, request: suspend () -> List<BirdSighting>) {
        loadJob?.cancel()                         // we do not want the same load function running twice
        loadJob = viewModelScope.launch {// we use a corroutine from viewModel to
                                         // delete itself if the viewModel is destroyed
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val result = request()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        sightings = result,
                        info = if (result.isEmpty()) emptyMessage else null
                    )
                }
            } catch (e: CancellationException) {
                throw e                           // cancelled
            } catch (e: Exception) {
                Log.e(TAG, "load failed: ${e::class.simpleName}: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false, error = errorMessage(e)) }
            }
        }
    }

}
