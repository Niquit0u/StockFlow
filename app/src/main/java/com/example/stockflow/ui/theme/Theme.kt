package com.example.stockflow.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = MarinoOscuro,
    onPrimary = SobreMarinoOscuro,
    primaryContainer = MarinoSuaveOscuro,
    onPrimaryContainer = SobreMarinoSuaveOscuro,
    secondary = FlujoOscuro,
    onSecondary = SobreFlujoOscuro,
    secondaryContainer = FlujoSuaveOscuro,
    onSecondaryContainer = SobreFlujoSuaveOscuro,
    tertiary = PizarraOscuro,
    onTertiary = SobrePizarraOscuro,
    tertiaryContainer = PizarraSuaveOscuro,
    onTertiaryContainer = SobrePizarraSuaveOscuro,
    background = FondoOscuro,
    onBackground = TintaOscura,
    surface = SuperficieOscura,
    onSurface = TintaOscura,
    surfaceVariant = SuperficieVarianteOscura,
    onSurfaceVariant = TintaSuaveOscura,
    outline = ContornoOscuro,
    outlineVariant = ContornoSuaveOscuro,
    surfaceContainerLowest = SuperficieContenedorMasBajaOscura,
    surfaceContainerLow = SuperficieContenedorBajaOscura,
    surfaceContainer = SuperficieContenedorOscura,
    surfaceContainerHigh = SuperficieContenedorAltaOscura,
    surfaceContainerHighest = SuperficieContenedorMasAltaOscura
)

private val LightColorScheme = lightColorScheme(
    primary = Marino,
    onPrimary = SobreMarino,
    primaryContainer = MarinoSuave,
    onPrimaryContainer = SobreMarinoSuave,
    secondary = Flujo,
    onSecondary = SobreFlujo,
    secondaryContainer = FlujoSuave,
    onSecondaryContainer = SobreFlujoSuave,
    tertiary = Pizarra,
    onTertiary = SobrePizarra,
    tertiaryContainer = PizarraSuave,
    onTertiaryContainer = SobrePizarraSuave,
    background = Fondo,
    onBackground = Tinta,
    surface = Superficie,
    onSurface = Tinta,
    surfaceVariant = SuperficieVariante,
    onSurfaceVariant = TintaSuave,
    outline = Contorno,
    outlineVariant = ContornoSuave,
    surfaceContainerLowest = SuperficieContenedorMasBaja,
    surfaceContainerLow = SuperficieContenedorBaja,
    surfaceContainer = SuperficieContenedor,
    surfaceContainerHigh = SuperficieContenedorAlta,
    surfaceContainerHighest = SuperficieContenedorMasAlta
)

@Composable
fun StockFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Color dinámico (Android 12+) toma los colores del fondo de pantalla del teléfono y taparía
    // los de la marca; queda apagado para que la app se vea igual en todos los teléfonos
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}