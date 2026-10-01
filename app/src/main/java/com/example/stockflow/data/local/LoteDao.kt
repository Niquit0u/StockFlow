package com.example.stockflow.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface LoteDao {

    @Insert
    suspend fun insertar(lote: LoteEntity): Long

    @Query("SELECT * FROM lotes")
    suspend fun obtenerTodos(): List<LoteEntity>

    @Query("SELECT * FROM lotes WHERE idLote = :id")
    suspend fun obtenerPorId(id: Int): LoteEntity?

    @Query("SELECT * FROM lotes WHERE idProducto = :idProducto")
    suspend fun obtenerPorProducto(idProducto: Int): List<LoteEntity>

    @Update
    suspend fun actualizar(lote: LoteEntity)

    @Delete
    suspend fun eliminar(lote: LoteEntity)

    @Query("DELETE FROM lotes")
    suspend fun eliminarTodos()
}