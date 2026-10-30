package com.example.stockflow.data.local.entity

/** Una fila del JOIN ventas + detalle_ventas + productos que usa el historial de ventas. */
data class DetalleVentaConProducto(
    val idVenta: Int,
    val fechaHora: Long,
    val montoTotal: Double,
    val vendedor: String,
    val nombreProducto: String,
    val cantidad: Int,
    val subtotal: Double
)
