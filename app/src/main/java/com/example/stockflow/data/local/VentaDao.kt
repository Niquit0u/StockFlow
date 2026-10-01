package com.example.stockflow.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface VentaDao {

    @Insert
    suspend fun insertar(venta: VentaEntity): Long

    @Query("SELECT * FROM ventas ORDER BY fechaHora DESC")
    suspend fun obtenerTodas(): List<VentaEntity>

    @Query("SELECT * FROM ventas WHERE idVenta = :id")
    suspend fun obtenerPorId(id: Int): VentaEntity?

    @Update
    suspend fun actualizar(venta: VentaEntity)

    @Delete
    suspend fun eliminar(venta: VentaEntity)
}