package com.example.stockflow.ui.screens.historial

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stockflow.EventoItem
import com.example.stockflow.TipoEvento
import com.example.stockflow.formatearPrecio
import com.example.stockflow.ui.componentes.TituloPantalla
import com.example.stockflow.ui.componentes.coloresBarraTitulo
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Historial del local: ventas, ingresos de stock y cambios en el inventario, de lo más nuevo a lo más viejo.
 * Se abre desde el botón de historial en Inventario; atrás vuelve al inventario.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaHistorial(
    eventos: List<EventoItem>,
    onVolver: () -> Unit
) {
    BackHandler(onBack = onVolver)

    // null = todos los tipos
    var filtro by rememberSaveable { mutableStateOf<TipoEvento?>(null) }
    val visibles = remember(eventos, filtro) { eventos.filter { filtro == null || it.tipo == filtro } }

    // Agrupados por día, en el mismo orden (más recientes primero)
    val porDia = remember(visibles) { visibles.groupBy { inicioDelDia(it.fechaHora) } }
    val ventasDeHoy = remember(eventos) {
        val hoy = inicioDelDia(System.currentTimeMillis())
        eventos.filter { it.tipo == TipoEvento.VENTA && inicioDelDia(it.fechaHora) == hoy }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { TituloPantalla(titulo = "Historial") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver al inventario")
                    }
                },
                colors = coloresBarraTitulo()
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Lo vendido hoy: lo primero que se quiere saber al abrir el historial
            if (filtro == null || filtro == TipoEvento.VENTA) {
                item {
                    ResumenDeHoy(
                        total = ventasDeHoy.sumOf { it.monto ?: 0.0 },
                        cantidadVentas = ventasDeHoy.size
                    )
                }
            }

            item {
                FiltrosHistorial(seleccionado = filtro, onSeleccionar = { filtro = it })
            }

            if (visibles.isEmpty()) {
                item {
                    Text(
                        text = when (filtro) {
                            null -> "Todavía no hay movimientos. Acá van a aparecer las ventas, los ingresos de stock y los cambios en el inventario."
                            TipoEvento.VENTA -> "Todavía no hay ventas. Registrá una desde Escáner con \"Vender\"."
                            TipoEvento.INGRESO -> "Todavía no se ingresó stock. Hacelo desde la pestaña Ingresar Stock."
                            TipoEvento.CAMBIO -> "Todavía no se modificó ningún producto ni lote."
                        },
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp, horizontal = 16.dp)
                    )
                }
            }

            porDia.forEach { (dia, eventosDelDia) ->
                item(key = "dia-$dia") {
                    Text(
                        text = nombreDelDia(dia),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                    )
                }

                items(eventosDelDia, key = { it.idEvento }) { evento ->
                    TarjetaEvento(evento)
                }
            }
        }
    }
}

@Composable
private fun FiltrosHistorial(seleccionado: TipoEvento?, onSeleccionar: (TipoEvento?) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = seleccionado == null,
            onClick = { onSeleccionar(null) },
            label = { Text("Todo") }
        )
        FilterChip(
            selected = seleccionado == TipoEvento.VENTA,
            onClick = { onSeleccionar(TipoEvento.VENTA) },
            label = { Text("Ventas") }
        )
        FilterChip(
            selected = seleccionado == TipoEvento.INGRESO,
            onClick = { onSeleccionar(TipoEvento.INGRESO) },
            label = { Text("Ingresos") }
        )
        FilterChip(
            selected = seleccionado == TipoEvento.CAMBIO,
            onClick = { onSeleccionar(TipoEvento.CAMBIO) },
            label = { Text("Cambios") }
        )
    }
}

@Composable
private fun ResumenDeHoy(total: Double, cantidadVentas: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Vendido hoy", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = when (cantidadVentas) {
                        0 -> "Sin ventas por ahora"
                        1 -> "1 venta"
                        else -> "$cantidadVentas ventas"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Text(
                text = formatearPrecio(total),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TarjetaEvento(evento: EventoItem) {
    var expandida by rememberSaveable(evento.idEvento) { mutableStateOf(false) }
    val tieneDetalle = evento.detalle.isNotEmpty()
    val (icono, colorIcono) = iconoDe(evento.tipo)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = tieneDetalle) { expandida = !expandida }
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Ícono del tipo de evento en un círculo de color suave
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(colorIcono.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icono, contentDescription = evento.tipo.nombreVisible, tint = colorIcono)
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    Text(evento.titulo, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "${formatoHora().format(evento.fechaHora)} hs, por ${evento.usuario.ifBlank { "usuario desconocido" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                evento.monto?.let { monto ->
                    Text(
                        text = formatearPrecio(monto),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                if (tieneDetalle) {
                    Icon(
                        imageVector = if (expandida) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expandida) "Ocultar detalle" else "Ver detalle",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            AnimatedVisibility(visible = expandida) {
                Column(
                    modifier = Modifier.padding(top = 8.dp, start = 52.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    HorizontalDivider()
                    evento.detalle.forEach { linea ->
                        Text(linea, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

// Cada tipo con su ícono y color: venta en verde azulado (la marca), ingreso en marino, cambio en gris pizarra
@Composable
private fun iconoDe(tipo: TipoEvento): Pair<ImageVector, Color> = when (tipo) {
    TipoEvento.VENTA -> Icons.Default.PointOfSale to MaterialTheme.colorScheme.secondary
    TipoEvento.INGRESO -> Icons.Default.AddBox to MaterialTheme.colorScheme.primary
    TipoEvento.CAMBIO -> Icons.Default.Edit to MaterialTheme.colorScheme.tertiary
}

private fun formatoHora() = SimpleDateFormat("HH:mm", Locale.getDefault())

private fun inicioDelDia(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun nombreDelDia(inicioDia: Long): String {
    val hoy = inicioDelDia(System.currentTimeMillis())
    val ayer = Calendar.getInstance().apply {
        timeInMillis = hoy
        add(Calendar.DAY_OF_YEAR, -1)
    }.timeInMillis
    return when (inicioDia) {
        hoy -> "Hoy"
        ayer -> "Ayer"
        else -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(inicioDia)
    }
}
