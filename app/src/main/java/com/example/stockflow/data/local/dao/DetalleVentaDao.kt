package com.example.stockflow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.stockflow.data.local.entity.DetalleVentaEntity
import com.example.stockflow.data.local.entity.ProductoVendido
import kotlinx.coroutines.flow.Flow

@Dao
interface DetalleVentaDao {

    @Insert
    suspend fun insertar(detalle: DetalleVentaEntity)

    @Query("SELECT * FROM detalle_ventas WHERE idVenta = :idVenta")
    suspend fun obtenerPorVenta(idVenta: Int): List<DetalleVentaEntity>

    @Query("DELETE FROM detalle_ventas WHERE idVenta = :idVenta")
    suspend fun eliminarPorVenta(idVenta: Int)

    // Productos más vendidos (por unidades) de un local entre [desde, hasta). Empate: por nombre
    @Query(
        """
        SELECT productos.idProducto, productos.nombre,
               SUM(detalle_ventas.cantidad) AS cantidad, SUM(detalle_ventas.subtotal) AS total
        FROM detalle_ventas
        INNER JOIN ventas ON ventas.idVenta = detalle_ventas.idVenta
        INNER JOIN productos ON productos.idProducto = detalle_ventas.idProducto
        WHERE ventas.idLocal = :idLocal AND ventas.fechaHora >= :desde AND ventas.fechaHora < :hasta
        GROUP BY productos.idProducto
        ORDER BY cantidad DESC, productos.nombre COLLATE NOCASE
        LIMIT :limite
        """
    )
    fun observarMasVendidos(idLocal: Int, desde: Long, hasta: Long, limite: Int): Flow<List<ProductoVendido>>
}