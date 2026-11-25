package com.ejemplo.gestion_incidencias.ui.common

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/** Utilidades de formato puras (sin dependencias de Android) para poder probarlas. */
object Formatos {
    private val LOCALE = Locale.forLanguageTag("es-PE")
    private val FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", LOCALE)
    private val DIA_MES = DateTimeFormatter.ofPattern("d MMM", LOCALE)
    private val MES_ANIO = DateTimeFormatter.ofPattern("MMM yy", LOCALE)

    fun fechaHora(iso: String?, zona: ZoneId = ZoneId.systemDefault()): String =
        iso?.let { runCatching { FECHA_HORA.format(Instant.parse(it).atZone(zona)) }.getOrNull() } ?: "—"

    /** "hace 5 min", "hace 3 h", "hace 2 d" o la fecha si es antigua. */
    fun relativo(iso: String?, ahora: Instant = Instant.now(), zona: ZoneId = ZoneId.systemDefault()): String {
        val instante = iso?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return "—"
        val d = Duration.between(instante, ahora)
        return when {
            d.isNegative || d.toMinutes() < 1 -> "ahora"
            d.toMinutes() < 60 -> "hace ${d.toMinutes()} min"
            d.toHours() < 24 -> "hace ${d.toHours()} h"
            d.toDays() < 7 -> "hace ${d.toDays()} d"
            else -> FECHA_HORA.format(instante.atZone(zona)).substring(0, 10)
        }
    }

    /** 0.5 -> "30 min", 5.25 -> "5.3 h", 49 -> "2 d 1 h". */
    fun horas(h: Double?): String {
        if (h == null) return "—"
        return when {
            h < 1 -> "${(h * 60).roundToInt()} min"
            h < 24 -> "${"%.1f".format(Locale.US, h)} h"
            else -> {
                val total = h.roundToInt()
                val dias = total / 24
                val resto = total % 24
                if (resto == 0) "$dias d" else "$dias d $resto h"
            }
        }
    }

    /** Etiqueta corta del eje X de la tendencia. */
    fun periodo(fechaIso: String, agrupacion: String): String = runCatching {
        val f = LocalDate.parse(fechaIso)
        if (agrupacion == "mes") MES_ANIO.format(f) else DIA_MES.format(f)
    }.getOrDefault(fechaIso)

    fun porcentaje(parte: Int, total: Int): String =
        if (total == 0) "0 %" else "${(parte * 100.0 / total).roundToInt()} %"
}
