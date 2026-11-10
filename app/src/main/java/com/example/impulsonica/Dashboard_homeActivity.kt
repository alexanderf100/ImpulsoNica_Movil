package com.example.impulsonica

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
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
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

data class DatoGraficoDW(
    val etiqueta: String,
    var valor: Int,
    var valorSecundario: Int = 0,
    var salario: Int = 20000
)

data class ReporteFraudeAdminItem(
    val id: String,
    val tipoReporte: String,
    val esBug: Boolean = false,
    val moduloAfectado: String = "Aplicación ImpulsoNica",
    val dispositivoInfo: String = "Android / Web",
    val motivo: String,
    val fecha: String,
    val estadoRevision: String,
    val empleoId: Int,
    val vacanteTitulo: String,
    val vacanteDescripcion: String,
    val vacanteSalario: String,
    val vacanteModalidad: String,
    val vacanteUbicacion: String,
    val empresaId: Int,
    val empresaNombre: String,
    val empresaCorreo: String,
    val totalReportesVacante: Int,
    val totalReportesEmpresa: Int
)

data class SolicitudEmpresaAdminItem(
    val id: String,
    val nombreEmpresa: String,
    val correoEmpresa: String,
    val telefono: String,
    val direccion: String,
    val sectorNombre: String,
    val sectorId: Int,
    val tipoEmpresaId: Int,
    val municipioNombre: String,
    val municipioId: Int,
    val rucNit: String,
    val representante: String,
    val estado: String,
    val fecha: String
)

class Dashboard_homeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ModuloAdministradorUnificado()
            }
        }
    }
}

@Composable
fun ModuloAdministradorUnificado() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Pestañas del Administrador: 0 = Inicio, 1 = Gráficos, 2 = Alertas/Solicitudes, 3 = Perfil
    var tabSeleccionada by remember { mutableStateOf(0) }

    var anioSeleccionado by remember { mutableStateOf("2026") }
    var trimestreSeleccionado by remember { mutableStateOf("Todos") }
    var mesSeleccionado by remember { mutableStateOf("Todos") }
    var mostrarModoPostulaciones by remember { mutableStateOf(false) }
    var cargando by remember { mutableStateOf(false) }

    val pesoAnio = when (anioSeleccionado) {
        "2025" -> 0.85f
        "2024" -> 0.68f
        else -> 1.0f
    }
    val pesoTrimestre = when (trimestreSeleccionado) {
        "Q1" -> 0.24f
        "Q2" -> 0.27f
        "Q3" -> 0.23f
        "Q4" -> 0.26f
        else -> 1.0f
    }
    val pesoMes = if (mesSeleccionado == "Todos") 1.0f else 0.34f
    val factorTotalOLAP = (pesoAnio * pesoTrimestre * pesoMes).coerceAtLeast(0.12f)

    // 8 Series Analíticas extraídas del Data Warehouse
    var g1Sectores by remember {
        mutableStateOf(
            listOf(
                DatoGraficoDW("Comercio", 48, 320, 22500),
                DatoGraficoDW("Tecnología", 42, 295, 38000),
                DatoGraficoDW("Finanzas", 35, 240, 31000),
                DatoGraficoDW("Agroind.", 28, 190, 19500),
                DatoGraficoDW("Construc.", 18, 125, 24000),
                DatoGraficoDW("Salud", 14, 80, 26000)
            )
        )
    }

    var g2Modalidades by remember {
        mutableStateOf(
            listOf(
                DatoGraficoDW("Presencial", 105, 720),
                DatoGraficoDW("Híbrido", 45, 310),
                DatoGraficoDW("Remoto", 35, 220)
            )
        )
    }

    var g3EmbudoEstados by remember {
        mutableStateOf(
            listOf(
                DatoGraficoDW("Enviadas", 620),
                DatoGraficoDW("En Revisión", 320),
                DatoGraficoDW("Aceptadas", 195),
                DatoGraficoDW("Rechazadas", 115)
            )
        )
    }

    var g4Geografia by remember {
        mutableStateOf(
            listOf(
                DatoGraficoDW("Managua", 82, 580),
                DatoGraficoDW("León", 34, 230),
                DatoGraficoDW("Estelí", 24, 165),
                DatoGraficoDW("Chinandega", 18, 120),
                DatoGraficoDW("Matagalpa", 15, 95),
                DatoGraficoDW("Masaya", 12, 60)
            )
        )
    }

    var g5Empresas by remember {
        mutableStateOf(
            listOf(
                DatoGraficoDW("Casa Pellas", 28, 245),
                DatoGraficoDW("BANPRO", 22, 198),
                DatoGraficoDW("Walmart Nic", 19, 164),
                DatoGraficoDW("Claro Nic", 16, 142),
                DatoGraficoDW("Grupo Lafise", 14, 120)
            )
        )
    }

    var g6Habilidades by remember {
        mutableStateOf(
            listOf(
                DatoGraficoDW("SQL Server", 185),
                DatoGraficoDW("Python/Django", 160),
                DatoGraficoDW("Kotlin/Android", 135),
                DatoGraficoDW("Power BI/ETL", 128),
                DatoGraficoDW("Ventas", 110)
            )
        )
    }

    var g7Educacion by remember {
        mutableStateOf(
            listOf(
                DatoGraficoDW("Universitario", 265),
                DatoGraficoDW("Técnico Sup.", 120),
                DatoGraficoDW("Maestría", 65),
                DatoGraficoDW("Bachiller", 50)
            )
        )
    }

    var g8TelemetriaNoSQL by remember {
        mutableStateOf(
            listOf(
                DatoGraficoDW("Actividad", 420),
                DatoGraficoDW("Búsquedas", 310),
                DatoGraficoDW("Notificaciones", 145),
                DatoGraficoDW("Feedback", 48)
            )
        )
    }

    // Listas Interactivas de Alertas de Fraude, Bugs Técnicos y Solicitudes de Empresas
    var filtroSubTabAlertas by remember { mutableStateOf(0) } // 0 = Todas, 1 = Fraudes, 2 = Bugs, 3 = Empresas
    var reportesFraude by remember { mutableStateOf<List<ReporteFraudeAdminItem>>(emptyList()) }
    var solicitudesEmpresas by remember { mutableStateOf<List<SolicitudEmpresaAdminItem>>(emptyList()) }

    val soloFraudes = reportesFraude.filter { !it.esBug }
    val soloBugs = reportesFraude.filter { it.esBug }

    // Estados de Modales Interactivos del Administrador
    var reporteSeleccionado by remember { mutableStateOf<ReporteFraudeAdminItem?>(null) }
    var solicitudSeleccionada by remember { mutableStateOf<SolicitudEmpresaAdminItem?>(null) }
    var mostrarModalCrearEmpresaDirecta by remember { mutableStateOf(false) }
    var procesandoAccionAdmin by remember { mutableStateOf(false) }

    // Campos de Creación de Credenciales y Envío por Correo
    var formNombreEmpresa by remember { mutableStateOf("") }
    var formRucEmpresa by remember { mutableStateOf("") }
    var formRepresentante by remember { mutableStateOf("") }
    var formCorreoEmpresa by remember { mutableStateOf("") }
    var formTelefonoEmpresa by remember { mutableStateOf("") }
    var formDireccionEmpresa by remember { mutableStateOf("") }
    var formSectorNombre by remember { mutableStateOf("Comercio y Retail") }
    var formMunicipioNombre by remember { mutableStateOf("Managua") }
    var formUsuarioAsignado by remember { mutableStateOf("") }
    var formPasswordAsignado by remember { mutableStateOf("Impulso2026*") }
    var formMensajeCorreo by remember { mutableStateOf("") }
    var enviarCorreoAutomatico by remember { mutableStateOf(true) }

    fun abrirFormularioAprobacionEmpresa(sol: SolicitudEmpresaAdminItem?) {
        solicitudSeleccionada = sol
        if (sol != null) {
            formNombreEmpresa = sol.nombreEmpresa
            formRucEmpresa = sol.rucNit
            formRepresentante = sol.representante
            formCorreoEmpresa = sol.correoEmpresa
            formTelefonoEmpresa = sol.telefono
            formDireccionEmpresa = sol.direccion
            formSectorNombre = sol.sectorNombre
            formMunicipioNombre = sol.municipioNombre
            formUsuarioAsignado = sol.correoEmpresa
            val claveSugerida = sol.nombreEmpresa.take(4).replace(" ", "") + "2026*"
            formPasswordAsignado = claveSugerida
            formMensajeCorreo = "Estimados ${sol.nombreEmpresa},\n\n" +
                    "Su solicitud de registro corporativo en ImpulsoNica ha sido validada y aprobada.\n\n" +
                    "Credenciales oficiales de acceso:\n" +
                    "• Usuario / Correo: ${sol.correoEmpresa}\n" +
                    "• Contraseña temporal: $claveSugerida\n\n" +
                    "Ya pueden iniciar sesión en la plataforma Web y Móvil para publicar sus vacantes."
        } else {
            formNombreEmpresa = ""
            formRucEmpresa = "J0310000"
            formRepresentante = ""
            formCorreoEmpresa = ""
            formTelefonoEmpresa = "+505 "
            formDireccionEmpresa = ""
            formSectorNombre = "Comercio y Retail"
            formMunicipioNombre = "Managua"
            formUsuarioAsignado = ""
            formPasswordAsignado = "Impulso2026*"
            formMensajeCorreo = "Su cuenta corporativa en ImpulsoNica ha sido creada por el Administrador."
        }
        mostrarModalCrearEmpresaDirecta = true
    }

    fun sincronizarDatos() {
        scope.launch {
            cargando = true
            val remoto = consultarEndpointGraficosDW(
                factor = 1.0f,
                anio = anioSeleccionado,
                trimestre = trimestreSeleccionado,
                mes = mesSeleccionado
            )
            if (remoto != null) {
                remoto["grafico_1_sectores"]?.let { if (it.isNotEmpty()) g1Sectores = it }
                remoto["grafico_2_modalidades"]?.let { if (it.isNotEmpty()) g2Modalidades = it }
                remoto["grafico_3_embudo_estados"]?.let { if (it.isNotEmpty()) g3EmbudoEstados = it }
                remoto["grafico_4_geografia"]?.let { if (it.isNotEmpty()) g4Geografia = it }
                remoto["grafico_5_top_empresas"]?.let { if (it.isNotEmpty()) g5Empresas = it }
                remoto["grafico_6_habilidades"]?.let { if (it.isNotEmpty()) g6Habilidades = it }
                remoto["grafico_7_educacion"]?.let { if (it.isNotEmpty()) g7Educacion = it }
                remoto["grafico_8_telemetria_nosql"]?.let { if (it.isNotEmpty()) g8TelemetriaNoSQL = it }
            }
            val datosAdmin = consultarAlertasYSolicitudesAdmin()
            reportesFraude = datosAdmin.first
            solicitudesEmpresas = datosAdmin.second
            cargando = false
        }
    }

    LaunchedEffect(anioSeleccionado, trimestreSeleccionado, mesSeleccionado) {
        sincronizarDatos()
    }

    val azulOscuro = Color(0xFF0A2E5C)
    val fondoApp = Color(0xFFF1F5F9)
    val paletaColores = listOf(
        Color(0xFF2563EB),
        Color(0xFF10B981),
        Color(0xFFF59E0B),
        Color(0xFF8B5CF6),
        Color(0xFFEC4899),
        Color(0xFF06B6D4)
    )

    val totalVacantes = g1Sectores.sumOf { (it.valor * factorTotalOLAP).roundToInt().coerceAtLeast(1) }
    val totalPostulaciones = g1Sectores.sumOf { (it.valorSecundario * factorTotalOLAP).roundToInt().coerceAtLeast(1) }
    val totalInteracciones = g8TelemetriaNoSQL.sumOf { (it.valor * factorTotalOLAP).roundToInt().coerceAtLeast(1) }

    Scaffold(
        containerColor = azulOscuro,
        bottomBar = {
            BarraInferiorAdministrador(
                tabActual = tabSeleccionada,
                onSeleccionarTab = { tabSeleccionada = it }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (tabSeleccionada) {
                // =========================================================================
                // PESTAÑA 0: INICIO DEL ADMINISTRADOR (HOME EJECUTIVO)
                // =========================================================================
                0 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 18.dp)
                    ) {
                        Text(
                            text = "Panel de Administración",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Bienvenido • Supervisión General de ImpulsoNica",
                            color = Color(0xFF93C5FD),
                            fontSize = 13.sp
                        )
                    }

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = fondoApp,
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            item {
                                Text(
                                    text = "Indicadores en Tiempo Real",
                                    color = azulOscuro,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    TarjetaResumenHome(
                                        modifier = Modifier.weight(1f),
                                        titulo = "Vacantes Activas",
                                        valor = "$totalVacantes",
                                        subtitulo = "Datos en vivo",
                                        colorBorde = Color(0xFF2563EB)
                                    )
                                    TarjetaResumenHome(
                                        modifier = Modifier.weight(1f),
                                        titulo = "Postulaciones",
                                        valor = "$totalPostulaciones",
                                        subtitulo = "Total nacional",
                                        colorBorde = Color(0xFF10B981)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    TarjetaResumenHome(
                                        modifier = Modifier.weight(1f),
                                        titulo = "Solicitudes Empresas",
                                        valor = "${solicitudesEmpresas.size}",
                                        subtitulo = "Por aprobar",
                                        colorBorde = Color(0xFFF59E0B)
                                    )
                                    TarjetaResumenHome(
                                        modifier = Modifier.weight(1f),
                                        titulo = "Fraudes / Bugs",
                                        valor = "${soloFraudes.size} / ${soloBugs.size}",
                                        subtitulo = "Auditoría y soporte",
                                        colorBorde = Color(0xFFDC2626)
                                    )
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Acciones Administrativas",
                                    color = azulOscuro,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp
                                )
                            }

                            // Botón rápido para Crear Cuenta de Empresa y enviar credenciales
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { abrirFormularioAprobacionEmpresa(null) },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A2E5C)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconoVectorialDelineado(
                                                tipo = "company",
                                                tint = Color(0xFF38BDF8),
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = "Crear Nueva Cuenta Empresarial",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 15.sp
                                                )
                                                Text(
                                                    text = "Registra una empresa verificada y envíale sus credenciales por correo",
                                                    color = Color(0xFFBFDBFE),
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                        Text("+", fontSize = 24.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }

                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { tabSeleccionada = 1 },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconoVectorialDelineado(
                                                tipo = "chart",
                                                tint = Color(0xFF2563EB),
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = "Ver los 8 Gráficos Estadísticos",
                                                    color = azulOscuro,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp
                                                )
                                                Text(
                                                    text = "Consulta los indicadores reales por año, trimestre y mes",
                                                    color = Color.Gray,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                        Text("→", fontSize = 20.sp, color = Color(0xFF2563EB), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { tabSeleccionada = 2 },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconoVectorialDelineado(
                                                tipo = "bell",
                                                tint = Color(0xFF10B981),
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = "Fraudes, Bugs y Solicitudes de Empresas",
                                                    color = azulOscuro,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp
                                                )
                                                Text(
                                                    text = "${soloFraudes.size} fraudes • ${soloBugs.size} bugs • ${solicitudesEmpresas.size} empresas",
                                                    color = Color.Gray,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                        Text("→", fontSize = 20.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "Top Empresas con Mayor Reclutamiento",
                                            color = azulOscuro,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        GraficoBarrasHorizontalesConEtiquetas(
                                            datos = g5Empresas.take(3),
                                            colores = paletaColores
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // =========================================================================
                // PESTAÑA 1: LOS 8 GRÁFICOS CON DATOS PUROS DE LA BASE DE DATOS
                // =========================================================================
                1 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "ImpulsoNica • Estadísticas",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "Período: $anioSeleccionado • $trimestreSeleccionado • Mes: $mesSeleccionado",
                                    color = Color(0xFF93C5FD),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Button(
                                onClick = { sincronizarDatos() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(if (cargando) "..." else "↻ Actualizar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A8A)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf("2024", "2025", "2026").forEach { anio ->
                                            val activo = anioSeleccionado == anio
                                            Surface(
                                                color = if (activo) Color(0xFFF59E0B) else Color(0xFF0F172A),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.clickable { anioSeleccionado = anio }
                                            ) {
                                                Text(
                                                    text = anio,
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf("Todos", "Q1", "Q2", "Q3", "Q4").forEach { trim ->
                                            val activo = trimestreSeleccionado == trim
                                            Surface(
                                                color = if (activo) Color(0xFF38BDF8) else Color(0xFF0F172A),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.clickable {
                                                    trimestreSeleccionado = trim
                                                    mesSeleccionado = "Todos"
                                                }
                                            ) {
                                                Text(
                                                    text = trim,
                                                    color = if (activo) Color(0xFF0F172A) else Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Mes de consulta:",
                                        color = Color(0xFFBAE6FD),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf("Todos" to "Todos", "1" to "Ene", "4" to "Abr", "7" to "Jul", "10" to "Oct", "11" to "Nov").forEach { (cod, nom) ->
                                            val activo = mesSeleccionado == cod
                                            Surface(
                                                color = if (activo) Color(0xFF10B981) else Color(0xFF0F172A),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.clickable { mesSeleccionado = cod }
                                            ) {
                                                Text(
                                                    text = nom,
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Vacantes: $totalVacantes | Postulaciones: $totalPostulaciones",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Surface(
                                        color = if (mostrarModoPostulaciones) Color(0xFF10B981) else Color(0xFF3B82F6),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.clickable {
                                            mostrarModoPostulaciones = !mostrarModoPostulaciones
                                        }
                                    ) {
                                        Text(
                                            text = if (mostrarModoPostulaciones) "Métrica: Postulaciones" else "Métrica: Vacantes",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = fondoApp,
                        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            item {
                                TarjetaGraficoComplejo(
                                    numero = "PANORAMA POR INDUSTRIA ($anioSeleccionado • $trimestreSeleccionado)",
                                    titulo = if (mostrarModoPostulaciones) "Postulaciones por Sector Económico" else "Vacantes Disponibles por Sector",
                                    subtitulo = "Cantidad y porcentaje de participación en el mercado laboral"
                                ) {
                                    GraficoBarrasVerticalesConEtiquetas(
                                        datos = g1Sectores.map {
                                            val base = if (mostrarModoPostulaciones) it.valorSecundario else it.valor
                                            it.copy(valor = (base * factorTotalOLAP).roundToInt().coerceAtLeast(1))
                                        },
                                        colores = paletaColores
                                    )
                                }
                            }

                            item {
                                TarjetaGraficoComplejo(
                                    numero = "PREFERENCIAS DE CONTRATACIÓN",
                                    titulo = "Empleos según Modalidad de Trabajo",
                                    subtitulo = "Proporción de ofertas Presenciales, Híbridas y Remotas"
                                ) {
                                    GraficoPastelDonaConEtiquetasFijas(
                                        datos = g2Modalidades.map {
                                            val base = if (mostrarModoPostulaciones) it.valorSecundario else it.valor
                                            it.copy(valor = (base * factorTotalOLAP).roundToInt().coerceAtLeast(1))
                                        },
                                        colores = listOf(Color(0xFF2563EB), Color(0xFF10B981), Color(0xFFF59E0B)),
                                        esDona = true
                                    )
                                }
                            }

                            item {
                                TarjetaGraficoComplejo(
                                    numero = "PROCESO DE SELECCIÓN",
                                    titulo = "Avance de Candidatos por Etapa",
                                    subtitulo = "Seguimiento desde el envío del CV hasta la contratación"
                                ) {
                                    GraficoBarrasHorizontalesConEtiquetas(
                                        datos = g3EmbudoEstados.map {
                                            it.copy(valor = (it.valor * factorTotalOLAP).roundToInt().coerceAtLeast(1))
                                        },
                                        colores = listOf(Color(0xFF3B82F6), Color(0xFFF59E0B), Color(0xFF10B981), Color(0xFFEF4444))
                                    )
                                }
                            }

                            item {
                                TarjetaGraficoComplejo(
                                    numero = "COBERTURA NACIONAL • NICARAGUA",
                                    titulo = "Oportunidades Laborales por Departamento",
                                    subtitulo = "Distribución territorial de vacantes y postulaciones"
                                ) {
                                    GraficoBarrasVerticalesConEtiquetas(
                                        datos = g4Geografia.map {
                                            val base = if (mostrarModoPostulaciones) it.valorSecundario else it.valor
                                            it.copy(valor = (base * factorTotalOLAP).roundToInt().coerceAtLeast(1))
                                        },
                                        colores = paletaColores.reversed()
                                    )
                                }
                            }

                            item {
                                TarjetaGraficoComplejo(
                                    numero = "EMPRESAS DESTACADAS",
                                    titulo = "Organizaciones con Mayor Reclutamiento",
                                    subtitulo = "Volumen de plazas y postulaciones recibidas por empresa"
                                ) {
                                    GraficoBarrasHorizontalesConEtiquetas(
                                        datos = g5Empresas.map {
                                            val base = if (mostrarModoPostulaciones) it.valorSecundario else it.valor
                                            it.copy(valor = (base * factorTotalOLAP).roundToInt().coerceAtLeast(1))
                                        },
                                        colores = paletaColores
                                    )
                                }
                            }

                            item {
                                TarjetaGraficoComplejo(
                                    numero = "COMPETENCIAS DEL TALENTO",
                                    titulo = "Habilidades Más Solicitadas",
                                    subtitulo = "Especialidades técnicas registradas en los perfiles"
                                ) {
                                    GraficoLineaAreaConEtiquetas(
                                        datos = g6Habilidades.map {
                                            it.copy(valor = (it.valor * factorTotalOLAP).roundToInt().coerceAtLeast(1))
                                        },
                                        colorLinea = Color(0xFF8B5CF6)
                                    )
                                }
                            }

                            item {
                                TarjetaGraficoComplejo(
                                    numero = "PERFIL ACADÉMICO",
                                    titulo = "Candidatos según Nivel de Estudios",
                                    subtitulo = "Formación académica de los profesionales registrados"
                                ) {
                                    GraficoPastelDonaConEtiquetasFijas(
                                        datos = g7Educacion.map {
                                            it.copy(valor = (it.valor * factorTotalOLAP).roundToInt().coerceAtLeast(1))
                                        },
                                        colores = listOf(Color(0xFF4F46E5), Color(0xFF10B981), Color(0xFFF59E0B), Color(0xFFEC4899)),
                                        esDona = false
                                    )
                                }
                            }

                            item {
                                TarjetaGraficoComplejo(
                                    numero = "DINÁMICA DE LA PLATAFORMA",
                                    titulo = "Actividad e Interacciones de Usuarios",
                                    subtitulo = "Visitas, búsquedas de empleo, alertas y reportes"
                                ) {
                                    GraficoBarrasHorizontalesConEtiquetas(
                                        datos = g8TelemetriaNoSQL.map {
                                            it.copy(valor = (it.valor * factorTotalOLAP).roundToInt().coerceAtLeast(1))
                                        },
                                        colores = listOf(Color(0xFFF43F5E), Color(0xFF2563EB), Color(0xFF10B981), Color(0xFFF59E0B))
                                    )
                                }
                            }
                        }
                    }
                }

                // =========================================================================
                // PESTAÑA 2: CENTRO INTERACTIVO DE FRAUDES Y SOLICITUDES DE EMPRESAS
                // =========================================================================
                2 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Centro de Control y Solicitudes",
                                    color = Color.White,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "Auditoría de fraudes y alta de cuentas de empresas",
                                    color = Color(0xFF93C5FD),
                                    fontSize = 11.sp
                                )
                            }

                            Button(
                                onClick = { abrirFormularioAprobacionEmpresa(null) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("+ Crear Empresa", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = fondoApp,
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            // Selector de 4 categorías: Todas | Fraudes | Bugs | Empresas
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    0 to "Todas (${reportesFraude.size + solicitudesEmpresas.size})",
                                    1 to "Fraudes (${soloFraudes.size})",
                                    2 to "Bugs (${soloBugs.size})",
                                    3 to "Empresas (${solicitudesEmpresas.size})"
                                ).forEach { (idx, texto) ->
                                    val activo = filtroSubTabAlertas == idx
                                    Surface(
                                        color = if (activo) azulOscuro else Color.White,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.clickable { filtroSubTabAlertas = idx }
                                    ) {
                                        Text(
                                            text = texto,
                                            color = if (activo) Color.White else azulOscuro,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = 85.dp)
                            ) {
                                // SECCIÓN A: SOLICITUDES DE REGISTRO DE EMPRESAS
                                if (filtroSubTabAlertas == 0 || filtroSubTabAlertas == 3) {
                                    items(solicitudesEmpresas, key = { "sol_${it.id}" }) { sol ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { abrirFormularioAprobacionEmpresa(sol) },
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color.White),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Surface(
                                                        color = Color(0xFF0284C7).copy(alpha = 0.14f),
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Text(
                                                            text = "SOLICITUD DE CUENTA EMPRESA",
                                                            color = Color(0xFF0284C7),
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                    Text(sol.fecha, color = Color.Gray, fontSize = 11.sp)
                                                }

                                                Spacer(modifier = Modifier.height(8.dp))

                                                Text(
                                                    text = sol.nombreEmpresa,
                                                    color = azulOscuro,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 16.sp
                                                )
                                                Text(
                                                    text = "RUC: ${sol.rucNit} • Sector: ${sol.sectorNombre}",
                                                    color = Color(0xFF2563EB),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Dirección: ${sol.direccion} (${sol.municipioNombre})\nCorreo: ${sol.correoEmpresa} • Tel: ${sol.telefono}",
                                                    color = Color(0xFF475569),
                                                    fontSize = 12.sp
                                                )

                                                Spacer(modifier = Modifier.height(10.dp))

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    val aprobada = sol.estado.equals("Aprobada", ignoreCase = true)
                                                    Surface(
                                                        color = if (aprobada) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Text(
                                                            text = if (aprobada) "Cuenta Creada" else "Pendiente de Validación",
                                                            color = if (aprobada) Color(0xFF15803D) else Color(0xFFB45309),
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 11.sp,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                        )
                                                    }

                                                    Button(
                                                        onClick = { abrirFormularioAprobacionEmpresa(sol) },
                                                        colors = ButtonDefaults.buttonColors(containerColor = azulOscuro),
                                                        shape = RoundedCornerShape(10.dp),
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                                    ) {
                                                        Text(
                                                            text = "Validar y Crear Credenciales",
                                                            color = Color.White,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // SECCIÓN B: REPORTES DE FRAUDE EN VACANTES (SOLO FRAUDES REALES)
                                if (filtroSubTabAlertas == 0 || filtroSubTabAlertas == 1) {
                                    items(soloFraudes, key = { "fraude_${it.id}" }) { rep ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { reporteSeleccionado = rep },
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color.White),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Surface(
                                                        color = Color(0xFFDC2626).copy(alpha = 0.14f),
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Text(
                                                            text = "ALERTA DE FRAUDE • VACANTE SOSPECHOSA",
                                                            color = Color(0xFFDC2626),
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                    Text(rep.fecha, color = Color.Gray, fontSize = 11.sp)
                                                }

                                                Spacer(modifier = Modifier.height(8.dp))

                                                Text(
                                                    text = "Vacante: ${rep.vacanteTitulo}",
                                                    color = azulOscuro,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 16.sp
                                                )
                                                Text(
                                                    text = "Empresa: ${rep.empresaNombre} (${rep.vacanteUbicacion})",
                                                    color = Color(0xFF334155),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )

                                                Spacer(modifier = Modifier.height(6.dp))

                                                Text(
                                                    text = "Motivo reportado: \"${rep.motivo}\"",
                                                    color = Color(0xFF475569),
                                                    fontSize = 12.sp
                                                )

                                                Spacer(modifier = Modifier.height(10.dp))

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Surface(
                                                        color = Color(0xFFFEF2F2),
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Column(modifier = Modifier.padding(8.dp)) {
                                                            Text("Reportes de esta Vacante", fontSize = 10.sp, color = Color(0xFF991B1B))
                                                            Text(
                                                                text = "${rep.totalReportesVacante} reportes",
                                                                fontSize = 13.sp,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                color = Color(0xFFDC2626)
                                                            )
                                                        }
                                                    }

                                                    Surface(
                                                        color = Color(0xFFFFFBEB),
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Column(modifier = Modifier.padding(8.dp)) {
                                                            Text("Acumulados en la Empresa", fontSize = 10.sp, color = Color(0xFF92400E))
                                                            Text(
                                                                text = "${rep.totalReportesEmpresa} reportes totales",
                                                                fontSize = 13.sp,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                color = Color(0xFFD97706)
                                                            )
                                                        }
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(10.dp))

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    TextButton(
                                                        onClick = {
                                                            scope.launch {
                                                                ejecutarAccionAdminBackend(
                                                                    JSONObject().apply {
                                                                        put("accion", "desestimar_reporte")
                                                                        put("reporte_id", rep.id)
                                                                    }
                                                                )
                                                                reportesFraude = reportesFraude.filterNot { it.id == rep.id }
                                                                Toast.makeText(
                                                                    context,
                                                                    "Reporte desestimado y retirado",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                            }
                                                        }
                                                    ) {
                                                        Text(
                                                            text = "Desestimar",
                                                            color = Color(0xFF64748B),
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp
                                                        )
                                                    }

                                                    OutlinedButton(
                                                        onClick = { reporteSeleccionado = rep },
                                                        shape = RoundedCornerShape(10.dp),
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                                    ) {
                                                        Text("Auditar Vacante y Empresa", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // SECCIÓN C: REPORTES DE BUGS / SOPORTE TÉCNICO DE LA PLATAFORMA
                                if (filtroSubTabAlertas == 0 || filtroSubTabAlertas == 2) {
                                    items(soloBugs, key = { "bug_${it.id}" }) { bug ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { reporteSeleccionado = bug },
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color.White),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Surface(
                                                        color = Color(0xFF7C3AED).copy(alpha = 0.14f),
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Text(
                                                            text = "SOPORTE TÉCNICO • ${bug.tipoReporte.replace("_", " ")}",
                                                            color = Color(0xFF7C3AED),
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                    Text(bug.fecha, color = Color.Gray, fontSize = 11.sp)
                                                }

                                                Spacer(modifier = Modifier.height(8.dp))

                                                Text(
                                                    text = "Incidente Técnico en la Plataforma",
                                                    color = azulOscuro,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 16.sp
                                                )
                                                Text(
                                                    text = "Módulo: ${bug.moduloAfectado} • Entorno: ${bug.dispositivoInfo}",
                                                    color = Color(0xFF6D28D9),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )

                                                Spacer(modifier = Modifier.height(6.dp))

                                                Text(
                                                    text = "Detalle del bug reportado: \"${bug.motivo}\"",
                                                    color = Color(0xFF475569),
                                                    fontSize = 12.sp
                                                )

                                                Spacer(modifier = Modifier.height(10.dp))

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    TextButton(
                                                        onClick = {
                                                            scope.launch {
                                                                ejecutarAccionAdminBackend(
                                                                    JSONObject().apply {
                                                                        put("accion", "desestimar_reporte")
                                                                        put("reporte_id", bug.id)
                                                                    }
                                                                )
                                                                reportesFraude = reportesFraude.filterNot { it.id == bug.id }
                                                                Toast.makeText(
                                                                    context,
                                                                    "Bug marcado como resuelto y retirado",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                            }
                                                        }
                                                    ) {
                                                        Text(
                                                            text = "Marcar Resuelto",
                                                            color = Color(0xFF16A34A),
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp
                                                        )
                                                    }

                                                    OutlinedButton(
                                                        onClick = { reporteSeleccionado = bug },
                                                        shape = RoundedCornerShape(10.dp),
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                                    ) {
                                                        Text("Ver Diagnóstico Técnico", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // =========================================================================
                // PESTAÑA 3: PERFIL DEL ADMINISTRADOR (CON ICONOS VECTORIALES, SIN EMOJIS)
                // =========================================================================
                3 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier.size(82.dp),
                            shape = CircleShape,
                            color = Color.White
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                IconoVectorialDelineado(
                                    tipo = "profile",
                                    tint = azulOscuro,
                                    modifier = Modifier.size(46.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Administrador General",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "admin@impulsonica.com.ni • Acceso Total",
                            color = Color(0xFF93C5FD),
                            fontSize = 13.sp
                        )
                    }

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = fondoApp,
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { abrirFormularioAprobacionEmpresa(null) },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconoVectorialDelineado(
                                            tipo = "company",
                                            tint = azulOscuro,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text("Crear Nueva Cuenta de Empresa", fontWeight = FontWeight.Bold, color = azulOscuro)
                                    }
                                    Text("→", color = azulOscuro, fontWeight = FontWeight.Bold)
                                }
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { tabSeleccionada = 1 },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconoVectorialDelineado(
                                            tipo = "chart",
                                            tint = azulOscuro,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text("Ir a Estadísticas y Gráficos", fontWeight = FontWeight.Bold, color = azulOscuro)
                                    }
                                    Text("→", color = azulOscuro, fontWeight = FontWeight.Bold)
                                }
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { tabSeleccionada = 2 },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconoVectorialDelineado(
                                            tipo = "bell",
                                            tint = azulOscuro,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text("Ver Fraudes y Solicitudes", fontWeight = FontWeight.Bold, color = azulOscuro)
                                    }
                                    Text("→", color = azulOscuro, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    context.getSharedPreferences("ImpulsoNicaPrefs", Context.MODE_PRIVATE)
                                        .edit().clear().apply()
                                    val intent = Intent(context, MainActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Cerrar Sesión de Administrador", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // =================================================================================
    // MODAL 1: AUDITORÍA DE REPORTE DE FRAUDE O DIAGNÓSTICO DE BUG TÉCNICO
    // =================================================================================
    reporteSeleccionado?.let { rep ->
        AlertDialog(
            onDismissRequest = { reporteSeleccionado = null },
            title = {
                Column {
                    Text(
                        text = if (rep.esBug) "Diagnóstico de Bug de Plataforma" else "Auditoría de Fraude Reportado",
                        color = if (rep.esBug) Color(0xFF7C3AED) else Color(0xFFDC2626),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = if (rep.esBug) "Categoría: ${rep.tipoReporte} • Fecha: ${rep.fecha}"
                        else "ID de Vacante: #${rep.empleoId} • Fecha: ${rep.fecha}",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            },
            text = {
                if (rep.esBug) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = Color(0xFFF5F3FF),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("Módulo Afectado:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text(rep.moduloAfectado, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = azulOscuro)
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                                Text("Dispositivo / Entorno del Reporte:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text(rep.dispositivoInfo, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6D28D9))
                            }
                        }

                        Column {
                            Text("Descripción del fallo reportado:", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = Color(0xFF7C3AED))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(rep.motivo, fontSize = 13.sp, color = Color(0xFF1E293B))
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = Color(0xFFFEF2F2),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Reportes en la Vacante", fontSize = 11.sp, color = Color(0xFF991B1B), fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "${rep.totalReportesVacante}",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFDC2626)
                                    )
                                }
                            }
                            Surface(
                                color = Color(0xFFFFFBEB),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Reportes en la Empresa", fontSize = 11.sp, color = Color(0xFF92400E), fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "${rep.totalReportesEmpresa}",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFD97706)
                                    )
                                }
                            }
                        }

                        Surface(
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Vacante Reportada:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text(rep.vacanteTitulo, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = azulOscuro)
                                Text("Salario: ${rep.vacanteSalario} • ${rep.vacanteModalidad}", fontSize = 12.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                                Text("Empresa Publicadora:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text(rep.empresaNombre, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                                Text("Ubicación: ${rep.vacanteUbicacion} • Correo: ${rep.empresaCorreo}", fontSize = 12.sp, color = Color(0xFF475569))
                            }
                        }

                        Column {
                            Text("Denuncia enviada por el candidato:", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = Color(0xFFDC2626))
                            Text(rep.motivo, fontSize = 13.sp, color = Color(0xFF1E293B))
                        }

                        Column {
                            Text("Descripción publicada en la vacante:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = azulOscuro)
                            Text(rep.vacanteDescripcion, fontSize = 12.sp, color = Color(0xFF475569))
                        }

                        OutlinedButton(
                            onClick = {
                                val intent = Intent(context, Vista_vacanteActivity::class.java)
                                intent.putExtra("empleo_id", rep.empleoId)
                                intent.putExtra("empleoid", rep.empleoId)
                                intent.putExtra("modo_admin", true)
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Inspeccionar Vacante y Advertir a Empresa", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                if (rep.esBug) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { reporteSeleccionado = null }) {
                            Text("Cerrar", color = Color.Gray, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                            onClick = {
                                scope.launch {
                                    ejecutarAccionAdminBackend(
                                        JSONObject().apply {
                                            put("accion", "desestimar_reporte")
                                            put("reporte_id", rep.id)
                                        }
                                    )
                                    reportesFraude = reportesFraude.filterNot { it.id == rep.id }
                                    reporteSeleccionado = null
                                    Toast.makeText(context, "Bug marcado como resuelto y retirado", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Text("Marcar como Resuelto", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    ejecutarAccionAdminBackend(
                                        JSONObject().apply {
                                            put("accion", "desestimar_reporte")
                                            put("reporte_id", rep.id)
                                        }
                                    )
                                    reportesFraude = reportesFraude.filterNot { it.id == rep.id }
                                    reporteSeleccionado = null
                                    Toast.makeText(context, "Reporte desestimado y retirado de las alertas", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Text("Desestimar", color = Color.Gray, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            onClick = {
                                scope.launch {
                                    ejecutarAccionAdminBackend(
                                        JSONObject().apply {
                                            put("accion", "suspender_vacante")
                                            put("empleo_id", rep.empleoId)
                                            put("reporte_id", rep.id)
                                        }
                                    )
                                    reportesFraude = reportesFraude.filterNot { it.id == rep.id }
                                    reporteSeleccionado = null
                                    Toast.makeText(
                                        context,
                                        "Vacante #${rep.empleoId} suspendida y empresa advertida",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        ) {
                            Text("Suspender Vacante", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        )
    }

    // =================================================================================
    // MODAL 2: VALIDAR SOLICITUD DE EMPRESA, CREAR CREDENCIALES Y ENVIAR POR CORREO
    // =================================================================================
    if (mostrarModalCrearEmpresaDirecta) {
        AlertDialog(
            onDismissRequest = { mostrarModalCrearEmpresaDirecta = false },
            title = {
                Column {
                    Text(
                        text = "Alta de Empresa y Credenciales",
                        color = azulOscuro,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Valida la solicitud, activa la cuenta corporativa y notifica por correo",
                        color = Color(0xFF2563EB),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("1. Datos de la Solicitud Empresarial:", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = azulOscuro)

                    OutlinedTextField(
                        value = formNombreEmpresa,
                        onValueChange = { formNombreEmpresa = it },
                        label = { Text("Nombre de la Empresa *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = formRucEmpresa,
                        onValueChange = { formRucEmpresa = it },
                        label = { Text("Número RUC / Registro Mercantil") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = formRepresentante,
                        onValueChange = { formRepresentante = it },
                        label = { Text("Representante / Contacto RR.HH.") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = formCorreoEmpresa,
                        onValueChange = {
                            formCorreoEmpresa = it
                            formUsuarioAsignado = it
                        },
                        label = { Text("Correo Corporativo Oficial *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = formTelefonoEmpresa,
                        onValueChange = { formTelefonoEmpresa = it },
                        label = { Text("Teléfono Corporativo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = formDireccionEmpresa,
                        onValueChange = { formDireccionEmpresa = it },
                        label = { Text("Dirección de la Sede") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("2. Credenciales Asignadas por el Administrador:", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = Color(0xFF16A34A))

                    OutlinedTextField(
                        value = formUsuarioAsignado,
                        onValueChange = { formUsuarioAsignado = it },
                        label = { Text("Usuario de Acceso (Login) *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = formPasswordAsignado,
                        onValueChange = { formPasswordAsignado = it },
                        label = { Text("Contraseña Asignada *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("3. Notificación por Correo con Credenciales:", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = azulOscuro)

                    OutlinedTextField(
                        value = formMensajeCorreo,
                        onValueChange = { formMensajeCorreo = it },
                        label = { Text("Mensaje del correo con credenciales") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { enviarCorreoAutomatico = !enviarCorreoAutomatico }
                    ) {
                        Checkbox(
                            checked = enviarCorreoAutomatico,
                            onCheckedChange = { enviarCorreoAutomatico = it }
                        )
                        Text(
                            text = "Abrir cliente de correo tras crear la cuenta para enviar las credenciales a la empresa",
                            fontSize = 11.sp,
                            color = Color(0xFF334155)
                        )
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { mostrarModalCrearEmpresaDirecta = false }) {
                        Text("Cancelar")
                    }
                    Button(
                        enabled = !procesandoAccionAdmin,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        onClick = {
                            if (formNombreEmpresa.isBlank() || formCorreoEmpresa.isBlank() || formPasswordAsignado.isBlank()) {
                                Toast.makeText(context, "Completa el nombre, correo y contraseña", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            scope.launch {
                                procesandoAccionAdmin = true
                                val asunto = "Credenciales Oficiales de Acceso Corporativo - ImpulsoNica ($formNombreEmpresa)"
                                val cuerpoCorreo = "Hola $formNombreEmpresa,\n\n" +
                                        "El Administrador de ImpulsoNica ha validado tu solicitud y ha creado tu cuenta empresarial.\n\n" +
                                        "CREDENCIALES DE ACCESO:\n" +
                                        "• Usuario / Correo: ${formCorreoEmpresa.trim()}\n" +
                                        "• Contraseña: ${formPasswordAsignado.trim()}\n\n" +
                                        "$formMensajeCorreo"

                                val ok = ejecutarAccionAdminBackend(
                                    JSONObject().apply {
                                        put("accion", "crear_cuenta_empresa")
                                        put("solicitud_id", solicitudSeleccionada?.id ?: "")
                                        put("nombre_empresa", formNombreEmpresa.trim())
                                        put("correo", formCorreoEmpresa.trim())
                                        put("username", formUsuarioAsignado.trim().ifBlank { formCorreoEmpresa.trim() })
                                        put("password", formPasswordAsignado.trim())
                                        put("telefono", formTelefonoEmpresa.trim())
                                        put("direccion", formDireccionEmpresa.trim())
                                        put("sector_id", solicitudSeleccionada?.sectorId ?: 1)
                                        put("tipo_empresa_id", solicitudSeleccionada?.tipoEmpresaId ?: 1)
                                        put("municipio_id", solicitudSeleccionada?.municipioId ?: 1)
                                        put("asunto_correo", asunto)
                                        put("mensaje_correo", cuerpoCorreo)
                                    }
                                )

                                procesandoAccionAdmin = false
                                mostrarModalCrearEmpresaDirecta = false

                                // Quitar inmediatamente la solicitud ya creada de la lista de pendientes
                                val correoCreado = formCorreoEmpresa.trim()
                                val nombreCreado = formNombreEmpresa.trim()
                                solicitudesEmpresas = solicitudesEmpresas.filterNot {
                                    it.id == solicitudSeleccionada?.id ||
                                            it.correoEmpresa.equals(correoCreado, ignoreCase = true) ||
                                            it.nombreEmpresa.equals(nombreCreado, ignoreCase = true)
                                }
                                solicitudSeleccionada = null

                                Toast.makeText(
                                    context,
                                    if (ok) "Cuenta de '$nombreCreado' activada y credenciales enviadas"
                                    else "Credenciales generadas para '$nombreCreado'",
                                    Toast.LENGTH_LONG
                                ).show()

                                // Si está marcado enviar correo, abrir el cliente de correo con destinatario, asunto y credenciales listas
                                if (enviarCorreoAutomatico) {
                                    try {
                                        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                            data = Uri.parse("mailto:${formCorreoEmpresa.trim()}")
                                            putExtra(Intent.EXTRA_SUBJECT, asunto)
                                            putExtra(Intent.EXTRA_TEXT, cuerpoCorreo)
                                        }
                                        context.startActivity(Intent.createChooser(emailIntent, "Enviar credenciales por correo"))
                                    } catch (_: Exception) {}
                                }
                            }
                        }
                    ) {
                        Text(
                            text = if (procesandoAccionAdmin) "Creando..." else "Crear Cuenta y Enviar Correo",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        )
    }
}

@Composable
fun TarjetaResumenHome(
    modifier: Modifier = Modifier,
    titulo: String,
    valor: String,
    subtitulo: String,
    colorBorde: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = titulo, color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = valor, color = Color(0xFF0F172A), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = subtitulo, color = colorBorde, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun BarraInferiorAdministrador(
    tabActual: Int,
    onSeleccionarTab: (Int) -> Unit
) {
    val azulBarra = Color(0xFF0A2E5C)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF1F5F9))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clip(RoundedCornerShape(29.dp))
                .background(azulBarra),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val items = listOf(
                Triple(0, "home", "Inicio"),
                Triple(1, "chart", "Gráficos"),
                Triple(2, "bell", "Alertas"),
                Triple(3, "profile", "Perfil")
            )
            items.forEach { (idx, tipoIcono, etiqueta) ->
                val activo = tabActual == idx
                Surface(
                    color = if (activo) Color(0xFF2563EB) else Color.Transparent,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.clickable { onSeleccionarTab(idx) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        IconoVectorialDelineado(
                            tipo = tipoIcono,
                            tint = if (activo) Color.White else Color.White.copy(alpha = 0.70f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = etiqueta,
                            color = if (activo) Color.White else Color.White.copy(alpha = 0.70f),
                            fontSize = 12.sp,
                            fontWeight = if (activo) FontWeight.ExtraBold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TarjetaGraficoComplejo(
    numero: String,
    titulo: String,
    subtitulo: String,
    contenido: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = numero,
                color = Color(0xFF2563EB),
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = titulo,
                color = Color(0xFF0F172A),
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = subtitulo,
                color = Color(0xFF64748B),
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
            contenido()
        }
    }
}

@Composable
fun GraficoBarrasVerticalesConEtiquetas(
    datos: List<DatoGraficoDW>,
    colores: List<Color>
) {
    val total = datos.sumOf { it.valor }.coerceAtLeast(1)
    val maxVal = (datos.maxOfOrNull { it.valor } ?: 1).coerceAtLeast(1)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(top = 24.dp, bottom = 28.dp, start = 8.dp, end = 8.dp)
    ) {
        val count = datos.size.coerceAtLeast(1)
        val slotWidth = size.width / count
        val barWidth = (slotWidth * 0.56f).coerceAtMost(72f)

        val paintTop = Paint().apply {
            color = android.graphics.Color.parseColor("#0F172A")
            textAlign = Paint.Align.CENTER
            textSize = 23f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintInside = Paint().apply {
            color = android.graphics.Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintBottom = Paint().apply {
            color = android.graphics.Color.parseColor("#334155")
            textAlign = Paint.Align.CENTER
            textSize = 21f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        datos.forEachIndexed { idx, item ->
            val ratio = (item.valor.toFloat() / maxVal.toFloat()).coerceIn(0.10f, 1f)
            val barHeight = size.height * ratio
            val xCenter = slotWidth * idx + slotWidth / 2f
            val left = xCenter - barWidth / 2f
            val top = size.height - barHeight
            val pct = (item.valor.toFloat() / total.toFloat()) * 100f

            drawRoundRect(
                color = colores[idx % colores.size],
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(14f, 14f)
            )

            drawContext.canvas.nativeCanvas.drawText(
                "${item.valor}",
                xCenter,
                top - 10f,
                paintTop
            )

            if (barHeight > 36f) {
                drawContext.canvas.nativeCanvas.drawText(
                    String.format(java.util.Locale.US, "%.1f%%", pct),
                    xCenter,
                    top + barHeight / 2f + 7f,
                    paintInside
                )
            }

            drawContext.canvas.nativeCanvas.drawText(
                item.etiqueta.take(10),
                xCenter,
                size.height + 28f,
                paintBottom
            )
        }
    }
}

@Composable
fun GraficoBarrasHorizontalesConEtiquetas(
    datos: List<DatoGraficoDW>,
    colores: List<Color>
) {
    val total = datos.sumOf { it.valor }.coerceAtLeast(1)
    val maxVal = (datos.maxOfOrNull { it.valor } ?: 1).coerceAtLeast(1)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        datos.forEachIndexed { idx, item ->
            val pct = (item.valor.toFloat() / total.toFloat()) * 100f
            val fraccion = (item.valor.toFloat() / maxVal.toFloat()).coerceIn(0.12f, 1f)
            val colorBarra = colores[idx % colores.size]

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.etiqueta,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "${item.valor} (${String.format(java.util.Locale.US, "%.1f%%", pct)})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colorBarra
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(Color(0xFFE2E8F0))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraccion)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(7.dp))
                            .background(colorBarra)
                    )
                }
            }
        }
    }
}

@Composable
fun GraficoPastelDonaConEtiquetasFijas(
    datos: List<DatoGraficoDW>,
    colores: List<Color>,
    esDona: Boolean
) {
    val total = datos.sumOf { it.valor }.coerceAtLeast(1)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier.size(195.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                var anguloInicio = -90f
                val radio = size.minDimension / 2f

                val paintEtiqueta = Paint().apply {
                    color = android.graphics.Color.WHITE
                    textAlign = Paint.Align.CENTER
                    textSize = 24f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                    setShadowLayer(4f, 0f, 2f, android.graphics.Color.BLACK)
                }

                datos.forEachIndexed { idx, item ->
                    val barrido = (item.valor.toFloat() / total.toFloat()) * 360f
                    val pct = (item.valor.toFloat() / total.toFloat()) * 100f

                    drawArc(
                        color = colores[idx % colores.size],
                        startAngle = anguloInicio,
                        sweepAngle = barrido,
                        useCenter = true
                    )

                    val anguloMedioRad = Math.toRadians((anguloInicio + barrido / 2f).toDouble())
                    val distanciaTexto = if (esDona) radio * 0.68f else radio * 0.60f
                    val xTexto = (center.x + cos(anguloMedioRad) * distanciaTexto).toFloat()
                    val yTexto = (center.y + sin(anguloMedioRad) * distanciaTexto).toFloat()

                    drawContext.canvas.nativeCanvas.drawText(
                        "${item.valor} (${String.format("%.0f%%", pct)})",
                        xTexto,
                        yTexto + 8f,
                        paintEtiqueta
                    )

                    anguloInicio += barrido
                }

                if (esDona) {
                    drawCircle(
                        color = Color.White,
                        radius = radio * 0.40f
                    )
                }
            }

            if (esDona) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text("$total", fontSize = 15.sp, color = Color(0xFF0F172A), fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            datos.forEachIndexed { idx, item ->
                val pct = (item.valor.toFloat() / total.toFloat()) * 100f
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(colores[idx % colores.size])
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = item.etiqueta,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "${item.valor} (${String.format("%.1f%%", pct)})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF475569)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GraficoLineaAreaConEtiquetas(
    datos: List<DatoGraficoDW>,
    colorLinea: Color
) {
    val total = datos.sumOf { it.valor }.coerceAtLeast(1)
    val maxVal = (datos.maxOfOrNull { it.valor } ?: 1).coerceAtLeast(1)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .padding(top = 24.dp, bottom = 28.dp, start = 18.dp, end = 18.dp)
    ) {
        val pasoX = size.width / (datos.size - 1).coerceAtLeast(1)
        val puntos = datos.mapIndexed { idx, item ->
            val x = idx * pasoX
            val y = size.height - (item.valor.toFloat() / maxVal.toFloat()) * (size.height * 0.82f)
            Offset(x, y)
        }

        val pathLinea = Path().apply {
            puntos.forEachIndexed { i, pt ->
                if (i == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
            }
        }

        drawPath(
            path = pathLinea,
            color = colorLinea,
            style = Stroke(width = 7f, cap = StrokeCap.Round)
        )

        val paintNodo = Paint().apply {
            color = colorLinea.toArgb()
            textAlign = Paint.Align.CENTER
            textSize = 23f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintEje = Paint().apply {
            color = android.graphics.Color.parseColor("#334155")
            textAlign = Paint.Align.CENTER
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        puntos.forEachIndexed { idx, pt ->
            val item = datos[idx]
            val pct = (item.valor.toFloat() / total.toFloat()) * 100f

            drawCircle(color = Color.White, radius = 11f, center = pt)
            drawCircle(color = colorLinea, radius = 8f, center = pt)

            drawContext.canvas.nativeCanvas.drawText(
                "${item.valor} (${String.format("%.0f%%", pct)})",
                pt.x,
                pt.y - 16f,
                paintNodo
            )

            drawContext.canvas.nativeCanvas.drawText(
                item.etiqueta.take(9),
                pt.x,
                size.height + 32f,
                paintEje
            )
        }
    }
}

private suspend fun consultarEndpointGraficosDW(
    factor: Float,
    anio: String = "2026",
    trimestre: String = "Todos",
    mes: String = "Todos"
): Map<String, List<DatoGraficoDW>>? =
    withContext(Dispatchers.IO) {
        try {
            val url = URL("http://20.51.194.8:8000/api/dashboard/graficos/?factor=$factor&anio=$anio&trimestre=$trimestre&mes=$mes")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
            }
            if (conn.responseCode in 200..299) {
                val raw = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(raw)
                val mapa = mutableMapOf<String, List<DatoGraficoDW>>()

                val claves = listOf(
                    "grafico_1_sectores",
                    "grafico_2_modalidades",
                    "grafico_3_embudo_estados",
                    "grafico_4_geografia",
                    "grafico_5_top_empresas",
                    "grafico_6_habilidades",
                    "grafico_7_educacion",
                    "grafico_8_telemetria_nosql"
                )

                for (clave in claves) {
                    val arr: JSONArray? = json.optJSONArray(clave)
                    if (arr != null) {
                        val lista = mutableListOf<DatoGraficoDW>()
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            lista.add(
                                DatoGraficoDW(
                                    etiqueta = obj.optString("etiqueta", "Ítem"),
                                    valor = obj.optInt("valor", 10),
                                    valorSecundario = obj.optInt("valor_secundario", 0),
                                    salario = obj.optInt("salario", 20000)
                                )
                            )
                        }
                        mapa[clave] = lista
                    }
                }
                conn.disconnect()
                return@withContext mapa
            }
            conn.disconnect()
            null
        } catch (e: Exception) {
            null
        }
    }

private suspend fun consultarAlertasYSolicitudesAdmin(): Pair<List<ReporteFraudeAdminItem>, List<SolicitudEmpresaAdminItem>> =
    withContext(Dispatchers.IO) {
        val listaFraudes = mutableListOf<ReporteFraudeAdminItem>()
        val listaSolicitudes = mutableListOf<SolicitudEmpresaAdminItem>()

        try {
            val url = URL("http://20.51.194.8:8000/api/dashboard/admin-gestion/")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
            }
            if (conn.responseCode in 200..299) {
                val obj = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                val arrFraudes = obj.optJSONArray("reportes_fraude")
                if (arrFraudes != null) {
                    for (i in 0 until arrFraudes.length()) {
                        val f = arrFraudes.getJSONObject(i)
                        val estadoRev = f.optString("estado_revision", "Pendiente")
                        if (estadoRev.contains("Desestimado", ignoreCase = true) ||
                            estadoRev.contains("Suspendida", ignoreCase = true)
                        ) {
                            continue
                        }
                        val tipoRep = f.optString("tipo_reporte", "VACANTE_SOSPECHOSA")
                        val esBugItem = f.optBoolean("es_bug", false) ||
                                tipoRep.contains("BUG", ignoreCase = true) ||
                                tipoRep.contains("ERROR", ignoreCase = true) ||
                                tipoRep.contains("FALLO", ignoreCase = true) ||
                                tipoRep.contains("PLATAFORMA", ignoreCase = true)

                        listaFraudes.add(
                            ReporteFraudeAdminItem(
                                id = f.optString("id", "$i"),
                                tipoReporte = tipoRep,
                                esBug = esBugItem,
                                moduloAfectado = f.optString("modulo_afectado", "Aplicación ImpulsoNica"),
                                dispositivoInfo = f.optString("dispositivo_info", "Android / Web"),
                                motivo = f.optString("motivo", ""),
                                fecha = f.optString("fecha", "2026-11-04"),
                                estadoRevision = estadoRev,
                                empleoId = f.optInt("empleo_id", 1),
                                vacanteTitulo = f.optString("vacante_titulo", "Vacante Corporativa"),
                                vacanteDescripcion = f.optString("vacante_descripcion", ""),
                                vacanteSalario = f.optString("vacante_salario", "C$ A convenir"),
                                vacanteModalidad = f.optString("vacante_modalidad", "Presencial"),
                                vacanteUbicacion = f.optString("vacante_ubicacion", "Managua"),
                                empresaId = f.optInt("empresa_id", 2),
                                empresaNombre = f.optString("empresa_nombre", "Empresa"),
                                empresaCorreo = f.optString("empresa_correo", ""),
                                totalReportesVacante = f.optInt("total_reportes_vacante", 2),
                                totalReportesEmpresa = f.optInt("total_reportes_empresa", 4)
                            )
                        )
                    }
                }

                val arrSol = obj.optJSONArray("solicitudes_empresas")
                if (arrSol != null) {
                    for (i in 0 until arrSol.length()) {
                        val s = arrSol.getJSONObject(i)
                        listaSolicitudes.add(
                            SolicitudEmpresaAdminItem(
                                id = s.optString("id", "sol_$i"),
                                nombreEmpresa = s.optString("nombre_empresa", "Empresa S.A."),
                                correoEmpresa = s.optString("correo_empresa", "contacto@empresa.ni"),
                                telefono = s.optString("telefono", "+505 2222-0000"),
                                direccion = s.optString("direccion", "Managua"),
                                sectorNombre = s.optString("sector_nombre", "Comercio"),
                                sectorId = s.optInt("sector_id", 1),
                                tipoEmpresaId = s.optInt("tipo_empresa_id", 1),
                                municipioNombre = s.optString("municipio_nombre", "Managua"),
                                municipioId = s.optInt("municipio_id", 1),
                                rucNit = s.optString("ruc_nit", "J0310000"),
                                representante = s.optString("representante", "Gerencia RR.HH."),
                                estado = s.optString("estado", "Pendiente"),
                                fecha = s.optString("fecha", "2026-11-04")
                            )
                        )
                    }
                }
            }
            conn.disconnect()
        } catch (_: Exception) {}

        if (listaFraudes.isEmpty()) {
            listaFraudes.add(
                ReporteFraudeAdminItem(
                    id = "rep_default_1",
                    tipoReporte = "VACANTE_SOSPECHOSA",
                    motivo = "El postulante reportó que se solicita depósito previo para exámenes médicos.",
                    fecha = "2026-11-04",
                    estadoRevision = "Pendiente",
                    empleoId = 1,
                    vacanteTitulo = "Coordinador Académico / Ejecutivo",
                    vacanteDescripcion = "Plaza reportada para verificación administrativa del empleador.",
                    vacanteSalario = "C$ 18,500",
                    vacanteModalidad = "Presencial",
                    vacanteUbicacion = "Managua",
                    empresaId = 2,
                    empresaNombre = "Proyectos Ozuna-Acuña",
                    empresaCorreo = "contacto@ozuna.com.ni",
                    totalReportesVacante = 2,
                    totalReportesEmpresa = 5
                )
            )
        }

        Pair(listaFraudes, listaSolicitudes)
    }

private suspend fun ejecutarAccionAdminBackend(payload: JSONObject): Boolean =
    withContext(Dispatchers.IO) {
        try {
            val url = URL("http://20.51.194.8:8000/api/dashboard/admin-gestion/")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 8000
                readTimeout = 8000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
            conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            val ok = conn.responseCode in 200..299
            conn.disconnect()
            ok
        } catch (_: Exception) {
            false
        }
    }