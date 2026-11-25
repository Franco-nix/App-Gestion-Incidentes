package com.ejemplo.gestion_incidencias

import android.content.Context
import com.ejemplo.gestion_incidencias.data.remote.ApiService
import com.ejemplo.gestion_incidencias.data.remote.AuthInterceptor
import com.ejemplo.gestion_incidencias.data.repository.AuthRepository
import com.ejemplo.gestion_incidencias.data.repository.IncidenciaRepository
import com.ejemplo.gestion_incidencias.data.repository.ReporteRepository
import com.ejemplo.gestion_incidencias.data.repository.UsuarioRepository
import com.ejemplo.gestion_incidencias.data.session.SessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/** Inyección de dependencias manual: un único contenedor por aplicación. */
class AppContainer(context: Context) {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    val session = SessionStore(context.applicationContext, json, appScope)

    private val okHttp = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(session))
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
            },
        )
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val api: ApiService = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(okHttp)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(ApiService::class.java)

    val authRepository = AuthRepository(api, session, json)
    val incidenciaRepository = IncidenciaRepository(api, json)
    val usuarioRepository = UsuarioRepository(api, json)
    val reporteRepository = ReporteRepository(api, json)
}
