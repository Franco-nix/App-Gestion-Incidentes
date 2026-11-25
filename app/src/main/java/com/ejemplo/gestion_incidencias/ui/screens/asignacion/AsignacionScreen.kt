package com.ejemplo.gestion_incidencias.ui.screens.asignacion

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Engineering
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ejemplo.gestion_incidencias.data.remote.IncidenciaDto
import com.ejemplo.gestion_incidencias.data.remote.TecnicoCargaDto
import com.ejemplo.gestion_incidencias.data.repository.IncidenciaRepository
import com.ejemplo.gestion_incidencias.data.repository.UsuarioRepository
import com.ejemplo.gestion_incidencias.ui.common.BarraSuperior
import com.ejemplo.gestion_incidencias.ui.common.Carga
import com.ejemplo.gestion_incidencias.ui.common.fabrica
import com.ejemplo.gestion_incidencias.ui.screens.incidencias.ListaIncidencias
import com.ejemplo.gestion_incidencias.ui.screens.incidencias.ListaViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AsignacionUi(
    val tecnicos: Carga<List<TecnicoCargaDto>> = Carga.Cargando,
    val procesando: Boolean = false,
    val mensaje: String? = null,
)

/** Bandeja de incidencias sin técnico + operación de asignar. */
class AsignacionViewModel(
    private val incidencias: IncidenciaRepository,
    private val usuarios: UsuarioRepository,
) : ListaViewModel(incidencias, soloSinAsignar = true) {
    private val _extra = MutableStateFlow(AsignacionUi())
    val extra = _extra.asStateFlow()

    fun cargarTecnicos() {
        _extra.update { it.copy(tecnicos = Carga.Cargando) }
        viewModelScope.launch {
            usuarios.tecnicos()
                .onSuccess { t -> _extra.update { it.copy(tecnicos = Carga.Exito(t)) } }
                .onFailure { e -> _extra.update { it.copy(tecnicos = Carga.Error(e.message ?: "Error")) } }
        }
    }

    fun asignar(inc: IncidenciaDto, tecnicoId: Int, comentario: String, alTerminar: () -> Unit) {
        if (_extra.value.procesando) return
        _extra.update { it.copy(procesando = true) }
        viewModelScope.launch {
            incidencias.asignar(inc.id, tecnicoId, comentario)
                .onSuccess { r ->
                    quitar(inc.id)
                    _extra.update { it.copy(procesando = false, mensaje = "${r.codigo} asignada a ${r.tecnico?.nombre}") }
                    alTerminar()
                }
                .onFailure { e -> _extra.update { it.copy(procesando = false, mensaje = e.message) } }
        }
    }

    fun mensajeMostrado() = _extra.update { it.copy(mensaje = null) }
}

@Composable
fun AsignacionScreen(onAbrir: (Int) -> Unit, onPerfil: (() -> Unit)?) {
    val vm: AsignacionViewModel = viewModel(
        factory = fabrica { AsignacionViewModel(it.incidenciaRepository, it.usuarioRepository) },
    )
    val ui by vm.ui.collectAsStateWithLifecycle()
    val extra by vm.extra.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var seleccion by remember { mutableStateOf<IncidenciaDto?>(null) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.alReanudar() }
    LaunchedEffect(extra.mensaje) { extra.mensaje?.let { snackbar.showSnackbar(it); vm.mensajeMostrado() } }

    Scaffold(
        topBar = { BarraSuperior("Pendientes de asignar", onPerfil = onPerfil) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ListaIncidencias(
                ui = ui,
                onAbrir = onAbrir,
                onRefrescar = vm::refrescar,
                onCargarMas = vm::cargarMas,
                vacioTitulo = "¡Todo asignado!",
                vacioDetalle = "No hay incidencias esperando técnico.",
                accion = { inc ->
                    FilledTonalButton(
                        onClick = { seleccion = inc; vm.cargarTecnicos() },
                        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                    ) {
                        Icon(Icons.Outlined.Engineering, null, Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text("Asignar técnico")
                    }
                },
            )
        }
    }

    seleccion?.let { inc ->
        SelectorTecnicoSheet(
            titulo = "Asignar ${inc.codigo}",
            tecnicos = extra.tecnicos,
            tecnicoActualId = inc.tecnico?.id,
            procesando = extra.procesando,
            onDismiss = { seleccion = null },
            onConfirmar = { tecnicoId, comentario -> vm.asignar(inc, tecnicoId, comentario) { seleccion = null } },
        )
    }
}
