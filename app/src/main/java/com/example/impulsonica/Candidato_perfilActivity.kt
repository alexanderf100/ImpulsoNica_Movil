package com.example.impulsonica

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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

class Candidato_perfilActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                PantallaPerfilCandidatoCompletaWebStyle()
            }
        }
    }
}

@Composable
fun PantallaPerfilCandidatoCompletaWebStyle() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val prefs = context.getSharedPreferences("ImpulsoNicaPrefs", Context.MODE_PRIVATE)
    var candidatoId by remember {
        mutableStateOf(prefs.getInt("candidato_id", prefs.getInt("usuario_sql_id", 1)))
    }
    val correoGuardado = prefs.getString("correo_usuario", prefs.getString("user_email", "")) ?: ""
    val token = prefs.getString("access_token", prefs.getString("jwt_token", "")) ?: ""

    var cargando by remember { mutableStateOf(true) }
    var nombres by remember { mutableStateOf("") }
    var apellidos by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf(correoGuardado) }
    var telefono by remember { mutableStateOf("") }
    var fechaNacimiento by remember { mutableStateOf("") }
    var direccion by remember { mutableStateOf("") }
    var nacionalidad by remember { mutableStateOf("") }
    var genero by remember { mutableStateOf("") }
    var ubicacion by remember { mutableStateOf("") }
    var tituloProfesional by remember { mutableStateOf("") }
    var nivelEducativo by remember { mutableStateOf("Universitario Completo") }
    var sobreMi by remember { mutableStateOf("") }
    var habilidadesActuales by remember { mutableStateOf<List<String>>(emptyList()) }
    var idiomasActuales by remember { mutableStateOf<List<String>>(emptyList()) }
    var catalogoHabilidadesBD by remember {
        mutableStateOf(
            listOf(
                "Python", "JavaScript", "SQL", "Excel Avanzado",
                "AutoCAD", "Atención al Cliente", "Contabilidad", "Diseño Gráfico",
                "Gestión de Proyectos", "Inglés", "Kotlin", "Power BI"
            )
        )
    }
    var experienciasBD by remember { mutableStateOf<List<String>>(emptyList()) }
    var referenciasBD by remember { mutableStateOf<List<String>>(emptyList()) }

    // Modales interactivos del Perfil
    var mostrarModalEditarPerfilYCV by remember { mutableStateOf(false) }
    var mostrarModalNuevaExperiencia by remember { mutableStateOf(false) }
    var mostrarModalNuevaReferencia by remember { mutableStateOf(false) }

    // Campos de edición del Modal de Perfil + Habilidades
    var editNombres by remember { mutableStateOf("") }
    var editApellidos by remember { mutableStateOf("") }
    var editTelefono by remember { mutableStateOf("") }
    var editFechaNacimiento by remember { mutableStateOf("") }
    var editDireccion by remember { mutableStateOf("") }
    var editTitulo by remember { mutableStateOf("") }
    var editSobreMi by remember { mutableStateOf("") }
    var editNivelEducativo by remember { mutableStateOf("Universitario Completo") }
    var editHabilidadesSeleccionadas by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Campos para agregar experiencia o referencia
    var expCargo by remember { mutableStateOf("") }
    var expEmpresa by remember { mutableStateOf("") }
    var expPeriodo by remember { mutableStateOf("2024-03-14 al 2026-01-14") }
    var expDescripcion by remember { mutableStateOf("") }

    var refNombre by remember { mutableStateOf("") }
    var refEmpresaCargo by remember { mutableStateOf("") }
    var refTelefono by remember { mutableStateOf("") }

    val coloresInputNegro = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.Black,
        unfocusedTextColor = Color.Black,
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        focusedBorderColor = Color(0xFF0066CC),
        unfocusedBorderColor = Color(0xFF94A3B8),
        focusedLabelColor = Color(0xFF0A2E5C),
        unfocusedLabelColor = Color(0xFF475569),
        cursorColor = Color.Black
    )

    fun sincronizarCamposDeEdicion() {
        editNombres = nombres
        editApellidos = apellidos
        editTelefono = if (telefono == "Sin teléfono registrado") "" else telefono
        editFechaNacimiento = fechaNacimiento
        editDireccion = direccion
        editTitulo = tituloProfesional
        editSobreMi = sobreMi
        editNivelEducativo = nivelEducativo
        editHabilidadesSeleccionadas = habilidadesActuales.toSet()
    }

    fun cargarTodoElPerfilDesdeBD() {
        scope.launch {
            cargando = true
            val cv = consultarCurriculumCompletoCandidato(
                candidatoId = candidatoId,
                mensajeNotificacion = "",
                correoPreferido = correoGuardado,
                token = token
            )
            candidatoId = cv.candidatoId
            nombres = cv.nombres
            apellidos = cv.apellidos
            correo = cv.correo
            telefono = cv.telefono
            fechaNacimiento = cv.fechaNacimiento
            direccion = cv.direccion
            nacionalidad = cv.nacionalidad
            genero = cv.genero
            ubicacion = cv.ubicacion
            tituloProfesional = cv.tituloProfesional
            nivelEducativo = cv.nivelEducativo
            sobreMi = cv.sobreMi
            habilidadesActuales = cv.habilidades
            idiomasActuales = cv.idiomas
            experienciasBD = cv.experiencias
            referenciasBD = cv.referencias
            if (cv.catalogoHabilidades.isNotEmpty()) {
                catalogoHabilidadesBD = (cv.catalogoHabilidades + catalogoHabilidadesBD).distinct()
            }

            prefs.edit()
                .putInt("candidato_id", cv.candidatoId)
                .putString("nombre_usuario", cv.nombreCompleto)
                .apply()

            sincronizarCamposDeEdicion()
            cargando = false
        }
    }

    LaunchedEffect(Unit) {
        cargarTodoElPerfilDesdeBD()
    }

    val azulOscuro = Color(0xFF0A2E5C)
    val fondoGris = Color(0xFFF4F6F9)
    val nombreCompletoMostrar = "$nombres $apellidos".trim().ifBlank { "Cargando perfil..." }

    Scaffold(
        containerColor = azulOscuro,
        bottomBar = {
            BarraInferiorCandidato5Iconos(context = context, itemActivo = 4)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Cabecera Superior con Avatar, Nombre, Título y Botón Editar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = Color.White
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        IconoVectorialDelineado(
                            tipo = "profile",
                            tint = azulOscuro,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Correo: $correo  •  Tel: $telefono",
                        color = Color.White.copy(alpha = 0.90f),
                        fontSize = 12.sp
                    )
                    Text(
                        text = "$nivelEducativo • $ubicacion",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            sincronizarCamposDeEdicion()
                            mostrarModalEditarPerfilYCV = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Editar Mi Perfil",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Contenedor Principal con TODAS las secciones iguales a PCandidato.html en la Web
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = fondoGris,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                if (cargando) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = azulOscuro)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 1. FICHA DE DATOS PERSONALES COMPLETA (IGUAL A LA WEB)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Información Personal",
                                    color = azulOscuro,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp
                                )
                                Divider(color = Color(0xFFE2E8F0))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Nacionalidad: ${nacionalidad.ifBlank { "Nicaragüense" }}", fontSize = 13.sp, color = Color(0xFF334155))
                                    Text("Género: ${genero.ifBlank { "Masculino" }}", fontSize = 13.sp, color = Color(0xFF334155))
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Nacimiento: ${fechaNacimiento.ifBlank { "No registrada" }}", fontSize = 13.sp, color = Color(0xFF334155))
                                    Text("Dirección: ${direccion.ifBlank { ubicacion }}", fontSize = 13.sp, color = Color(0xFF334155))
                                }
                            }
                        }

                        // 2. SECCIÓN: SOBRE MÍ
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Sobre mí",
                                    color = azulOscuro,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp
                                )
                                Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFE2E8F0))
                                Text(
                                    text = if (sobreMi.isNotBlank()) sobreMi else "Sin descripción registrada en el currículum.",
                                    color = Color(0xFF1E293B),
                                    fontSize = 14.sp
                                )
                            }
                        }

                        // 3. SECCIÓN: HABILIDADES TÉCNICAS E IDIOMAS (CON CHIPS AZULES IGUAL A LA WEB)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
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
                                    Text(
                                        text = "Habilidades Técnicas",
                                        color = azulOscuro,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "Editar →",
                                        color = Color(0xFF0066CC),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.clickable {
                                            sincronizarCamposDeEdicion()
                                            mostrarModalEditarPerfilYCV = true
                                        }
                                    )
                                }
                                Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFE2E8F0))
                                if (habilidadesActuales.isEmpty()) {
                                    Text("No hay habilidades registradas.", color = Color.Gray, fontSize = 13.sp)
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        habilidadesActuales.chunked(3).forEach { fila ->
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                fila.forEach { hab ->
                                                    Surface(
                                                        color = Color(0xFFEFF6FF),
                                                        shape = RoundedCornerShape(16.dp)
                                                    ) {
                                                        Text(
                                                            text = hab,
                                                            color = Color(0xFF0284C7),
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp,
                                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                if (idiomasActuales.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Idiomas",
                                        color = azulOscuro,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    idiomasActuales.forEach { idm ->
                                        Text("• $idm", fontSize = 13.sp, color = Color(0xFF334155))
                                    }
                                }
                            }
                        }

                        // 4. SECCIÓN: EXPERIENCIA LABORAL (VISIBLE EN PANTALLA IGUAL A LA WEB)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
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
                                    Text(
                                        text = "Experiencia Laboral",
                                        color = azulOscuro,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp
                                    )
                                    OutlinedButton(
                                        onClick = { mostrarModalNuevaExperiencia = true },
                                        shape = RoundedCornerShape(18.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                    ) {
                                        Text("+ Añadir", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = azulOscuro)
                                    }
                                }
                                Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFE2E8F0))
                                if (experienciasBD.isEmpty()) {
                                    Text("Sin experiencias laborales registradas.", color = Color.Gray, fontSize = 13.sp)
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        experienciasBD.forEach { exp ->
                                            Surface(
                                                color = Color(0xFFF8FAFC),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(12.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.Top
                                                ) {
                                                    Text(
                                                        text = exp,
                                                        color = Color(0xFF1E293B),
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    IconButton(
                                                        onClick = {
                                                            experienciasBD = experienciasBD.filterNot { it == exp }
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        IconoVectorialDelineado(
                                                            tipo = "trash",
                                                            tint = Color(0xFFDC2626),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 5. SECCIÓN: REFERENCIAS PROFESIONALES (VISIBLE EN PANTALLA IGUAL A LA WEB)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
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
                                    Text(
                                        text = "Referencias",
                                        color = azulOscuro,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp
                                    )
                                    OutlinedButton(
                                        onClick = { mostrarModalNuevaReferencia = true },
                                        shape = RoundedCornerShape(18.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                    ) {
                                        Text("+ Añadir", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = azulOscuro)
                                    }
                                }
                                Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFE2E8F0))
                                if (referenciasBD.isEmpty()) {
                                    Text("Sin referencias registradas.", color = Color.Gray, fontSize = 13.sp)
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        referenciasBD.forEach { ref ->
                                            Surface(
                                                color = Color(0xFFF8FAFC),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(12.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.Top
                                                ) {
                                                    Text(
                                                        text = ref,
                                                        color = Color(0xFF1E293B),
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    IconButton(
                                                        onClick = {
                                                            referenciasBD = referenciasBD.filterNot { it == ref }
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        IconoVectorialDelineado(
                                                            tipo = "trash",
                                                            tint = Color(0xFFDC2626),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 6. ACCESOS DIRECTOS A POSTULACIONES Y CERRAR SESIÓN
                        OpcionPerfilCandidato(
                            titulo = "Ver Mis Postulaciones Recientes",
                            onClick = {
                                context.startActivity(Intent(context, Candidato_postulacionesActivity::class.java))
                            }
                        )

                        OpcionPerfilCandidato(
                            titulo = "Cerrar sesión",
                            esDestructivo = true,
                            onClick = {
                                prefs.edit().clear().apply()
                                val intent = Intent(context, MainActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                context.startActivity(intent)
                            }
                        )

                        Spacer(modifier = Modifier.height(60.dp))
                    }
                }
            }
        }
    }

    // MODAL: EDITAR PERFIL + ELEGIR HABILIDADES PREDEFINIDAS (PRE-LLENADO CON TODOS LOS DATOS DE LA BD)
    if (mostrarModalEditarPerfilYCV) {
        AlertDialog(
            onDismissRequest = { mostrarModalEditarPerfilYCV = false },
            containerColor = Color.White,
            title = {
                Text(
                    text = "Editar Mi Perfil y Habilidades",
                    color = azulOscuro,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 19.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = editNombres,
                        onValueChange = { editNombres = it },
                        label = { Text("Nombre *") },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.Black, fontWeight = FontWeight.SemiBold),
                        colors = coloresInputNegro,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editApellidos,
                        onValueChange = { editApellidos = it },
                        label = { Text("Apellido *") },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.Black, fontWeight = FontWeight.SemiBold),
                        colors = coloresInputNegro,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editTelefono,
                        onValueChange = { editTelefono = it },
                        label = { Text("Teléfono") },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.Black, fontWeight = FontWeight.SemiBold),
                        colors = coloresInputNegro,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editFechaNacimiento,
                        onValueChange = { editFechaNacimiento = it },
                        label = { Text("Fecha Nacimiento (YYYY-MM-DD)") },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.Black, fontWeight = FontWeight.SemiBold),
                        colors = coloresInputNegro,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editDireccion,
                        onValueChange = { editDireccion = it },
                        label = { Text("Dirección Exacta") },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.Black, fontWeight = FontWeight.SemiBold),
                        colors = coloresInputNegro,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editTitulo,
                        onValueChange = { editTitulo = it },
                        label = { Text("Título / Profesión") },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.Black, fontWeight = FontWeight.SemiBold),
                        colors = coloresInputNegro,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editSobreMi,
                        onValueChange = { editSobreMi = it },
                        label = { Text("Sobre mí (Resumen de tu CV)") },
                        textStyle = TextStyle(color = Color.Black, fontWeight = FontWeight.SemiBold),
                        colors = coloresInputNegro,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Nivel Educativo:", fontWeight = FontWeight.Bold, color = azulOscuro, fontSize = 13.sp)
                    listOf("Maestría", "Universitario Completo", "Técnico Superior", "Secundaria Completa").forEach { niv ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { editNivelEducativo = niv }
                        ) {
                            RadioButton(
                                selected = editNivelEducativo.equals(niv, ignoreCase = true),
                                onClick = { editNivelEducativo = niv }
                            )
                            Text(niv, fontSize = 13.sp, color = Color.Black)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Selecciona tus Habilidades Predefinidas (Toca para marcar/desmarcar):",
                        fontWeight = FontWeight.ExtraBold,
                        color = azulOscuro,
                        fontSize = 13.sp
                    )

                    val listaCombinada = (catalogoHabilidadesBD + editHabilidadesSeleccionadas).distinct()
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listaCombinada.chunked(2).forEach { par ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                par.forEach { hab ->
                                    val marcada = editHabilidadesSeleccionadas.any { it.equals(hab, ignoreCase = true) }
                                    Surface(
                                        color = if (marcada) Color(0xFF0066CC) else Color(0xFFF1F5F9),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                editHabilidadesSeleccionadas = if (marcada) {
                                                    editHabilidadesSeleccionadas.filterNot { it.equals(hab, ignoreCase = true) }.toSet()
                                                } else {
                                                    editHabilidadesSeleccionadas + hab
                                                }
                                            }
                                    ) {
                                        Text(
                                            text = if (marcada) "✓ $hab" else "+ $hab",
                                            color = if (marcada) Color.White else Color(0xFF1E293B),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066CC)),
                    onClick = {
                        scope.launch {
                            nombres = editNombres.trim()
                            apellidos = editApellidos.trim()
                            telefono = editTelefono.trim()
                            fechaNacimiento = editFechaNacimiento.trim()
                            direccion = editDireccion.trim()
                            tituloProfesional = editTitulo.trim()
                            sobreMi = editSobreMi.trim()
                            nivelEducativo = editNivelEducativo
                            habilidadesActuales = editHabilidadesSeleccionadas.toList()

                            guardarPerfilYHabilidadesEnSQL(
                                token = token,
                                candidatoId = candidatoId,
                                nombres = nombres,
                                apellidos = apellidos,
                                telefono = telefono,
                                direccion = direccion,
                                tituloProfesional = tituloProfesional,
                                sobreMi = sobreMi,
                                habilidades = habilidadesActuales
                            )
                            prefs.edit().putString("nombre_usuario", "$nombres $apellidos".trim()).apply()
                            mostrarModalEditarPerfilYCV = false
                            Toast.makeText(context, "Perfil y habilidades actualizados", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Guardar Cambios", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarModalEditarPerfilYCV = false }) {
                    Text("Cancelar", color = azulOscuro, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // MODAL: AÑADIR EXPERIENCIA LABORAL
    if (mostrarModalNuevaExperiencia) {
        AlertDialog(
            onDismissRequest = { mostrarModalNuevaExperiencia = false },
            containerColor = Color.White,
            title = { Text("Añadir Experiencia Laboral", fontWeight = FontWeight.ExtraBold, color = azulOscuro) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = expCargo,
                        onValueChange = { expCargo = it },
                        label = { Text("Cargo o Puesto *") },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.Black, fontWeight = FontWeight.SemiBold),
                        colors = coloresInputNegro
                    )
                    OutlinedTextField(
                        value = expEmpresa,
                        onValueChange = { expEmpresa = it },
                        label = { Text("Empresa *") },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.Black, fontWeight = FontWeight.SemiBold),
                        colors = coloresInputNegro
                    )
                    OutlinedTextField(
                        value = expPeriodo,
                        onValueChange = { expPeriodo = it },
                        label = { Text("Período (Ej: 2024-03-14 al 2026-01-14)") },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.Black, fontWeight = FontWeight.SemiBold),
                        colors = coloresInputNegro
                    )
                    OutlinedTextField(
                        value = expDescripcion,
                        onValueChange = { expDescripcion = it },
                        label = { Text("Funciones principales") },
                        textStyle = TextStyle(color = Color.Black, fontWeight = FontWeight.SemiBold),
                        colors = coloresInputNegro
                    )
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066CC)),
                    onClick = {
                        if (expCargo.isNotBlank() && expEmpresa.isNotBlank()) {
                            val nueva = "${expCargo.trim()} | ${expEmpresa.trim()} (${expPeriodo.trim()})\n${expDescripcion.trim()}"
                            experienciasBD = experienciasBD + nueva
                            expCargo = ""
                            expEmpresa = ""
                            expDescripcion = ""
                            mostrarModalNuevaExperiencia = false
                            Toast.makeText(context, "Experiencia agregada a tu perfil", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Guardar Experiencia", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarModalNuevaExperiencia = false }) {
                    Text("Cerrar", color = azulOscuro)
                }
            }
        )
    }

    // MODAL: AÑADIR REFERENCIA PROFESIONAL
    if (mostrarModalNuevaReferencia) {
        AlertDialog(
            onDismissRequest = { mostrarModalNuevaReferencia = false },
            containerColor = Color.White,
            title = { Text("Añadir Referencia Profesional", fontWeight = FontWeight.ExtraBold, color = azulOscuro) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = refNombre,
                        onValueChange = { refNombre = it },
                        label = { Text("Nombre del Contacto *") },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.Black, fontWeight = FontWeight.SemiBold),
                        colors = coloresInputNegro
                    )
                    OutlinedTextField(
                        value = refEmpresaCargo,
                        onValueChange = { refEmpresaCargo = it },
                        label = { Text("Cargo y Empresa (Ej: Gerente TI - E-tech) *") },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.Black, fontWeight = FontWeight.SemiBold),
                        colors = coloresInputNegro
                    )
                    OutlinedTextField(
                        value = refTelefono,
                        onValueChange = { refTelefono = it },
                        label = { Text("Teléfono y/o Correo *") },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.Black, fontWeight = FontWeight.SemiBold),
                        colors = coloresInputNegro
                    )
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066CC)),
                    onClick = {
                        if (refNombre.isNotBlank() && refTelefono.isNotBlank()) {
                            val nueva = "${refNombre.trim()}\n${refEmpresaCargo.trim()}\n📞 ${refTelefono.trim()}"
                            referenciasBD = referenciasBD + nueva
                            refNombre = ""
                            refEmpresaCargo = ""
                            refTelefono = ""
                            mostrarModalNuevaReferencia = false
                            Toast.makeText(context, "Referencia profesional guardada", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Guardar Referencia", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarModalNuevaReferencia = false }) {
                    Text("Cerrar", color = azulOscuro)
                }
            }
        )
    }
}

@Composable
fun OpcionPerfilCandidato(
    titulo: String,
    esDestructivo: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = titulo,
                color = if (esDestructivo) Color(0xFFDC2626) else Color(0xFF0A2E5C),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f)
            )
            Text("❯", color = Color(0xFF64748B), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

private suspend fun guardarPerfilYHabilidadesEnSQL(
    token: String,
    candidatoId: Int,
    nombres: String,
    apellidos: String,
    telefono: String,
    direccion: String,
    tituloProfesional: String,
    sobreMi: String,
    habilidades: List<String>
): Boolean = withContext(Dispatchers.IO) {
    try {
        val urlCand = URL("http://20.51.194.8:8000/api/candidatos/$candidatoId/")
        val connC = (urlCand.openConnection() as HttpURLConnection).apply {
            requestMethod = "PATCH"
            connectTimeout = 6000
            readTimeout = 6000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            if (token.isNotBlank()) setRequestProperty("Authorization", "Bearer $token")
        }
        val bodyCand = JSONObject().apply {
            put("nombre", nombres)
            put("nombres", nombres)
            put("apellido", apellidos)
            put("apellidos", apellidos)
            put("telefono", telefono)
            put("direccion", direccion)
        }
        connC.outputStream.use { it.write(bodyCand.toString().toByteArray(Charsets.UTF_8)) }
        connC.responseCode
        connC.disconnect()

        val url = URL("http://20.51.194.8:8000/api/dashboard/curriculum-completo/")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 6000
            readTimeout = 6000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            if (token.isNotBlank()) setRequestProperty("Authorization", "Bearer $token")
        }
        val payload = JSONObject().apply {
            put("candidato_id", candidatoId)
            put("nombres", nombres)
            put("apellidos", apellidos)
            put("telefono", telefono)
            put("direccion", direccion)
            put("titulo_profesional", tituloProfesional)
            put("sobre_mi", sobreMi)
            put("habilidades", JSONArray(habilidades))
        }
        conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
        val ok = conn.responseCode in 200..299
        conn.disconnect()
        ok
    } catch (_: Exception) {
        false
    }
}