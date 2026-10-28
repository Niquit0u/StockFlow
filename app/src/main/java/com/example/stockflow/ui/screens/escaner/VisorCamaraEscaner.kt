package com.example.stockflow.ui.screens.escaner

import android.Manifest
import android.content.pm.PackageManager
import android.os.SystemClock
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

/**
 * Evita lecturas repetidas: la cámara detecta el mismo código en muchos cuadros seguidos.
 * Las lecturas llegan por el hilo principal (listeners de ML Kit), así que no hace falta sincronizar.
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
 */
@OptIn(ExperimentalGetImage::class)
@Composable
fun VisorCamaraEscaner(
    onCodigoEscaneado: (String) -> Unit,
    modifier: Modifier = Modifier,
    activo: Boolean = true
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val callbackActual by rememberUpdatedState(onCodigoEscaneado)
    val activoActual by rememberUpdatedState(activo)
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
            val analisisExecutor = remember { Executors.newSingleThreadExecutor() }
            val barcodeScanner = remember { BarcodeScanning.getClient() }
            var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

            // Libera la cámara al salir de la pantalla (por ejemplo, al cerrar el escáner a pantalla completa)
            DisposableEffect(Unit) {
                onDispose {
                    cameraProvider?.unbindAll()
                    barcodeScanner.close()
                    analisisExecutor.shutdown()
                }
            }

            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        val provider = cameraProviderFuture.get()
                        cameraProvider = provider

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setTargetResolution(Size(1280, 720))
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        imageAnalysis.setAnalyzer(analisisExecutor) { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage != null && activoActual) {
                                val image = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )
                                barcodeScanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        val rawValue = barcodes.firstNotNullOfOrNull { it.rawValue }
                                        if (rawValue != null && activoActual &&
                                            control.acepta(rawValue, SystemClock.elapsedRealtime())
                                        ) {
                                            callbackActual(rawValue)
                                        }
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            } else {
                                imageProxy.close()
                            }
                        }

                        try {
                            provider.unbindAll()
                            provider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageAnalysis
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
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

/** Escáner a pantalla completa. Se cierra solo al leer un código o con el botón / gesto atrás. */
@Composable
fun EscanerPantallaCompleta(
    onCodigoEscaneado: (String) -> Unit,
    onCerrar: () -> Unit
) {
    Dialog(
        onDismissRequest = onCerrar,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            VisorCamaraEscaner(
                onCodigoEscaneado = onCodigoEscaneado,
                modifier = Modifier.fillMaxSize()
            )

            // Marco guía para centrar el código
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.8f)
                    .height(180.dp)
                    .border(3.dp, Color.White, RoundedCornerShape(16.dp))
            )

            Text(
                text = "Apunta la cámara al código de barras",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 48.dp)
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
