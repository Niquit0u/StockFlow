package com.example.stockflow

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.stockflow.datosSimuladosLotes
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AplicacionInteractivaStockFlow()
            }
        }
    }
}

@Composable
fun AplicacionInteractivaStockFlow() {
    val context = LocalContext.current
    val listaLotes = remember { mutableStateListOf(*datosSimuladosLotes.toTypedArray()) }
    var pestañaSeleccionada by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = pestañaSeleccionada == 0,
                    onClick = { pestañaSeleccionada = 0 },
                    icon = { Icon(Icons.Default.Info, contentDescription = "Alertas") },
                    label = { Text("Productos") }
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
                    Toast.makeText(context, "Código detectado: $codigo", Toast.LENGTH_SHORT).show()
                })
                2 -> PantallaAgregar(onAgregarLote = { nuevoLote ->
                    listaLotes.add(0, nuevoLote)
                })
            }
        }
    }
}