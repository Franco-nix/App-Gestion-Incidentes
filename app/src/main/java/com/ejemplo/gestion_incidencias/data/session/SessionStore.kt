package com.ejemplo.gestion_incidencias.data.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ejemplo.gestion_incidencias.data.remote.UsuarioDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "sesion")

sealed interface EstadoSesion {
    data object Cargando : EstadoSesion
    data object SinSesion : EstadoSesion
    data class Activa(val token: String, val usuario: UsuarioDto) : EstadoSesion
}

/**
 * Guarda el token y el usuario en DataStore. Mantiene una copia en memoria
 * para que el interceptor HTTP pueda leer el token sin bloquear.
 */
class SessionStore(
    private val context: Context,
    private val json: Json,
    private val scope: CoroutineScope,
) {
    private val keyToken = stringPreferencesKey("token")
    private val keyUsuario = stringPreferencesKey("usuario")

    private val _estado = MutableStateFlow<EstadoSesion>(EstadoSesion.Cargando)
    val estado: StateFlow<EstadoSesion> = _estado.asStateFlow()

    /** Mensaje a mostrar en el login cuando la sesión caducó. */
    private val _aviso = MutableStateFlow<String?>(null)
    val aviso: StateFlow<String?> = _aviso.asStateFlow()

    init {
        scope.launch {
            val prefs = context.dataStore.data.first()
            val token = prefs[keyToken]
            val usuario = prefs[keyUsuario]?.let { runCatching { json.decodeFromString<UsuarioDto>(it) }.getOrNull() }
            _estado.value = if (token != null && usuario != null) EstadoSesion.Activa(token, usuario) else EstadoSesion.SinSesion
        }
    }

    fun tokenActual(): String? = (_estado.value as? EstadoSesion.Activa)?.token

    suspend fun guardar(token: String, usuario: UsuarioDto) {
        context.dataStore.edit {
            it[keyToken] = token
            it[keyUsuario] = json.encodeToString(UsuarioDto.serializer(), usuario)
        }
        _aviso.value = null
        _estado.value = EstadoSesion.Activa(token, usuario)
    }

    suspend fun cerrar() {
        context.dataStore.edit { it.clear() }
        _estado.value = EstadoSesion.SinSesion
    }

    /** Llamado desde el interceptor (hilo de red) cuando el token deja de ser válido. */
    fun sesionExpirada() {
        if (_estado.value !is EstadoSesion.Activa) return
        _aviso.value = "Tu sesión expiró. Inicia sesión nuevamente."
        _estado.value = EstadoSesion.SinSesion
        scope.launch { context.dataStore.edit { it.clear() } }
    }
}
