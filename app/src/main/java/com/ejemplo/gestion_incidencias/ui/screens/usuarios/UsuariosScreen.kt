package com.ejemplo.gestion_incidencias.ui.screens.usuarios

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ejemplo.gestion_incidencias.data.remote.ActualizarUsuarioRequest
import com.ejemplo.gestion_incidencias.data.remote.UsuarioDto
import com.ejemplo.gestion_incidencias.data.repository.UsuarioRepository
import com.ejemplo.gestion_incidencias.domain.Rol
import com.ejemplo.gestion_incidencias.ui.common.BarraSuperior
import com.ejemplo.gestion_incidencias.ui.common.Carga
import com.ejemplo.gestion_incidencias.ui.common.CargandoBox
import com.ejemplo.gestion_incidencias.ui.common.MensajeError
import com.ejemplo.gestion_incidencias.ui.common.MensajeVacio
import com.ejemplo.gestion_incidencias.ui.common.fabrica
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UsuariosUi(
    val usuarios: Carga<List<UsuarioDto>> = Carga.Cargando,
    val rol: Rol? = null,
    val refrescando: Boolean = false,
    val mensaje: String? = null,
)

class UsuariosViewModel(private val repo: UsuarioRepository) : ViewModel() {
    private val _ui = MutableStateFlow(UsuariosUi())
    val ui = _ui.asStateFlow()

    fun cargar(indicador: Boolean = false) = viewModelScope.launch {
        _ui.update { it.copy(refrescando = indicador) }
        repo.listar(_ui.value.rol)
            .onSuccess { l -> _ui.update { it.copy(usuarios = Carga.Exito(l), refrescando = false) } }
            .onFailure { e -> _ui.update { it.copy(usuarios = Carga.Error(e.message ?: "Error"), refrescando = false) } }
    }

    fun rol(r: Rol?) {
        _ui.update { it.copy(rol = r, usuarios = Carga.Cargando) }
        cargar()
    }

    fun alternarActivo(u: UsuarioDto, activo: Boolean) = viewModelScope.launch {
        repo.actualizar(u.id, ActualizarUsuarioRequest(activo = activo))
            .onSuccess { act ->
                _ui.update { s ->
                    val lista = (s.usuarios as? Carga.Exito)?.datos?.map { if (it.id == act.id) act else it }
                    s.copy(
                        usuarios = lista?.let { Carga.Exito(it) } ?: s.usuarios,
                        mensaje = "${act.nombre} ${if (act.activo) "activado" else "desactivado"}",
                    )
                }
            }
            .onFailure { e -> _ui.update { it.copy(mensaje = e.message) } }
    }

    fun mensajeMostrado() = _ui.update { it.copy(mensaje = null) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsuariosScreen(onNuevo: () -> Unit, onEditar: (Int) -> Unit, onPerfil: (() -> Unit)?) {
    val vm: UsuariosViewModel = viewModel(factory = fabrica { UsuariosViewModel(it.usuarioRepository) })
    val ui by vm.ui.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.cargar() }
    LaunchedEffect(ui.mensaje) { ui.mensaje?.let { snackbar.showSnackbar(it); vm.mensajeMostrado() } }

    Scaffold(
        topBar = { BarraSuperior("Usuarios", onPerfil = onPerfil) },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onNuevo, icon = { Icon(Icons.Outlined.PersonAdd, null) }, text = { Text("Nuevo") })
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(selected = ui.rol == null, onClick = { vm.rol(null) }, label = { Text("Todos") })
                Rol.entries.forEach { r ->
                    FilterChip(selected = ui.rol == r, onClick = { vm.rol(r) }, label = { Text(r.etiqueta) })
                }
            }
            PullToRefreshBox(isRefreshing = ui.refrescando, onRefresh = { vm.cargar(true) }, modifier = Modifier.fillMaxSize()) {
                when (val c = ui.usuarios) {
                    Carga.Cargando -> CargandoBox()
                    is Carga.Error -> MensajeError(c.mensaje, { vm.cargar() })
                    is Carga.Exito -> if (c.datos.isEmpty()) MensajeVacio("Sin usuarios") else LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(c.datos, key = { it.id }) { u ->
                            Card(
                                onClick = { onEditar(u.id) },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                            ) {
                                ListItem(
                                    headlineContent = { Text(u.nombre) },
                                    supportingContent = {
                                        Text(listOfNotNull(u.rol.etiqueta, u.especialidad ?: u.area, u.email).joinToString(" · "))
                                    },
                                    leadingContent = { Iniciales(u) },
                                    trailingContent = {
                                        Switch(checked = u.activo, onCheckedChange = { vm.alternarActivo(u, it) })
                                    },
                                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Iniciales(u: UsuarioDto) {
    val color = when (u.rol) {
        Rol.ADMIN -> MaterialTheme.colorScheme.primaryContainer
        Rol.TECNICO -> MaterialTheme.colorScheme.tertiaryContainer
        Rol.USUARIO -> MaterialTheme.colorScheme.secondaryContainer
    }
    Surface(shape = CircleShape, color = if (u.activo) color else MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(40.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Text(u.nombre.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString(""))
        }
    }
}
