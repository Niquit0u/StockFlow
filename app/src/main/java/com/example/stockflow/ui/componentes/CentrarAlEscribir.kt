package com.example.stockflow.ui.componentes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay

/**
 * Cuando un campo de texto recibe el foco (o el teclado achica la pantalla), la lista o columna que lo
 * contiene se desplaza para dejarlo en el centro de la zona visible, en vez de pegado al borde o al teclado.
 * Solo desplaza: no hace zoom.
 */
@OptIn(ExperimentalFoundationApi::class)
private object CentrarEnPantalla : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        // offset = dónde empieza el campo respecto del borde visible; se lleva su centro al centro
        return offset + size / 2f - containerSize / 2f
    }
}

/** Aplica [CentrarEnPantalla] a todas las pantallas que estén dentro de [contenido]. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CentrarCamposAlEscribir(contenido: @Composable () -> Unit) {
    CompositionLocalProvider(LocalBringIntoViewSpec provides CentrarEnPantalla, content = contenido)
}

/**
 * Pone el campo a la vista (centrado, según [CentrarCamposAlEscribir]) cuando recibe el foco,
 * esperando a que el teclado termine de abrirse: si se pide antes, el teclado todavía está animando
 * y el campo puede quedar tapado (pasaba con la fecha de vencimiento, que es el último campo).
 */
fun Modifier.visibleAlEscribir(): Modifier = composed {
    val pedido = remember { BringIntoViewRequester() }
    var enfocado by remember { mutableStateOf(false) }
    // Alto actual del teclado; cambia en cada cuadro mientras se abre
    val altoTeclado = WindowInsets.ime.getBottom(LocalDensity.current)

    LaunchedEffect(enfocado, altoTeclado) {
        if (!enfocado) return@LaunchedEffect
        // Cada cambio de alto reinicia la espera: se pide una sola vez, cuando el teclado quedó quieto
        delay(ESPERA_TECLADO_MS)
        pedido.bringIntoView()
    }

    bringIntoViewRequester(pedido).onFocusEvent { enfocado = it.isFocused }
}

private const val ESPERA_TECLADO_MS = 120L
