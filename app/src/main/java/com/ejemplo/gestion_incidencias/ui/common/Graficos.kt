package com.ejemplo.gestion_incidencias.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ejemplo.gestion_incidencias.data.remote.PuntoTendenciaDto
import com.ejemplo.gestion_incidencias.ui.theme.LocalChartColors
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

data class Barra(val etiqueta: String, val valor: Double, val textoValor: String, val color: Color? = null)

/**
 * Barras horizontales finas con etiqueta y valor directo (sin leyenda: una sola serie,
 * el título de la tarjeta la nombra). Extremo redondeado de 4dp anclado a la base.
 */
@Composable
fun BarrasHorizontales(barras: List<Barra>, modifier: Modifier = Modifier) {
    val colores = LocalChartColors.current
    val maximo = barras.maxOfOrNull { it.valor }?.takeIf { it > 0 } ?: 1.0
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        barras.forEach { b ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.semantics { contentDescription = "${b.etiqueta}: ${b.textoValor}" },
            ) {
                Text(
                    b.etiqueta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(112.dp),
                )
                Box(Modifier.weight(1f).height(14.dp)) {
                    val color = b.color ?: colores.serie1
                    Canvas(Modifier.matchParentSize()) {
                        drawLine(colores.eje, Offset(0f, 0f), Offset(0f, size.height), strokeWidth = 1.dp.toPx())
                        val w = (size.width * (b.valor / maximo)).toFloat()
                        if (w > 0f) {
                            val r = 4.dp.toPx()
                            // Rectángulo con esquinas redondeadas solo en el extremo del dato
                            val path = Path().apply {
                                moveTo(0f, 0f)
                                lineTo(max(w - r, 0f), 0f)
                                quadraticTo(w, 0f, w, r.coerceAtMost(size.height / 2))
                                lineTo(w, size.height - r.coerceAtMost(size.height / 2))
                                quadraticTo(w, size.height, max(w - r, 0f), size.height)
                                lineTo(0f, size.height)
                                close()
                            }
                            drawPath(path, color)
                        }
                    }
                }
                Text(
                    b.textoValor,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.width(64.dp).padding(start = 8.dp),
                )
            }
        }
    }
}

/** Leyenda: muestra + texto en tinta normal (el texto nunca usa el color de la serie). */
@Composable
fun Leyenda(items: List<Pair<String, Color>>) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        items.forEach { (texto, color) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(width = 14.dp, height = 3.dp).background(color, RoundedCornerShape(2.dp)))
                Spacer(Modifier.width(6.dp))
                Text(texto, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/**
 * Líneas de creadas vs. resueltas por periodo. Un solo eje Y (conteos).
 * Tocar o arrastrar muestra una guía vertical con los valores del periodo.
 */
@Composable
fun GraficoTendencia(puntos: List<PuntoTendenciaDto>, agrupacion: String, modifier: Modifier = Modifier) {
    val colores = LocalChartColors.current
    val medidor = rememberTextMeasurer()
    val tinta = MaterialTheme.colorScheme.onSurface
    val superficieTooltip = MaterialTheme.colorScheme.inverseSurface
    val textoTooltip = MaterialTheme.colorScheme.inverseOnSurface
    var seleccion by remember(puntos) { mutableStateOf<Int?>(null) }

    val maxValor = puntos.maxOfOrNull { max(it.creadas, it.resueltas) } ?: 0
    val paso = when {
        maxValor <= 4 -> 1
        maxValor <= 10 -> 2
        maxValor <= 25 -> 5
        else -> ceil(maxValor / 5.0 / 5).toInt() * 5
    }
    val techo = max(paso, ceil(maxValor.toDouble() / paso).toInt() * paso)
    val estiloEje = TextStyle(fontSize = 10.sp, color = colores.textoTenue)

    Column(modifier) {
        Leyenda(listOf("Creadas" to colores.serie1, "Resueltas" to colores.serie2))
        Spacer(Modifier.height(12.dp))
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(200.dp)
                .semantics {
                    contentDescription = "Tendencia: " + puntos.joinToString { "${it.periodo} ${it.creadas} creadas, ${it.resueltas} resueltas" }
                }
                .pointerInput(puntos) {
                    detectTapGestures { pos -> seleccion = indiceCercano(pos.x, size.width.toFloat(), 28.dp.toPx(), puntos.size) }
                }
                .pointerInput(puntos) {
                    detectDragGestures(
                        onDragEnd = {},
                        onDrag = { change, _ -> seleccion = indiceCercano(change.position.x, size.width.toFloat(), 28.dp.toPx(), puntos.size) },
                    )
                },
        ) {
            if (puntos.isEmpty()) return@Canvas
            val izq = 28.dp.toPx()
            val abajo = 22.dp.toPx()
            val arriba = 6.dp.toPx()
            val ancho = size.width - izq
            val alto = size.height - abajo - arriba
            val x = { i: Int -> izq + if (puntos.size == 1) ancho / 2 else ancho * i / (puntos.size - 1) }
            val y = { v: Int -> arriba + alto * (1f - v.toFloat() / techo) }

            // Grid recesivo + marcas del eje Y
            var v = 0
            while (v <= techo) {
                val yy = y(v)
                drawLine(if (v == 0) colores.eje else colores.grid, Offset(izq, yy), Offset(size.width, yy), strokeWidth = 1.dp.toPx())
                val t = medidor.measure(v.toString(), estiloEje)
                drawText(t, topLeft = Offset(izq - t.size.width - 6.dp.toPx(), yy - t.size.height / 2))
                v += paso
            }
            // Etiquetas X: como máximo ~6 para que no se encimen
            val cadaN = max(1, ceil(puntos.size / 6.0).toInt())
            puntos.forEachIndexed { i, p ->
                if (i % cadaN == 0 || i == puntos.lastIndex && puntos.size <= 8) {
                    val t = medidor.measure(Formatos.periodo(p.periodo, agrupacion), estiloEje)
                    val cx = (x(i) - t.size.width / 2).coerceIn(izq - 8.dp.toPx(), size.width - t.size.width)
                    drawText(t, topLeft = Offset(cx, size.height - t.size.height))
                }
            }

            fun serie(valores: List<Int>, color: Color) {
                val path = Path()
                valores.forEachIndexed { i, vv -> if (i == 0) path.moveTo(x(i), y(vv)) else path.lineTo(x(i), y(vv)) }
                drawPath(path, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            serie(puntos.map { it.creadas }, colores.serie1)
            serie(puntos.map { it.resueltas }, colores.serie2)

            seleccion?.let { i ->
                val p = puntos[i]
                val cx = x(i)
                drawLine(colores.textoTenue, Offset(cx, arriba), Offset(cx, arriba + alto), strokeWidth = 1.dp.toPx())
                listOf(p.creadas to colores.serie1, p.resueltas to colores.serie2).forEach { (vv, c) ->
                    drawCircle(Color.White, radius = 6.dp.toPx(), center = Offset(cx, y(vv)))
                    drawCircle(c, radius = 4.dp.toPx(), center = Offset(cx, y(vv)))
                }
                val texto = "${Formatos.periodo(p.periodo, agrupacion)}\nCreadas: ${p.creadas}\nResueltas: ${p.resueltas}"
                val t = medidor.measure(texto, TextStyle(fontSize = 11.sp, color = textoTooltip, lineHeight = 15.sp))
                val pad = 8.dp.toPx()
                val w = t.size.width + pad * 2
                val h = t.size.height + pad * 2
                val tx = if (cx + 12.dp.toPx() + w < size.width) cx + 12.dp.toPx() else cx - 12.dp.toPx() - w
                drawRoundRect(superficieTooltip, Offset(tx, arriba), Size(w, h), CornerRadius(6.dp.toPx()))
                drawText(t, topLeft = Offset(tx + pad, arriba + pad))
            }
        }
        if (puntos.isNotEmpty()) {
            val totalC = puntos.sumOf { it.creadas }
            val totalR = puntos.sumOf { it.resueltas }
            Text(
                "En el periodo: $totalC creadas y $totalR resueltas. Toca el gráfico para ver el detalle.",
                style = MaterialTheme.typography.bodySmall,
                color = tinta.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

private fun indiceCercano(x: Float, ancho: Float, margenIzq: Float, n: Int): Int? {
    if (n == 0) return null
    if (n == 1) return 0
    val rel = ((x - margenIzq) / (ancho - margenIzq)).coerceIn(0f, 1f)
    return (rel * (n - 1)).roundToInt()
}
