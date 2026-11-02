package com.example.stockflow.ui.componentes

import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Arrastrar para reordenar elementos de una LazyColumn.
 * Solo se mueve dentro de [rango] (índices de la lista): así, por ejemplo, una categoría
 * nunca queda debajo del botón "Agregar categoría".
 *
 * Uso: [iniciar] al mantener apretado el ícono de arrastre, [arrastrar] con cada movimiento del dedo,
 * [terminar] al soltar. [desplazamiento] es cuánto dibujar corrido el elemento arrastrado.
 */
class EstadoReordenar(
    private val lista: LazyListState,
    private val rango: () -> IntRange,
    private val onMover: (desde: Int, hacia: Int) -> Unit
) {
    var indiceArrastrado by mutableStateOf<Int?>(null)
        private set

    private var delta by mutableFloatStateOf(0f)
    private var offsetInicial = 0

    private fun info(indice: Int): LazyListItemInfo? =
        lista.layoutInfo.visibleItemsInfo.firstOrNull { it.index == indice }

    /** Cuánto se dibuja corrido (en píxeles) el elemento arrastrado respecto de su lugar actual. */
    val desplazamiento: Float
        get() {
            val actual = indiceArrastrado ?: return 0f
            val elemento = info(actual) ?: return 0f
            return offsetInicial + delta - elemento.offset
        }

    fun iniciar(indice: Int) {
        val elemento = info(indice) ?: return
        indiceArrastrado = indice
        offsetInicial = elemento.offset
        delta = 0f
    }

    fun arrastrar(dy: Float) {
        val actual = indiceArrastrado ?: return
        val elemento = info(actual) ?: return
        val permitido = rango()

        // Puede pasarse de los extremos del rango solo media altura (como un elástico): se nota que se mueve
        // aunque no haya adónde ir (por ejemplo, con una sola categoría), pero nunca llega a otro lugar
        val margen = elemento.size / 2f
        val primero = info(permitido.first)
        val ultimo = info(permitido.last)
        var nuevoDelta = delta + dy
        primero?.let { nuevoDelta = maxOf(nuevoDelta, it.offset - offsetInicial - margen) }
        ultimo?.let { nuevoDelta = minOf(nuevoDelta, it.offset + it.size - elemento.size - offsetInicial + margen) }
        delta = nuevoDelta

        // Cuando el centro del elemento pasa sobre otro del rango, intercambian lugar
        val centro = offsetInicial + delta + elemento.size / 2f
        val destino = lista.layoutInfo.visibleItemsInfo.firstOrNull {
            it.index != actual && it.index in permitido && centro.toInt() in it.offset..(it.offset + it.size)
        } ?: return
        onMover(actual, destino.index)
        indiceArrastrado = destino.index
    }

    fun terminar() {
        indiceArrastrado = null
        delta = 0f
    }
}
