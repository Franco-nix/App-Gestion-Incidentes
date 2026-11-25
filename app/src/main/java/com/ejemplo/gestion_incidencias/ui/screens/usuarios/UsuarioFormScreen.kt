package com.ejemplo.gestion_incidencias.ui.screens.usuarios

import android.util.Patterns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ejemplo.gestion_incidencias.data.remote.ActualizarUsuarioRequest
import com.ejemplo.gestion_incidencias.data.remote.CrearUsuarioRequest
import com.ejemplo.gestion_incidencias.data.repository.UsuarioRepository
import com.ejemplo.gestion_incidencias.domain.Rol
import com.ejemplo.gestion_incidencias.ui.common.BarraSuperior
import com.ejemplo.gestion_incidencias.ui.common.CargandoBox
import com.ejemplo.gestion_incidencias.ui.common.MensajeError
import com.ejemplo.gestion_incidencias.ui.common.fabrica
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UsuarioFormUi(
    val esNuevo: Boolean,
    val cargando: Boolean = !esNuevo,
    val errorCarga: String? = null,
    val nombre: String = "",
    val email: String = "",
    val password: String = "",
    val rol: Rol = Rol.USUARIO,
    val area: String = "",
    val especialidad: String = "",
    val activo: Boolean = true,
    val intento: Boolean = false,
    val guardando: Boolean = false,
    val error: String? = null,
    val guardado: Boolean = false,
) {
    val errorNombre get() = if (nombre.trim().length < 2) "Ingresa el nombre" else null
    val errorEmail get() = if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) "Correo inválido" else null
    val errorPassword get() = when {
        esNuevo && password.length < 6 -> "Mínimo 6 caracteres"
        !esNuevo && password.isNotEmpty() && password.length < 6 -> "Mínimo 6 caracteres"
        else -> null
    }
    val valido get() = errorNombre == null && errorEmail == null && errorPassword == null
}

class UsuarioFormViewModel(private val id: Int?, private val repo: UsuarioRepository) : ViewModel() {
    private val _ui = MutableStateFlow(UsuarioFormUi(esNuevo = id == null))
    val ui = _ui.asStateFlow()

    init { if (id != null) cargar() }

    fun cargar() = viewModelScope.launch {
        _ui.update { it.copy(cargando = true, errorCarga = null) }
        repo.obtener(id!!)
            .onSuccess { u ->
                _ui.update {
                    it.copy(
                        cargando = false, nombre = u.nombre, email = u.email, rol = u.rol,
                        area = u.area.orEmpty(), especialidad = u.especialidad.orEmpty(), activo = u.activo,
                    )
                }
            }
            .onFailure { e -> _ui.update { it.copy(cargando = false, errorCarga = e.message) } }
    }

    fun editar(f: (UsuarioFormUi) -> UsuarioFormUi) = _ui.update { f(it).copy(error = null) }

    fun guardar() {
        val s = _ui.value
        _ui.update { it.copy(intento = true) }
        if (!s.valido || s.guardando) return
        _ui.update { it.copy(guardando = true) }
        viewModelScope.launch {
            val especialidad = s.especialidad.trim().ifBlank { null }.takeIf { s.rol == Rol.TECNICO }
            val r = if (id == null) {
                repo.crear(
                    CrearUsuarioRequest(s.nombre.trim(), s.email.trim(), s.password, s.rol, s.area.trim().ifBlank { null }, especialidad),
                )
            } else {
                repo.actualizar(
                    id,
                    ActualizarUsuarioRequest(
                        nombre = s.nombre.trim(), email = s.email.trim(), password = s.password.ifEmpty { null },
                        rol = s.rol, area = s.area.trim(), especialidad = especialidad ?: "", activo = s.activo,
                    ),
                )
            }
            r.onSuccess { _ui.update { it.copy(guardando = false, guardado = true) } }
                .onFailure { e -> _ui.update { it.copy(guardando = false, error = e.message) } }
        }
    }
}

@Composable
fun UsuarioFormScreen(id: Int?, onVolver: () -> Unit) {
    val vm: UsuarioFormViewModel = viewModel(
        key = "usuario-form-${id ?: "nuevo"}",
        factory = fabrica { UsuarioFormViewModel(id, it.usuarioRepository) },
    )
    val ui by vm.ui.collectAsStateWithLifecycle()
    LaunchedEffect(ui.guardado) { if (ui.guardado) onVolver() }

    Scaffold(topBar = { BarraSuperior(if (id == null) "Nuevo usuario" else "Editar usuario", onVolver = onVolver) }) { padding ->
        when {
            ui.cargando -> CargandoBox(Modifier.padding(padding))
            ui.errorCarga != null -> MensajeError(ui.errorCarga!!, { vm.cargar() }, Modifier.padding(padding))
            else -> Column(
                Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val m = ui.intento
                OutlinedTextField(
                    ui.nombre, { v -> vm.editar { it.copy(nombre = v) } },
                    label = { Text("Nombre completo *") }, singleLine = true,
                    isError = m && ui.errorNombre != null, supportingText = { if (m) ui.errorNombre?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    ui.email, { v -> vm.editar { it.copy(email = v) } },
                    label = { Text("Correo *") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    isError = m && ui.errorEmail != null, supportingText = { if (m) ui.errorEmail?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    ui.password, { v -> vm.editar { it.copy(password = v) } },
                    label = { Text(if (ui.esNuevo) "Contraseña *" else "Nueva contraseña (dejar vacío para no cambiar)") },
                    singleLine = true, visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = m && ui.errorPassword != null, supportingText = { if (m) ui.errorPassword?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Rol", style = MaterialTheme.typography.labelLarge)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    Rol.entries.forEachIndexed { i, r ->
                        SegmentedButton(
                            selected = ui.rol == r,
                            onClick = { vm.editar { it.copy(rol = r) } },
                            shape = SegmentedButtonDefaults.itemShape(i, Rol.entries.size),
                        ) { Text(r.etiqueta) }
                    }
                }
                OutlinedTextField(
                    ui.area, { v -> vm.editar { it.copy(area = v) } },
                    label = { Text("Área / oficina") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                if (ui.rol == Rol.TECNICO) {
                    OutlinedTextField(
                        ui.especialidad, { v -> vm.editar { it.copy(especialidad = v) } },
                        label = { Text("Especialidad") }, placeholder = { Text("Ej.: Redes, Hardware") },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (!ui.esNuevo) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Usuario activo", modifier = Modifier.weight(1f))
                        Switch(checked = ui.activo, onCheckedChange = { v -> vm.editar { it.copy(activo = v) } })
                    }
                }
                ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Spacer(Modifier.height(4.dp))
                Button(onClick = vm::guardar, enabled = !ui.guardando, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                    if (ui.guardando) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    else Text(if (ui.esNuevo) "Crear usuario" else "Guardar cambios")
                }
            }
        }
    }
}
