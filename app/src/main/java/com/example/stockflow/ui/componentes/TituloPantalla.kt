package com.example.stockflow.ui.componentes

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow

/**
 * Título de la barra superior, igual en todas las pantallas: el nombre de la pantalla y, debajo,
 * más chico, el contexto (el local, o la sección en la que se está).
 */
@Composable
fun TituloPantalla(titulo: String, subtitulo: String? = null) {
    Column {
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!subtitulo.isNullOrBlank()) {
            Text(
                text = subtitulo,
                style = MaterialTheme.typography.labelMedium,
                // Mismo color que el título, pero más suave: se lee como información secundaria
                color = LocalContentColor.current.copy(alpha = 0.72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Colores de la barra superior, iguales en todas las pantallas: celeste suave con texto marino.
 * (Se probó marino sólido y al usuario no le gustó.)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun coloresBarraTitulo(): TopAppBarColors = TopAppBarDefaults.topAppBarColors(
    containerColor = MaterialTheme.colorScheme.primaryContainer,
    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
)
