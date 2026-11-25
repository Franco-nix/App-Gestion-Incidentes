package com.ejemplo.gestion_incidencias

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ejemplo.gestion_incidencias.data.session.EstadoSesion
import com.ejemplo.gestion_incidencias.ui.common.CargandoBox
import com.ejemplo.gestion_incidencias.ui.navigation.AppNavHost
import com.ejemplo.gestion_incidencias.ui.screens.login.LoginScreen
import com.ejemplo.gestion_incidencias.ui.theme.Gestion_IncidenciasTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as IncidenciasApp).container
        setContent {
            Gestion_IncidenciasTheme {
                val sesion by container.session.estado.collectAsStateWithLifecycle()
                val aviso by container.session.aviso.collectAsStateWithLifecycle()
                val scope = rememberCoroutineScope()
                when (val s = sesion) {
                    EstadoSesion.Cargando -> CargandoBox()
                    EstadoSesion.SinSesion -> LoginScreen(aviso)
                    // key: al cambiar de usuario se descarta toda la navegación y ViewModels anteriores
                    is EstadoSesion.Activa -> key(s.usuario.id) {
                        AppNavHost(
                            usuario = s.usuario,
                            onCerrarSesion = { scope.launch { container.authRepository.logout() } },
                        )
                    }
                }
            }
        }
    }
}
