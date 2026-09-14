package com.example.stockflow

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaEscaner(onCodigoEscaneado: (String) -> Unit) {
    val context = LocalContext.current
    var tienePermiso by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcherPermiso = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { esConcedido ->
        tienePermiso = esConcedido
    }

    LaunchedEffect(Unit) {
        if (!tienePermiso) {
            launcherPermiso.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stock Flow - Escáner de Código", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Apunta la cámara al código de barras del producto",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )

            // Cuadro visual del visor de la cámara
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(Color.Black, shape = RoundedCornerShape(16.dp))
                    .border(2.dp, MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (tienePermiso) {
                    Text(
                        text = "📷 Visor de Cámara Activo (CameraX + ML Kit)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "⚠️ Permiso de cámara no concedido",
                        color = Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Botón de prueba para simular la captura de un código EAN-13 real
            Button(
                onClick = {
                    val codigoSimulado = "779" + (10000000..99999999).random()
                    onCodigoEscaneado(codigoSimulado)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Simular Captura de Código EAN-13", fontSize = 16.sp)
            }
        }
    }
}