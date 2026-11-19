package com.example.birdexplorer.data.remote

import kotlinx.serialization.Serializable

// A page of results from /v1/observations
@Serializable
data class ObservationsResponse(
    val total_results: Int = 0,   // total observations matching the query
    val page: Int = 1,            // current page
    val per_page: Int = 0,        // page size
    val results: List<Observation> = emptyList() // the results will be a list of sightings
)

// One sighting: who/when/where a bird was seen
@Serializable
data class Observation(
    val id: Long,           // identifier
    val observed_on: String? = null,    // date, e.g. "2026-11-02"
    val place_guess: String? = null,    // place name as text
    val geojson: GeoJson? = null,    // null if the location is hidden
    val taxon: Taxon? = null,        // the species observed
    val photos: List<Photo> = emptyList()  //photos uploaded in Naturalist by the users
)

// JSON object { "type": "Point", "coordinates": [lon, lat] }
// an other way to do this is to get the string (no new class needed but
// a little more complex)
@Serializable
data class GeoJson(val coordinates: List<Double> = emptyList())

// The species
@Serializable
data class Taxon(
    val id: Long,
    val name: String,                           // scientific name
    val preferred_common_name: String? = null,  // common name (may be missing)
    val default_photo: Photo? = null         // representative photo of the species in case not provided
)

@Serializable
data class Photo(
    // iNaturalist photo
    val url: String? = null,          // square thumbnail
    val medium_url: String? = null,   // medium size, Naturalist stores the images in different
                                      // sizes (may be missing in observations)
    val attribution: String? = null   // credit text, must be shown with the photo
)

// Response of /v1/taxa (species search)
@Serializable
data class TaxonResponse(val results: List<Taxon> = emptyList())