package com.ejemplo.gestion_incidencias

import com.ejemplo.gestion_incidencias.domain.Acciones
import com.ejemplo.gestion_incidencias.domain.Estado
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccionesTest {
    @Test
    fun `EN_PROGRESO se presenta como iniciar o reabrir segun el estado actual`() {
        assertEquals("Iniciar atención", Acciones.etiqueta(Estado.ASIGNADA, Estado.EN_PROGRESO))
        assertEquals("Reabrir", Acciones.etiqueta(Estado.RESUELTA, Estado.EN_PROGRESO))
    }

    @Test
    fun `resolver exige comentario y el resto no`() {
        assertTrue(Acciones.comentarioObligatorio(Estado.RESUELTA))
        assertFalse(Acciones.comentarioObligatorio(Estado.CERRADA))
        assertFalse(Acciones.comentarioObligatorio(Estado.CANCELADA))
    }

    @Test
    fun `cancelar y reabrir son acciones secundarias`() {
        assertTrue(Acciones.esSecundaria(Estado.ABIERTA, Estado.CANCELADA))
        assertTrue(Acciones.esSecundaria(Estado.RESUELTA, Estado.EN_PROGRESO))
        assertFalse(Acciones.esSecundaria(Estado.RESUELTA, Estado.CERRADA))
        assertFalse(Acciones.esSecundaria(Estado.EN_PROGRESO, Estado.RESUELTA))
    }
}
