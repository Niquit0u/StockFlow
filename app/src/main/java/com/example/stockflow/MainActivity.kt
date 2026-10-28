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
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val inventario = InventarioRepository(StockFlowDatabase.getDatabase(this))
        setContent {
            MaterialTheme {
                AplicacionInteractivaStockFlow(inventario)
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

    // Estado de la pestaña activa (0: Vencimientos, 1: Escáner, 2: Agregar)
    var pestañaSeleccionada by remember { mutableIntStateOf(0) }

    // Variable para almacenar el código escaneado y transferirlo a la pantalla de Agregar
    var codigoEscaneadoGlobal by remember { mutableStateOf("") }

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
                    onClick = { pestañaSeleccionada = 0 },
                    icon = { Icon(Icons.Default.Info, contentDescription = "Alertas") },
                    label = { Text("Vencimientos") }
                )
                NavigationBarItem(
                    selected = pestañaSeleccionada == 1,
                    onClick = { pestañaSeleccionada = 1 },
                    icon = { Icon(Icons.Default.Search, contentDescription = "Escáner") },
                    label = { Text("Escáner") }
                )
                NavigationBarItem(
                    selected = pestañaSeleccionada == 2,
                    onClick = { pestañaSeleccionada = 2 },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Agregar") },
                    label = { Text("Ingresar Stock") }
                )
            }
        }
    ) { padding ->
        Surface(modifier = Modifier.padding(padding)) {
            when (pestañaSeleccionada) {
                0 -> PantallaAlertas(lotes = listaLotes)

                1 -> PantallaEscaner(onCodigoEscaneado = { codigo ->
                    // 1. Guardamos el código capturado por la cámara
                    codigoEscaneadoGlobal = codigo

                    // 2. Notificamos al usuario
                    Toast.makeText(context, "¡Código $codigo capturado!", Toast.LENGTH_SHORT).show()

                    // 3. Redirigimos automáticamente a la pestaña 2 ("Ingresar Stock")
                    pestañaSeleccionada = 2
                })

                2 -> PantallaAgregar(
                    codigoInicial = codigoEscaneadoGlobal,
                    onAgregarLote = { nuevoLote ->
                        scope.launch {
                            try {
                                inventario.registrarLote(nuevoLote)
                            } catch (e: IllegalArgumentException) {
                                Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                            }
                        }
                        // Limpiamos el código global tras guardar el lote
                        codigoEscaneadoGlobal = ""
                    }
                )
            }
        }
    }
}