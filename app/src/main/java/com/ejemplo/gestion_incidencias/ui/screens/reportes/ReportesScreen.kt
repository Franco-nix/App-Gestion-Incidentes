package com.ejemplo.gestion_incidencias.ui.screens.reportes

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ejemplo.gestion_incidencias.data.remote.ConteoDto
import com.ejemplo.gestion_incidencias.data.remote.ResumenDto
import com.ejemplo.gestion_incidencias.data.remote.TecnicoReporteDto
import com.ejemplo.gestion_incidencias.data.remote.TiempoDto
import com.ejemplo.gestion_incidencias.data.repository.DatosReportes
import com.ejemplo.gestion_incidencias.data.repository.ReporteRepository
import com.ejemplo.gestion_incidencias.domain.Estado
import com.ejemplo.gestion_incidencias.domain.Prioridad
import com.ejemplo.gestion_incidencias.ui.common.Barra
import com.ejemplo.gestion_incidencias.ui.common.BarraSuperior
import com.ejemplo.gestion_incidencias.ui.common.BarrasHorizontales
import com.ejemplo.gestion_incidencias.ui.common.Carga
import com.ejemplo.gestion_incidencias.ui.common.CargandoBox
import com.ejemplo.gestion_incidencias.ui.common.Formatos
import com.ejemplo.gestion_incidencias.ui.common.GraficoTendencia
import com.ejemplo.gestion_incidencias.ui.common.MensajeError
import com.ejemplo.gestion_incidencias.ui.common.fabrica
import com.ejemplo.gestion_incidencias.ui.theme.LocalChartColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Rangos predefinidos: (etiqueta, días hacia atrás o null = todo, agrupación de la tendencia). */
enum class Rango(val etiqueta: String, val dias: Long?, val agrupacion: String) {
    D7("7 días", 7, "dia"),
    D30("30 días", 30, "semana"),
    D90("90 días", 90, "semana"),
    TODO("Último año", 365, "mes"),
}

data class ReportesUi(
    val rango: Rango = Rango.D30,
    val datos: Carga<DatosReportes> = Carga.Cargando,
    val refrescando: Boolean = false,
)

class ReportesViewModel(private val repo: ReporteRepository) : ViewModel() {
    private val _ui = MutableStateFlow(ReportesUi())
    val ui = _ui.asStateFlow()

    init { cargar() }

    fun rango(r: Rango) {
        if (r == _ui.value.rango) return
        _ui.update { it.copy(rango = r, datos = Carga.Cargando) }
        cargar()
    }

    fun refrescar() {
        _ui.update { it.copy(refrescando = true) }
        cargar()
    }

    fun cargar() = viewModelScope.launch {
        val r = _ui.value.rango
        val hasta = LocalDate.now()
        val desde = r.dias?.let { hasta.minusDays(it - 1) }
        repo.cargar(desde?.toString(), hasta.toString(), r.agrupacion)
            .onSuccess { d -> _ui.update { it.copy(datos = Carga.Exito(d), refrescando = false) } }
            .onFailure { e -> _ui.update { it.copy(datos = Carga.Error(e.message ?: "Error"), refrescando = false) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportesScreen(onPerfil: (() -> Unit)?) {
    val vm: ReportesViewModel = viewModel(factory = fabrica { ReportesViewModel(it.reporteRepository) })
    val ui by vm.ui.collectAsStateWithLifecycle()

    Scaffold(topBar = { BarraSuperior("Reportes y estadísticas", onPerfil = onPerfil) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Filtro de rango: una sola fila sobre todos los gráficos
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Rango.entries.forEach { r ->
                    FilterChip(selected = ui.rango == r, onClick = { vm.rango(r) }, label = { Text(r.etiqueta) })
                }
            }
            PullToRefreshBox(isRefreshing = ui.refrescando, onRefresh = vm::refrescar, modifier = Modifier.fillMaxSize()) {
                when (val d = ui.datos) {
                    Carga.Cargando -> CargandoBox()
                    is Carga.Error -> MensajeError(d.mensaje, { vm.cargar() })
                    is Carga.Exito -> Panel(d.datos)
                }
            }
        }
    }
}

@Composable
private fun Panel(d: DatosReportes) {
    val colores = LocalChartColors.current
    val r = d.resumen
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Kpis(r)

        Tarjeta("Creadas vs. resueltas", "Por ${when (d.tendencia.agrupacion) { "dia" -> "día"; "semana" -> "semana (inicio de semana)"; else -> "mes" }}") {
            GraficoTendencia(d.tendencia.puntos, d.tendencia.agrupacion)
        }

        Tarjeta("Por estado", "${r.total} incidencias en el periodo") {
            BarrasHorizontales(
                r.porEstado.map { c ->
                    val e = runCatching { Estado.valueOf(c.clave) }.getOrNull()
                    Barra(e?.etiqueta ?: c.clave, c.total.toDouble(), "${c.total} · ${Formatos.porcentaje(c.total, r.total)}")
                },
            )
        }

        Tarjeta("Por categoría", null) {
            if (r.porCategoria.isEmpty()) SinDatos()
            else BarrasHorizontales(r.porCategoria.map { Barra(it.clave, it.total.toDouble(), it.total.toString()) })
        }

        Tarjeta("Por prioridad", null) {
            // Rampa ordinal de un solo tono: más oscuro = más urgente
            BarrasHorizontales(
                r.porPrioridad.map { c ->
                    val p = runCatching { Prioridad.valueOf(c.clave) }.getOrNull()
                    Barra(p?.etiqueta ?: c.clave, c.total.toDouble(), c.total.toString(), p?.let { colores.rampaOrdinal[it.ordinal] })
                },
            )
        }

        Tarjeta("Tiempo promedio de resolución", "Desde la asignación hasta marcarla como resuelta") {
            Text("Por prioridad", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            if (d.tiempos.porPrioridad.isEmpty()) SinDatos() else BarrasHorizontales(barrasTiempo(d.tiempos.porPrioridad, true))
            Spacer(Modifier.height(16.dp))
            Text("Por categoría", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            if (d.tiempos.porCategoria.isEmpty()) SinDatos() else BarrasHorizontales(barrasTiempo(d.tiempos.porCategoria, false))
        }

        Tarjeta("Desempeño por técnico", null) { TablaTecnicos(d.tecnicos) }
        Spacer(Modifier.height(8.dp))
    }
}

private fun barrasTiempo(t: List<TiempoDto>, esPrioridad: Boolean) = t.map {
    val etiqueta = if (esPrioridad) runCatching { Prioridad.valueOf(it.clave).etiqueta }.getOrDefault(it.clave) else it.clave
    Barra(etiqueta, it.promedioHoras ?: 0.0, Formatos.horas(it.promedioHoras))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Kpis(r: ResumenDto) {
    val items = listOf(
        Triple("Total", r.total.toString(), null),
        Triple("Sin asignar", r.pendientes.toString(), null),
        Triple("En curso", r.enCurso.toString(), null),
        Triple("Resueltas", r.resueltas.toString(), Formatos.porcentaje(r.resueltas, r.total)),
        Triple("Críticas abiertas", r.criticasAbiertas.toString(), null),
        Triple("Prom. resolución", Formatos.horas(r.promedioHorasResolucion), null),
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        maxItemsInEachRow = 3,
        modifier = Modifier.fillMaxWidth(),
    ) {
        items.forEach { (titulo, valor, sub) ->
            val alerta = titulo == "Críticas abiertas" && r.criticasAbiertas > 0
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (alerta) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerLowest,
                ),
                modifier = Modifier.weight(1f),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        titulo,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(valor, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    if (sub != null) Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun Tarjeta(titulo: String, subtitulo: String?, contenido: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            if (subtitulo != null) {
                Text(subtitulo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(14.dp))
            contenido()
        }
    }
}

@Composable
private fun SinDatos() {
    Text("Sin datos en este periodo", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun TablaTecnicos(tecnicos: List<TecnicoReporteDto>) {
    if (tecnicos.isEmpty()) { SinDatos(); return }
    val estiloCab = MaterialTheme.typography.labelSmall
    val colorCab = MaterialTheme.colorScheme.onSurfaceVariant
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Técnico", style = estiloCab, color = colorCab, modifier = Modifier.weight(1f))
            listOf("Asig.", "Curso", "Resu.", "Prom.").forEach {
                Text(it, style = estiloCab, color = colorCab, textAlign = TextAlign.End, modifier = Modifier.width(if (it == "Prom.") 64.dp else 44.dp))
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 6.dp))
        tecnicos.forEach { t ->
            Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(t.nombre, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    t.especialidad?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = colorCab, maxLines = 1) }
                }
                listOf(t.asignadas, t.enCurso, t.resueltas).forEach {
                    Text(it.toString(), textAlign = TextAlign.End, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(44.dp))
                }
                Text(
                    Formatos.horas(t.promedioHorasResolucion),
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(64.dp),
                )
            }
        }
    }
}
