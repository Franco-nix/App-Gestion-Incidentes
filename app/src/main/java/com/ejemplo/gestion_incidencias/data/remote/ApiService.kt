package com.ejemplo.gestion_incidencias.data.remote

import com.ejemplo.gestion_incidencias.domain.Estado
import com.ejemplo.gestion_incidencias.domain.Prioridad
import com.ejemplo.gestion_incidencias.domain.Rol
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // ---- Auth ----
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @GET("auth/me")
    suspend fun me(): UsuarioDto

    // ---- Categorías ----
    @GET("categorias")
    suspend fun categorias(): List<CategoriaDto>

    // ---- Incidencias ----
    @GET("incidencias")
    suspend fun incidencias(
        @Query("estado") estado: Estado? = null,
        @Query("prioridad") prioridad: Prioridad? = null,
        @Query("categoriaId") categoriaId: Int? = null,
        @Query("sinAsignar") sinAsignar: Boolean? = null,
        @Query("q") q: String? = null,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20,
    ): Pagina<IncidenciaDto>

    @GET("incidencias/{id}")
    suspend fun incidencia(@Path("id") id: Int): IncidenciaDto

    @POST("incidencias")
    suspend fun crearIncidencia(@Body body: GuardarIncidenciaRequest): IncidenciaDto

    @PATCH("incidencias/{id}")
    suspend fun actualizarIncidencia(@Path("id") id: Int, @Body body: GuardarIncidenciaRequest): IncidenciaDto

    @PATCH("incidencias/{id}/asignar")
    suspend fun asignar(@Path("id") id: Int, @Body body: AsignarRequest): IncidenciaDto

    @PATCH("incidencias/{id}/estado")
    suspend fun cambiarEstado(@Path("id") id: Int, @Body body: CambiarEstadoRequest): IncidenciaDto

    @POST("incidencias/{id}/comentarios")
    suspend fun comentar(@Path("id") id: Int, @Body body: ComentarioRequest): SeguimientoDto

    // ---- Usuarios (admin) ----
    @GET("usuarios")
    suspend fun usuarios(@Query("rol") rol: Rol? = null): List<UsuarioDto>

    @GET("usuarios/{id}")
    suspend fun usuario(@Path("id") id: Int): UsuarioDto

    @GET("usuarios/tecnicos")
    suspend fun tecnicos(): List<TecnicoCargaDto>

    @POST("usuarios")
    suspend fun crearUsuario(@Body body: CrearUsuarioRequest): UsuarioDto

    @PATCH("usuarios/{id}")
    suspend fun actualizarUsuario(@Path("id") id: Int, @Body body: ActualizarUsuarioRequest): UsuarioDto

    // ---- Reportes (admin) ----
    @GET("reportes/resumen")
    suspend fun resumen(@Query("desde") desde: String?, @Query("hasta") hasta: String?): ResumenDto

    @GET("reportes/tendencia")
    suspend fun tendencia(
        @Query("desde") desde: String?,
        @Query("hasta") hasta: String?,
        @Query("agrupacion") agrupacion: String,
    ): TendenciaDto

    @GET("reportes/tecnicos")
    suspend fun reporteTecnicos(@Query("desde") desde: String?, @Query("hasta") hasta: String?): List<TecnicoReporteDto>

    @GET("reportes/tiempos")
    suspend fun tiempos(@Query("desde") desde: String?, @Query("hasta") hasta: String?): TiemposDto
}
