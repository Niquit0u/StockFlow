package com.example.stockflow.data.local.entity

import androidx.room.Embedded

/** Resultado del JOIN lotes + productos que usa la pantalla de vencimientos. */
data class LoteConProducto(
    @Embedded
    val lote: LoteEntity,

    val nombre: String,

    val codigoBarra: String?,

    val categoria: String,

    val precioVenta: Double
)
