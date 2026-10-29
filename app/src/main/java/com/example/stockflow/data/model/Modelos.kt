package com.example.stockflow

import java.text.NumberFormat
import java.util.Locale

enum class EstadoSemaforo {
    CRITICO,     // Rojo: Vencimiento inmediato
    ADVERTENCIA, // Amarillo: Vencimiento próximo
    OPTIMO       // Verde: Buen estado
}

data class LoteItem(
    val idLote: String,
    val nombreProducto: String,
    val categoria: String,
    val codigoBarra: String,
    val cantidadActual: Int,
    val fechaVencimiento: String,
    val diasParaVencer: Int,
    val estado: EstadoSemaforo,
    val precio: Double? = null // Precio de venta del producto (null si no tiene)
)

data class Producto(
    val nombreProducto: String,
    val categoria: String,
    val codigoBarra: String,
    val totalCantidad: Int,
    val lotes: List<LoteItem>,
    val precio: Double? = null // Precio de venta unitario (null si no tiene)
)

// Producto escaneado que se va a descontar del stock
data class ItemDescuento(
    val codigoBarra: String,
    val nombreProducto: String,
    val cantidad: Int,
    val precioUnitario: Double? = null
) {
    // null si el producto no tiene precio cargado
    val subtotal: Double? get() = precioUnitario?.times(cantidad)
}

// Cambios hechos sobre un lote desde el lápiz de Inventario
data class CambiosLote(
    val idLote: Int,
    val nombre: String,
    val codigoBarra: String, // vacío = producto sin código
    val cantidad: Int,
    val precio: Double?, // null = sin precio
    val fechaVencimiento: String // dd/MM/yyyy
)

/** Formatea un importe como "$1.234,50" según el idioma del teléfono. */
fun formatearPrecio(valor: Double): String {
    val formato = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    return "$" + formato.format(valor)
}

// Lista de categorías disponibles en la tienda
val categoriasDisponibles = listOf("Lácteos", "Bebidas", "Carnes", "Snacks", "Frutas & Verduras", "Otros")
