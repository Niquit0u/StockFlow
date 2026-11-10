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
        ),
        ForeignKey(
            entity = CategoriaEntity::class,
            parentColumns = ["idCategoria"],
            childColumns = ["idCategoria"],
            // Una categoría con productos no se borra: primero hay que vaciarla (CategoriaRepository.eliminar)
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        // Dentro de un local, un código de barras identifica a un solo producto
        // (dos locales pueden tener el mismo producto, cada uno con su stock).
        // Es nullable: SQLite permite varios NULL en un índice único (productos sin código).
        Index(value = ["idLocal", "codigoBarra"], unique = true),
        Index(value = ["idCategoria"])
    ]
)
data class ProductoEntity(
    @PrimaryKey(autoGenerate = true)
    val idProducto: Int = 0,

    val idLocal: Int,

    val nombre: String,

    val codigoBarra: String?,

    val idCategoria: Int,

    val precioVenta: Double = 0.0,

    val stockMinimo: Int = 0
)
