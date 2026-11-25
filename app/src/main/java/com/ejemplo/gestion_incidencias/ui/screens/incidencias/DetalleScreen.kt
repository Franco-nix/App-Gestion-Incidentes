package com.ejemplo.gestion_incidencias.ui.screens.incidencias

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AddTask
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Engineering
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.PublishedWithChanges
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ejemplo.gestion_incidencias.data.remote.IncidenciaDto
import com.ejemplo.gestion_incidencias.data.remote.SeguimientoDto
import com.ejemplo.gestion_incidencias.data.remote.TecnicoCargaDto
import com.ejemplo.gestion_incidencias.data.repository.IncidenciaRepository
import com.ejemplo.gestion_incidencias.data.repository.UsuarioRepository
import com.ejemplo.gestion_incidencias.domain.Acciones
import com.ejemplo.gestion_incidencias.domain.Estado
import com.ejemplo.gestion_incidencias.domain.TipoSeguimiento
import com.ejemplo.gestion_incidencias.ui.common.BarraSuperior
import com.ejemplo.gestion_incidencias.ui.common.Carga
import com.ejemplo.gestion_incidencias.ui.common.CargandoBox
import com.ejemplo.gestion_incidencias.ui.common.DatoFila
import com.ejemplo.gestion_incidencias.ui.common.EstadoChip
import com.ejemplo.gestion_incidencias.ui.common.Formatos
import com.ejemplo.gestion_incidencias.ui.common.MensajeError
import com.ejemplo.gestion_incidencias.ui.common.PrioridadChip
import com.ejemplo.gestion_incidencias.ui.common.color
import com.ejemplo.gestion_incidencias.ui.common.fabrica
import com.ejemplo.gestion_incidencias.ui.screens.asignacion.SelectorTecnicoSheet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetalleUiState(
    val incidencia: Carga<IncidenciaDto> = Carga.Cargando,
    val procesando: Boolean = false,
    val mensaje: String? = null,
    val tecnicos: Carga<List<TecnicoCargaDto>> = Carga.Cargando,
    val comentario: String = "",
)

class DetalleViewModel(
    private val id: Int,
    private val repo: IncidenciaRepository,
    private val usuarios: UsuarioRepository,
) : ViewModel() {
    private val _ui = MutableStateFlow(DetalleUiState())
    val ui = _ui.asStateFlow()

    fun cargar() = viewModelScope.launch {
        repo.detalle(id)
            .onSuccess { inc -> _ui.update { it.copy(incidencia = Carga.Exito(inc)) } }
            .onFailure { e ->
                // Si ya había datos, no los reemplazamos por un error: solo avisamos
                if (_ui.value.incidencia is Carga.Exito) _ui.update { it.copy(mensaje = e.message) }
                else _ui.update { it.copy(incidencia = Carga.Error(e.message ?: "Error")) }
            }
    }

    fun comentario(v: String) = _ui.update { it.copy(comentario = v) }
    fun mensajeMostrado() = _ui.update { it.copy(mensaje = null) }

    fun cambiarEstado(estado: Estado, comentario: String) = operar("Estado actualizado a «${estado.etiqueta}»") {
        repo.cambiarEstado(id, estado, comentario)
    }

    fun enviarComentario() {
        val texto = _ui.value.comentario.trim()
        if (texto.isEmpty() || _ui.value.procesando) return
        _ui.update { it.copy(procesando = true) }
        viewModelScope.launch {
            repo.comentar(id, texto)
                .onSuccess {
                    _ui.update { it.copy(comentario = "", procesando = false) }
                    cargar()
                }
                .onFailure { e -> _ui.update { it.copy(procesando = false, mensaje = e.message) } }
        }
    }

    fun cargarTecnicos() {
        _ui.update { it.copy(tecnicos = Carga.Cargando) }
        viewModelScope.launch {
            usuarios.tecnicos()
                .onSuccess { t -> _ui.update { it.copy(tecnicos = Carga.Exito(t)) } }
                .onFailure { e -> _ui.update { it.copy(tecnicos = Carga.Error(e.message ?: "Error")) } }
        }
    }

    fun asignar(tecnicoId: Int, comentario: String, alTerminar: () -> Unit) =
        operar("Técnico asignado", alTerminar) { repo.asignar(id, tecnicoId, comentario) }

    private fun operar(ok: String, alTerminar: () -> Unit = {}, op: suspend () -> Result<IncidenciaDto>) {
        if (_ui.value.procesando) return
        _ui.update { it.copy(procesando = true) }
        viewModelScope.launch {
            op()
                .onSuccess { inc ->
                    _ui.update { it.copy(incidencia = Carga.Exito(inc), procesando = false, mensaje = ok) }
                    alTerminar()
                }
                .onFailure { e -> _ui.update { it.copy(procesando = false, mensaje = e.message) } }
        }
    }
}

@Composable
fun DetalleScreen(id: Int, onVolver: () -> Unit, onEditar: (Int) -> Unit) {
    val vm: DetalleViewModel = viewModel(
        key = "detalle-$id",
        factory = fabrica { DetalleViewModel(id, it.incidenciaRepository, it.usuarioRepository) },
    )
    val ui by vm.ui.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var transicion by remember { mutableStateOf<Estado?>(null) }
    var mostrarAsignar by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.cargar() }
    LaunchedEffect(ui.mensaje) {
        ui.mensaje?.let { snackbar.showSnackbar(it); vm.mensajeMostrado() }
    }

    val inc = (ui.incidencia as? Carga.Exito)?.datos
    val acciones = inc?.acciones

    Scaffold(
        topBar = {
            BarraSuperior(inc?.codigo ?: "Incidencia", onVolver = onVolver) {
                if (acciones?.puedeEditar == true) {
                    IconButton(onClick = { onEditar(id) }) { Icon(Icons.Outlined.Edit, "Editar") }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (acciones?.puedeComentar == true) {
                BarraComentario(ui.comentario, ui.procesando, vm::comentario, vm::enviarComentario)
            }
        },
    ) { padding ->
        when (val c = ui.incidencia) {
            Carga.Cargando -> CargandoBox(Modifier.padding(padding))
            is Carga.Error -> MensajeError(c.mensaje, { vm.cargar() }, Modifier.padding(padding))
            is Carga.Exito -> Contenido(
                inc = c.datos,
                procesando = ui.procesando,
                onTransicion = { transicion = it },
                onAsignar = { mostrarAsignar = true; vm.cargarTecnicos() },
                modifier = Modifier.padding(padding),
            )
        }
    }

    val destino = transicion
    if (destino != null && inc != null) {
        DialogoTransicion(
            actual = inc.estado,
            destino = destino,
            onDismiss = { transicion = null },
            onConfirmar = { comentario ->
                vm.cambiarEstado(destino, comentario)
                transicion = null
            },
        )
    }
    if (mostrarAsignar && inc != null) {
        SelectorTecnicoSheet(
            titulo = if (inc.tecnico == null) "Asignar técnico" else "Reasignar técnico",
            tecnicos = ui.tecnicos,
            tecnicoActualId = inc.tecnico?.id,
            procesando = ui.procesando,
            onDismiss = { mostrarAsignar = false },
            onConfirmar = { tecnicoId, comentario -> vm.asignar(tecnicoId, comentario) { mostrarAsignar = false } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Contenido(
    inc: IncidenciaDto,
    procesando: Boolean,
    onTransicion: (Estado) -> Unit,
    onAsignar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val acciones = inc.acciones
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Encabezado
        Column {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EstadoChip(inc.estado)
                PrioridadChip(inc.prioridad)
            }
            Spacer(Modifier.height(10.dp))
            Text(inc.titulo, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(inc.descripcion, style = MaterialTheme.typography.bodyLarge)
        }

        // Acciones disponibles (calculadas por el backend según rol y estado)
        if (acciones != null && (acciones.transiciones.isNotEmpty() || acciones.puedeAsignar)) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Acciones", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(10.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (acciones.puedeAsignar) {
                            val principal = inc.tecnico == null
                            val etiqueta = if (principal) "Asignar técnico" else "Reasignar"
                            if (principal) Button(onClick = onAsignar, enabled = !procesando) { Text(etiqueta) }
                            else OutlinedButton(onClick = onAsignar, enabled = !procesando) { Text(etiqueta) }
                        }
                        acciones.transiciones.forEach { destino ->
                            val texto = Acciones.etiqueta(inc.estado, destino)
                            if (Acciones.esSecundaria(inc.estado, destino)) {
                                OutlinedButton(onClick = { onTransicion(destino) }, enabled = !procesando) { Text(texto) }
                            } else {
                                Button(onClick = { onTransicion(destino) }, enabled = !procesando) { Text(texto) }
                            }
                        }
                    }
                }
            }
        }

        // Ficha
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp)) {
                DatoFila("Categoría", inc.categoria.nombre, Icons.Outlined.Category)
                DatoFila("Ubicación", inc.ubicacion ?: "—", Icons.Outlined.Place)
                DatoFila("Reportado por", inc.reportadoPor.nombre + (inc.reportadoPor.area?.let { "\n$it" } ?: ""), Icons.Outlined.Person)
                DatoFila("Técnico", inc.tecnico?.nombre ?: "Sin asignar", Icons.Outlined.Engineering)
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                DatoFila("Registrada", Formatos.fechaHora(inc.fechaCreacion), Icons.Outlined.Event)
                if (inc.fechaAsignacion != null) DatoFila("Asignada", Formatos.fechaHora(inc.fechaAsignacion))
                if (inc.fechaResolucion != null) DatoFila("Resuelta", Formatos.fechaHora(inc.fechaResolucion))
                if (inc.fechaCierre != null) DatoFila("Cerrada", Formatos.fechaHora(inc.fechaCierre))
            }
        }

        // Historial
        Text("Seguimiento", style = MaterialTheme.typography.titleMedium)
        Column {
            inc.seguimientos.reversed().forEachIndexed { i, s ->
                ItemLineaTiempo(s, esUltimo = i == inc.seguimientos.lastIndex)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ItemLineaTiempo(s: SeguimientoDto, esUltimo: Boolean) {
    val (icono, titulo) = describir(s)
    val colorPunto = s.estadoNuevo?.color() ?: MaterialTheme.colorScheme.outline
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(36.dp)) {
            Surface(shape = CircleShape, color = colorPunto.copy(alpha = 0.15f), modifier = Modifier.size(32.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icono, null, Modifier.size(18.dp), tint = colorPunto) }
            }
            if (!esUltimo) {
                Box(
                    Modifier.width(2.dp).weight(1f).padding(vertical = 2.dp),
                ) { Surface(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.fillMaxSize()) {} }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).padding(bottom = 18.dp)) {
            Text(titulo, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "${s.usuario.nombre} · ${Formatos.fechaHora(s.createdAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val comentario = s.comentario?.takeIf { s.tipo != TipoSeguimiento.ASIGNACION && s.tipo != TipoSeguimiento.CREACION }
            if (comentario != null) {
                Spacer(Modifier.height(6.dp))
                Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium) {
                    Text(comentario, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(10.dp))
                }
            }
        }
    }
}

private fun describir(s: SeguimientoDto): Pair<ImageVector, String> = when (s.tipo) {
    TipoSeguimiento.CREACION -> Icons.Outlined.AddTask to "Incidencia registrada"
    TipoSeguimiento.ASIGNACION -> Icons.Outlined.SwapHoriz to (s.comentario ?: "Técnico asignado")
    TipoSeguimiento.COMENTARIO -> Icons.AutoMirrored.Outlined.Comment to "Comentario"
    TipoSeguimiento.CAMBIO_ESTADO -> Icons.Outlined.PublishedWithChanges to
        "${s.estadoAnterior?.etiqueta ?: "—"} → ${s.estadoNuevo?.etiqueta ?: "—"}"
}

@Composable
private fun BarraComentario(texto: String, procesando: Boolean, onCambio: (String) -> Unit, onEnviar: () -> Unit) {
    Surface(tonalElevation = 3.dp) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = texto,
                onValueChange = onCambio,
                placeholder = { Text("Escribe un comentario…") },
                maxLines = 4,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onEnviar, enabled = texto.isNotBlank() && !procesando) {
                Icon(Icons.AutoMirrored.Outlined.Send, "Enviar comentario")
            }
        }
    }
}

@Composable
private fun DialogoTransicion(actual: Estado, destino: Estado, onDismiss: () -> Unit, onConfirmar: (String) -> Unit) {
    var comentario by remember { mutableStateOf("") }
    val obligatorio = Acciones.comentarioObligatorio(destino)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Acciones.etiqueta(actual, destino)) },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EstadoChip(actual)
                    Text("  →  ")
                    EstadoChip(destino)
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = comentario,
                    onValueChange = { comentario = it },
                    label = { Text(Acciones.pistaComentario(actual, destino)) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirmar(comentario) }, enabled = !obligatorio || comentario.isNotBlank()) {
                Text("Confirmar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Volver") } },
    )
}
