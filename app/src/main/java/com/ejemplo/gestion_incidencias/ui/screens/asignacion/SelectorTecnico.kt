package com.ejemplo.gestion_incidencias.ui.screens.asignacion

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Engineering
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ejemplo.gestion_incidencias.data.remote.TecnicoCargaDto
import com.ejemplo.gestion_incidencias.ui.common.Carga

/** Hoja inferior para elegir técnico; muestra la carga actual para repartir el trabajo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorTecnicoSheet(
    titulo: String,
    tecnicos: Carga<List<TecnicoCargaDto>>,
    tecnicoActualId: Int?,
    procesando: Boolean,
    onDismiss: () -> Unit,
    onConfirmar: (tecnicoId: Int, comentario: String) -> Unit,
) {
    val estado = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var elegido by remember { mutableStateOf<Int?>(null) }
    var comentario by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = estado) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 16.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp))
            Text(
                "La carga indica cuántas incidencias tiene en curso cada técnico.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
            )
            when (tecnicos) {
                Carga.Cargando -> Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is Carga.Error -> Text(
                    tecnicos.mensaje,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(24.dp),
                )
                is Carga.Exito -> {
                    if (tecnicos.datos.isEmpty()) {
                        Text("No hay técnicos activos.", modifier = Modifier.padding(24.dp))
                    }
                    LazyColumn(Modifier.heightIn(max = 360.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
                        items(tecnicos.datos, key = { it.id }) { t ->
                            val esActual = t.id == tecnicoActualId
                            ListItem(
                                headlineContent = { Text(t.nombre + if (esActual) " (actual)" else "") },
                                supportingContent = { Text(t.especialidad ?: t.email) },
                                leadingContent = {
                                    RadioButton(selected = elegido == t.id, onClick = { elegido = t.id }, enabled = !esActual)
                                },
                                trailingContent = {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Badge(
                                            containerColor = when {
                                                t.carga == 0 -> MaterialTheme.colorScheme.tertiaryContainer
                                                t.carga >= 4 -> MaterialTheme.colorScheme.errorContainer
                                                else -> MaterialTheme.colorScheme.secondaryContainer
                                            },
                                        ) { Text("${t.carga}", modifier = Modifier.padding(horizontal = 4.dp)) }
                                        Text("en curso", style = MaterialTheme.typography.labelSmall)
                                    }
                                },
                                colors = ListItemDefaults.colors(),
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .clickable(enabled = !esActual) { elegido = t.id },
                            )
                        }
                    }
                }
            }
            OutlinedTextField(
                value = comentario,
                onValueChange = { comentario = it },
                label = { Text("Indicaciones para el técnico (opcional)") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                maxLines = 3,
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { elegido?.let { onConfirmar(it, comentario) } },
                enabled = elegido != null && !procesando,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(48.dp),
            ) {
                if (procesando) CircularProgressIndicator(Modifier.height(20.dp), strokeWidth = 2.dp)
                else {
                    Icon(Icons.Outlined.Engineering, null)
                    Spacer(Modifier.padding(4.dp))
                    Text("Asignar")
                }
            }
        }
    }
}
