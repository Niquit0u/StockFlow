package com.example.stockflow.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "productos",
    indices = [
        // Un código de barras identifica a un solo producto.
        // Es nullable: SQLite permite varios NULL en un índice único (productos sin código).
        Index(value = ["codigoBarra"], unique = true)
    ]
)
data class ProductoEntity(
    @PrimaryKey(autoGenerate = true)
    val idProducto: Int = 0,

    val nombre: String,

    val codigoBarra: String?,

    val categoria: String,

    val precioVenta: Double = 0.0,

    val stockMinimo: Int = 0
)
