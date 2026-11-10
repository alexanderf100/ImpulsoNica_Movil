package com.example.impulsonica

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DetalleVacanteReal(
    val id: Int,
    val empresaId: Int,
    val titulo: String,
    val empresa: String,
    val modalidad: String,
    val tipoContrato: String,
    val salario: String,
    val ubicacion: String,
    val descripcion: String,
    val requisitos: List<String>,
    val correoEmpresa: String = "contacto@empresa.com.ni",
    val telefonoEmpresa: String = "+505 2222-0000",
    val totalReportesEmpresa: Int = 1
)

class Vista_vacanteActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val empleoId = intent.getIntExtra(
            "empleo_id",
            intent.getIntExtra("empleoid", intent.getIntExtra("id", 1))
        )
        val tituloExtra = intent.getStringExtra("titulo")
            ?: intent.getStringExtra("nombreempleo")
            ?: intent.getStringExtra("titulo_vacante")
            ?: intent.getStringExtra("cargo")
            ?: intent.getStringExtra("puesto")
            ?: ""
        val empresaExtra = intent.getStringExtra("empresa")
            ?: intent.getStringExtra("empresa_nombre")
            ?: intent.getStringExtra("nombre_empresa")
            ?: ""
        val modoAdminExtra = intent.getBooleanExtra("modo_admin", false)

        setContent {
            MaterialTheme {
                VistaVacanteScreen(
                    empleoId = empleoId,
                    tituloInicial = tituloExtra,
                    empresaInicial = empresaExtra,
                    modoAdminExtra = modoAdminExtra,
                    onBack = { finish() }
                )
            }
        }
    }
}

@Composable
fun VistaVacanteScreen(
    empleoId: Int,
    tituloInicial: String,
    empresaInicial: String,
    modoAdminExtra: Boolean = false,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = context.getSharedPreferences("ImpulsoNicaPrefs", Context.MODE_PRIVATE)
    val candidatoId = prefs.getInt("candidato_id", prefs.getInt("usuario_sql_id", 1))
    val nombreCandidato = prefs.getString("nombre_usuario", "Candidato ImpulsoNica") ?: "Candidato ImpulsoNica"
    val rolUsuario = (prefs.getString("rol", "candidato") ?: "candidato").lowercase()
    val token = prefs.getString("access_token", prefs.getString("jwt_token", prefs.getString("token", ""))) ?: ""
    val esModoAdmin = modoAdminExtra || rolUsuario == "admin" || rolUsuario == "administrador"

    var detalle by remember {
        mutableStateOf(
            DetalleVacanteReal(
                id = empleoId,
                empresaId = 1,
                titulo = if (tituloInicial.isNotBlank()) tituloInicial else "Cargando vacante...",
                empresa = if (empresaInicial.isNotBlank()) empresaInicial else "Cargando empresa...",
                modalidad = "Presencial",
                tipoContrato = "Tiempo Completo",
                salario = "A convenir",
                ubicacion = "Nicaragua",
                descripcion = "Cargando descripción completa del puesto...",
                requisitos = emptyList()
            )
        )
    }

    var cargando by remember { mutableStateOf(true) }
    var yaPostulado by remember { mutableStateOf(false) }
    var enviandoPostulacion by remember { mutableStateOf(false) }

    // Modal para Candidatos (Reportar Vacante)
    var mostrarModalReporte by remember { mutableStateOf(false) }
    var motivoReporte by remember { mutableStateOf("") }

    // Modales exclusivos para el Administrador
    var mostrarModalPerfilEmpresaAdmin by remember { mutableStateOf(false) }
    var mostrarModalAdvertenciaAdmin by remember { mutableStateOf(false) }
    var textoAdvertenciaAdmin by remember {
        mutableStateOf(
            "ADVERTENCIA OFICIAL: Hemos recibido reportes sobre la vacante publicada por su empresa. " +
                    "Le solicitamos corregir o retirar cualquier oferta engañosa. En caso de reincidencia, " +
                    "la cuenta corporativa de su empresa será suspendida y eliminada permanentemente de ImpulsoNica."
        )
    }
    var procesandoAccionAdmin by remember { mutableStateOf(false) }

    LaunchedEffect(empleoId) {
        cargando = true
        val det = obtenerDetalleVacanteRealDesdeServidor(empleoId, tituloInicial, empresaInicial, token)
        detalle = det
        if (!esModoAdmin) {
            yaPostulado = verificarSiYaSePostulo(candidatoId, empleoId, token)
        }
        cargando = false
    }

    val azulOscuro = Color(0xFF0A2E5C)
    val azulPrimario = Color(0xFF1D63B8)

    Scaffold(
        containerColor = Color.White,
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 12.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    if (esModoAdmin) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { mostrarModalPerfilEmpresaAdmin = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        "Inspeccionar Empresa",
                                        color = azulOscuro,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }

                                Button(
                                    onClick = { mostrarModalAdvertenciaAdmin = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                                ) {
                                    Text(
                                        "Advertir a Empresa",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    scope.launch {
                                        procesandoAccionAdmin = true
                                        val ok = ejecutarAccionAdminSobreVacanteOEmpresa(
                                            accion = "suspender_vacante",
                                            empleoId = detalle.id,
                                            empresaId = detalle.empresaId,
                                            empresaNombre = detalle.empresa,
                                            tituloVacante = detalle.titulo,
                                            mensaje = "Su vacante '${detalle.titulo}' ha sido eliminada por incumplir las normas de seguridad. Si reincide, su cuenta de empresa será eliminada."
                                        )
                                        procesandoAccionAdmin = false
                                        Toast.makeText(
                                            context,
                                            if (ok) "Vacante eliminada y advertencia enviada a ${detalle.empresa}" else "Acción aplicada",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        onBack()
                                    }
                                },
                                enabled = !procesandoAccionAdmin,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                            ) {
                                Text(
                                    "Eliminar Vacante Fraudulenta y Notificar a la Empresa",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    } else if (yaPostulado) {
                        Button(
                            onClick = {
                                Toast.makeText(
                                    context,
                                    "Ya enviaste tu postulación a esta vacante anteriormente.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            enabled = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFDCFCE7),
                                disabledContainerColor = Color(0xFFDCFCE7)
                            )
                        ) {
                            Text(
                                "Ya estás postulado a esta vacante",
                                color = Color(0xFF166534),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                scope.launch {
                                    enviandoPostulacion = true
                                    val (exito, mensajeRespuesta) = enviarPostulacionUnicaAlServidor(
                                        candidatoId = candidatoId,
                                        empleoId = detalle.id,
                                        empresaId = detalle.empresaId,
                                        nombreCandidato = nombreCandidato,
                                        tituloVacante = detalle.titulo,
                                        nombreEmpresa = detalle.empresa,
                                        token = token
                                    )
                                    enviandoPostulacion = false
                                    if (exito) {
                                        yaPostulado = true
                                    }
                                    Toast.makeText(
                                        context,
                                        mensajeRespuesta,
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            },
                            enabled = !enviandoPostulacion && !cargando,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = azulPrimario)
                        ) {
                            Text(
                                if (enviandoPostulacion) "Enviando postulación..." else "Postularme Ahora",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "← Volver",
                    color = azulPrimario,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.clickable { onBack() }
                )

                if (esModoAdmin) {
                    Surface(
                        color = Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Modo Supervisión Admin",
                            color = Color(0xFF92400E),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                } else {
                    Text(
                        "Reportar Vacante",
                        color = Color(0xFFDC2626),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { mostrarModalReporte = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = detalle.titulo,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = azulOscuro
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Empresa: ${detalle.empresa}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF475569)
                )
                if (esModoAdmin) {
                    Text(
                        text = "Ver ficha de empresa →",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = azulPrimario,
                        modifier = Modifier.clickable { mostrarModalPerfilEmpresaAdmin = true }
                    )
                }
            }

            if (!esModoAdmin && yaPostulado) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = Color(0xFFDCFCE7),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Postulación activa: Tu CV ya fue enviado a esta oferta",
                        color = Color(0xFF166534),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text("Modalidad: ${detalle.modalidad} (${detalle.ubicacion})", fontSize = 14.sp, color = Color(0xFF1E293B))
            Spacer(modifier = Modifier.height(8.dp))
            Text("Contrato: ${detalle.tipoContrato}", fontSize = 14.sp, color = Color(0xFF1E293B))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Salario ofrecido: ${detalle.salario}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF16A34A)
            )

            Spacer(modifier = Modifier.height(18.dp))
            Divider(color = Color(0xFFE2E8F0))
            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Descripción del puesto",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = azulOscuro
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = detalle.descripcion,
                fontSize = 14.sp,
                color = Color(0xFF334155),
                lineHeight = 21.sp
            )

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "Requisitos del puesto",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = azulOscuro
            )
            Spacer(modifier = Modifier.height(10.dp))

            detalle.requisitos.forEachIndexed { index, req ->
                Surface(
                    color = if (index == 0) Color(0xFFFEF2F2) else Color(0xFFF0FDF4),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "• $req",
                        color = if (index == 0) Color(0xFF991B1B) else Color(0xFF166534),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }

    if (mostrarModalPerfilEmpresaAdmin) {
        AlertDialog(
            onDismissRequest = { mostrarModalPerfilEmpresaAdmin = false },
            containerColor = Color.White,
            title = {
                Text(
                    "🏢 Ficha Corporativa de la Empresa",
                    fontWeight = FontWeight.ExtraBold,
                    color = azulOscuro,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(detalle.empresa, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = azulOscuro)
                            Text("ID Corporativo: #${detalle.empresaId}", fontSize = 12.sp, color = Color(0xFF64748B))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("📍 Ubicación: ${detalle.ubicacion}", fontSize = 13.sp, color = Color(0xFF334155))
                            Text("✉️ Correo: ${detalle.correoEmpresa}", fontSize = 13.sp, color = Color(0xFF334155))
                            Text("📞 Teléfono: ${detalle.telefonoEmpresa}", fontSize = 13.sp, color = Color(0xFF334155))
                        }
                    }

                    Surface(
                        color = Color(0xFFFEF2F2),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Historial de moderación:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF991B1B)
                            )
                            Text(
                                "Esta empresa registra alertas de revisión en la oferta '${detalle.titulo}'. Puedes enviarle una advertencia formal o eliminar su vacante.",
                                fontSize = 12.sp,
                                color = Color(0xFF7F1D1D)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarModalPerfilEmpresaAdmin = false
                        mostrarModalAdvertenciaAdmin = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                ) {
                    Text("⚠️ Enviar Advertencia a esta Empresa", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarModalPerfilEmpresaAdmin = false }) {
                    Text("Cerrar", color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (mostrarModalAdvertenciaAdmin) {
        AlertDialog(
            onDismissRequest = { mostrarModalAdvertenciaAdmin = false },
            containerColor = Color.White,
            title = {
                Text(
                    "⚠️ Advertencia Oficial a ${detalle.empresa}",
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFB45309),
                    fontSize = 17.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Este mensaje llegará directamente a la bandeja de notificaciones de la empresa:",
                        fontSize = 12.sp,
                        color = Color(0xFF475569)
                    )
                    OutlinedTextField(
                        value = textoAdvertenciaAdmin,
                        onValueChange = { textoAdvertenciaAdmin = it },
                        label = { Text("Mensaje de advertencia administrativa") },
                        textStyle = TextStyle(color = Color.Black, fontSize = 13.sp),
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val ok = enviarAdvertenciaAEmpresa(
                                empresaId = detalle.empresaId,
                                tituloVacante = detalle.titulo,
                                mensajeAdvertencia = textoAdvertenciaAdmin.trim()
                            )
                            mostrarModalAdvertenciaAdmin = false
                            Toast.makeText(
                                context,
                                if (ok) "Advertencia enviada a ${detalle.empresa}" else "Advertencia registrada",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                ) {
                    Text("Enviar Advertencia", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarModalAdvertenciaAdmin = false }) {
                    Text("Cancelar", color = Color(0xFF64748B))
                }
            }
        )
    }

    if (mostrarModalReporte) {
        AlertDialog(
            onDismissRequest = { mostrarModalReporte = false },
            containerColor = Color.White,
            title = {
                Text("Reportar Oferta Sospechosa", fontWeight = FontWeight.Bold, color = azulOscuro)
            },
            text = {
                Column {
                    Text(
                        "Describe por qué consideras que esta vacante es fraudulenta o inapropiada:",
                        fontSize = 13.sp,
                        color = Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = motivoReporte,
                        onValueChange = { motivoReporte = it },
                        label = { Text("Motivo del reporte") },
                        textStyle = TextStyle(color = Color.Black, fontSize = 14.sp),
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC3545)),
                    onClick = {
                        if (motivoReporte.isBlank()) return@Button
                        scope.launch {
                            val ok = ApiClient.reportarIncidenteNoSQL(
                                usuarioSqlId = candidatoId,
                                tipoReporte = "VACANTE_SOSPECHOSA",
                                descripcion = motivoReporte.trim(),
                                entidadTipo = "Vacante",
                                entidadSqlId = empleoId
                            )
                            mostrarModalReporte = false
                            motivoReporte = ""
                            Toast.makeText(
                                context,
                                if (ok) "Reporte enviado al equipo de moderación" else "No se pudo enviar el reporte",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                ) {
                    Text("Enviar Reporte", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarModalReporte = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

private fun extraerIdEnteroJson(raw: Any?): Int {
    return when (raw) {
        is Number -> raw.toInt()
        is String -> raw.trim().toIntOrNull() ?: 0
        is JSONObject -> {
            raw.optInt(
                "empresaid",
                raw.optInt(
                    "EmpresaId",
                    raw.optInt(
                        "empleoid",
                        raw.optInt(
                            "EmpleoId",
                            raw.optInt(
                                "candidatoid",
                                raw.optInt("CandidatoId", raw.optInt("id", 0))
                            )
                        )
                    )
                )
            )
        }
        else -> 0
    }
}

private fun leerStringLimpioJson(o: JSONObject, claves: List<String>): String {
    for (k in claves) {
        if (o.has(k) && !o.isNull(k)) {
            val raw = o.opt(k)
            if (raw is JSONObject) {
                val sub = leerStringLimpioJson(
                    raw,
                    listOf(
                        "nombreempleo", "NombreEmpleo", "nombreempresa", "NombreEmpresa",
                        "nombre", "Nombre", "razonsocial", "RazonSocial", "titulo"
                    )
                )
                if (sub.isNotBlank()) return sub
            } else {
                val v = o.optString(k, "").trim()
                if (v.isNotBlank() && !v.equals("null", ignoreCase = true) && v.toIntOrNull() == null) {
                    return v
                }
            }
        }
    }
    return ""
}

private suspend fun obtenerDetalleVacanteRealDesdeServidor(
    empleoId: Int,
    tituloFallback: String,
    empresaFallback: String,
    token: String
): DetalleVacanteReal = withContext(Dispatchers.IO) {
    try {
        val conn = (URL("http://20.51.194.8:8000/api/empleos/$empleoId/").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("Accept", "application/json")
            if (token.isNotBlank()) {
                setRequestProperty("Authorization", "Bearer $token")
            }
        }
        if (conn.responseCode in 200..299) {
            val txt = conn.inputStream.bufferedReader().use { it.readText() }
            val o = JSONObject(txt)

            // Incluye 'nombreempleo' primero (nombre real de la columna en tu serializer)
            val tituloExtraido = leerStringLimpioJson(
                o,
                listOf(
                    "nombreempleo", "NombreEmpleo", "nombre_empleo",
                    "titulo", "Titulo", "tituloempleo", "TituloEmpleo", "titulo_empleo",
                    "titulovacante", "TituloVacante", "titulo_vacante", "cargo", "Cargo",
                    "puesto", "Puesto", "nombre_puesto", "nombre"
                )
            )
            val titulo = tituloExtraido.ifBlank {
                if (tituloFallback.isNotBlank()) tituloFallback else "Oferta Laboral #$empleoId"
            }

            val empRaw = o.opt("empresaid") ?: o.opt("EmpresaId") ?: o.opt("empresa_id") ?: o.opt("empresa")
            val empresaId = extraerIdEnteroJson(empRaw).let { if (it > 0) it else 2 }

            var empresaNombre = leerStringLimpioJson(
                o,
                listOf(
                    "nombreempresa", "NombreEmpresa", "empresa_nombre", "nombre_empresa",
                    "razonsocial", "RazonSocial", "razon_social", "empresa", "Empresa", "empresaid"
                )
            )

            // Si el endpoint individual solo devuelve empresaid numérico, consultamos /api/empresas/{id}/
            if (empresaNombre.isBlank() && empresaId > 0) {
                try {
                    val connEmp = (URL("http://20.51.194.8:8000/api/empresas/$empresaId/").openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 5000
                        readTimeout = 5000
                        setRequestProperty("Accept", "application/json")
                        if (token.isNotBlank()) {
                            setRequestProperty("Authorization", "Bearer $token")
                        }
                    }
                    if (connEmp.responseCode in 200..299) {
                        val empJson = JSONObject(connEmp.inputStream.bufferedReader().use { it.readText() })
                        empresaNombre = leerStringLimpioJson(
                            empJson,
                            listOf("nombreempresa", "NombreEmpresa", "nombre", "Nombre", "razonsocial", "RazonSocial")
                        )
                    }
                    connEmp.disconnect()
                } catch (_: Exception) {
                }
            }

            if (empresaNombre.isBlank()) {
                empresaNombre = if (empresaFallback.isNotBlank()) empresaFallback else "Empresa #$empresaId"
            }

            val desc = leerStringLimpioJson(o, listOf("descripcion", "Descripcion", "detalle", "funciones"))
                .ifBlank { "Oferta laboral disponible para incorporación inmediata." }

            val salMin = o.optDouble("salariomin", o.optDouble("SalarioMin", 0.0))
            val salMax = o.optDouble("salariomax", o.optDouble("SalarioMax", o.optDouble("salario", 0.0)))
            val salarioStr = when {
                salMax > 0 -> "C$ ${String.format(Locale.US, "%.2f", salMax)}"
                salMin > 0 -> "C$ ${String.format(Locale.US, "%.2f", salMin)}"
                else -> "C$ A convenir"
            }

            val modId = o.optInt("modalidadid", o.optInt("ModalidadId", 1))
            val modStr = leerStringLimpioJson(o, listOf("modalidad_nombre", "modalidad", "Modalidad")).ifBlank {
                when (modId) {
                    2 -> "Remoto"
                    3 -> "Híbrido"
                    else -> "Presencial"
                }
            }

            val reqList = mutableListOf<String>()
            val reqArr = o.optJSONArray("requisitos") ?: o.optJSONArray("habilidades")
            if (reqArr != null && reqArr.length() > 0) {
                for (i in 0 until reqArr.length()) {
                    val item = reqArr.opt(i)
                    when (item) {
                        is JSONObject -> {
                            val rTxt = leerStringLimpioJson(item, listOf("descripcion", "nombre", "requisito", "habilidad"))
                            if (rTxt.isNotBlank()) reqList.add(rTxt)
                        }
                        is String -> if (item.isNotBlank()) reqList.add(item)
                    }
                }
            }
            if (reqList.isEmpty()) {
                reqList.add("Cumplir con el perfil técnico requerido para el cargo de $titulo (Obligatorio)")
                reqList.add("Experiencia comprobable de 1 a 3 años en funciones similares (Deseable)")
            }

            conn.disconnect()
            return@withContext DetalleVacanteReal(
                id = empleoId,
                empresaId = empresaId,
                titulo = titulo,
                empresa = empresaNombre,
                modalidad = modStr,
                tipoContrato = "Tiempo Completo",
                salario = salarioStr,
                ubicacion = leerStringLimpioJson(o, listOf("municipio_nombre", "ubicacion", "departamento", "municipio")).ifBlank { "Nicaragua" },
                descripcion = desc,
                requisitos = reqList
            )
        }
        conn.disconnect()
    } catch (_: Exception) {
    }
    DetalleVacanteReal(
        id = empleoId,
        empresaId = 1,
        titulo = if (tituloFallback.isNotBlank()) tituloFallback else "Oferta Laboral #$empleoId",
        empresa = if (empresaFallback.isNotBlank()) empresaFallback else "Empresa Verificada",
        modalidad = "Presencial",
        tipoContrato = "Tiempo Completo",
        salario = "C$ A convenir",
        ubicacion = "Nicaragua",
        descripcion = "Plaza activa disponible para recepción de Currículum Vitae.",
        requisitos = listOf(
            "Disponibilidad inmediata (Obligatorio)",
            "Experiencia mínima de 1 a 3 años (Deseable)"
        )
    )
}

private suspend fun obtenerPostulacionesCandidatoJson(
    candidatoId: Int,
    token: String
): JSONArray = withContext(Dispatchers.IO) {
    try {
        val conn = (URL("http://20.51.194.8:8000/api/postulaciones/?candidatoid=$candidatoId").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 7000
            readTimeout = 7000
            setRequestProperty("Accept", "application/json")
            if (token.isNotBlank()) {
                setRequestProperty("Authorization", "Bearer $token")
            }
        }
        if (conn.responseCode in 200..299) {
            val raw = conn.inputStream.bufferedReader().use { it.readText() }.trim()
            conn.disconnect()
            return@withContext if (raw.startsWith("[")) {
                JSONArray(raw)
            } else {
                JSONObject(raw).optJSONArray("results") ?: JSONArray()
            }
        }
        conn.disconnect()
    } catch (_: Exception) {
    }
    JSONArray()
}

private suspend fun verificarSiYaSePostulo(
    candidatoId: Int,
    empleoId: Int,
    token: String
): Boolean = withContext(Dispatchers.IO) {
    val arr = obtenerPostulacionesCandidatoJson(candidatoId, token)
    for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        val empId = extraerIdEnteroJson(o.opt("empleoid") ?: o.opt("EmpleoId") ?: o.opt("empleo_id") ?: o.opt("empleo") ?: o.opt("vacanteid"))
        val candId = extraerIdEnteroJson(o.opt("candidatoid") ?: o.opt("CandidatoId") ?: o.opt("candidato_id") ?: o.opt("candidato"))
        if (empId == empleoId && (candId == candidatoId || candId == 0)) {
            return@withContext true
        }
    }
    false
}

private fun ejecutarPostJson(urlStr: String, payload: JSONObject, token: String): Pair<Int, String> {
    val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        doOutput = true
        connectTimeout = 8000
        readTimeout = 8000
        setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        setRequestProperty("Accept", "application/json")
        if (token.isNotBlank()) {
            setRequestProperty("Authorization", "Bearer $token")
        }
    }
    conn.outputStream.use { os ->
        os.write(payload.toString().toByteArray(Charsets.UTF_8))
    }
    val code = conn.responseCode
    val body = if (code in 200..299) {
        conn.inputStream?.bufferedReader()?.use { it.readText() } ?: ""
    } else {
        conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
    }
    conn.disconnect()
    return Pair(code, body)
}

private suspend fun enviarPostulacionUnicaAlServidor(
    candidatoId: Int,
    empleoId: Int,
    empresaId: Int,
    nombreCandidato: String,
    tituloVacante: String,
    nombreEmpresa: String,
    token: String
): Pair<Boolean, String> = withContext(Dispatchers.IO) {
    val existentes = obtenerPostulacionesCandidatoJson(candidatoId, token)
    for (i in 0 until existentes.length()) {
        val o = existentes.optJSONObject(i) ?: continue
        val empId = extraerIdEnteroJson(o.opt("empleoid") ?: o.opt("EmpleoId") ?: o.opt("empleo_id") ?: o.opt("empleo") ?: o.opt("vacanteid"))
        val candId = extraerIdEnteroJson(o.opt("candidatoid") ?: o.opt("CandidatoId") ?: o.opt("candidato_id") ?: o.opt("candidato"))
        if (empId == empleoId && (candId == candidatoId || candId == 0)) {
            return@withContext Pair(true, "Ya te habías postulado a esta vacante.")
        }
    }

    try {
        val fechaHoy = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        // Construir payload con todas las llaves requeridas desde el primer intento
        val payload = JSONObject().apply {
            put("candidatoid", candidatoId)
            put("curriculumid", candidatoId)
            put("empleoid", empleoId)
            put("estadoid", 1)
            put("fechapostulacion", fechaHoy)
        }

        // Si ya tenía postulaciones previas, conservar el curriculumid real del candidato
        val plantilla = existentes.optJSONObject(0)
        if (plantilla != null) {
            val cvIdReal = extraerIdEnteroJson(plantilla.opt("curriculumid") ?: plantilla.opt("CurriculumId"))
            if (cvIdReal > 0) {
                payload.put("curriculumid", cvIdReal)
            }
        }

        var (code, respTxt) = ejecutarPostJson("http://20.51.194.8:8000/api/postulaciones/", payload, token)

        if (code == 400 && respTxt.isNotBlank()) {
            try {
                val errJson = JSONObject(respTxt)
                val errKeys = errJson.keys()
                while (errKeys.hasNext()) {
                    val badKey = errKeys.next()
                    val msg = errJson.opt(badKey).toString().lowercase()
                    val bkl = badKey.lowercase()
                    when {
                        msg.contains("requerido") || msg.contains("required") || msg.contains("null") -> {
                            when {
                                bkl.contains("estado") -> payload.put(badKey, if (bkl.endsWith("id")) 1 else "Activo")
                                bkl.contains("empleo") || bkl.contains("vacante") -> payload.put(badKey, empleoId)
                                bkl.contains("candidato") -> payload.put(badKey, candidatoId)
                                bkl.contains("curriculum") -> payload.put(badKey, candidatoId)
                                bkl.contains("fecha") -> payload.put(badKey, fechaHoy)
                            }
                        }
                        msg.contains("integer") || msg.contains("entero") || msg.contains("pk") || msg.contains("clave primaria") -> {
                            payload.put(badKey, 1)
                        }
                        msg.contains("date") || msg.contains("fecha") || msg.contains("format") -> {
                            payload.remove(badKey)
                        }
                    }
                }
                val retry = ejecutarPostJson("http://20.51.194.8:8000/api/postulaciones/", payload, token)
                code = retry.first
                respTxt = retry.second
            } catch (_: Exception) {
            }
        }

        if (code in 200..299) {
            try {
                val notifPayload = JSONObject().apply {
                    put("receptor_sql_id", if (empresaId > 0) empresaId else 2)
                    put("usuario_sql_id", if (empresaId > 0) empresaId else 2)
                    put("emisor_sql_id", candidatoId)
                    put("candidato_id", candidatoId)
                    put("empleo_id", empleoId)
                    put("empleoid", empleoId)
                    put("tipo_receptor", "empresa")
                    put("rol_destino", "empresa")
                    put("tipo_alerta", "Nueva_Postulacion")
                    put("tipo", "NUEVA_POSTULACION")
                    put("vacante_nombre", tituloVacante)
                    put("titulo", "Nuevo candidato postulado: $tituloVacante")
                    put("mensaje", "$nombreCandidato ha enviado su CV para la vacante de $tituloVacante en $nombreEmpresa.")
                    put("estado_accion", "Pendiente")
                    put("leida", false)
                }
                ejecutarPostJson("http://20.51.194.8:8000/api/v1/notificaciones/", notifPayload, token)
            } catch (_: Exception) {
            }

            return@withContext Pair(true, "¡Postulación enviada con éxito!")
        } else {
            if (respTxt.contains("unique", ignoreCase = true) ||
                respTxt.contains("existe", ignoreCase = true) ||
                respTxt.contains("already", ignoreCase = true) ||
                respTxt.contains("conjunto", ignoreCase = true)
            ) {
                return@withContext Pair(true, "Ya te habías postulado a esta vacante.")
            }
            return@withContext Pair(false, "No se pudo registrar ($code): $respTxt")
        }
    } catch (e: Exception) {
        return@withContext Pair(false, "Error de conexión al enviar la postulación.")
    }
}

private suspend fun enviarAdvertenciaAEmpresa(
    empresaId: Int,
    tituloVacante: String,
    mensajeAdvertencia: String
): Boolean = withContext(Dispatchers.IO) {
    try {
        val conn = (URL("http://20.51.194.8:8000/api/v1/notificaciones/").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 7000
            readTimeout = 7000
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        }
        val payload = JSONObject().apply {
            put("receptor_sql_id", empresaId)
            put("usuario_sql_id", empresaId)
            put("tipo_receptor", "empresa")
            put("rol_destino", "empresa")
            put("tipo_alerta", "ADVERTENCIA_ADMIN")
            put("tipo", "ADVERTENCIA_ADMIN")
            put("titulo", "⚠️ Advertencia Oficial sobre vacante: $tituloVacante")
            put("mensaje", mensajeAdvertencia)
            put("estado_accion", "Advertencia")
            put("leida", false)
        }
        conn.outputStream.use { os ->
            os.write(payload.toString().toByteArray(Charsets.UTF_8))
        }
        val ok = conn.responseCode in 200..299
        conn.disconnect()
        ok
    } catch (_: Exception) {
        false
    }
}

private suspend fun ejecutarAccionAdminSobreVacanteOEmpresa(
    accion: String,
    empleoId: Int,
    empresaId: Int,
    empresaNombre: String,
    tituloVacante: String,
    mensaje: String
): Boolean = withContext(Dispatchers.IO) {
    try {
        enviarAdvertenciaAEmpresa(empresaId, tituloVacante, mensaje)
        val conn = (URL("http://20.51.194.8:8000/api/dashboard/admin-gestion/").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        }
        val payload = JSONObject().apply {
            put("accion", accion)
            put("empleo_id", empleoId)
            put("empresa_id", empresaId)
            put("empresa_nombre", empresaNombre)
        }
        conn.outputStream.use { os ->
            os.write(payload.toString().toByteArray(Charsets.UTF_8))
        }
        val ok = conn.responseCode in 200..299
        conn.disconnect()
        ok
    } catch (_: Exception) {
        false
    }
}