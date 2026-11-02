package com.example.stockflow.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/*
 * Paleta de StockFlow, tomada del logo: azul marino (confianza, "control total")
 * y verde azulado (movimiento, "flujo constante"). Fondos fríos y neutros para que
 * los únicos colores fuertes de la pantalla sean la marca y el semáforo de vencimientos.
 */

// ---------- Tema claro ----------
val Marino = Color(0xFF1D4E7A)
val SobreMarino = Color(0xFFFFFFFF)
val MarinoSuave = Color(0xFFD3E4F5)
val SobreMarinoSuave = Color(0xFF0B2A45)

val Flujo = Color(0xFF0E7F78)
val SobreFlujo = Color(0xFFFFFFFF)
val FlujoSuave = Color(0xFFC8EEEA)
val SobreFlujoSuave = Color(0xFF00302C)

val Pizarra = Color(0xFF55667A)
val SobrePizarra = Color(0xFFFFFFFF)
val PizarraSuave = Color(0xFFDCE3EB)
val SobrePizarraSuave = Color(0xFF1A2633)

val Fondo = Color(0xFFF5F7F9)
val Superficie = Color(0xFFFFFFFF)
val Tinta = Color(0xFF17212B)
val SuperficieVariante = Color(0xFFE3E9EF)
val TintaSuave = Color(0xFF4A5764)
val Contorno = Color(0xFF8693A0)
val ContornoSuave = Color(0xFFC7D0D8)

val SuperficieContenedorMasBaja = Color(0xFFFFFFFF)
val SuperficieContenedorBaja = Color(0xFFF7F9FB)
val SuperficieContenedor = Color(0xFFF0F3F6)
val SuperficieContenedorAlta = Color(0xFFEAEEF2)
val SuperficieContenedorMasAlta = Color(0xFFE3E8ED)

// ---------- Tema oscuro ----------
val MarinoOscuro = Color(0xFF9CC6EE)
val SobreMarinoOscuro = Color(0xFF003256)
val MarinoSuaveOscuro = Color(0xFF1A4268)
val SobreMarinoSuaveOscuro = Color(0xFFD3E4F5)

val FlujoOscuro = Color(0xFF6FD6CC)
val SobreFlujoOscuro = Color(0xFF003733)
val FlujoSuaveOscuro = Color(0xFF005049)
val SobreFlujoSuaveOscuro = Color(0xFFC8EEEA)

val PizarraOscuro = Color(0xFFB8C7D9)
val SobrePizarraOscuro = Color(0xFF233140)
val PizarraSuaveOscuro = Color(0xFF3A4858)
val SobrePizarraSuaveOscuro = Color(0xFFDCE3EB)

val FondoOscuro = Color(0xFF0F161D)
val SuperficieOscura = Color(0xFF121A22)
val TintaOscura = Color(0xFFE1E6EB)
val SuperficieVarianteOscura = Color(0xFF2A3540)
val TintaSuaveOscura = Color(0xFFB9C4CF)
val ContornoOscuro = Color(0xFF84909C)
val ContornoSuaveOscuro = Color(0xFF3A4652)

val SuperficieContenedorMasBajaOscura = Color(0xFF0B1117)
val SuperficieContenedorBajaOscura = Color(0xFF161E26)
val SuperficieContenedorOscura = Color(0xFF1A232C)
val SuperficieContenedorAltaOscura = Color(0xFF222C36)
val SuperficieContenedorMasAltaOscura = Color(0xFF2C3742)

/**
 * Semáforo de vencimientos. Son colores con significado (no de marca): rojo, ámbar y verde.
 * Hay una versión para fondos claros y otra para oscuros, así el texto de "N días" se lee bien en ambos
 * (contraste de al menos 4.5:1, como pide la guía de accesibilidad WCAG AA).
 */
object Semaforo {
    private val enTemaOscuro: Boolean
        @Composable get() = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val critico: Color
        @Composable get() = if (enTemaOscuro) Color(0xFFFF7A7A) else Color(0xFFC62828)

    val advertencia: Color
        @Composable get() = if (enTemaOscuro) Color(0xFFF2A93B) else Color(0xFF975600)

    val optimo: Color
        @Composable get() = if (enTemaOscuro) Color(0xFF4CC38A) else Color(0xFF1E7A4C)
}
