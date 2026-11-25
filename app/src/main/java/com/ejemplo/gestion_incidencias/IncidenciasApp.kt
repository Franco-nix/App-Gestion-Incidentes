package com.ejemplo.gestion_incidencias

import android.app.Application

class IncidenciasApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
