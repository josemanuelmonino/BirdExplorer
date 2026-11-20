package com.example.birdexplorer.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

const val BASE_URL = "https://api.inaturalist.org/"
const val BIRDS_TAXON_ID = 3

// main interface
interface INatApiService {

    // Observations of birds. Used for "near me" (lat/lng/radius)
    // and for "by area" (bounding box) and for a species (taxonId).
    @GET("v1/observations")
    suspend fun getObservations(
        @Query("taxon_id") taxonId: Int = BIRDS_TAXON_ID, //  constant defined
        @Query("lat") lat: Double? = null,
        @Query("lng") lng: Double? = null,
        @Query("radius") radiusKm: Int? = null,         // define a radius (useful in near me function)
        // bounding box with two opposite corners
        @Query("nelat") neLat: Double? = null,
        @Query("nelng") neLng: Double? = null,
        @Query("swlat") swLat: Double? = null,
        @Query("swlng") swLng: Double? = null,
        @Query("photos") photos: Boolean = true,
        @Query("geo") geo: Boolean = true,  // true if there is gps information available
        @Query("captive") captive: Boolean = false,
        @Query("quality_grade") qualityGrade: String = "research",  // research is the most reliable
                                                            // because it has date location photo and other's
                                                            // confirmation of the species. The other two options
                                                            // are:
                                                            // - needs_id (yet to be confirmed)
                                                            // - casual (missing values)
        @Query("locale") locale: String = "es",     // maybe it can be used placed id
        @Query("per_page") perPage: Int = 50,
        @Query("page") page: Int = 1,
        @Query("order_by") orderBy: String = "observed_on"
    ): ObservationsResponse

    // Species search by name ("Search for a Species")
    @GET("v1/taxa")
    suspend fun searchTaxa(
        @Query("q") query: String,
        @Query("taxon_id") taxonId: Int = BIRDS_TAXON_ID, // number 3 is to filter only birds
        @Query("rank") rank: String = "species",
        @Query("locale") locale: String = "es", // only in spain
        @Query("per_page") perPage: Int = 20    // limit to 20 in spain
    ): TaxonResponse

}
