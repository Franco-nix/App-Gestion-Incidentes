package com.ejemplo.gestion_incidencias

import com.ejemplo.gestion_incidencias.ui.common.Formatos
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class FormatosTest {
    private val lima = ZoneId.of("America/Lima")

    @Test
    fun `fecha ISO UTC se muestra en hora local`() {
        assertEquals("23/11/2026 21:30", Formatos.fechaHora("2026-11-24T02:30:00.000Z", lima))
        assertEquals("—", Formatos.fechaHora(null, lima))
        assertEquals("—", Formatos.fechaHora("no-es-fecha", lima))
    }

    @Test
    fun `tiempo relativo`() {
        val ahora = Instant.parse("2026-11-24T12:00:00Z")
        assertEquals("ahora", Formatos.relativo("2026-11-24T11:59:30Z", ahora, lima))
        assertEquals("hace 15 min", Formatos.relativo("2026-11-24T11:45:00Z", ahora, lima))
        assertEquals("hace 3 h", Formatos.relativo("2026-11-24T09:00:00Z", ahora, lima))
        assertEquals("hace 2 d", Formatos.relativo("2026-11-22T12:00:00Z", ahora, lima))
        assertEquals("01/11/2026", Formatos.relativo("2026-11-01T17:00:00Z", ahora, lima))
    }

    @Test
    fun `horas de resolucion legibles`() {
        assertEquals("30 min", Formatos.horas(0.5))
        assertEquals("5.3 h", Formatos.horas(5.25))
        assertEquals("2 d", Formatos.horas(48.0))
        assertEquals("2 d 1 h", Formatos.horas(49.2))
        assertEquals("—", Formatos.horas(null))
    }

    @Test
    fun `porcentaje sin division por cero`() {
        assertEquals("0 %", Formatos.porcentaje(3, 0))
        assertEquals("33 %", Formatos.porcentaje(1, 3))
    }
}
