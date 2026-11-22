package com.example.birdexplorer

import java.text.Normalizer

// Common birds in Spain, used to suggest names while the user types.
// It is a local list, so the suggestions do not use any API request
val commonBirdNames = listOf(
    "Petirrojo europeo", "Ánade azulón", "Garza gris", "Pinzón vulgar", "Mirlo común",
    "Gorrión común", "Carbonero común", "Busardo ratonero", "Paloma torcaz", "Gaviota encapuchada",
    "Cernícalo común", "Cormorán grande", "Herrerillo común", "Estornino pinto eurasiático", "Colirrojo tizón",
    "Corneja negra", "Garceta común", "Urraca común", "Cisne vulgar", "Paloma Doméstica",
    "Focha común", "Gallineta común", "Lavandera blanca", "Mosquitero común", "Curruca capirotada",
    "Jilguero europeo", "Tórtola turca", "Gaviota patiamarilla", "Tarabilla europea", "Arrendajo euroasiático",
    "Flamenco común", "Somormujo lavanco", "Milano negro", "Golondrina tijereta", "Buitre leonado",
    "Cigüeña blanca", "Pico picapinos", "Mito", "Gaviota argéntea europea", "Martín pescador euroasiático",
    "Serín Verdecillo", "Carpintero verde", "Grajilla occidental", "Tarro blanco", "Garza blanca",
    "Cigüeñuela común", "Milano real", "Chochín paleártico", "Zorzal Común", "Curruca cabecinegra",
    "Pardillo Común", "Verderón Común", "Garcilla bueyera occidental", "Acentor común", "Cotorra de Kramer",
    "Collalba norteña", "Trepador Azul", "Faisán asiático de collar", "Trepatroncos europeo", "Lavandera cascadeña",
    "Andarríos chico", "Abubilla común", "Ganso Canadiense Mayor", "Escribano soteño", "Zampullín común",
    "Avión común occidental", "Bisbita pratense", "Avefría europea", "Abejaruco Europeo", "Aguilucho lagunero occidental",
    "Culebrera europea", "Avoceta Común", "Vuelvepiedras Rojizo", "Cerceta Alas Verdes", "Charrán Común",
    "Alcaudón dorsirrojo", "Vencejo común", "Gavilán común", "Papamoscas cerrojillo", "Ibis cara oscura",
    "Reyezuelo listado", "Grulla común", "Colirrojo real", "Ganso común", "Escribano triguero",
    "Cárabo", "Ruiseñor común", "Pato cucharón norteño", "Cuervo Común", "Escribano palustre",
    "Lavandera boyera", "Espátula Común", "Buitrón", "Pato colorado", "Archibebe común",
    "Gaviota Mayor", "Zorzal Charlo", "Pato de copete", "Cetia ruiseñor", "Carbonero garrapinos"
)

// Removes accents and capital letters so "aguila" also matches "Águila"
private fun normalize(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}"), "")
        .lowercase()

// Names that contain what the user has typed, without repeating a name already written in full
fun suggestionsFor(query: String, max: Int = 4): List<String> {
    val q = normalize(query.trim())
    if (q.length < 2) return emptyList()
    return commonBirdNames
        .filter { normalize(it).contains(q) && normalize(it) != q }
        .take(max)
}
