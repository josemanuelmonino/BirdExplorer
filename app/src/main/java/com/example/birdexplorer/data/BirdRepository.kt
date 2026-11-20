package com.example.birdexplorer.data

import com.example.birdexplorer.data.remote.*

data class BirdSighting(
    val id: Long,
    val commonName: String,
    val scientificName: String,
    val place: String?,
    val lat: Double,
    val lon: Double,
    val photoUrl: String?,
    val attribution: String?
)

// In this repository we will present the results of the serialization and conversion of the
// iNaturalist JSON in a more clean way so in the UI
class BirdRepository(private val api: INatApiService = NetworkDataSource.service) {

    suspend fun nearMe(lat: Double, lon: Double, radiusKm: Int = 10) =
        api.getObservations(lat = lat, lng = lon, radiusKm = radiusKm).toSightings()

    suspend fun byArea(neLat: Double, neLng: Double, swLat: Double, swLng: Double) =
        api.getObservations(neLat = neLat, neLng = neLng, swLat = swLat, swLng = swLng).toSightings()

    suspend fun bySpecies(taxonId: Int) =
        api.getObservations(taxonId = taxonId).toSightings()

    suspend fun searchSpecies(query: String) = api.searchTaxa(query).results

    private fun ObservationsResponse.toSightings(): List<BirdSighting> =
        results.mapNotNull { o ->
            val c = o.geojson?.coordinates
            if (c == null || c.size < 2) return@mapNotNull null   // ubicación oculta
            val photo = o.photos.firstOrNull() ?: o.taxon?.default_photo
            BirdSighting(
                id = o.id,
                commonName = o.taxon?.preferred_common_name ?: o.taxon?.name ?: "Desconocida",
                scientificName = o.taxon?.name ?: "",
                place = o.place_guess,
                lat = c[1], lon = c[0],                            // [lon, lat]
                photoUrl = (photo?.medium_url ?: photo?.url)?.replace("square", "medium"),
                attribution = photo?.attribution
            )
        }
}