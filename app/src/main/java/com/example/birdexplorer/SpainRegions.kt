package com.example.birdexplorer

// Bounding boxes aproximados de las comunidades autónomas
data class SpainRegion(
    val name: String,
    val neLat: Double, val neLng: Double,
    val swLat: Double, val swLng: Double
)

val spainRegions = listOf(
    SpainRegion("Andalucía", 38.73, -1.63, 36.00, -7.52),
    SpainRegion("Aragón", 42.93, 0.77, 39.85, -2.17),
    SpainRegion("Asturias", 43.67, -4.51, 42.89, -7.19),
    SpainRegion("Baleares", 40.10, 4.33, 38.64, 1.16),
    SpainRegion("Canarias", 29.42, -13.33, 27.63, -18.16),
    SpainRegion("Cantabria", 43.52, -3.16, 42.77, -4.88),
    SpainRegion("Castilla-La Mancha", 41.35, -0.95, 38.01, -5.45),
    SpainRegion("Castilla y León", 43.25, -1.78, 40.05, -7.05),
    SpainRegion("Cataluña", 42.86, 3.33, 40.52, 0.16),
    SpainRegion("C. Valenciana", 40.79, 0.52, 37.84, -1.53),
    SpainRegion("Extremadura", 40.48, -4.64, 37.94, -7.55),
    SpainRegion("Galicia", 43.79, -6.73, 41.81, -9.30),
    SpainRegion("La Rioja", 42.65, -1.77, 41.95, -3.10),
    SpainRegion("Madrid", 41.17, -3.05, 39.88, -4.58),
    SpainRegion("Murcia", 38.77, -0.68, 37.38, -2.35),
    SpainRegion("Navarra", 43.31, -1.14, 41.90, -2.51),
    SpainRegion("País Vasco", 43.46, -1.73, 42.47, -3.45)
)