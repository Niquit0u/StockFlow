package com.example.stockflow

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAgregar(onAgregarLote: (LoteItem) -> Unit) {
    var nombre by remember { mutableStateOf("") }
    var codigoBarra by remember { mutableStateOf("") }
    var cantidad by remember { mutableStateOf("") }
    var fechaVencimiento by remember { mutableStateOf("") }
    var mensajeExito by remember { mutableStateOf(false) }

    // Estados para controlar el menú desplegable de categorías
    var expandidoCategoria by remember { mutableStateOf(false) }
    // Por defecto selecciona la primera opción ("Lácteos")
    var categoriaSeleccionada by remember { mutableStateOf(categoriasDisponibles[0]) }

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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Ingreso de Mercadería",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )

            Button(
                onClick = {
                    codigoBarra = "779" + (10000000..99999999).random().toString()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("📷 Escanear Código de Barras")
            }

            OutlinedTextField(
                value = codigoBarra,
                onValueChange = { codigoBarra = it },
                label = { Text("Código de Barras") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre del Producto") },
                modifier = Modifier.fillMaxWidth()
            )

            // MENÚ DESPLEGABLE DE SECCIONES (RUBROS)
            ExposedDropdownMenuBox(
                expanded = expandidoCategoria,
                onExpandedChange = { expandidoCategoria = !expandidoCategoria }
            ) {
                OutlinedTextField(
                    value = categoriaSeleccionada,
                    onValueChange = {},
                    readOnly = true, // Evita que el teclado se abra, comportándose como un botón
                    label = { Text("Sección / Rubro") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandidoCategoria) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
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

            OutlinedTextField(
                value = cantidad,
                onValueChange = { cantidad = it },
                label = { Text("Cantidad Recibida (Unidades)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = fechaVencimiento,
                onValueChange = { fechaVencimiento = it },
                label = { Text("Fecha Vencimiento (DD/MM/AAAA)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (nombre.isNotBlank() && cantidad.isNotBlank() && fechaVencimiento.isNotBlank()) {
                        val cantInt = cantidad.toIntOrNull() ?: 1
                        val nuevoLote = LoteItem(
                            idLote = "L" + (100..999).random(),
                            nombreProducto = nombre,
                            categoria = categoriaSeleccionada, // Guarda la categoría elegida (ej. "Otros")
                            codigoBarra = if (codigoBarra.isBlank()) "779000000000" else codigoBarra,
                            cantidadActual = cantInt,
                            fechaVencimiento = fechaVencimiento,
                            diasParaVencer = (1..30).random(),
                            estado = if ((1..10).random() > 5) EstadoSemaforo.OPTIMO else EstadoSemaforo.ADVERTENCIA
                        )
                        onAgregarLote(nuevoLote)
                        mensajeExito = true

                        // Limpiar formulario tras guardar
                        nombre = ""
                        codigoBarra = ""
                        cantidad = ""
                        fechaVencimiento = ""
                        categoriaSeleccionada = categoriasDisponibles[0] // Reinicia a la primera opción
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Confirmar Ingreso de Lote", fontSize = 16.sp)
            }

            if (mensajeExito) {
                Text(
                    text = "✅ ¡Lote registrado con éxito en el sistema!",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}