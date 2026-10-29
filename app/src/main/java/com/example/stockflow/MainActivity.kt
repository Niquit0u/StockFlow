@file:kotlin.OptIn(ExperimentalMaterial3Api::class)

package com.example.stockflow

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.stockflow.data.local.StockFlowDatabase
import com.example.stockflow.data.repository.InventarioRepository
import com.example.stockflow.ui.screens.escaner.PantallaEscaner
import kotlinx.coroutines.launch
import androidx.camera.core.ExperimentalGetImage
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.dp
import com.example.stockflow.ui.screens.login.PantallaLogin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val inventario = InventarioRepository(StockFlowDatabase.getDatabase(this))
        setContent {
            MaterialTheme {
                // Estado que controla si el usuario ha iniciado sesión
                var isLoggedIn by rememberSaveable { mutableStateOf(false) }

                if (isLoggedIn) {
                    // Muestra el sistema principal de StockFlow
                    AplicacionInteractivaStockFlow(inventario)
                } else {
                    // Muestra la pantalla de Login al iniciar la app
                    PantallaLogin(
                        onIniciarSesion = { usuario, contrasena ->
                            // TODO: validar contra la tabla de usuarios cuando se conecte la base de datos.
                            // Por ahora cualquier usuario y contraseña no vacíos ingresan.
                            isLoggedIn = true
                            true
                        }
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalGetImage::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class)
@Composable
fun AplicacionInteractivaStockFlow(inventario: InventarioRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listaLotes by remember(inventario) { inventario.observarLotes() }.collectAsState(initial = emptyList())

    LaunchedEffect(inventario) { inventario.sembrarDatosDePruebaSiVacio() }

    // Estado de la pestaña activa (0: Inventario, 1: Escáner, 2: Agregar)
    var pestañaSeleccionada by remember { mutableIntStateOf(0) }

    // Texto que el escáner manda a buscar en la pantalla de Inventario (opción "Buscar")
    var busquedaInventario by remember { mutableStateOf("") }

    // Agrupa los lotes con ese código en un Producto con su stock total, o null si no existe
    fun buscarProductoPorCodigo(codigo: String): Producto? {
        val lotes = listaLotes.filter { it.codigoBarra == codigo }
        val primero = lotes.firstOrNull() ?: return null
        return Producto(
            nombreProducto = primero.nombreProducto,
            categoria = primero.categoria,
            codigoBarra = codigo,
            totalCantidad = lotes.sumOf { it.cantidadActual },
            lotes = lotes,
            precio = primero.precio
        )
    }

    fun cambiarPestaña(pestaña: Int) {
        busquedaInventario = ""
        pestañaSeleccionada = pestaña
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar (
                // Usa las Insets nativas de las barras de navegación:
                // Si hay botones (Atrás, Inicio, Recientes), asigna su espacio exacto automáticamente.
                // Si el usuario usa navegación por gestos, la barra se acomoda al borde inferior aprovechando la pantalla.
                windowInsets = WindowInsets(0)
            ) {
                NavigationBarItem(
                    selected = pestañaSeleccionada == 0,
                    onClick = { cambiarPestaña(0) },
                    icon = { Icon(Icons.Default.Info, contentDescription = "Inventario") },
                    label = { Text("Inventario") }
                )
                NavigationBarItem(
                    selected = pestañaSeleccionada == 1,
                    onClick = { cambiarPestaña(1) },
                    icon = { Icon(Icons.Default.Search, contentDescription = "Escáner") },
                    label = { Text("Escáner") }
                )
                NavigationBarItem(
                    selected = pestañaSeleccionada == 2,
                    onClick = { cambiarPestaña(2) },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Agregar") },
                    label = { Text("Ingresar Stock") }
                )
            }
        }
    ) { padding ->
        Surface(modifier = Modifier.padding(padding)) {
            when (pestañaSeleccionada) {
                0 -> PantallaAlertas(
                    lotes = listaLotes,
                    busquedaInicial = busquedaInventario,
                    onEditarLote = { idLote, nombre, cantidad, precio, fecha ->
                        scope.launch {
                            try {
                                inventario.editarLote(idLote, nombre, cantidad, precio, fecha)
                                Toast.makeText(context, "Lote actualizado", Toast.LENGTH_SHORT).show()
                            } catch (e: IllegalArgumentException) {
                                Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )

                1 -> PantallaEscaner(
                    buscarProducto = ::buscarProductoPorCodigo,
                    onBuscarEnInventario = { producto ->
                        // Abre Inventario con la búsqueda del producto ya cargada
                        busquedaInventario = producto.nombreProducto
                        pestañaSeleccionada = 0
                    },
                    onDescontar = { items ->
                        scope.launch {
                            try {
                                inventario.descontarStock(items)
                                Toast.makeText(
                                    context,
                                    "Stock descontado: ${items.sumOf { it.cantidad }} unidades",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } catch (e: IllegalStateException) {
                                Toast.makeText(context, "No se descontó nada: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )

                2 -> PantallaAgregar(
                    buscarProductoPorCodigo = { codigo ->
                        listaLotes.firstOrNull { it.codigoBarra == codigo }
                    },
                    onAgregarLote = { nuevoLote ->
                        scope.launch {
                            try {
                                inventario.registrarLote(nuevoLote)
                            } catch (e: IllegalArgumentException) {
                                Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )
            }
        }
    }
}