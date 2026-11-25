package com.ejemplo.gestion_incidencias.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Azul40,
    onPrimary = Color.White,
    primaryContainer = Azul90,
    onPrimaryContainer = Azul10,
    secondary = Pizarra40,
    secondaryContainer = Pizarra90,
    tertiary = Turquesa40,
    tertiaryContainer = Turquesa90,
    background = Color(0xFFF9F9FC),
    surface = Color(0xFFF9F9FC),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF3F3F7),
    surfaceContainer = Color(0xFFEDEDF1),
)

private val DarkColorScheme = darkColorScheme(
    primary = Azul80,
    onPrimary = Azul20,
    primaryContainer = Azul30,
    onPrimaryContainer = Azul90,
    secondary = Pizarra80,
    tertiary = Turquesa80,
)

/** Colores de la capa de datos (gráficos), seleccionados por modo y no invertidos automáticamente. */
data class ChartColors(
    val serie1: Color,
    val serie2: Color,
    val rampaOrdinal: List<Color>,
    val grid: Color,
    val eje: Color,
    val textoSecundario: Color,
    val textoTenue: Color,
)

private val ChartLight = ChartColors(
    serie1 = Color(0xFF2A78D6),
    serie2 = Color(0xFFEB6834),
    rampaOrdinal = listOf(Color(0xFF86B6EF), Color(0xFF5598E7), Color(0xFF2A78D6), Color(0xFF184F95)),
    grid = Color(0xFFE1E0D9),
    eje = Color(0xFFC3C2B7),
    textoSecundario = Color(0xFF52514E),
    textoTenue = Color(0xFF898781),
)

private val ChartDark = ChartColors(
    serie1 = Color(0xFF3987E5),
    serie2 = Color(0xFFD95926),
    rampaOrdinal = listOf(Color(0xFF184F95), Color(0xFF256ABF), Color(0xFF3987E5), Color(0xFF86B6EF)),
    grid = Color(0xFF2C2C2A),
    eje = Color(0xFF383835),
    textoSecundario = Color(0xFFC3C2B7),
    textoTenue = Color(0xFF898781),
)

val LocalChartColors = staticCompositionLocalOf { ChartLight }

@Composable
fun Gestion_IncidenciasTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalChartColors provides if (darkTheme) ChartDark else ChartLight) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = Typography,
            content = content,
        )
    }
}
