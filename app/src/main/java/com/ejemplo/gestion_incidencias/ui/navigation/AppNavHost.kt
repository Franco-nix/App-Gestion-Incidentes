package com.ejemplo.gestion_incidencias.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.ejemplo.gestion_incidencias.data.remote.UsuarioDto
import com.ejemplo.gestion_incidencias.ui.screens.asignacion.AsignacionScreen
import com.ejemplo.gestion_incidencias.ui.screens.incidencias.DetalleScreen
import com.ejemplo.gestion_incidencias.ui.screens.incidencias.FormIncidenciaScreen
import com.ejemplo.gestion_incidencias.ui.screens.incidencias.ListaScreen
import com.ejemplo.gestion_incidencias.ui.screens.perfil.PerfilScreen
import com.ejemplo.gestion_incidencias.ui.screens.reportes.ReportesScreen
import com.ejemplo.gestion_incidencias.ui.screens.usuarios.UsuarioFormScreen
import com.ejemplo.gestion_incidencias.ui.screens.usuarios.UsuariosScreen
import kotlinx.serialization.Serializable

// ---------- Rutas tipadas ----------
@Serializable object ListaRoute
@Serializable object NuevaRoute
@Serializable object AsignarRoute
@Serializable object ReportesRoute
@Serializable object UsuariosRoute
@Serializable object PerfilRoute
@Serializable data class DetalleRoute(val id: Int)
@Serializable data class EditarIncidenciaRoute(val id: Int)
@Serializable data class UsuarioFormRoute(val id: Int = 0)

private fun Tab.ruta(): Any = when (this) {
    Tab.INCIDENCIAS -> ListaRoute
    Tab.NUEVA -> NuevaRoute
    Tab.ASIGNAR -> AsignarRoute
    Tab.REPORTES -> ReportesRoute
    Tab.USUARIOS -> UsuariosRoute
    Tab.PERFIL -> PerfilRoute
}

private fun Tab.icono(): ImageVector = when (this) {
    Tab.INCIDENCIAS -> Icons.AutoMirrored.Outlined.ListAlt
    Tab.NUEVA -> Icons.Outlined.AddCircleOutline
    Tab.ASIGNAR -> Icons.AutoMirrored.Outlined.Assignment
    Tab.REPORTES -> Icons.Outlined.BarChart
    Tab.USUARIOS -> Icons.Outlined.Group
    Tab.PERFIL -> Icons.Outlined.Person
}

@Composable
fun AppNavHost(usuario: UsuarioDto, onCerrarSesion: () -> Unit) {
    val nav = rememberNavController()
    val tabs = tabsPara(usuario.rol)
    val entrada by nav.currentBackStackEntryAsState()
    val destino = entrada?.destination
    val enPestana = tabs.any { t -> destino?.hierarchy?.any { it.hasRoute(t.ruta()::class) } == true }

    fun irAPestana(tab: Tab) = nav.navigate(tab.ruta()) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
    val abrirPerfil: (() -> Unit)? = if (Tab.PERFIL in tabs) null else ({ nav.navigate(PerfilRoute) })

    Scaffold(
        // Cada pantalla gestiona su propia barra superior e insets
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (enPestana) {
                NavigationBar {
                    tabs.forEach { tab ->
                        val seleccionada = destino?.hierarchy?.any { it.hasRoute(tab.ruta()::class) } == true
                        NavigationBarItem(
                            selected = seleccionada,
                            onClick = { irAPestana(tab) },
                            icon = { Icon(tab.icono(), contentDescription = null) },
                            label = { Text(tab.etiqueta(usuario.rol)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = ListaRoute,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
        ) {
            composable<ListaRoute> {
                ListaScreen(
                    usuario = usuario,
                    onAbrir = { nav.navigate(DetalleRoute(it)) },
                    onNueva = { nav.navigate(NuevaRoute) },
                    onPerfil = abrirPerfil,
                )
            }
            composable<NuevaRoute> {
                FormIncidenciaScreen(
                    id = null,
                    onVolver = if (Tab.NUEVA in tabs) null else ({ nav.popBackStack() }),
                    onGuardado = { id ->
                        nav.navigate(DetalleRoute(id)) {
                            popUpTo(ListaRoute)
                        }
                    },
                )
            }
            composable<EditarIncidenciaRoute> { e ->
                FormIncidenciaScreen(
                    id = e.toRoute<EditarIncidenciaRoute>().id,
                    onVolver = { nav.popBackStack() },
                    onGuardado = { nav.popBackStack() },
                )
            }
            composable<DetalleRoute> { e ->
                DetalleScreen(
                    id = e.toRoute<DetalleRoute>().id,
                    onVolver = { nav.popBackStack() },
                    onEditar = { nav.navigate(EditarIncidenciaRoute(it)) },
                )
            }
            composable<AsignarRoute> {
                AsignacionScreen(onAbrir = { nav.navigate(DetalleRoute(it)) }, onPerfil = abrirPerfil)
            }
            composable<ReportesRoute> { ReportesScreen(onPerfil = abrirPerfil) }
            composable<UsuariosRoute> {
                UsuariosScreen(
                    onNuevo = { nav.navigate(UsuarioFormRoute()) },
                    onEditar = { nav.navigate(UsuarioFormRoute(it)) },
                    onPerfil = abrirPerfil,
                )
            }
            composable<UsuarioFormRoute> { e ->
                UsuarioFormScreen(
                    id = e.toRoute<UsuarioFormRoute>().id.takeIf { it > 0 },
                    onVolver = { nav.popBackStack() },
                )
            }
            composable<PerfilRoute> {
                PerfilScreen(
                    usuario = usuario,
                    onCerrarSesion = onCerrarSesion,
                    onVolver = if (Tab.PERFIL in tabs) null else ({ nav.popBackStack() }),
                )
            }
        }
    }
}
