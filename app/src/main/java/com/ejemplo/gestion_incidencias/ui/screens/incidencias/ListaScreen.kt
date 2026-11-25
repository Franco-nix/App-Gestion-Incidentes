package com.ejemplo.gestion_incidencias.ui.screens.incidencias

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Engineering
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ejemplo.gestion_incidencias.data.remote.IncidenciaDto
import com.ejemplo.gestion_incidencias.data.remote.UsuarioDto
import com.ejemplo.gestion_incidencias.data.repository.FiltroIncidencias
import com.ejemplo.gestion_incidencias.data.repository.IncidenciaRepository
import com.ejemplo.gestion_incidencias.domain.Estado
import com.ejemplo.gestion_incidencias.domain.Prioridad
import com.ejemplo.gestion_incidencias.domain.Rol
import com.ejemplo.gestion_incidencias.ui.common.BarraSuperior
import com.ejemplo.gestion_incidencias.ui.common.CargandoBox
import com.ejemplo.gestion_incidencias.ui.common.EstadoChip
import com.ejemplo.gestion_incidencias.ui.common.Formatos
import com.ejemplo.gestion_incidencias.ui.common.MensajeError
import com.ejemplo.gestion_incidencias.ui.common.MensajeVacio
import com.ejemplo.gestion_incidencias.ui.common.PrioridadChip
import com.ejemplo.gestion_incidencias.ui.common.fabrica
import com.ejemplo.gestion_incidencias.ui.navigation.tituloLista
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ListaUiState(
    val items: List<IncidenciaDto> = emptyList(),
    val total: Int = 0,
    val pagina: Int = 0,
    val filtro: FiltroIncidencias = FiltroIncidencias(),
    val cargandoInicial: Boolean = true,
    val refrescando: Boolean = false,
    val cargandoMas: Boolean = false,
    val error: String? = null,
) {
    val hayMas get() = items.size < total
}

/**
 * Lista paginada de incidencias. Reutilizable: con [soloSinAsignar] alimenta la pantalla de asignación.
 */
open class ListaViewModel(
    private val repo: IncidenciaRepository,
    soloSinAsignar: Boolean = false,
) : ViewModel() {
    private val _ui = MutableStateFlow(ListaUiState(filtro = FiltroIncidencias(sinAsignar = soloSinAsignar)))
    val ui = _ui.asStateFlow()
    private var job: Job? = null
    private var yaCargado = false

    /** Se llama al volver a la pantalla: carga inicial o refresco silencioso. */
    fun alReanudar() {
        if (!yaCargado) cargar(reiniciar = true, mostrarIndicador = false) else refrescarSilencioso()
    }

    fun refrescar() = cargar(reiniciar = true, mostrarIndicador = true)

    fun cargarMas() {
        val s = _ui.value
        if (s.cargandoMas || s.refrescando || s.cargandoInicial || !s.hayMas) return
        cargar(reiniciar = false, mostrarIndicador = false)
    }

    fun estado(e: Estado?) = cambiarFiltro { it.copy(estado = e) }
    fun prioridad(p: Prioridad?) = cambiarFiltro { it.copy(prioridad = p) }

    fun busqueda(q: String) {
        _ui.update { it.copy(filtro = it.filtro.copy(q = q)) }
        job?.cancel()
        job = viewModelScope.launch {
            delay(400) // debounce
            cargarAhora(reiniciar = true, mostrarIndicador = false)
        }
    }

    fun quitar(id: Int) = _ui.update { s -> s.copy(items = s.items.filterNot { it.id == id }, total = (s.total - 1).coerceAtLeast(0)) }

    private fun cambiarFiltro(f: (FiltroIncidencias) -> FiltroIncidencias) {
        _ui.update { it.copy(filtro = f(it.filtro), cargandoInicial = true) }
        cargar(reiniciar = true, mostrarIndicador = false)
    }

    private fun refrescarSilencioso() {
        // Recarga las páginas ya visibles para reflejar cambios hechos en el detalle
        val s = _ui.value
        if (s.cargandoInicial || s.refrescando) return
        job?.cancel()
        job = viewModelScope.launch {
            val tam = (s.pagina.coerceAtLeast(1) * TAM).coerceAtMost(100)
            repo.listar(s.filtro, page = 1, size = tam).onSuccess { p ->
                _ui.update { it.copy(items = p.items, total = p.total, pagina = (p.items.size + TAM - 1) / TAM, error = null) }
            }
        }
    }

    private fun cargar(reiniciar: Boolean, mostrarIndicador: Boolean) {
        job?.cancel()
        job = viewModelScope.launch { cargarAhora(reiniciar, mostrarIndicador) }
    }

    private suspend fun cargarAhora(reiniciar: Boolean, mostrarIndicador: Boolean) {
        val pagina = if (reiniciar) 1 else _ui.value.pagina + 1
        _ui.update {
            it.copy(
                refrescando = mostrarIndicador,
                cargandoMas = !reiniciar,
                cargandoInicial = it.cargandoInicial && reiniciar,
                error = null,
            )
        }
        repo.listar(_ui.value.filtro, pagina, TAM)
            .onSuccess { p ->
                yaCargado = true
                _ui.update {
                    it.copy(
                        items = if (reiniciar) p.items else it.items + p.items,
                        total = p.total,
                        pagina = pagina,
                        cargandoInicial = false,
                        refrescando = false,
                        cargandoMas = false,
                    )
                }
            }
            .onFailure { e ->
                _ui.update { it.copy(error = e.message, cargandoInicial = false, refrescando = false, cargandoMas = false) }
            }
    }

    companion object {
        const val TAM = 20
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaScreen(
    usuario: UsuarioDto,
    onAbrir: (Int) -> Unit,
    onNueva: () -> Unit,
    onPerfil: (() -> Unit)?,
) {
    val vm: ListaViewModel = viewModel(factory = fabrica { ListaViewModel(it.incidenciaRepository) })
    val ui by vm.ui.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.alReanudar() }

    Scaffold(
        topBar = { BarraSuperior(tituloLista(usuario.rol), onPerfil = onPerfil) },
        floatingActionButton = {
            // Los usuarios tienen la pestaña "Nueva"; el resto también puede registrar desde aquí
            if (usuario.rol != Rol.USUARIO) {
                ExtendedFloatingActionButton(
                    onClick = onNueva,
                    icon = { Icon(Icons.Outlined.Add, null) },
                    text = { Text("Registrar") },
                )
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Filtros(ui.filtro, vm::busqueda, vm::estado, vm::prioridad)
            ListaIncidencias(
                ui = ui,
                onAbrir = onAbrir,
                onRefrescar = vm::refrescar,
                onCargarMas = vm::cargarMas,
                vacioTitulo = when (usuario.rol) {
                    Rol.USUARIO -> "Aún no registraste incidencias"
                    Rol.TECNICO -> "No tienes incidencias asignadas"
                    Rol.ADMIN -> "No hay incidencias"
                },
                vacioDetalle = if (usuario.rol == Rol.USUARIO) "Usa la pestaña «Nueva» para reportar un problema técnico." else null,
            )
        }
    }
}

@Composable
private fun Filtros(
    filtro: FiltroIncidencias,
    onBusqueda: (String) -> Unit,
    onEstado: (Estado?) -> Unit,
    onPrioridad: (Prioridad?) -> Unit,
) {
    var menuPrioridad by remember { mutableStateOf(false) }
    Column(Modifier.padding(top = 4.dp)) {
        OutlinedTextField(
            value = filtro.q,
            onValueChange = onBusqueda,
            placeholder = { Text("Buscar incidencias", maxLines = 1) },
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            trailingIcon = {
                if (filtro.q.isNotEmpty()) IconButton(onClick = { onBusqueda("") }) { Icon(Icons.Outlined.Close, "Limpiar") }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Box {
                    FilterChip(
                        selected = filtro.prioridad != null,
                        onClick = { menuPrioridad = true },
                        label = { Text(filtro.prioridad?.let { "Prioridad: ${it.etiqueta}" } ?: "Prioridad") },
                        leadingIcon = { Icon(Icons.Outlined.FilterList, null, Modifier.size(18.dp)) },
                    )
                    DropdownMenu(expanded = menuPrioridad, onDismissRequest = { menuPrioridad = false }) {
                        DropdownMenuItem(text = { Text("Todas") }, onClick = { onPrioridad(null); menuPrioridad = false })
                        Prioridad.entries.reversed().forEach { p ->
                            DropdownMenuItem(text = { Text(p.etiqueta) }, onClick = { onPrioridad(p); menuPrioridad = false })
                        }
                    }
                }
            }
            item {
                FilterChip(selected = filtro.estado == null, onClick = { onEstado(null) }, label = { Text("Todos") })
            }
            items(Estado.entries) { e ->
                FilterChip(
                    selected = filtro.estado == e,
                    onClick = { onEstado(if (filtro.estado == e) null else e) },
                    label = { Text(e.etiqueta) },
                )
            }
        }
    }
}

/** Lista con pull-to-refresh y paginación infinita; compartida con la pantalla de asignación. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaIncidencias(
    ui: ListaUiState,
    onAbrir: (Int) -> Unit,
    onRefrescar: () -> Unit,
    onCargarMas: () -> Unit,
    vacioTitulo: String,
    vacioDetalle: String? = null,
    accion: (@Composable (IncidenciaDto) -> Unit)? = null,
) {
    val estadoLista = rememberLazyListState()
    val cercaDelFinal by remember {
        derivedStateOf {
            val ultimo = estadoLista.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            ultimo >= estadoLista.layoutInfo.totalItemsCount - 4
        }
    }
    LaunchedEffect(cercaDelFinal, ui.items.size) { if (cercaDelFinal) onCargarMas() }

    PullToRefreshBox(isRefreshing = ui.refrescando, onRefresh = onRefrescar, modifier = Modifier.fillMaxSize()) {
        when {
            ui.cargandoInicial -> CargandoBox()
            ui.error != null && ui.items.isEmpty() -> MensajeError(ui.error, onRefrescar)
            ui.items.isEmpty() -> {
                val filtrando = ui.filtro.q.isNotBlank() || ui.filtro.estado != null || ui.filtro.prioridad != null
                // LazyColumn para que el pull-to-refresh funcione también con la lista vacía
                LazyColumn(Modifier.fillMaxSize()) {
                    item {
                        Box(Modifier.fillParentMaxSize()) {
                            if (filtrando) MensajeVacio("Sin resultados", "Prueba con otros filtros.", Icons.Outlined.SearchOff)
                            else MensajeVacio(vacioTitulo, vacioDetalle)
                        }
                    }
                }
            }
            else -> LazyColumn(
                state = estadoLista,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item {
                    Text(
                        "${ui.total} incidencia${if (ui.total == 1) "" else "s"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(ui.items, key = { it.id }) { inc ->
                    TarjetaIncidencia(inc, onClick = { onAbrir(inc.id) }, accion = accion)
                }
                if (ui.cargandoMas) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(Modifier.size(28.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TarjetaIncidencia(
    inc: IncidenciaDto,
    onClick: () -> Unit,
    accion: (@Composable (IncidenciaDto) -> Unit)? = null,
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    inc.codigo,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    Formatos.relativo(inc.fechaCreacion),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                inc.titulo,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                inc.categoria.nombre,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                EstadoChip(inc.estado)
                PrioridadChip(inc.prioridad)
            }
            val ubicacion = inc.ubicacion
            if (ubicacion != null || inc.tecnico != null) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (ubicacion != null) {
                        Icon(Icons.Outlined.Place, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            ubicacion,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Spacer(Modifier.width(12.dp))
                    }
                    inc.tecnico?.let { t ->
                        Icon(Icons.Outlined.Engineering, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(4.dp))
                        Text(t.nombre, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }
            }
            if (accion != null) {
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().clickable(enabled = false) {}, contentAlignment = Alignment.CenterEnd) { accion(inc) }
            }
        }
    }
}
