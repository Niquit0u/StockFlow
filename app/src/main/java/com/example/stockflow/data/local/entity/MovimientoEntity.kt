package com.example.stockflow.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "movimientos",
    foreignKeys = [
        ForeignKey(
            entity = LoteEntity::class,
            parentColumns = ["idLote"],
            childColumns = ["idLote"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["idLote"])
    ]
)
data class MovimientoEntity(
    @PrimaryKey(autoGenerate = true)
    val idMovimiento: Int = 0,

    val tipo: String,

    val idLote: Int,

    val cantidad: Int,

    val fechaHora: Long = System.currentTimeMillis()
) {
    companion object {
        const val TIPO_ENTRADA = "ENTRADA"
        const val TIPO_SALIDA = "SALIDA"
    }
}
