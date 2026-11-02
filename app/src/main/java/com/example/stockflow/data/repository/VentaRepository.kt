package com.example.stockflow.data.repository

import com.example.stockflow.DetalleVentaItem
import com.example.stockflow.VentaItem
import com.example.stockflow.data.local.dao.VentaDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Historial de ventas de un local ([idLocal]). */
class VentaRepository(private val ventaDao: VentaDao, private val idLocal: Int) {

    /** Ventas del local, de la más reciente a la más vieja, cada una con sus productos. */
    fun observarHistorial(): Flow<List<VentaItem>> = ventaDao.observarHistorial(idLocal).map { filas ->
        // groupBy mantiene el orden de la consulta (fecha descendente)
        filas.groupBy { it.idVenta }.map { (idVenta, detalles) ->
            val venta = detalles.first()
            VentaItem(
                idVenta = idVenta,
                fechaHora = venta.fechaHora,
                montoTotal = venta.montoTotal,
                vendedor = venta.vendedor,
                productos = detalles.map { DetalleVentaItem(it.nombreProducto, it.cantidad, it.subtotal) }
            )
        }
    }
}
