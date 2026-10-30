package com.example.stockflow.ui.screens.ventas

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stockflow.VentaItem
import com.example.stockflow.formatearPrecio
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Historial de ventas registradas con "Descontar productos".
 * Se abre desde el botón de recibo en Inventario; atrás vuelve al inventario.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaHistorialVentas(
    ventas: List<VentaItem>,
    onVolver: () -> Unit
) {
    BackHandler(onBack = onVolver)

    // Ventas agrupadas por día, en el mismo orden (más recientes primero)
    val ventasPorDia = remember(ventas) { ventas.groupBy { inicioDelDia(it.fechaHora) } }
    val ventasDeHoy = ventasPorDia[inicioDelDia(System.currentTimeMillis())].orEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial de ventas", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver al inventario")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { padding ->
        if (ventas.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Todavía no hay ventas.\nRegistrá una desde Escáner con \"Descontar productos\".",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                ResumenDeHoy(
                    total = ventasDeHoy.sumOf { it.montoTotal },
                    cantidadVentas = ventasDeHoy.size
                )
            }

            ventasPorDia.forEach { (dia, ventasDelDia) ->
                item(key = "dia-$dia") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = nombreDelDia(dia),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = formatearPrecio(ventasDelDia.sumOf { it.montoTotal }),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                items(ventasDelDia, key = { it.idVenta }) { venta ->
                    TarjetaVenta(venta)
                }
            }
        }
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
private fun TarjetaVenta(venta: VentaItem) {
    var expandida by rememberSaveable(venta.idVenta) { mutableStateOf(false) }
    val unidades = venta.productos.sumOf { it.cantidad }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expandida = !expandida }
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Venta #${venta.idVenta}", fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "${formatoHora().format(venta.fechaHora)} hs, " +
                            if (unidades == 1) "1 unidad" else "$unidades unidades",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = formatearPrecio(venta.montoTotal),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Icon(
                    imageVector = if (expandida) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expandida) "Ocultar productos" else "Ver productos",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            AnimatedVisibility(visible = expandida) {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    HorizontalDivider()
                    venta.productos.forEach { detalle ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = detalle.nombreProducto,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "x${detalle.cantidad}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            Text(
                                // Subtotal 0 = el producto no tenía precio al venderse
                                text = if (detalle.subtotal > 0) formatearPrecio(detalle.subtotal) else "Sin precio",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
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
