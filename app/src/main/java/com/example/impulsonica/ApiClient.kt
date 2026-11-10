package com.example.impulsonica

import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object ApiClient {
    // IP pública de tu Máquina Virtual en Azure (o tu IP local si pruebas en red local)
    const val HOST_URL = "http://20.51.194.8:8000"
    const val API_URL = "$HOST_URL/api"
    const val DASHBOARD_URL = "$HOST_URL/api/dashboard"
    const val NOSQL_URL = "$HOST_URL/api/v1"

    fun guardarSesion(
        context: Context,
        token: String,
        refresh: String,
        rol: String,
        sqlId: Int,
        correo: String = ""
    ) {
        val prefs = context.getSharedPreferences("ImpulsoNicaSession", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("auth_token", token)
            .putString("refresh_token", refresh)
            .putString("user_type", rol.lowercase().trim())
            .putInt("usuario_sql_id", sqlId)
            .putString("usuario_correo", correo)
            .apply()
    }

    fun obtenerToken(context: Context): String? {
        return context.getSharedPreferences("ImpulsoNicaSession", Context.MODE_PRIVATE)
            .getString("auth_token", null)
    }

    fun obtenerRol(context: Context): String {
        return context.getSharedPreferences("ImpulsoNicaSession", Context.MODE_PRIVATE)
            .getString("user_type", "candidato") ?: "candidato"
    }

    fun obtenerUsuarioSqlId(context: Context): Int {
        return context.getSharedPreferences("ImpulsoNicaSession", Context.MODE_PRIVATE)
            .getInt("usuario_sql_id", -1)
    }

    fun cerrarSesion(context: Context) {
        context.getSharedPreferences("ImpulsoNicaSession", Context.MODE_PRIVATE).edit().clear()
            .apply()
        context.getSharedPreferences("SesionUsuario", Context.MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences("SesionEmpresa", Context.MODE_PRIVATE).edit().clear().apply()
    }

    suspend fun request(
        fullUrl: String,
        method: String = "GET",
        token: String? = null,
        jsonBody: JSONObject? = null
    ): Pair<Int, String> = withContext(Dispatchers.IO) {
        try {
            val url = URL(fullUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
                if (!token.isNullOrEmpty()) {
                    setRequestProperty("Authorization", "Bearer $token")
                }
                if (jsonBody != null && (method == "POST" || method == "PUT" || method == "PATCH")) {
                    doOutput = true
                    OutputStreamWriter(outputStream, "UTF-8").use { writer ->
                        writer.write(jsonBody.toString())
                        writer.flush()
                    }
                }
            }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = if (stream != null) {
                BufferedReader(InputStreamReader(stream, "UTF-8")).use { it.readText() }
            } else ""
            Pair(code, text)
        } catch (e: Exception) {
            Pair(500, JSONObject().put("error", "Error de red: ${e.localizedMessage}").toString())
        }
    }

    // =========================================================================
    // MÉTODOS DE TELEMETRÍA Y EVENTOS HACIA AZURE COSMOS DB (NoSQL)
    // =========================================================================

    /** 1. Registra vistas y clics en la colección RegistroActividad */
    suspend fun registrarTelemetriaNoSQL(
        tipoEvento: String,
        actorTipo: String,
        actorId: Int,
        objetivoTipo: String,
        objetivoId: Int,
        tiempoSegundos: Int = 15
    ) {
        val payload = JSONObject().apply {
            put("tipo_evento", tipoEvento)
            put(
                "actor",
                JSONObject().put("tipo", actorTipo).put("sql_id", if (actorId > 0) actorId else 0)
            )
            put("objetivo", JSONObject().put("tipo", objetivoTipo).put("sql_id", objetivoId))
            put("plataforma_origen", "App Movil")
            put("tiempo_en_pantalla_segundos", tiempoSegundos.coerceAtLeast(1))
        }
        request("$NOSQL_URL/telemetria/actividad/", "POST", null, payload)
    }

    /** 2. Registra búsquedas en la colección HistorialBusquedas */
    suspend fun registrarBusquedaNoSQL(
        usuarioSqlId: Int,
        termino: String,
        resultadosCount: Int,
        modalidadFiltro: String = "Todas",
        salarioMin: Int? = null,
        salarioMax: Int? = null
    ) {
        val filtros = JSONObject().apply {
            put("modalidad", modalidadFiltro)
            put("plataforma", "App Movil")
            if (salarioMin != null || salarioMax != null) {
                put("rango_salarial", JSONObject().apply {
                    put("min", salarioMin ?: 0)
                    put("max", salarioMax ?: 50000)
                })
            }
        }
        val payload = JSONObject().apply {
            if (usuarioSqlId > 0) put("usuario_sql_id", usuarioSqlId) else put(
                "usuario_sql_id",
                JSONObject.NULL
            )
            put("termino_principal", termino)
            put("filtros_aplicados", filtros)
            put("resultados_encontrados", resultadosCount)
        }
        request("$NOSQL_URL/historial-busqueda/", "POST", null, payload)
    }

    /** 3. Crea una notificación en tiempo real en la colección Notificaciones */
    suspend fun enviarNotificacionNoSQL(
        receptorSqlId: Int,
        tipoAlerta: String,
        titulo: String,
        mensaje: String,
        empleoSqlId: Int = -1,
        candidatoSqlId: Int = -1
    ) {
        if (receptorSqlId <= 0) return
        val extra = JSONObject().apply {
            if (empleoSqlId > 0) put("empleo_sql_id", empleoSqlId)
            if (candidatoSqlId > 0) put("candidato_sql_id", candidatoSqlId)
            put("urgencia", "Alta")
            put("origen", "App Movil")
        }
        val payload = JSONObject().apply {
            put("receptor_sql_id", receptorSqlId)
            put("tipo_alerta", tipoAlerta)
            put("titulo", titulo)
            put("mensaje", mensaje)
            put("data_extra", extra)
            put("leida", false)
        }
        request("$NOSQL_URL/notificaciones/", "POST", null, payload)
    }

    /** 4. Marca una notificación como leída en Cosmos DB */
    suspend fun marcarNotificacionLeidaNoSQL(notificacionId: String) {
        val payload = JSONObject().put("notificacion_id", notificacionId)
        request("$NOSQL_URL/notificaciones/", "PATCH", null, payload)
    }

    /** 4.1 Aceptar o Rechazar una postulación desde las notificaciones de Empresa */
    suspend fun gestionarNotificacionEmpresaNoSQL(
        notificacionId: String,
        accion: String, // "aceptar" o "rechazar"
        nombreEmpresa: String = "Casa Pellas"
    ): Boolean {
        val payload = JSONObject().apply {
            put("id", notificacionId)
            put("notificacion_id", notificacionId)
            put("accion", accion)
            put("nombre_empresa", nombreEmpresa)
        }
        val res = request(
            fullUrl = "$NOSQL_URL/notificaciones/",
            method = "PATCH",
            token = null,
            jsonBody = payload
        )
        return res.first in 200..299
    }

    /** 4.2 Eliminar una notificación de Azure Cosmos DB */
    suspend fun eliminarNotificacionNoSQL(notificacionId: String): Boolean {
        val res = request(
            fullUrl = "$NOSQL_URL/notificaciones/?id=$notificacionId",
            method = "DELETE",
            token = null,
            jsonBody = null
        )
        return res.first in 200..299
    }

    /** 5. Envía un reporte de seguridad / fraude a la colección FeedbackEIncidentes */
    suspend fun reportarIncidenteNoSQL(
        usuarioSqlId: Int,
        tipoReporte: String,
        descripcion: String,
        entidadTipo: String,
        entidadSqlId: Int
    ): Boolean {
        val metadata = JSONObject().apply {
            put("sistema_operativo", "Android ${android.os.Build.VERSION.RELEASE}")
            put("version_app", "2.0.0-Cloud")
            put("navegador", "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
        }
        val payload = JSONObject().apply {
            put("usuario_sql_id", usuarioSqlId)
            put("tipo_reporte", tipoReporte)
            put("descripcion", descripcion)
            put("entidad_tipo", entidadTipo)
            put("entidad_sql_id", entidadSqlId)
            put("estado_revision", "Pendiente")
            put("metadata", metadata)
        }
        val res = request(
            fullUrl = "$NOSQL_URL/feedback/",
            method = "POST",
            token = null,
            jsonBody = payload
        )
        return res.first in 200..299
    }
}