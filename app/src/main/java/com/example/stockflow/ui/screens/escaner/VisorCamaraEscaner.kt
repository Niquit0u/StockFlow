package com.example.stockflow.ui.screens.escaner

import android.Manifest
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import java.util.concurrent.Executors

/**
 * Evita lecturas repetidas: la cámara detecta el mismo código en muchos cuadros seguidos.
 * Las lecturas llegan por el hilo principal (MlKitAnalyzer entrega en el executor principal), así que no hace falta sincronizar.
 */
private class ControlLecturas {
    private var ultimoCodigo: String? = null
    private var ultimaLectura = 0L
    var reactivadoEn = 0L

    fun acepta(codigo: String, ahora: Long): Boolean {
        // Pausa corta después de cualquier lectura aceptada
        if (ahora - ultimaLectura < ESPERA_ENTRE_LECTURAS_MS) return false
        // El mismo código sigue frente a la cámara: se ignora un rato tras leerlo o tras reanudar
        if (codigo == ultimoCodigo && ahora - maxOf(ultimaLectura, reactivadoEn) < ESPERA_MISMO_CODIGO_MS) return false
        ultimoCodigo = codigo
        ultimaLectura = ahora
        return true
    }

    companion object {
        const val ESPERA_ENTRE_LECTURAS_MS = 1_500L
        const val ESPERA_MISMO_CODIGO_MS = 3_000L
    }
}

/**
 * Vista previa de la cámara trasera que detecta códigos de barra con ML Kit.
 * Pide el permiso de cámara si no está concedido. Mientras [activo] sea false ignora lo que detecte
 * (por ejemplo, mientras hay un diálogo abierto).
 *
 * @param areaDeLectura si se indica (en píxeles, relativo a este visor), solo se acepta el código cuyo
 * centro cae dentro de esa zona; así no se lee por error otro código que aparezca en cámara.
 */
@Composable
fun VisorCamaraEscaner(
    onCodigoEscaneado: (String) -> Unit,
    modifier: Modifier = Modifier,
    activo: Boolean = true,
    areaDeLectura: Rect? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val callbackActual by rememberUpdatedState(onCodigoEscaneado)
    val activoActual by rememberUpdatedState(activo)
    val areaActual by rememberUpdatedState(areaDeLectura)
    val control = remember { ControlLecturas() }

    LaunchedEffect(activo) {
        if (activo) control.reactivadoEn = SystemClock.elapsedRealtime()
    }

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

    Box(modifier = modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        if (tienePermiso) {
            val controlador = remember {
                LifecycleCameraController(context).apply {
                    cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                }
            }

            DisposableEffect(lifecycleOwner) {
                val analisisExecutor = Executors.newSingleThreadExecutor()
                val lectorCodigos = BarcodeScanning.getClient()

                // COORDINATE_SYSTEM_VIEW_REFERENCED: cada código llega con su posición en píxeles de la vista
                // (ya convertida desde la imagen de la cámara), comparable directamente con areaDeLectura
                controlador.setImageAnalysisAnalyzer(
                    analisisExecutor,
                    MlKitAnalyzer(
                        listOf(lectorCodigos),
                        CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED,
                        ContextCompat.getMainExecutor(context)
                    ) { resultado ->
                        if (!activoActual) return@MlKitAnalyzer
                        val codigos = resultado.getValue(lectorCodigos) ?: return@MlKitAnalyzer
                        val valor = elegirCodigo(codigos, areaActual) ?: return@MlKitAnalyzer
                        if (control.acepta(valor, SystemClock.elapsedRealtime())) {
                            callbackActual(valor)
                        }
                    }
                )
                controlador.bindToLifecycle(lifecycleOwner)

                // Libera la cámara al salir de la pantalla (por ejemplo, al cerrar el escáner a pantalla completa)
                onDispose {
                    controlador.clearImageAnalysisAnalyzer()
                    controlador.unbind()
                    lectorCodigos.close()
                    analisisExecutor.shutdown()
                }
            }

            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        controller = controlador
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = "⚠️ Se requiere permiso de cámara para escanear",
                color = Color.Red,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

/**
 * Sin [area]: el primer código con valor. Con [area]: solo los que tienen el centro adentro,
 * y si hay varios, el más cercano al centro del área.
 */
private fun elegirCodigo(codigos: List<Barcode>, area: Rect?): String? {
    if (area == null) return codigos.firstNotNullOfOrNull { it.rawValue }
    return codigos
        .mapNotNull { codigo ->
            val caja = codigo.boundingBox ?: return@mapNotNull null
            val valor = codigo.rawValue ?: return@mapNotNull null
            val centro = Offset(caja.exactCenterX(), caja.exactCenterY())
            if (area.contains(centro)) valor to (centro - area.center).getDistance() else null
        }
        .minByOrNull { it.second }
        ?.first
}

/** Escáner a pantalla completa. Se cierra solo al leer un código o con el botón / gesto atrás. */
@Composable
fun EscanerPantallaCompleta(
    onCodigoEscaneado: (String) -> Unit,
    onCerrar: () -> Unit
) {
    // Posición del rectángulo guía dentro de la pantalla; solo se leen códigos que caen ahí
    var areaGuia by remember { mutableStateOf<Rect?>(null) }
    val radioEsquinas = with(LocalDensity.current) { 16.dp.toPx() }

    Dialog(
        onDismissRequest = onCerrar,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // El visor y el rectángulo comparten el mismo contenedor, así sus coordenadas coinciden
            VisorCamaraEscaner(
                onCodigoEscaneado = onCodigoEscaneado,
                areaDeLectura = areaGuia,
                modifier = Modifier.fillMaxSize()
            )

            // Oscurece todo menos el rectángulo, para que se vea dónde hay que apuntar
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            ) {
                drawRect(Color.Black.copy(alpha = 0.55f))
                areaGuia?.let { area ->
                    drawRoundRect(
                        color = Color.Transparent,
                        topLeft = area.topLeft,
                        size = area.size,
                        cornerRadius = CornerRadius(radioEsquinas),
                        blendMode = BlendMode.Clear
                    )
                }
            }

            // Marco guía: solo se acepta el código que está adentro
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.8f)
                    .height(180.dp)
                    .onGloballyPositioned { areaGuia = it.boundsInParent() }
                    .border(3.dp, Color.White, RoundedCornerShape(16.dp))
            )

            Text(
                text = "Ubicá el código de barras dentro del rectángulo",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, bottom = 48.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            IconButton(
                onClick = onCerrar,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
            ) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar escáner", tint = Color.White)
            }
        }
    }
}
