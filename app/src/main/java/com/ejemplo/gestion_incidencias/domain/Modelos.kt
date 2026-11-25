package com.ejemplo.gestion_incidencias.domain

import kotlinx.serialization.Serializable

/** Enumeraciones compartidas con la API (los nombres coinciden con los valores del backend). */

@Serializable
enum class Rol(val etiqueta: String) {
    ADMIN("Administrador"),
    TECNICO("Técnico"),
    USUARIO("Usuario"),
}

@Serializable
enum class Estado(val etiqueta: String) {
    ABIERTA("Abierta"),
    ASIGNADA("Asignada"),
    EN_PROGRESO("En progreso"),
    RESUELTA("Resuelta"),
    CERRADA("Cerrada"),
    CANCELADA("Cancelada"),
}

@Serializable
enum class Prioridad(val etiqueta: String) {
    BAJA("Baja"),
    MEDIA("Media"),
    ALTA("Alta"),
    CRITICA("Crítica"),
}

@Serializable
enum class TipoSeguimiento {
    CREACION,
    COMENTARIO,
    CAMBIO_ESTADO,
    ASIGNACION,
}
