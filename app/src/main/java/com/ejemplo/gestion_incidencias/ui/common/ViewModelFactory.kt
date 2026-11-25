package com.ejemplo.gestion_incidencias.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ejemplo.gestion_incidencias.AppContainer
import com.ejemplo.gestion_incidencias.IncidenciasApp

/** Crea una fábrica de ViewModel con acceso al contenedor de dependencias. */
inline fun <reified VM : ViewModel> fabrica(crossinline crear: CreationExtras.(AppContainer) -> VM) =
    viewModelFactory {
        initializer { crear((this[APPLICATION_KEY] as IncidenciasApp).container) }
    }
