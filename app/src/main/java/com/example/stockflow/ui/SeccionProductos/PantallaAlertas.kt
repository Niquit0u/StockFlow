package com.example.stockflow

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAlertas(lotes: List<LoteItem>) {
    // Si es null, mostramos la lista de categorías. Si tiene texto, mostramos los productos.
    var categoriaSeleccionada by remember { mutableStateOf<String?>(null) }

    // Filtramos los productos según la categoría seleccionada (si hay alguna)
    val productosAgrupados = remember(lotes, categoriaSeleccionada) {
        if (categoriaSeleccionada != null) {
            obtenerProductosAgrupados(lotes, categoriaSeleccionada!!)
        } else {
            emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (categoriaSeleccionada == null) "Stock Flow - Inventario" else "Rubro: $categoriaSeleccionada",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    // Flecha de retroceso dinámica: Solo aparece si entramos a una categoría
                    if (categoriaSeleccionada != null) {
                        IconButton(onClick = { categoriaSeleccionada = null }) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Volver")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (categoriaSeleccionada == null) {
                // PANTALLA 1: MENÚ DE CATEGORÍAS
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Usamos la lista global que ahora incluye "Otros"
                    items(categoriasDisponibles) { categoria ->
                        TarjetaCategoria(
                            nombreCategoria = categoria,
                            onClick = { categoriaSeleccionada = categoria }
                        )
                    }
                }
            } else {
                // PANTALLA 2: LISTA DE PRODUCTOS DE LA CATEGORÍA SELECCIONADA
                if (productosAgrupados.isEmpty()) {
                    Text(
                        text = "No hay productos registrados en $categoriaSeleccionada.",
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(productosAgrupados) { producto ->
                            TarjetaProductoExpandible(producto = producto)
                        }
                    }
                }
            }
        }
    }
}

// Interfaz para el botón gigante de cada categoría
@Composable
fun TarjetaCategoria(nombreCategoria: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.List,
                contentDescription = "Icono Categoría",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = nombreCategoria,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TarjetaProductoExpandible(producto: Producto) {
    var expandido by remember { mutableStateOf(false) }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expandido = !expandido }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = producto.nombreProducto,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = "EAN: ${producto.codigoBarra} • ${producto.lotes.size} lote(s)",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${producto.totalCantidad} u. total",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = if (expandido) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expandir"
                )
            }

            AnimatedVisibility(visible = expandido) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "Detalle de Lotes (Orden FEFO):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    producto.lotes.forEach { lote ->
                        FilaDetalleLote(lote = lote)
                    }
                }
            }
        }
    }
}

@Composable
fun FilaDetalleLote(lote: LoteItem) {
    val colorEstado = when (lote.estado) {
        EstadoSemaforo.CRITICO -> Color(0xFFE53935)
        EstadoSemaforo.ADVERTENCIA -> Color(0xFFFB8C00)
        EstadoSemaforo.OPTIMO -> Color(0xFF43A047)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5), shape = RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color = colorEstado, shape = RoundedCornerShape(6.dp))
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Lote: ${lote.idLote}", fontWeight = FontWeight.Medium, fontSize = 13.sp)
            Text(text = "Vence: ${lote.fechaVencimiento}", fontSize = 12.sp, color = Color.DarkGray)
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(text = "${lote.cantidadActual} u.", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(text = "${lote.diasParaVencer} días", fontSize = 11.sp, color = colorEstado)
        }
    }
}