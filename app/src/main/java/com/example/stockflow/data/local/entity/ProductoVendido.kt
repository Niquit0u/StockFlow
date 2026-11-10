package com.example.stockflow.data.local.entity

/** Resultado de la consulta de productos más vendidos (ventas sumadas por producto). */
data class ProductoVendido(
    val idProducto: Int,
    val nombre: String,
    // Unidades vendidas en el período
    val cantidad: Int,
    // Plata que entró por ese producto en el período
    val total: Double
)
