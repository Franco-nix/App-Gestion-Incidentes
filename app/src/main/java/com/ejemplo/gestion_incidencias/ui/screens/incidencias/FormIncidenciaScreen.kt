package com.ejemplo.gestion_incidencias.ui.screens.incidencias

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ejemplo.gestion_incidencias.data.remote.CategoriaDto
import com.ejemplo.gestion_incidencias.data.remote.GuardarIncidenciaRequest
import com.ejemplo.gestion_incidencias.data.repository.IncidenciaRepository
import com.ejemplo.gestion_incidencias.domain.Prioridad
import com.ejemplo.gestion_incidencias.ui.common.BarraSuperior
import com.ejemplo.gestion_incidencias.ui.common.CargandoBox
import com.ejemplo.gestion_incidencias.ui.common.MensajeError
import com.ejemplo.gestion_incidencias.ui.common.fabrica
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FormIncidenciaUi(
    val cargando: Boolean = true,
    val errorCarga: String? = null,
    val categorias: List<CategoriaDto> = emptyList(),
    val titulo: String = "",
    val descripcion: String = "",
    val ubicacion: String = "",
    val categoriaId: Int? = null,
    val prioridad: Prioridad = Prioridad.MEDIA,
    val intentoGuardar: Boolean = false,
    val guardando: Boolean = false,
    val error: String? = null,
    val guardadoId: Int? = null,
) {
    // Mismas reglas que valida el backend
    val errorTitulo get() = if (titulo.trim().length < 5) "Mínimo 5 caracteres" else null
    val errorDescripcion get() = if (descripcion.trim().length < 10) "Describe el problema (mínimo 10 caracteres)" else null
    val errorCategoria get() = if (categoriaId == null) "Selecciona una categoría" else null
    val valido get() = errorTitulo == null && errorDescripcion == null && errorCategoria == null
}

class FormIncidenciaViewModel(private val id: Int?, private val repo: IncidenciaRepository) : ViewModel() {
    private val _ui = MutableStateFlow(FormIncidenciaUi())
    val ui = _ui.asStateFlow()

    init { cargar() }

    fun cargar() = viewModelScope.launch {
        _ui.update { it.copy(cargando = true, errorCarga = null) }
        val cats = repo.categorias().getOrElse { e ->
            _ui.update { it.copy(cargando = false, errorCarga = e.message) }
            return@launch
        }
        if (id == null) {
            _ui.update { it.copy(cargando = false, categorias = cats) }
            return@launch
        }
        repo.detalle(id)
            .onSuccess { inc ->
                _ui.update {
                    it.copy(
                        cargando = false,
                        categorias = cats,
                        titulo = inc.titulo,
                        descripcion = inc.descripcion,
                        ubicacion = inc.ubicacion.orEmpty(),
                        categoriaId = inc.categoria.id,
                        prioridad = inc.prioridad,
                    )
                }
            }
            .onFailure { e -> _ui.update { it.copy(cargando = false, errorCarga = e.message) } }
    }

    fun titulo(v: String) = _ui.update { it.copy(titulo = v.take(150), error = null) }
    fun descripcion(v: String) = _ui.update { it.copy(descripcion = v, error = null) }
    fun ubicacion(v: String) = _ui.update { it.copy(ubicacion = v.take(150), error = null) }
    fun categoria(v: Int) = _ui.update { it.copy(categoriaId = v, error = null) }
    fun prioridad(v: Prioridad) = _ui.update { it.copy(prioridad = v) }

    fun guardar() {
        val s = _ui.value
        _ui.update { it.copy(intentoGuardar = true) }
        if (!s.valido || s.guardando) return
        _ui.update { it.copy(guardando = true, error = null) }
        val req = GuardarIncidenciaRequest(
            titulo = s.titulo.trim(),
            descripcion = s.descripcion.trim(),
            ubicacion = s.ubicacion.trim().ifBlank { null },
            categoriaId = s.categoriaId!!,
            prioridad = s.prioridad,
        )
        viewModelScope.launch {
            (if (id == null) repo.crear(req) else repo.actualizar(id, req))
                .onSuccess { inc -> _ui.update { it.copy(guardando = false, guardadoId = inc.id) } }
                .onFailure { e -> _ui.update { it.copy(guardando = false, error = e.message) } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormIncidenciaScreen(id: Int?, onVolver: (() -> Unit)?, onGuardado: (Int) -> Unit) {
    val vm: FormIncidenciaViewModel = viewModel(
        key = "form-incidencia-${id ?: "nueva"}",
        factory = fabrica { FormIncidenciaViewModel(id, it.incidenciaRepository) },
    )
    val ui by vm.ui.collectAsStateWithLifecycle()
    LaunchedEffect(ui.guardadoId) { ui.guardadoId?.let(onGuardado) }

    Scaffold(topBar = { BarraSuperior(if (id == null) "Registrar incidencia" else "Editar incidencia", onVolver = onVolver) }) { padding ->
        when {
            ui.cargando -> CargandoBox(Modifier.padding(padding))
            ui.errorCarga != null -> MensajeError(ui.errorCarga!!, { vm.cargar() }, Modifier.padding(padding))
            else -> Column(
                Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                val mostrar = ui.intentoGuardar
                OutlinedTextField(
                    value = ui.titulo,
                    onValueChange = vm::titulo,
                    label = { Text("Título *") },
                    placeholder = { Text("Ej.: La impresora no imprime") },
                    isError = mostrar && ui.errorTitulo != null,
                    supportingText = { if (mostrar) ui.errorTitulo?.let { Text(it) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = ui.descripcion,
                    onValueChange = vm::descripcion,
                    label = { Text("Descripción *") },
                    placeholder = { Text("¿Qué ocurre? ¿Desde cuándo? ¿Aparece algún mensaje de error?") },
                    isError = mostrar && ui.errorDescripcion != null,
                    supportingText = { if (mostrar) ui.errorDescripcion?.let { Text(it) } },
                    minLines = 4,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                SelectorCategoria(ui.categorias, ui.categoriaId, mostrar && ui.errorCategoria != null, vm::categoria)
                OutlinedTextField(
                    value = ui.ubicacion,
                    onValueChange = vm::ubicacion,
                    label = { Text("Ubicación") },
                    placeholder = { Text("Ej.: Pabellón A - Aula 101") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Column {
                    Text("Prioridad", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        Prioridad.entries.forEachIndexed { i, p ->
                            SegmentedButton(
                                selected = ui.prioridad == p,
                                onClick = { vm.prioridad(p) },
                                shape = SegmentedButtonDefaults.itemShape(i, Prioridad.entries.size),
                            ) { Text(p.etiqueta) }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        when (ui.prioridad) {
                            Prioridad.BAJA -> "No impide trabajar; puede esperar."
                            Prioridad.MEDIA -> "Afecta parcialmente el trabajo."
                            Prioridad.ALTA -> "Impide trabajar a una persona o área."
                            Prioridad.CRITICA -> "Afecta a toda la institución o a un servicio esencial."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (ui.error != null) {
                    Text(ui.error!!, color = MaterialTheme.colorScheme.error)
                }
                Button(
                    onClick = vm::guardar,
                    enabled = !ui.guardando,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                ) {
                    if (ui.guardando) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    else Text(if (id == null) "Registrar incidencia" else "Guardar cambios")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorCategoria(categorias: List<CategoriaDto>, seleccion: Int?, error: Boolean, onSeleccion: (Int) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = abierto, onExpandedChange = { abierto = it }) {
        OutlinedTextField(
            value = categorias.firstOrNull { it.id == seleccion }?.nombre.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Categoría *") },
            isError = error,
            supportingText = { if (error) Text("Selecciona una categoría") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = abierto) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            categorias.forEach { c ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(c.nombre)
                            c.descripcion?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    },
                    onClick = { onSeleccion(c.id); abierto = false },
                )
            }
        }
    }
}
