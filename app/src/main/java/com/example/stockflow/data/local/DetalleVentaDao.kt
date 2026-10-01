package com.example.stockflow.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface DetalleVentaDao {

    @Insert
    suspend fun insertar(detalle: DetalleVentaEntity)

    @Query("SELECT * FROM detalle_ventas WHERE idVenta = :idVenta")
    suspend fun obtenerPorVenta(idVenta: Int): List<DetalleVentaEntity>

    @Query("DELETE FROM detalle_ventas WHERE idVenta = :idVenta")
    suspend fun eliminarPorVenta(idVenta: Int)
}