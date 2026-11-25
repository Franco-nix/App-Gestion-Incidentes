package com.ejemplo.gestion_incidencias.ui.navigation

import com.ejemplo.gestion_incidencias.domain.Rol

/** Pestañas de la barra inferior. Lógica pura para poder probarla sin Android. */
enum class Tab { INCIDENCIAS, NUEVA, ASIGNAR, REPORTES, USUARIOS, PERFIL }

fun tabsPara(rol: Rol): List<Tab> = when (rol) {
    Rol.ADMIN -> listOf(Tab.INCIDENCIAS, Tab.ASIGNAR, Tab.REPORTES, Tab.USUARIOS)
    Rol.TECNICO -> listOf(Tab.INCIDENCIAS, Tab.PERFIL)
    Rol.USUARIO -> listOf(Tab.INCIDENCIAS, Tab.NUEVA, Tab.PERFIL)
}

fun Tab.etiqueta(rol: Rol): String = when (this) {
    Tab.INCIDENCIAS -> when (rol) {
        Rol.ADMIN -> "Incidencias"
        Rol.TECNICO -> "Asignadas"
        Rol.USUARIO -> "Mis incidencias"
    }
    Tab.NUEVA -> "Nueva"
    Tab.ASIGNAR -> "Asignar"
    Tab.REPORTES -> "Reportes"
    Tab.USUARIOS -> "Usuarios"
    Tab.PERFIL -> "Perfil"
}

/** Título de la lista principal según el rol. */
fun tituloLista(rol: Rol): String = when (rol) {
    Rol.ADMIN -> "Todas las incidencias"
    Rol.TECNICO -> "Asignadas a mí"
    Rol.USUARIO -> "Mis incidencias"
}
