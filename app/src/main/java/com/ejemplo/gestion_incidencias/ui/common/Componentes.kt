package com.ejemplo.gestion_incidencias.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.isSystemInDarkTheme
import com.ejemplo.gestion_incidencias.domain.Estado
import com.ejemplo.gestion_incidencias.domain.Prioridad

/** Estado genérico de carga para las pantallas. */
sealed interface Carga<out T> {
    data object Cargando : Carga<Nothing>
    data class Exito<T>(val datos: T) : Carga<T>
    data class Error(val mensaje: String) : Carga<Nothing>
}

private data class ParColores(val fondo: Color, val texto: Color)

@Composable
private fun tono(base: Color): ParColores {
    val oscuro = isSystemInDarkTheme()
    return if (oscuro) ParColores(base.copy(alpha = 0.22f), base.copy(alpha = 1f).mezclar(Color.White, 0.45f))
    else ParColores(base.copy(alpha = 0.12f), base.mezclar(Color.Black, 0.25f))
}

private fun Color.mezclar(otro: Color, t: Float) = Color(
    red = red + (otro.red - red) * t,
    green = green + (otro.green - green) * t,
    blue = blue + (otro.blue - blue) * t,
    alpha = 1f,
)

fun Estado.color(): Color = when (this) {
    Estado.ABIERTA -> Color(0xFF2A78D6)
    Estado.ASIGNADA -> Color(0xFF6250D6)
    Estado.EN_PROGRESO -> Color(0xFFC98500)
    Estado.RESUELTA -> Color(0xFF0C8A0C)
    Estado.CERRADA -> Color(0xFF6B6A66)
    Estado.CANCELADA -> Color(0xFFD03B3B)
}

fun Prioridad.color(): Color = when (this) {
    Prioridad.BAJA -> Color(0xFF6B6A66)
    Prioridad.MEDIA -> Color(0xFF2A78D6)
    Prioridad.ALTA -> Color(0xFFD95926)
    Prioridad.CRITICA -> Color(0xFFD03B3B)
}

@Composable
private fun Etiqueta(texto: String, base: Color, modifier: Modifier = Modifier, conPunto: Boolean = true) {
    val c = tono(base)
    Surface(color = c.fondo, shape = RoundedCornerShape(50), modifier = modifier) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (conPunto) {
                Box(Modifier.size(7.dp).padding(0.dp)) {
                    Surface(color = c.texto, shape = CircleShape, modifier = Modifier.fillMaxSize()) {}
                }
                Spacer(Modifier.size(6.dp))
            }
            Text(texto, style = MaterialTheme.typography.labelMedium, color = c.texto, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun EstadoChip(estado: Estado, modifier: Modifier = Modifier) = Etiqueta(estado.etiqueta, estado.color(), modifier)

@Composable
fun PrioridadChip(prioridad: Prioridad, modifier: Modifier = Modifier) =
    Etiqueta(prioridad.etiqueta, prioridad.color(), modifier, conPunto = false)

@Composable
fun CargandoBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
fun MensajeVacio(
    titulo: String,
    detalle: String? = null,
    icono: ImageVector = Icons.Outlined.Inbox,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icono, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(12.dp))
        Text(titulo, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        if (detalle != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                detalle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun MensajeError(mensaje: String, onReintentar: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Outlined.CloudOff, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(12.dp))
        Text(mensaje, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onReintentar) { Text("Reintentar") }
    }
}

/** Fila "etiqueta: valor" para fichas de detalle. */
@Composable
fun DatoFila(etiqueta: String, valor: String, icono: ImageVector? = null) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
        // Se reserva el espacio del icono aunque no haya, para que las columnas queden alineadas
        if (icono != null) Icon(icono, null, Modifier.size(18.dp).padding(top = 1.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        else Spacer(Modifier.size(18.dp))
        Spacer(Modifier.size(10.dp))
        Text(
            etiqueta,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.42f),
        )
        Text(valor, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.58f))
    }
}
