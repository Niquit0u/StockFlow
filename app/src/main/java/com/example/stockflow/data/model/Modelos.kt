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

// Venta registrada, para el historial de ventas
data class VentaItem(
    val idVenta: Int,
    val fechaHora: Long, // epoch millis
    val montoTotal: Double,
    val vendedor: String, // nombre del usuario que hizo la venta
    val productos: List<DetalleVentaItem>
)

data class DetalleVentaItem(
    val nombreProducto: String,
    val cantidad: Int,
    val subtotal: Double
)

/**
 * Roles de usuario y lo que cada uno puede hacer.
 * [clave] es lo que se guarda en la base (columna usuarios.rol).
 */
enum class Rol(val clave: String, val nombreVisible: String) {
    DUENO("DUENO", "Dueño"),
    ADMINISTRADOR("ADMINISTRADOR", "Administrador"),
    EMPLEADO("EMPLEADO", "Empleado");

    // Editar lotes desde Inventario
    val puedeEditarInventario: Boolean get() = this != EMPLEADO

    // Pestaña Ingresar Stock
    val puedeIngresarStock: Boolean get() = this != EMPLEADO

    // Crear y eliminar usuarios
    val puedeGestionarUsuarios: Boolean get() = this == DUENO

    companion object {
        // Un valor desconocido (por ejemplo, de una versión vieja) se trata con el menor permiso
        fun desdeClave(clave: String): Rol = entries.firstOrNull { it.clave == clave } ?: EMPLEADO
    }
}

// Usuario que inició sesión
data class SesionUsuario(
    val idUsuario: Int,
    val nombre: String,
    val rol: Rol
)

// Usuario para la lista de "Cuenta" (gestión de usuarios del dueño)
data class UsuarioItem(
    val idUsuario: Int,
    val nombre: String,
    val email: String?,
    val rol: Rol
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
