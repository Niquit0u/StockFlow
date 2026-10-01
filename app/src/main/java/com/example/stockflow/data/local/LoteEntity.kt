package com.example.stockflow.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "lotes",
    foreignKeys = [
        ForeignKey(
            entity = ProductoEntity::class,
            parentColumns = ["idProducto"],
            childColumns = ["idProducto"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["idProducto"])
    ]
)
data class LoteEntity(
    @PrimaryKey(autoGenerate = true)
    val idLote: Int = 0,

    val idProducto: Int,

    val cantidadActual: Int,

    val fechaVencimiento: Long
)