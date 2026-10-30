package com.example.stockflow.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.stockflow.data.local.entity.DetalleVentaConProducto
import com.example.stockflow.data.local.entity.VentaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VentaDao {

    @Insert
    suspend fun insertar(venta: VentaEntity): Long

    @Query("SELECT * FROM ventas ORDER BY fechaHora DESC")
    suspend fun obtenerTodas(): List<VentaEntity>

    // Una fila por producto vendido; el repositorio las agrupa por venta
    @Query(
        """
        SELECT ventas.idVenta, ventas.fechaHora, ventas.montoTotal,
               productos.nombre AS nombreProducto, detalle_ventas.cantidad, detalle_ventas.subtotal
        FROM ventas
        INNER JOIN detalle_ventas ON detalle_ventas.idVenta = ventas.idVenta
        INNER JOIN productos ON productos.idProducto = detalle_ventas.idProducto
        ORDER BY ventas.fechaHora DESC, productos.nombre
        """
    )
    fun observarHistorial(): Flow<List<DetalleVentaConProducto>>

    @Query("SELECT * FROM ventas WHERE idVenta = :id")
    suspend fun obtenerPorId(id: Int): VentaEntity?

    @Update
    suspend fun actualizar(venta: VentaEntity)

    @Delete
    suspend fun eliminar(venta: VentaEntity)
}