package com.ejemplo.gestion_incidencias.ui.screens.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ejemplo.gestion_incidencias.BuildConfig
import com.ejemplo.gestion_incidencias.data.repository.AuthRepository
import com.ejemplo.gestion_incidencias.ui.common.fabrica
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val cargando: Boolean = false,
    val error: String? = null,
)

class LoginViewModel(private val repo: AuthRepository) : ViewModel() {
    private val _ui = MutableStateFlow(LoginUiState())
    val ui = _ui.asStateFlow()

    fun email(v: String) = _ui.update { it.copy(email = v, error = null) }
    fun password(v: String) = _ui.update { it.copy(password = v, error = null) }
    fun rellenar(email: String, password: String) = _ui.update { it.copy(email = email, password = password, error = null) }

    fun ingresar() {
        val s = _ui.value
        if (s.email.isBlank() || s.password.isBlank()) {
            _ui.update { it.copy(error = "Ingresa tu correo y contraseña") }
            return
        }
        _ui.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            // Si tiene éxito, SessionStore cambia de estado y la app navega sola
            repo.login(s.email, s.password).onFailure { e ->
                _ui.update { it.copy(cargando = false, error = e.message) }
            }
        }
    }
}

@Composable
fun LoginScreen(aviso: String?) {
    val vm: LoginViewModel = viewModel(factory = fabrica { LoginViewModel(it.authRepository) })
    val ui by vm.ui.collectAsStateWithLifecycle()
    var verPassword by remember { mutableStateOf(false) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Box(
            Modifier.fillMaxSize().systemBarsPadding().imePadding().verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier.widthIn(max = 420.dp).fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.size(72.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.SupportAgent,
                            null,
                            Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Gestión de Incidencias", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "Mesa de ayuda institucional",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(32.dp))

                if (aviso != null && ui.error == null) {
                    Text(aviso, color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                }

                OutlinedTextField(
                    value = ui.email,
                    onValueChange = vm::email,
                    label = { Text("Correo institucional") },
                    leadingIcon = { Icon(Icons.Outlined.Email, null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = ui.password,
                    onValueChange = vm::password,
                    label = { Text("Contraseña") },
                    leadingIcon = { Icon(Icons.Outlined.Lock, null) },
                    trailingIcon = {
                        IconButton(onClick = { verPassword = !verPassword }) {
                            Icon(
                                if (verPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                if (verPassword) "Ocultar contraseña" else "Mostrar contraseña",
                            )
                        }
                    },
                    visualTransformation = if (verPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { vm.ingresar() }),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (ui.error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(ui.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = vm::ingresar,
                    enabled = !ui.cargando,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                ) {
                    if (ui.cargando) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    else Text("Ingresar")
                }

                if (BuildConfig.DEBUG) {
                    Spacer(Modifier.height(32.dp))
                    Text(
                        "Cuentas de demostración",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(onClick = { vm.rellenar("admin@inst.edu", "Admin123*") }, label = { Text("Admin") })
                        AssistChip(onClick = { vm.rellenar("carlos.ruiz@inst.edu", "Tecnico123*") }, label = { Text("Técnico") })
                        AssistChip(onClick = { vm.rellenar("maria.lopez@inst.edu", "Usuario123*") }, label = { Text("Usuario") })
                    }
                }
            }
        }
    }
}
