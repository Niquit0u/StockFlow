package com.example.stockflow.data.repository

import androidx.room.withTransaction
import com.example.stockflow.CategoriaItem
import com.example.stockflow.NOMBRE_SIN_CATEGORIA
import com.example.stockflow.data.local.StockFlowDatabase
import com.example.stockflow.data.local.entity.CategoriaEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Categorías de productos de un local ([idLocal]). Siempre queda al menos una. */
class CategoriaRepository(private val database: StockFlowDatabase, private val idLocal: Int) {

    private val categoriaDao = database.categoriaDao()

    sealed interface ResultadoEliminar {
        /** Se eliminó; sus productos pasaron a [destino]. */
        data class Eliminada(val destino: String) : ResultadoEliminar
        /** Es la única categoría del local: no se puede quedar sin ninguna. */
        data object EsLaUltima : ResultadoEliminar
    }

    fun observar(): Flow<List<CategoriaItem>> = categoriaDao.observarDelLocal(idLocal).map { lista ->
        lista.map { CategoriaItem(it.idCategoria, it.nombre) }
    }

    /** Crea "Sin categoría" si el local todavía no tiene ninguna (por ejemplo, la primera vez que se entra). */
    suspend fun asegurarCategoriaPorDefecto() {
        if (categoriaDao.obtenerDelLocal(idLocal).isEmpty()) {
            categoriaDao.insertar(CategoriaEntity(idLocal = idLocal, nombre = NOMBRE_SIN_CATEGORIA))
        }
    }

    /** @return null si se creó, o el motivo por el que no (para mostrar). */
    suspend fun crear(nombre: String): String? {
        val limpio = nombre.trim()
        if (limpio.isEmpty()) return "Escribí un nombre"
        if (categoriaDao.buscarPorNombre(idLocal, limpio) != null) return "Ya existe una categoría \"$limpio\""
        categoriaDao.insertar(
            CategoriaEntity(idLocal = idLocal, nombre = limpio, orden = categoriaDao.siguienteOrden(idLocal))
        )
        return null
    }

    /** Guarda el orden elegido arrastrando: [idsEnOrden] es la lista completa de ids, de arriba a abajo. */
    suspend fun reordenar(idsEnOrden: List<Int>) {
        database.withTransaction {
            idsEnOrden.forEachIndexed { posicion, idCategoria ->
                categoriaDao.cambiarOrden(idLocal, idCategoria, posicion)
            }
        }
    }

    /**
     * Elimina la categoría. Sus productos pasan a "Sin categoría" (o, si esa es la que se borra o no existe,
     * a la primera que quede). No se puede borrar la última.
     */
    suspend fun eliminar(idCategoria: Int): ResultadoEliminar = database.withTransaction {
        val todas = categoriaDao.obtenerDelLocal(idLocal)
        val aBorrar = todas.firstOrNull { it.idCategoria == idCategoria }
            ?: throw IllegalArgumentException("La categoría ya no existe")
        val restantes = todas - aBorrar
        if (restantes.isEmpty()) return@withTransaction ResultadoEliminar.EsLaUltima

        val destino = restantes.firstOrNull { it.nombre.equals(NOMBRE_SIN_CATEGORIA, ignoreCase = true) }
            ?: restantes.first()
        categoriaDao.moverProductos(desde = aBorrar.idCategoria, hacia = destino.idCategoria)
        categoriaDao.eliminarPorId(aBorrar.idCategoria)
        ResultadoEliminar.Eliminada(destino.nombre)
    }
}
