package com.ejemplo.gestion_incidencias.domain

/**
 * Textos de las acciones de seguimiento. Los permisos (qué transiciones se ofrecen)
 * los decide el backend y llegan en `acciones.transiciones`; aquí solo se presentan.
 */
object Acciones {
    fun etiqueta(actual: Estado, destino: Estado): String = when (destino) {
        Estado.EN_PROGRESO -> if (actual == Estado.RESUELTA) "Reabrir" else "Iniciar atención"
        Estado.RESUELTA -> "Marcar como resuelta"
        Estado.CERRADA -> "Confirmar y cerrar"
        Estado.CANCELADA -> "Cancelar incidencia"
        Estado.ASIGNADA -> "Asignar"
        Estado.ABIERTA -> "Abrir"
    }

    fun comentarioObligatorio(destino: Estado): Boolean = destino == Estado.RESUELTA

    fun pistaComentario(actual: Estado, destino: Estado): String = when {
        destino == Estado.RESUELTA -> "Describe la solución aplicada (obligatorio)"
        destino == Estado.EN_PROGRESO && actual == Estado.RESUELTA -> "¿Por qué se reabre? (opcional)"
        destino == Estado.CANCELADA -> "Motivo de la cancelación (opcional)"
        else -> "Comentario (opcional)"
    }

    /** Acciones destructivas o de cierre se muestran con menor énfasis. */
    fun esSecundaria(actual: Estado, destino: Estado): Boolean =
        destino == Estado.CANCELADA || (actual == Estado.RESUELTA && destino == Estado.EN_PROGRESO)
}
