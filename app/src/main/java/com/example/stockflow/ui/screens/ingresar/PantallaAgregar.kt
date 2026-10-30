package com.example.stockflow

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stockflow.ui.componentes.CampoCantidad
import com.example.stockflow.ui.componentes.CampoFecha
import com.example.stockflow.ui.componentes.esFechaValida
import com.example.stockflow.ui.screens.escaner.EscanerPantallaCompleta
import com.example.stockflow.ui.componentes.visibleAlEscribir

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAgregar(
    codigoInicial: String = "", // Recibe el código capturado por el escáner
    // Devuelve un lote existente con ese código (para autocompletar), o null si el producto es nuevo
    buscarProductoPorCodigo: (String) -> LoteItem? = { null },
    onAgregarLote: (LoteItem) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var codigoBarra by remember { mutableStateOf(codigoInicial) }
    var cantidad by remember { mutableStateOf("") }
    var fechaVencimiento by remember { mutableStateOf("") }
    var precio by remember { mutableStateOf("") }
    var errorPrecio by remember { mutableStateOf(false) }
    // Resumen del último lote ingresado ("10 u. de Leche, vence 12/03/2027"); null = todavía no hubo ingresos
    var ultimoIngreso by remember { mutableStateOf<String?>(null) }
    // Cambia en cada ingreso para volver a bajar hasta el mensaje aunque el texto se repita
    var cantidadIngresos by remember { mutableIntStateOf(0) }
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current

    // Después de confirmar: baja hasta el final para que se vea el mensaje de ingreso.
    // Se sigue el tamaño del contenido un momento porque cambia mientras se cierra el teclado.
    LaunchedEffect(cantidadIngresos) {
        if (cantidadIngresos == 0) return@LaunchedEffect
        withTimeoutOrNull(800) {
            snapshotFlow { scrollState.maxValue }.collect { scrollState.animateScrollTo(it) }
        }
    }
    var mostrarEscaner by remember { mutableStateOf(false) }

    var expandidoCategoria by remember { mutableStateOf(false) }
    var categoriaSeleccionada by remember { mutableStateOf(categoriasDisponibles[0]) }

    // Actualiza el campo de código de barras si cambia el valor transferido desde el escáner
    LaunchedEffect(codigoInicial) {
        if (codigoInicial.isNotBlank()) {
            codigoBarra = codigoInicial
        }
    }

    // Producto reconocido por el código ingresado (escaneado o escrito a mano)
    val productoIdentificado = remember(codigoBarra) {
        codigoBarra.trim().takeIf { it.isNotEmpty() }?.let(buscarProductoPorCodigo)
    }

    // Si el código corresponde a un producto ya cargado, autocompletamos sus datos
    LaunchedEffect(productoIdentificado) {
        productoIdentificado?.let {
            nombre = it.nombreProducto
            categoriaSeleccionada = it.categoria
            errorPrecio = false
        }
    }

    if (mostrarEscaner) {
        EscanerPantallaCompleta(
            onCodigoEscaneado = { codigo ->
                codigoBarra = codigo
                mostrarEscaner = false
            },
            onCerrar = { mostrarEscaner = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stock Flow - Registrar Ingreso FEFO", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                // Reserva el lugar del teclado: así no tapa los campos de abajo
                .imePadding()
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Ingreso de Mercadería",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )

            // El texto de ayuda solo se agrega cuando hay algo que decir: si se pasa vacío,
            // el campo igual reserva su lugar y queda más separado que los demás
            val ayudaCodigo = when {
                productoIdentificado != null -> "✅ Producto identificado: ${productoIdentificado.nombreProducto}"
                codigoBarra.isNotBlank() -> "Producto no identificado: ingresá sus datos y precio"
                else -> null
            }
            OutlinedTextField(
                value = codigoBarra,
                onValueChange = { codigoBarra = it },
                label = { Text("Código de Barras (EAN/QR)") },
                // Botón para escanear con la cámara, dentro del mismo campo
                trailingIcon = {
                    IconButton(onClick = { mostrarEscaner = true }) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Escanear código de barras",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                singleLine = true,
                supportingText = ayudaCodigo?.let { texto -> { Text(texto) } },
                modifier = Modifier.fillMaxWidth().visibleAlEscribir()
            )

            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre del Producto") },
                modifier = Modifier.fillMaxWidth().visibleAlEscribir()
            )

            ExposedDropdownMenuBox(
                expanded = expandidoCategoria,
                onExpandedChange = { expandidoCategoria = !expandidoCategoria }
            ) {
                OutlinedTextField(
                    value = categoriaSeleccionada,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Sección / Rubro") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandidoCategoria) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth().visibleAlEscribir()
                )

                ExposedDropdownMenu(
                    expanded = expandidoCategoria,
                    onDismissRequest = { expandidoCategoria = false }
                ) {
                    categoriasDisponibles.forEach { seleccion ->
                        DropdownMenuItem(
                            text = { Text(seleccion) },
                            onClick = {
                                categoriaSeleccionada = seleccion
                                expandidoCategoria = false
                            }
                        )
                    }
                }
            }

            // El precio solo se pide cuando el código no corresponde a un producto ya cargado
            if (productoIdentificado == null) {
                OutlinedTextField(
                    value = precio,
                    onValueChange = {
                        precio = it
                        errorPrecio = false
                    },
                    label = { Text("Precio de Venta ($)") },
                    isError = errorPrecio,
                    supportingText = if (errorPrecio) {
                        { Text("Ingresá un precio válido mayor a 0") }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().visibleAlEscribir()
                )
            }

            CampoCantidad(
                valor = cantidad,
                onValorChange = { cantidad = it },
                etiqueta = "Cantidad Recibida (Unidades)",
                modifier = Modifier.fillMaxWidth()
            )

            // Marca error solo cuando la fecha está completa (10 caracteres) y no existe, ej. 31/02/2027
            val fechaInvalida = fechaVencimiento.length == 10 && !esFechaValida(fechaVencimiento)
            CampoFecha(
                fecha = fechaVencimiento,
                onFechaChange = { fechaVencimiento = it },
                esError = fechaInvalida,
                textoAyuda = if (fechaInvalida) {
                    { Text("Esa fecha no existe") }
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    // Acepta coma o punto como separador decimal
                    val precioIngresado = precio.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 }
                    errorPrecio = productoIdentificado == null && precioIngresado == null

                    val cantInt = cantidad.toIntOrNull()?.takeIf { it > 0 }
                    if (nombre.isNotBlank() && cantInt != null && esFechaValida(fechaVencimiento) && !errorPrecio) {
                        val nuevoLote = LoteItem(
                            idLote = "L" + (100..999).random(),
                            nombreProducto = nombre,
                            categoria = categoriaSeleccionada,
                            codigoBarra = if (codigoBarra.isBlank()) "779000000000" else codigoBarra,
                            cantidadActual = cantInt,
                            fechaVencimiento = fechaVencimiento,
                            diasParaVencer = (1..30).random(),
                            estado = if ((1..10).random() > 5) EstadoSemaforo.OPTIMO else EstadoSemaforo.ADVERTENCIA,
                            precio = productoIdentificado?.precio ?: precioIngresado
                        )
                        onAgregarLote(nuevoLote)
                        ultimoIngreso = "$cantInt u. de $nombre ($categoriaSeleccionada), vence $fechaVencimiento"
                        cantidadIngresos++
                        // Cierra el teclado para que el mensaje quede a la vista
                        focusManager.clearFocus()

                        // Limpiamos los campos
                        nombre = ""
                        codigoBarra = ""
                        precio = ""
                        cantidad = ""
                        fechaVencimiento = ""
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Confirmar Ingreso de Lote", fontSize = 16.sp)
            }

            ultimoIngreso?.let { resumen ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(
                                text = "Stock ingresado",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = resumen,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}