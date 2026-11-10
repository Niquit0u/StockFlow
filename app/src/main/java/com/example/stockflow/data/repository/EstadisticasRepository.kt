package com.example.stockflow.data.repository

import com.example.stockflow.ProductoMasVendido
import com.example.stockflow.data.local.dao.DetalleVentaDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

/** Estadísticas de ventas de un local ([idLocal]). Salen de las ventas ya registradas: no guardan nada aparte. */
class EstadisticasRepository(private val detalleVentaDao: DetalleVentaDao, private val idLocal: Int) {

    /**
     * Los [limite] productos más vendidos (por unidades) en el mes de [enMes], del más al menos vendido.
     * Se actualiza solo con cada venta nueva.
     */
    fun observarMasVendidosDelMes(enMes: Long = System.currentTimeMillis(), limite: Int = TOP): Flow<List<ProductoMasVendido>> {
        val (desde, hasta) = limitesDelMes(enMes)
        return detalleVentaDao.observarMasVendidos(idLocal, desde, hasta, limite).map { lista ->
            lista.map { ProductoMasVendido(nombre = it.nombre, cantidad = it.cantidad, total = it.total) }
        }
    }

    companion object {
        const val TOP = 10

        /** Inicio del mes de [millis] (día 1, 00:00) e inicio del mes siguiente, en hora local. */
        fun limitesDelMes(millis: Long): Pair<Long, Long> {
            val inicio = Calendar.getInstance().apply {
                timeInMillis = millis
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val fin = (inicio.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
            return inicio.timeInMillis to fin.timeInMillis
        }
    }
}
