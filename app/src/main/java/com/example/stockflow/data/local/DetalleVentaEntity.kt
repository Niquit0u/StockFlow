package com.example.stockflow.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "detalle_ventas",
    primaryKeys = ["idVenta", "idProducto"],
    foreignKeys = [
        ForeignKey(
            entity = VentaEntity::class,
            parentColumns = ["idVenta"],
            childColumns = ["idVenta"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProductoEntity::class,
            parentColumns = ["idProducto"],
            childColumns = ["idProducto"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["idProducto"])
    ]
)
data class DetalleVentaEntity(
    val idVenta: Int,
    val idProducto: Int,
    val cantidad: Int,
    val subtotal: Double
)