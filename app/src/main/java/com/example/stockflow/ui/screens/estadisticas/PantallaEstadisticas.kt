package com.example.stockflow.ui.screens.estadisticas

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stockflow.ProductoMasVendido
import com.example.stockflow.formatearPrecio
import com.example.stockflow.ui.componentes.TituloPantalla
import com.example.stockflow.ui.componentes.coloresBarraTitulo
import com.example.stockflow.ui.theme.StockFlowTheme
import java.text.SimpleDateFormat
import java.util.Locale

/** Cantidad fija de lugares del gráfico: si hay menos productos, los que faltan quedan vacíos. */
private const val LUGARES = 10

/**
 * Estadísticas del local: los 10 productos más vendidos del mes, en un gráfico de barras
 * (a la izquierda el más vendido) y en una lista con las unidades y la plata de cada uno.
 * Se abre desde el botón de estadísticas en Inventario; atrás vuelve al inventario.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaEstadisticas(
    // Del más al menos vendido; como mucho 10
    masVendidos: List<ProductoMasVendido>,
    onVolver: () -> Unit
) {
    BackHandler(onBack = onVolver)

    // Lugar tocado en el gráfico (0 = el más vendido); null = ninguno
    var seleccionado by remember { mutableStateOf<Int?>(null) }
    // Si cambia el ranking (entró una venta) y el lugar tocado ya no existe, se deselecciona
    if ((seleccionado ?: 0) >= masVendidos.size) seleccionado = null

    val mes = remember {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(System.currentTimeMillis())
            .replaceFirstChar { it.titlecase(Locale.getDefault()) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { TituloPantalla(titulo = "Estadísticas", subtitulo = mes) },
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
            item {
                TarjetaGrafico(
                    masVendidos = masVendidos,
                    seleccionado = seleccionado,
                    onSeleccionar = { lugar -> seleccionado = if (seleccionado == lugar) null else lugar }
                )
            }

            item {
                Text(
                    "Ranking del mes",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )
            }

            if (masVendidos.isEmpty()) {
                item {
                    Text(
                        "Todavía no hay ventas este mes. A medida que vendas desde Escáner, " +
                            "acá van a aparecer los productos que más salen.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp, horizontal = 16.dp)
                    )
                }
            }

            itemsIndexed(masVendidos) { indice, producto ->
                FilaRanking(
                    puesto = indice + 1,
                    producto = producto,
                    resaltada = seleccionado == indice,
                    onClick = { seleccionado = if (seleccionado == indice) null else indice }
                )
            }
        }
    }
}

@Composable
private fun TarjetaGrafico(
    masVendidos: List<ProductoMasVendido>,
    seleccionado: Int?,
    onSeleccionar: (Int) -> Unit
) {
    val maximo = masVendidos.maxOfOrNull { it.cantidad }?.coerceAtLeast(1) ?: 1
    val colorBarra = MaterialTheme.colorScheme.secondary

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Top $LUGARES más vendidos", fontWeight = FontWeight.Bold)
            Text(
                "Unidades vendidas en el mes",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Detalle de la barra tocada (hace de tooltip); sin selección, una ayuda
            val detalle = seleccionado?.let { masVendidos.getOrNull(it) }
            Text(
                text = when {
                    detalle != null -> "${seleccionado!! + 1}. ${detalle.nombre}: ${detalle.cantidad} u."
                    masVendidos.isEmpty() -> "Sin ventas todavía"
                    else -> "Tocá una barra para ver el producto"
                },
                fontSize = 13.sp,
                fontWeight = if (detalle != null) FontWeight.SemiBold else FontWeight.Normal,
                color = if (detalle != null) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
            )

            // Barras: siempre 10 lugares del mismo ancho, del más vendido (izquierda) al menos vendido
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                repeat(LUGARES) { lugar ->
                    val producto = masVendidos.getOrNull(lugar)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .then(
                                if (producto != null) Modifier
                                    .clickable { onSeleccionar(lugar) }
                                    .semantics {
                                        contentDescription =
                                            "Puesto ${lugar + 1}: ${producto.nombre}, ${producto.cantidad} unidades"
                                    }
                                else Modifier
                            ),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        if (producto != null) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                // Unidades vendidas arriba de cada barra (los lugares vacíos no tienen)
                                Text(
                                    "${producto.cantidad}",
                                    fontSize = 11.sp,
                                    fontWeight = if (lugar == seleccionado) FontWeight.Bold else FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Spacer(Modifier.height(2.dp))
                                val alto = (176f * producto.cantidad / maximo).coerceAtLeast(4f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.62f)
                                        .height(alto.dp)
                                        .background(
                                            // Con una barra tocada, las demás quedan más tenues
                                            color = if (seleccionado == null || seleccionado == lugar) colorBarra
                                            else colorBarra.copy(alpha = 0.35f),
                                            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                        )
                                )
                            }
                        } else {
                            // Lugar vacío: queda marcado pero sin barra
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.62f)
                                    .height(2.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(1.dp))
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Número de puesto debajo de cada lugar
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                repeat(LUGARES) { lugar ->
                    Text(
                        "${lugar + 1}",
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = if (lugar == seleccionado) FontWeight.Bold else FontWeight.Normal,
                        color = if (lugar < masVendidos.size) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun FilaRanking(
    puesto: Int,
    producto: ProductoMasVendido,
    resaltada: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (resaltada) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(MaterialTheme.colorScheme.secondary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$puesto",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondary
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                producto.nombre,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text("${producto.cantidad} u.", fontWeight = FontWeight.Bold)
                Text(
                    formatearPrecio(producto.total),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewPantallaEstadisticas() {
    StockFlowTheme {
        PantallaEstadisticas(
            masVendidos = listOf(
                ProductoMasVendido("Yerba Playadito 1kg", 42, 168000.0),
                ProductoMasVendido("Leche entera 1L", 35, 52500.0),
                ProductoMasVendido("Galletitas Oreo", 21, 25200.0),
                ProductoMasVendido("Coca-Cola 2,25L", 18, 54000.0),
                ProductoMasVendido("Pan lactal", 9, 18000.0),
                ProductoMasVendido("Azúcar 1kg", 4, 4800.0)
            ),
            onVolver = {}
        )
    }
}
