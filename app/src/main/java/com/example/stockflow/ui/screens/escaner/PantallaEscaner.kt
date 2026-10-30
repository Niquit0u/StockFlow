package com.example.stockflow.ui.screens.escaner

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stockflow.ItemDescuento
import com.example.stockflow.Producto
import com.example.stockflow.formatearPrecio
import com.example.stockflow.ui.componentes.visibleAlEscribir

@ExperimentalMaterial3Api
@Composable
fun PantallaEscaner(
    // Devuelve el producto con ese código (con su stock total), o null si no está registrado
    buscarProducto: (String) -> Producto?,
    onBuscarEnInventario: (Producto) -> Unit,
    onDescontar: (List<ItemDescuento>) -> Unit
) {
    val context = LocalContext.current

    // Modo descuento: cada producto escaneado se suma a la lista hasta confirmar
    var modoDescuento by remember { mutableStateOf(false) }
    val itemsADescontar = remember { mutableStateListOf<ItemDescuento>() }

    // Diálogos
    var productoParaElegir by remember { mutableStateOf<Producto?>(null) }
    var productoParaCantidad by remember { mutableStateOf<Producto?>(null) }
    var confirmarDescuento by remember { mutableStateOf(false) }

    val hayDialogoAbierto = productoParaElegir != null || productoParaCantidad != null || confirmarDescuento

    fun cantidadEnLista(codigo: String) =
        itemsADescontar.filter { it.codigoBarra == codigo }.sumOf { it.cantidad }

    fun alEscanear(codigo: String) {
        val producto = buscarProducto(codigo)
        when {
            producto == null ->
                Toast.makeText(context, "Producto $codigo no registrado", Toast.LENGTH_SHORT).show()
            modoDescuento -> productoParaCantidad = producto
            else -> productoParaElegir = producto
        }
    }

    fun salirDelModoDescuento() {
        itemsADescontar.clear()
        modoDescuento = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (modoDescuento) "Stock Flow - Vender" else "Stock Flow - Escáner de Código",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()), // Permite scroll si la pantalla del cel es reducida
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp) // Espaciado fijo para evitar superposiciones
        ) {
            Text(
                text = if (modoDescuento) {
                    "Escaneá cada producto que quieras vender"
                } else {
                    "Apunta la cámara al código de barras del producto"
                },
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )

            VisorCamaraEscaner(
                onCodigoEscaneado = ::alEscanear,
                activo = !hayDialogoAbierto,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (modoDescuento) 260.dp else 350.dp)
                    .clip(RoundedCornerShape(16.dp)) // Recorta la transmisión de la cámara dentro del borde
                    .border(2.dp, MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(16.dp))
            )

            Button(
                onClick = { alEscanear("779" + (10000000..99999999).random()) },
                enabled = !hayDialogoAbierto,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Simular Captura (Prueba)", fontSize = 16.sp)
            }

            if (modoDescuento) {
                ListaADescontar(
                    items = itemsADescontar,
                    onQuitar = { itemsADescontar.remove(it) }
                )

                Button(
                    onClick = { confirmarDescuento = true },
                    enabled = itemsADescontar.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Terminar venta (${itemsADescontar.sumOf { it.cantidad }} u.)", fontSize = 16.sp)
                }

                OutlinedButton(
                    onClick = ::salirDelModoDescuento,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancelar venta")
                }
            }
        }
    }

    productoParaElegir?.let { producto ->
        AlertDialog(
            onDismissRequest = { productoParaElegir = null },
            title = { Text(producto.nombreProducto) },
            text = {
                Text("Código: ${producto.codigoBarra}\nStock actual: ${producto.totalCantidad} u.\n\n¿Qué querés hacer?")
            },
            confirmButton = {
                TextButton(onClick = {
                    productoParaElegir = null
                    modoDescuento = true
                    productoParaCantidad = producto
                }) { Text("Vender") }
            },
            dismissButton = {
                TextButton(onClick = {
                    productoParaElegir = null
                    onBuscarEnInventario(producto)
                }) { Text("Buscar") }
            }
        )
    }

    productoParaCantidad?.let { producto ->
        DialogoCantidad(
            producto = producto,
            disponible = producto.totalCantidad - cantidadEnLista(producto.codigoBarra),
            onConfirmar = { cantidad ->
                val indice = itemsADescontar.indexOfFirst { it.codigoBarra == producto.codigoBarra }
                if (indice >= 0) {
                    // Escaneado de nuevo: se acumula sobre el mismo renglón
                    val item = itemsADescontar[indice]
                    itemsADescontar[indice] = item.copy(cantidad = item.cantidad + cantidad)
                } else {
                    itemsADescontar.add(
                        ItemDescuento(producto.codigoBarra, producto.nombreProducto, cantidad, producto.precio)
                    )
                }
                productoParaCantidad = null
            },
            onCancelar = { productoParaCantidad = null }
        )
    }

    if (confirmarDescuento) {
        AlertDialog(
            onDismissRequest = { confirmarDescuento = false },
            title = { Text("Confirmar venta") },
            text = {
                Text(
                    "Se venderán ${itemsADescontar.sumOf { it.cantidad }} unidades de " +
                        "${itemsADescontar.size} producto(s).\n" +
                        "Valor total: ${formatearPrecio(itemsADescontar.sumOf { it.subtotal ?: 0.0 })}\n\n" +
                        "Esta acción no se puede deshacer."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDescontar(itemsADescontar.toList())
                    confirmarDescuento = false
                    salirDelModoDescuento()
                }) { Text("Vender") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarDescuento = false }) { Text("Volver") }
            }
        )
    }
}

@Composable
private fun ListaADescontar(
    items: List<ItemDescuento>,
    onQuitar: (ItemDescuento) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Productos a vender", fontWeight = FontWeight.Bold)

            if (items.isEmpty()) {
                Text(
                    "Todavía no escaneaste productos",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.nombreProducto, fontWeight = FontWeight.Medium)
                        Text(
                            item.codigoBarra,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("x${item.cantidad}", fontWeight = FontWeight.Bold)
                        Text(
                            item.subtotal?.let(::formatearPrecio) ?: "Sin precio",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onQuitar(item) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Quitar ${item.nombreProducto}")
                    }
                }
            }

            if (items.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Total", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                    Text(
                        formatearPrecio(items.sumOf { it.subtotal ?: 0.0 }),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                val sinPrecio = items.count { it.precioUnitario == null }
                if (sinPrecio > 0) {
                    Text(
                        "$sinPrecio producto(s) sin precio cargado no suman al total",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogoCantidad(
    producto: Producto,
    disponible: Int,
    onConfirmar: (Int) -> Unit,
    onCancelar: () -> Unit
) {
    var texto by remember { mutableStateOf(if (disponible > 0) "1" else "") }
    val cantidad = texto.toIntOrNull()
    val valida = cantidad != null && cantidad in 1..disponible

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(producto.nombreProducto) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Disponible para vender: $disponible u.")
                OutlinedTextField(
                    value = texto,
                    onValueChange = { nuevo -> texto = nuevo.filter { it.isDigit() } },
                    label = { Text("Cantidad a vender") },
                    isError = texto.isNotEmpty() && !valida,
                    supportingText = {
                        when {
                            disponible <= 0 -> Text("No queda stock de este producto")
                            texto.isNotEmpty() && !valida -> Text("Ingresá un número entre 1 y $disponible")
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.visibleAlEscribir()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { cantidad?.let(onConfirmar) }, enabled = valida) { Text("Agregar") }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        }
    )
}
