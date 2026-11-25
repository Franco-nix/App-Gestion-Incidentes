package com.ejemplo.gestion_incidencias

import com.ejemplo.gestion_incidencias.domain.Rol
import com.ejemplo.gestion_incidencias.ui.navigation.Tab
import com.ejemplo.gestion_incidencias.ui.navigation.etiqueta
import com.ejemplo.gestion_incidencias.ui.navigation.tabsPara
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavegacionPorRolTest {
    @Test
    fun `admin ve asignacion, reportes y usuarios`() {
        assertEquals(listOf(Tab.INCIDENCIAS, Tab.ASIGNAR, Tab.REPORTES, Tab.USUARIOS), tabsPara(Rol.ADMIN))
    }

    @Test
    fun `tecnico no ve modulos administrativos`() {
        val tabs = tabsPara(Rol.TECNICO)
        assertFalse(Tab.REPORTES in tabs)
        assertFalse(Tab.ASIGNAR in tabs)
        assertFalse(Tab.USUARIOS in tabs)
        assertTrue(Tab.PERFIL in tabs)
    }

    @Test
    fun `usuario puede registrar incidencias`() {
        assertEquals(listOf(Tab.INCIDENCIAS, Tab.NUEVA, Tab.PERFIL), tabsPara(Rol.USUARIO))
    }

    @Test
    fun `la pestana de lista cambia de nombre segun el rol`() {
        assertEquals("Incidencias", Tab.INCIDENCIAS.etiqueta(Rol.ADMIN))
        assertEquals("Asignadas", Tab.INCIDENCIAS.etiqueta(Rol.TECNICO))
        assertEquals("Mis incidencias", Tab.INCIDENCIAS.etiqueta(Rol.USUARIO))
    }
}
