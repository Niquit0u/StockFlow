package com.example.stockflow.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "productos",
    foreignKeys = [
        ForeignKey(
            entity = LocalEntity::class,
            parentColumns = ["idLocal"],
            childColumns = ["idLocal"],
            // Si se borra un local se borra todo lo suyo
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        // Dentro de un local, un código de barras identifica a un solo producto
        // (dos locales pueden tener el mismo producto, cada uno con su stock).
        // Es nullable: SQLite permite varios NULL en un índice único (productos sin código).
        Index(value = ["idLocal", "codigoBarra"], unique = true)
    ]
)
data class ProductoEntity(
    @PrimaryKey(autoGenerate = true)
    val idProducto: Int = 0,

    val idLocal: Int,

    val nombre: String,

    val codigoBarra: String?,

    val categoria: String,

    val precioVenta: Double = 0.0,

    val stockMinimo: Int = 0
)
