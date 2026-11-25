package com.ejemplo.gestion_incidencias.data.repository

import com.ejemplo.gestion_incidencias.data.remote.ErrorDto
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import retrofit2.HttpException
import java.io.IOException

/** Ejecuta una llamada a la API y traduce los errores a mensajes legibles en español. */
suspend fun <T> apiCall(json: Json, block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        Result.failure(ApiException(mensajeHttp(json, e), e.code()))
    } catch (e: IOException) {
        Result.failure(ApiException("No se pudo conectar con el servidor. Verifica tu conexión y que la API esté en ejecución.", null))
    } catch (e: Exception) {
        Result.failure(ApiException(e.message ?: "Error inesperado", null))
    }

class ApiException(message: String, val codigo: Int?) : Exception(message)

private fun mensajeHttp(json: Json, e: HttpException): String {
    val cuerpo = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
    val delServidor = cuerpo?.let {
        runCatching {
            when (val m = json.decodeFromString<ErrorDto>(it).message) {
                is JsonArray -> m.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }.joinToString("\n")
                is JsonPrimitive -> m.contentOrNull
                else -> null
            }
        }.getOrNull()
    }
    return when {
        !delServidor.isNullOrBlank() && e.code() != 403 -> delServidor
        e.code() == 401 -> "Sesión no válida"
        e.code() == 403 -> delServidor?.takeIf { it != "Forbidden resource" } ?: "No tienes permisos para esta acción"
        e.code() == 404 -> "Recurso no encontrado"
        e.code() >= 500 -> "Error del servidor (${e.code()})"
        else -> "Error ${e.code()}"
    }
}
