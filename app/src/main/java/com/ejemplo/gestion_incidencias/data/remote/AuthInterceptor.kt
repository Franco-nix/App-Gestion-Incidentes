package com.ejemplo.gestion_incidencias.data.remote

import com.ejemplo.gestion_incidencias.data.session.SessionStore
import okhttp3.Interceptor
import okhttp3.Response

/** Agrega el token JWT a cada petición y cierra la sesión si la API responde 401. */
class AuthInterceptor(private val session: SessionStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = session.tokenActual()
        val request = if (token != null && original.header("Authorization") == null) {
            original.newBuilder().header("Authorization", "Bearer $token").build()
        } else {
            original
        }
        val response = chain.proceed(request)
        val esLogin = original.url.encodedPath.endsWith("/auth/login")
        if (response.code == 401 && token != null && !esLogin) {
            session.sesionExpirada()
        }
        return response
    }
}
