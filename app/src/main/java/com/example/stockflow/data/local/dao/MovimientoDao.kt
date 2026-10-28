package com.example.stockflow.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.stockflow.data.local.entity.MovimientoEntity

@Dao
interface MovimientoDao {

    @Insert
    suspend fun insertar(movimiento: MovimientoEntity): Long

    @Query("SELECT * FROM movimientos ORDER BY idMovimiento DESC")
    suspend fun obtenerTodos(): List<MovimientoEntity>

    @Query("SELECT * FROM movimientos WHERE idLote = :idLote")
    suspend fun obtenerPorLote(idLote: Int): List<MovimientoEntity>

    @Delete
    suspend fun eliminar(movimiento: MovimientoEntity)
}