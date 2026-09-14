package com.example.stockflow

enum class EstadoSemaforo {
    CRITICO,     // Rojo: Vencimiento inmediato
    ADVERTENCIA, // Amarillo: Vencimiento próximo
    OPTIMO       // Verde: Buen estado
}

data class LoteItem(
    val idLote: String,
    val nombreProducto: String,
    val categoria: String, // Nueva categoría del producto
    val codigoBarra: String,
    val cantidadActual: Int,
    val fechaVencimiento: String,
    val diasParaVencer: Int,
    val estado: EstadoSemaforo
)

data class Producto(
    val nombreProducto: String,
    val categoria: String,
    val codigoBarra: String,
    val totalCantidad: Int,
    val lotes: List<LoteItem>
)

// Lista de categorías disponibles en la tienda
val categoriasDisponibles = listOf("Lácteos", "Bebidas", "Carnes", "Snacks", "Frutas & Verduras", "Otros")

// Datos simulados clasificados por categoría
val datosSimuladosLotes = listOf(
    LoteItem("L001", "Leche Entera 1L", "Lácteos", "779123456789", 5, "18/09/2026", 4, EstadoSemaforo.CRITICO),
    LoteItem("L002", "Leche Entera 1L", "Lácteos", "779123456789", 15, "05/10/2026", 21, EstadoSemaforo.OPTIMO),
    LoteItem("L003", "Coca-Cola 2.25L", "Bebidas", "779888877771", 24, "20/11/2026", 67, EstadoSemaforo.OPTIMO),
    LoteItem("L004", "Manzana Red (Kg)", "Frutas & Verduras", "779000011112", 10, "17/09/2026", 3, EstadoSemaforo.CRITICO),
    LoteItem("L005", "Papas Fritas 150g", "Snacks", "779333344445", 18, "28/09/2026", 14, EstadoSemaforo.ADVERTENCIA),
    LoteItem("L006", "Carne Picada Especial", "Carnes", "779222233334", 8, "16/09/2026", 2, EstadoSemaforo.CRITICO)
)

// Función para agrupar por producto y filtrar por categoría
fun obtenerProductosAgrupados(lotes: List<LoteItem>, categoriaFiltro: String): List<Producto> {
    val lotesFiltrados = if (categoriaFiltro == "Todas") {
        lotes
    } else {
        lotes.filter { it.categoria.equals(categoriaFiltro, ignoreCase = true) }
    }

    return lotesFiltrados.groupBy { it.nombreProducto }.map { (nombre, listaLotes) ->
        Producto(
            nombreProducto = nombre,
            categoria = listaLotes.firstOrNull()?.categoria ?: "General",
            codigoBarra = listaLotes.firstOrNull()?.codigoBarra ?: "",
            totalCantidad = listaLotes.sumOf { it.cantidadActual },
            lotes = listaLotes.sortedBy { it.diasParaVencer } // Orden FEFO
        )
    }
}