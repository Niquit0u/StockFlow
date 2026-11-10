package com.example.stockflow

import java.text.NumberFormat
import java.util.Locale

enum class EstadoSemaforo {
    CRITICO,     // Rojo: vence en 1 día o menos (o ya venció). No se puede vender
    ADVERTENCIA, // Amarillo: vence en 2 a 14 días
    OPTIMO       // Verde: Buen estado
}

/** Último día (días que faltan) en que un lote todavía se puede vender: con menos queda en rojo y bloqueado. */
const val DIAS_MINIMOS_PARA_VENDER = 2

fun sePuedeVender(diasParaVencer: Int): Boolean = diasParaVencer >= DIAS_MINIMOS_PARA_VENDER

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
) {
    // Lo vencido o a 1 día de vencer no se vende: solo cuentan los lotes con 2 días o más
    val cantidadVendible: Int get() = lotes.filter { sePuedeVender(it.diasParaVencer) }.sumOf { it.cantidadActual }
    val cantidadNoVendible: Int get() = totalCantidad - cantidadVendible
    // true si todo su stock ya pasó la fecha (para el mensaje: "vencido" o "por vencer")
    val todoVencido: Boolean get() = lotes.all { it.diasParaVencer < 0 }
}

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
    val fechaVencimiento: String, // dd/MM/yyyy
    val categoria: String // nombre de una categoría del local
)

// Tipos de entrada del historial
enum class TipoEvento(val nombreVisible: String) {
    VENTA("Venta"),
    INGRESO("Ingreso de stock"),
    CAMBIO("Cambio en inventario")
}

// Entrada del historial del local
data class EventoItem(
    val idEvento: Int,
    val tipo: TipoEvento,
    val fechaHora: Long, // epoch millis
    val usuario: String, // quién lo hizo
    val titulo: String,
    val detalle: List<String>, // líneas de detalle (puede estar vacía)
    val monto: Double? // solo en ventas
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
    val rol: Rol,
    // Local del usuario: todo lo que ve y hace (inventario, ventas, empleados) es de este local
    val idLocal: Int,
    val nombreLocal: String
)

// Usuario para la lista de "Cuenta" (gestión de usuarios del dueño)
data class UsuarioItem(
    val idUsuario: Int,
    val nombre: String,
    val email: String,
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

// Categoría que tiene todo local desde el principio, para productos sin una categoría asignada
const val NOMBRE_SIN_CATEGORIA = "Sin categoría"

// Categoría de productos de un local
data class CategoriaItem(
    val idCategoria: Int,
    val nombre: String
)
