package com.example.stockflow.data.repository

import com.example.stockflow.EventoItem
import com.example.stockflow.TipoEvento
import com.example.stockflow.data.local.dao.EventoDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Historial de un local ([idLocal]): ventas, ingresos de stock y cambios en el inventario. */
class HistorialRepository(private val eventoDao: EventoDao, private val idLocal: Int) {

    fun observar(): Flow<List<EventoItem>> = eventoDao.observarDelLocal(idLocal).map { lista ->
        lista.map { evento ->
            EventoItem(
                idEvento = evento.idEvento,
                // Un tipo desconocido (de una versión futura) se muestra como cambio
                tipo = TipoEvento.entries.firstOrNull { it.name == evento.tipo } ?: TipoEvento.CAMBIO,
                fechaHora = evento.fechaHora,
                usuario = evento.usuario,
                titulo = evento.titulo,
                detalle = evento.detalle.lines().filter { it.isNotBlank() },
                monto = evento.monto
            )
        }
    }
}
