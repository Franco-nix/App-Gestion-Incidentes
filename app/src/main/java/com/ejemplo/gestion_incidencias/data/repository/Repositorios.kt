package com.ejemplo.gestion_incidencias.data.repository

import com.ejemplo.gestion_incidencias.data.remote.ActualizarUsuarioRequest
import com.ejemplo.gestion_incidencias.data.remote.ApiService
import com.ejemplo.gestion_incidencias.data.remote.AsignarRequest
import com.ejemplo.gestion_incidencias.data.remote.CambiarEstadoRequest
import com.ejemplo.gestion_incidencias.data.remote.ComentarioRequest
import com.ejemplo.gestion_incidencias.data.remote.CrearUsuarioRequest
import com.ejemplo.gestion_incidencias.data.remote.GuardarIncidenciaRequest
import com.ejemplo.gestion_incidencias.data.remote.LoginRequest
import com.ejemplo.gestion_incidencias.data.remote.UsuarioDto
import com.ejemplo.gestion_incidencias.data.session.SessionStore
import com.ejemplo.gestion_incidencias.domain.Estado
import com.ejemplo.gestion_incidencias.domain.Prioridad
import com.ejemplo.gestion_incidencias.domain.Rol
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json

class AuthRepository(private val api: ApiService, private val session: SessionStore, private val json: Json) {
    suspend fun login(email: String, password: String): Result<UsuarioDto> = apiCall(json) {
        val res = api.login(LoginRequest(email.trim().lowercase(), password))
        session.guardar(res.accessToken, res.usuario)
        res.usuario
    }

    suspend fun logout() = session.cerrar()
}

data class FiltroIncidencias(
    val estado: Estado? = null,
    val prioridad: Prioridad? = null,
    val q: String = "",
    val sinAsignar: Boolean = false,
)

class IncidenciaRepository(private val api: ApiService, private val json: Json) {
    suspend fun listar(f: FiltroIncidencias, page: Int, size: Int = 20) = apiCall(json) {
        api.incidencias(
            estado = f.estado,
            prioridad = f.prioridad,
            q = f.q.trim().ifBlank { null },
            sinAsignar = if (f.sinAsignar) true else null,
            page = page,
            size = size,
        )
    }

    suspend fun detalle(id: Int) = apiCall(json) { api.incidencia(id) }
    suspend fun categorias() = apiCall(json) { api.categorias() }
    suspend fun crear(req: GuardarIncidenciaRequest) = apiCall(json) { api.crearIncidencia(req) }
    suspend fun actualizar(id: Int, req: GuardarIncidenciaRequest) = apiCall(json) { api.actualizarIncidencia(id, req) }
    suspend fun asignar(id: Int, tecnicoId: Int, comentario: String?) =
        apiCall(json) { api.asignar(id, AsignarRequest(tecnicoId, comentario?.ifBlank { null })) }
    suspend fun cambiarEstado(id: Int, estado: Estado, comentario: String?) =
        apiCall(json) { api.cambiarEstado(id, CambiarEstadoRequest(estado, comentario?.trim()?.ifBlank { null })) }
    suspend fun comentar(id: Int, texto: String) = apiCall(json) { api.comentar(id, ComentarioRequest(texto.trim())) }
}

class UsuarioRepository(private val api: ApiService, private val json: Json) {
    suspend fun listar(rol: Rol? = null) = apiCall(json) { api.usuarios(rol) }
    suspend fun obtener(id: Int) = apiCall(json) { api.usuario(id) }
    suspend fun tecnicos() = apiCall(json) { api.tecnicos() }
    suspend fun crear(req: CrearUsuarioRequest) = apiCall(json) { api.crearUsuario(req) }
    suspend fun actualizar(id: Int, req: ActualizarUsuarioRequest) = apiCall(json) { api.actualizarUsuario(id, req) }
}

/** Todos los datos de la pantalla de reportes, cargados en paralelo. */
data class DatosReportes(
    val resumen: com.ejemplo.gestion_incidencias.data.remote.ResumenDto,
    val tendencia: com.ejemplo.gestion_incidencias.data.remote.TendenciaDto,
    val tecnicos: List<com.ejemplo.gestion_incidencias.data.remote.TecnicoReporteDto>,
    val tiempos: com.ejemplo.gestion_incidencias.data.remote.TiemposDto,
)

class ReporteRepository(private val api: ApiService, private val json: Json) {
    suspend fun cargar(desde: String?, hasta: String?, agrupacion: String) = apiCall(json) {
        coroutineScope {
            val resumen = async { api.resumen(desde, hasta) }
            val tendencia = async { api.tendencia(desde, hasta, agrupacion) }
            val tecnicos = async { api.reporteTecnicos(desde, hasta) }
            val tiempos = async { api.tiempos(desde, hasta) }
            DatosReportes(resumen.await(), tendencia.await(), tecnicos.await(), tiempos.await())
        }
    }
}
