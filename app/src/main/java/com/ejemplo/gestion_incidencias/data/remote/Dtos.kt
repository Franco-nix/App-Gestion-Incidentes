package com.ejemplo.gestion_incidencias.data.remote

import com.ejemplo.gestion_incidencias.domain.Estado
import com.ejemplo.gestion_incidencias.domain.Prioridad
import com.ejemplo.gestion_incidencias.domain.Rol
import com.ejemplo.gestion_incidencias.domain.TipoSeguimiento
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// ---------- Auth / usuarios ----------

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class LoginResponse(val accessToken: String, val usuario: UsuarioDto)

@Serializable
data class UsuarioDto(
    val id: Int,
    val nombre: String,
    val email: String,
    val rol: Rol,
    val area: String? = null,
    val especialidad: String? = null,
    val activo: Boolean = true,
)

@Serializable
data class CrearUsuarioRequest(
    val nombre: String,
    val email: String,
    val password: String,
    val rol: Rol,
    val area: String? = null,
    val especialidad: String? = null,
)

@Serializable
data class ActualizarUsuarioRequest(
    val nombre: String? = null,
    val email: String? = null,
    val password: String? = null,
    val rol: Rol? = null,
    val area: String? = null,
    val especialidad: String? = null,
    val activo: Boolean? = null,
)

@Serializable
data class TecnicoCargaDto(
    val id: Int,
    val nombre: String,
    val email: String,
    val especialidad: String? = null,
    val carga: Int,
)

// ---------- Incidencias ----------

@Serializable
data class CategoriaDto(val id: Int, val nombre: String, val descripcion: String? = null, val activo: Boolean = true)

@Serializable
data class AccionesDto(
    val transiciones: List<Estado> = emptyList(),
    val puedeAsignar: Boolean = false,
    val puedeEditar: Boolean = false,
    val puedeComentar: Boolean = false,
)

@Serializable
data class SeguimientoDto(
    val id: Int,
    val tipo: TipoSeguimiento,
    val estadoAnterior: Estado? = null,
    val estadoNuevo: Estado? = null,
    val comentario: String? = null,
    val createdAt: String,
    val usuario: UsuarioDto,
)

@Serializable
data class IncidenciaDto(
    val id: Int,
    val codigo: String,
    val titulo: String,
    val descripcion: String,
    val ubicacion: String? = null,
    val categoria: CategoriaDto,
    val prioridad: Prioridad,
    val estado: Estado,
    val reportadoPor: UsuarioDto,
    val tecnico: UsuarioDto? = null,
    val fechaCreacion: String,
    val fechaAsignacion: String? = null,
    val fechaResolucion: String? = null,
    val fechaCierre: String? = null,
    val updatedAt: String? = null,
    val seguimientos: List<SeguimientoDto> = emptyList(),
    val acciones: AccionesDto? = null,
)

@Serializable
data class Pagina<T>(val items: List<T>, val total: Int, val page: Int, val size: Int)

@Serializable
data class GuardarIncidenciaRequest(
    val titulo: String,
    val descripcion: String,
    val ubicacion: String? = null,
    val categoriaId: Int,
    val prioridad: Prioridad,
)

@Serializable
data class AsignarRequest(val tecnicoId: Int, val comentario: String? = null)

@Serializable
data class CambiarEstadoRequest(val estado: Estado, val comentario: String? = null)

@Serializable
data class ComentarioRequest(val comentario: String)

// ---------- Reportes ----------

@Serializable
data class ConteoDto(val clave: String, val total: Int)

@Serializable
data class ResumenDto(
    val total: Int,
    val pendientes: Int,
    val enCurso: Int,
    val resueltas: Int,
    val canceladas: Int,
    val criticasAbiertas: Int,
    val promedioHorasResolucion: Double? = null,
    val porEstado: List<ConteoDto>,
    val porPrioridad: List<ConteoDto>,
    val porCategoria: List<ConteoDto>,
)

@Serializable
data class PuntoTendenciaDto(val periodo: String, val creadas: Int, val resueltas: Int)

@Serializable
data class TendenciaDto(val agrupacion: String, val desde: String, val hasta: String, val puntos: List<PuntoTendenciaDto>)

@Serializable
data class TecnicoReporteDto(
    val tecnicoId: Int,
    val nombre: String,
    val especialidad: String? = null,
    val asignadas: Int,
    val enCurso: Int,
    val resueltas: Int,
    val promedioHorasResolucion: Double? = null,
)

@Serializable
data class TiempoDto(val clave: String, val promedioHoras: Double? = null, val resueltas: Int)

@Serializable
data class TiemposDto(val porPrioridad: List<TiempoDto>, val porCategoria: List<TiempoDto>)

/** Cuerpo de error de NestJS: `message` puede ser un texto o una lista de textos (validación). */
@Serializable
data class ErrorDto(val message: JsonElement? = null, val statusCode: Int? = null)
