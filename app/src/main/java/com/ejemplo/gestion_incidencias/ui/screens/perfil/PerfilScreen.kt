package com.ejemplo.gestion_incidencias.ui.screens.perfil

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ejemplo.gestion_incidencias.BuildConfig
import com.ejemplo.gestion_incidencias.data.remote.UsuarioDto
import com.ejemplo.gestion_incidencias.ui.common.BarraSuperior
import com.ejemplo.gestion_incidencias.ui.common.DatoFila

@Composable
fun PerfilScreen(usuario: UsuarioDto, onCerrarSesion: () -> Unit, onVolver: (() -> Unit)?) {
    var confirmar by remember { mutableStateOf(false) }
    Scaffold(topBar = { BarraSuperior("Mi perfil", onVolver = onVolver) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape, modifier = Modifier.size(88.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        usuario.nombre.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString(""),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(usuario.nombre, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(usuario.rol.etiqueta, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(24.dp))
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    DatoFila("Correo", usuario.email, Icons.Outlined.Email)
                    DatoFila("Rol", usuario.rol.etiqueta, Icons.Outlined.Badge)
                    DatoFila("Área", usuario.area ?: "—", Icons.Outlined.Business)
                    if (usuario.especialidad != null) DatoFila("Especialidad", usuario.especialidad, Icons.Outlined.Build)
                }
            }
            Spacer(Modifier.height(24.dp))
            OutlinedButton(
                onClick = { confirmar = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.AutoMirrored.Outlined.Logout, null)
                Spacer(Modifier.size(8.dp))
                Text("Cerrar sesión")
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Servidor: ${BuildConfig.BASE_URL}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (confirmar) {
        AlertDialog(
            onDismissRequest = { confirmar = false },
            title = { Text("¿Cerrar sesión?") },
            confirmButton = { TextButton(onClick = onCerrarSesion) { Text("Cerrar sesión") } },
            dismissButton = { TextButton(onClick = { confirmar = false }) { Text("Cancelar") } },
        )
    }
}
