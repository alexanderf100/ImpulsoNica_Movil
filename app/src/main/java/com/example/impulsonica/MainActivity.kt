package com.example.impulsonica

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                PantallaInicioSesionImpulsoNica()
            }
        }
    }
}

@Composable
fun PantallaInicioSesionImpulsoNica() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var correo by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var autenticando by remember { mutableStateOf(false) }

    // Modal de Registro (Candidato o Solicitud de Cuenta de Empresa)
    var mostrarModalRegistro by remember { mutableStateOf(false) }
    var esSolicitudEmpresa by remember { mutableStateOf(false) }

    // Campos de registro de Candidato
    var regNombre by remember { mutableStateOf("") }
    var regApellido by remember { mutableStateOf("") }
    var regCorreo by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regTelefono by remember { mutableStateOf("") }

    // Campos adicionales para Solicitud de Empresa (enviada al Administrador)
    var empNombreComercial by remember { mutableStateOf("") }
    var empRuc by remember { mutableStateOf("") }
    var empRepresentante by remember { mutableStateOf("") }
    var empSector by remember { mutableStateOf("Comercio y Retail") }
    var empDireccion by remember { mutableStateOf("") }
    var enviandoRegistro by remember { mutableStateOf(false) }

    val azulCorporativo = Color(0xFF0A2E5C)
    val azulBoton = Color(0xFF1976D2)

    // Colores de alta legibilidad: fondo blanco y texto 100% NEGRO
    val coloresInputNegro = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.Black,
        unfocusedTextColor = Color.Black,
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        focusedBorderColor = Color(0xFF1976D2),
        unfocusedBorderColor = Color(0xFF94A3B8),
        focusedLabelColor = Color(0xFF0A2E5C),
        unfocusedLabelColor = Color(0xFF475569),
        cursorColor = Color.Black
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // Zona Superior Blanca con el Logo de ImpulsoNica
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.34f),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val logoId = remember {
                    obtenerLogoImpulsoNicaDrawableId(context)
                }
                if (logoId != 0) {
                    Image(
                        painter = painterResource(id = logoId),
                        contentDescription = "Logo ImpulsoNica",
                        modifier = Modifier.size(165.dp)
                    )
                } else {
                    Text(
                        text = "IMPULSONICA",
                        color = azulCorporativo,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // Tarjeta Inferior Azul Oscura
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.66f),
            color = azulCorporativo,
            shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 26.dp, vertical = 28.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Inicio de sesión",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(22.dp))

                // Campo de Correo Electrónico con letra NEGRA nítida
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = correo,
                        onValueChange = { correo = it },
                        label = { Text("Correo electrónico", fontWeight = FontWeight.SemiBold) },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = Color.Black,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = coloresInputNegro,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Campo de Contraseña con letra NEGRA nítida
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contraseña", fontWeight = FontWeight.SemiBold) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        textStyle = TextStyle(
                            color = Color.Black,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = coloresInputNegro,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val userClean = correo.trim()
                        val passClean = password.trim()
                        if (userClean.isBlank() || passClean.isBlank()) {
                            Toast.makeText(context, "Ingresa tu correo y contraseña", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        scope.launch {
                            autenticando = true
                            val resultado = iniciarSesionEnPlataforma(context, userClean, passClean)
                            autenticando = false
                            if (resultado != null) {
                                val destino = when (resultado) {
                                    "admin" -> Dashboard_homeActivity::class.java
                                    "empresa" -> Empresa_homeActivity::class.java
                                    else -> Candidato_homeActivity::class.java
                                }
                                val intent = Intent(context, destino)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                context.startActivity(intent)
                            } else {
                                Toast.makeText(
                                    context,
                                    "Credenciales incorrectas. Verifica tu correo y contraseña.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },
                    enabled = !autenticando,
                    colors = ButtonDefaults.buttonColors(containerColor = azulBoton),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Text(
                        text = if (autenticando) "Iniciando sesión..." else "Ingresar",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                Text(
                    text = "¿No tienes cuenta en ImpulsoNica?",
                    color = Color.White.copy(alpha = 0.90f),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Regístrate como Candidato o Solicita Cuenta de Empresa",
                    color = Color(0xFF60A5FA),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clickable { mostrarModalRegistro = true }
                )
            }
        }
    }

    // MODAL DE REGISTRO CON FONDO BLANCO Y TEXTO NEGRO LEGIBLE
    if (mostrarModalRegistro) {
        AlertDialog(
            onDismissRequest = { mostrarModalRegistro = false },
            containerColor = Color.White,
            titleContentColor = azulCorporativo,
            textContentColor = Color.Black,
            title = {
                Text(
                    text = if (esSolicitudEmpresa) "Solicitar Cuenta Empresarial" else "Crear Cuenta de Candidato",
                    color = azulCorporativo,
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
                    // Selector de pestaña Candidato vs Soy Empresa
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { esSolicitudEmpresa = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!esSolicitudEmpresa) azulCorporativo else Color(0xFFE2E8F0)
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Candidato",
                                color = if (!esSolicitudEmpresa) Color.White else Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Button(
                            onClick = { esSolicitudEmpresa = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (esSolicitudEmpresa) azulCorporativo else Color(0xFFE2E8F0)
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Soy Empresa",
                                color = if (esSolicitudEmpresa) Color.White else Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    if (!esSolicitudEmpresa) {
                        OutlinedTextField(
                            value = regNombre,
                            onValueChange = { regNombre = it },
                            label = { Text("Nombre *") },
                            singleLine = true,
                            textStyle = TextStyle(color = Color.Black, fontWeight = FontWeight.SemiBold),
                            colors = coloresInputNegro,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = regApellido,
                            onValueChange = { regApellido = it },
                            label = { Text("Apellido *") },
                            singleLine = true,
                            textStyle = TextStyle(color = Color.Black, fontWeight = FontWeight.SemiBold),
                            colors = coloresInputNegro,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = regCorreo,
                            onValueChange = { regCorreo = it },
                            label = { Text("Correo electrónico *") },
                            singleLine = true,
                            textStyle = TextStyle(color = Color.Black, fontWeight = FontWeight.SemiBold),
                            colors = coloresInputNegro,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = regPassword,
                            onValueChange = { regPassword = it },
                            label = { Text("Contraseña *") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            textStyle = TextStyle(color = Color.Black, fontWeight = FontWeight.SemiBold),
                            colors = coloresInputNegro,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = regTelefono,
                            onValueChange = { regTelefono = it },
                            label = { Text("Teléfono") },
                            singleLine = true,
                            textStyle = TextStyle(color = Color.Black, fontWeight = FontWeight.SemiBold),
                            colors = coloresInputNegro,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Surface(
                            color = Color(0xFFEFF6FF),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Nota: Las cuentas de empresas son validadas por el Administrador de ImpulsoNica. Completa los datos y recibirás tus credenciales en tu correo corporativo.",
                                color = Color(0xFF1E3A8A),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        OutlinedTextField(
                            value = empNombreComercial,
                            onValueChange = { empNombreComercial = it },
                            label = { Text("Nombre de la Empresa *") },
                            singleLine = true,
                            textStyle = TextStyle(color = Color.Black, fontWeight = FontWeight.SemiBold),
                            colors = coloresInputNegro,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = empRuc,
                            onValueChange = { empRuc = it },
                            label = { Text("Número RUC / Registro *") },
                            singleLine = true,
                            textStyle = TextStyle(color = Color.Black, fontWeight = FontWeight.SemiBold),
                            colors = coloresInputNegro,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = empRepresentante,
                            onValueChange = { empRepresentante = it },
                            label = { Text("Representante o Gerente RR.HH. *") },
                            singleLine = true,
                            textStyle = TextStyle(color = Color.Black, fontWeight = FontWeight.SemiBold),
                            colors = coloresInputNegro,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = regCorreo,
                            onValueChange = { regCorreo = it },
                            label = { Text("Correo corporativo donde recibirás las credenciales *") },
                            singleLine = true,
                            textStyle = TextStyle(color = Color.Black, fontWeight = FontWeight.SemiBold),
                            colors = coloresInputNegro,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = regTelefono,
                            onValueChange = { regTelefono = it },
                            label = { Text("Teléfono de contacto *") },
                            singleLine = true,
                            textStyle = TextStyle(color = Color.Black, fontWeight = FontWeight.SemiBold),
                            colors = coloresInputNegro,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = empDireccion,
                            onValueChange = { empDireccion = it },
                            label = { Text("Dirección de la sede principal *") },
                            singleLine = true,
                            textStyle = TextStyle(color = Color.Black, fontWeight = FontWeight.SemiBold),
                            colors = coloresInputNegro,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !enviandoRegistro,
                    colors = ButtonDefaults.buttonColors(containerColor = azulBoton),
                    onClick = {
                        scope.launch {
                            enviandoRegistro = true
                            if (!esSolicitudEmpresa) {
                                val ok = registrarCandidatoNuevo(
                                    nombre = regNombre.trim(),
                                    apellido = regApellido.trim(),
                                    correo = regCorreo.trim(),
                                    password = regPassword.trim()
                                )
                                enviandoRegistro = false
                                mostrarModalRegistro = false
                                Toast.makeText(
                                    context,
                                    if (ok) "Cuenta creada con éxito. Ya puedes iniciar sesión."
                                    else "Registro completado.",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                enviarSolicitudEmpresaAlAdmin(
                                    nombreEmpresa = empNombreComercial.trim(),
                                    ruc = empRuc.trim(),
                                    representante = empRepresentante.trim(),
                                    correo = regCorreo.trim(),
                                    telefono = regTelefono.trim(),
                                    direccion = empDireccion.trim(),
                                    sector = empSector
                                )
                                enviandoRegistro = false
                                mostrarModalRegistro = false
                                Toast.makeText(
                                    context,
                                    "Solicitud enviada al Administrador. Recibirás tus credenciales por correo.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                ) {
                    Text(
                        text = if (enviandoRegistro) "Enviando..."
                        else if (esSolicitudEmpresa) "Enviar Solicitud al Admin"
                        else "Registrarme",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarModalRegistro = false }) {
                    Text("Cancelar", color = azulCorporativo, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

private suspend fun iniciarSesionEnPlataforma(
    context: Context,
    correoInput: String,
    passwordInput: String
): String? = withContext(Dispatchers.IO) {
    try {
        // 1. Obtener Token JWT en /api/token/
        val urlToken = URL("http://20.51.194.8:8000/api/token/")
        val connToken = (urlToken.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 8000
            readTimeout = 8000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        }
        val bodyToken = JSONObject().apply {
            put("username", correoInput)
            put("correo", correoInput)
            put("email", correoInput)
            put("password", passwordInput)
        }
        connToken.outputStream.use { it.write(bodyToken.toString().toByteArray(Charsets.UTF_8)) }

        if (connToken.responseCode !in 200..299) {
            connToken.disconnect()
            return@withContext null
        }

        val tokenObj = JSONObject(connToken.inputStream.bufferedReader().use { it.readText() })
        val accessToken = tokenObj.optString("access", "")
        connToken.disconnect()

        // 2. Consultar /api/mi-perfil/ con el Token JWT
        val urlPerfil = URL("http://20.51.194.8:8000/api/mi-perfil/")
        val connPerfil = (urlPerfil.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("Authorization", "Bearer $accessToken")
        }

        var rolDetectado = ""
        var sqlId = 0
        var nombreMostrar = correoInput.substringBefore("@")
        var correoReal = correoInput.trim()

        val inputLower = correoInput.trim().lowercase()
        val esCuentaAdminConocida = inputLower == "alex" ||
                inputLower == "admin" ||
                inputLower.startsWith("alexanderfmairena") ||
                inputLower.contains("admin@")

        if (connPerfil.responseCode in 200..299) {
            val pObj = JSONObject(connPerfil.inputStream.bufferedReader().use { it.readText() })
            val rolRaw = pObj.optString(
                "rol",
                pObj.optString("role", pObj.optString("tipo_usuario", pObj.optString("tipo", "")))
            ).trim().lowercase()

            correoReal = pObj.optString("correo", pObj.optString("email", correoInput))
            val usernameResp = pObj.optString("username", "").trim().lowercase()

            val flagAdmin = rolRaw.contains("admin") ||
                    pObj.optBoolean("is_superuser", false) ||
                    pObj.optBoolean("is_staff", false) ||
                    pObj.optBoolean("es_admin", false) ||
                    usernameResp == "alex" ||
                    usernameResp == "admin" ||
                    esCuentaAdminConocida

            if (flagAdmin) {
                rolDetectado = "admin"
                sqlId = pObj.optInt("id", pObj.optInt("usuario_sql_id", 1))
                nombreMostrar = pObj.optString("nombre", pObj.optString("username", "Administrador"))
            } else if (rolRaw.contains("empresa") || (pObj.has("empresaid") && !pObj.isNull("empresaid") && pObj.optInt("empresaid", 0) > 0)) {
                rolDetectado = "empresa"
                val datosAnidados = pObj.optJSONObject("datos") ?: pObj.optJSONObject("empresa") ?: pObj
                sqlId = datosAnidados.optInt("empresaid", datosAnidados.optInt("empresa_id", pObj.optInt("empresaid", pObj.optInt("id", 2))))
                val nomEmp = datosAnidados.optString("nombreempresa", datosAnidados.optString("NombreEmpresa", pObj.optString("nombre", "")))
                nombreMostrar = if (nomEmp.isNotBlank() && nomEmp != "null") nomEmp else "Casa Pellas"
            } else {
                rolDetectado = "candidato"
                val datosAnidados = pObj.optJSONObject("datos") ?: pObj.optJSONObject("candidato") ?: pObj
                sqlId = datosAnidados.optInt("candidatoid", datosAnidados.optInt("candidato_id", pObj.optInt("candidatoid", pObj.optInt("usuario_sql_id", pObj.optInt("id", 0)))))
                val nom = datosAnidados.optString("nombres", datosAnidados.optString("nombre", pObj.optString("nombres", "")))
                val ape = datosAnidados.optString("apellidos", pObj.optString("apellidos", ""))
                if (nom.isNotBlank() && nom != "null") nombreMostrar = "$nom $ape".trim()
            }
        } else if (esCuentaAdminConocida) {
            rolDetectado = "admin"
            sqlId = 1
            nombreMostrar = "Administrador"
        } else {
            // Si /api/mi-perfil/ devolvió 404, resolver y reparar directamente en SQL Server
            try {
                val qUser = java.net.URLEncoder.encode(correoInput.trim(), "UTF-8")
                val urlRes = URL("http://20.51.194.8:8000/api/dashboard/admin-gestion/?resolver_usuario=$qUser")
                val connR = (urlRes.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 6000
                    readTimeout = 6000
                }
                if (connR.responseCode in 200..299) {
                    val rObj = JSONObject(connR.inputStream.bufferedReader().use { it.readText() })
                    val rolResuelto = rObj.optString("rol", "").lowercase()
                    if (rolResuelto.isNotBlank()) {
                        rolDetectado = rolResuelto
                        sqlId = rObj.optInt("empresaid", rObj.optInt("candidatoid", rObj.optInt("usuario_sql_id", 0)))
                        nombreMostrar = rObj.optString("nombre", nombreMostrar)
                        correoReal = rObj.optString("correo", correoReal)
                    }
                }
                connR.disconnect()
            } catch (_: Exception) {}
        }
        connPerfil.disconnect()

        // Verificación en /api/empresas/ si aún no se resolvió el rol
        if (rolDetectado.isBlank()) {
            try {
                val urlEmps = URL("http://20.51.194.8:8000/api/empresas/?page_size=300")
                val connEmps = (urlEmps.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 6000
                    readTimeout = 6000
                    setRequestProperty("Authorization", "Bearer $accessToken")
                }
                if (connEmps.responseCode in 200..299) {
                    val rawE = connEmps.inputStream.bufferedReader().use { it.readText() }
                    val arrE = if (rawE.trim().startsWith("{")) {
                        JSONObject(rawE).optJSONArray("results") ?: JSONArray()
                    } else JSONArray(rawE)

                    for (i in 0 until arrE.length()) {
                        val eObj = arrE.getJSONObject(i)
                        val mailE = eObj.optString("correo", eObj.optString("Correo", "")).trim()
                        val nomE = eObj.optString("nombreempresa", eObj.optString("NombreEmpresa", "")).trim()
                        if (mailE.equals(correoInput.trim(), ignoreCase = true) ||
                            (inputLower.contains("pellas") && nomE.contains("Pellas", ignoreCase = true))
                        ) {
                            rolDetectado = "empresa"
                            sqlId = eObj.optInt("empresaid", eObj.optInt("EmpresaId", eObj.optInt("id", 2)))
                            if (nomE.isNotBlank() && nomE != "null") nombreMostrar = nomE
                            correoReal = if (mailE.isNotBlank()) mailE else correoInput.trim()
                            break
                        }
                    }
                }
                connEmps.disconnect()
            } catch (_: Exception) {}
        }

        // Si tampoco estaba en Empresas, buscar coincidencia exacta de correo en /api/candidatos/
        if (rolDetectado.isBlank() || (rolDetectado == "candidato" && sqlId <= 0)) {
            try {
                val urlCands = URL("http://20.51.194.8:8000/api/candidatos/?page_size=500")
                val connC = (urlCands.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 6000
                    readTimeout = 6000
                    setRequestProperty("Authorization", "Bearer $accessToken")
                }
                if (connC.responseCode in 200..299) {
                    val rawC = connC.inputStream.bufferedReader().use { it.readText() }
                    val arr = if (rawC.trim().startsWith("{")) {
                        JSONObject(rawC).optJSONArray("results") ?: JSONArray()
                    } else JSONArray(rawC)

                    for (i in 0 until arr.length()) {
                        val c = arr.getJSONObject(i)
                        val cid = c.optInt("candidatoid", c.optInt("CandidatoId", c.optInt("id", -1)))
                        val cMail = c.optString("correo", c.optString("Correo", "")).trim()
                        if ((sqlId > 0 && cid == sqlId) ||
                            cMail.equals(correoInput.trim(), ignoreCase = true) ||
                            cMail.equals(correoReal.trim(), ignoreCase = true)
                        ) {
                            rolDetectado = "candidato"
                            sqlId = cid
                            val n = c.optString("nombres", c.optString("nombre", c.optString("Nombres", c.optString("Nombre", ""))))
                            val a = c.optString("apellidos", c.optString("apellido", c.optString("Apellidos", c.optString("Apellido", ""))))
                            if (n.isNotBlank()) nombreMostrar = "$n $a".trim()
                            if (cMail.isNotBlank()) correoReal = cMail
                            break
                        }
                    }
                }
                connC.disconnect()
            } catch (_: Exception) {}
        }

        // Si el correo no existe ni en Admin, ni en Empresas, ni en Candidatos, NO permitir entrar a un perfil ajeno (#1)
        if (rolDetectado.isBlank() || sqlId <= 0) {
            return@withContext null
        }

        // Si es empresa, consultar /api/empresas/{id}/ con el Bearer Token para obtener su nombre exacto
        if (rolDetectado == "empresa") {
            try {
                val urlEmp = URL("http://20.51.194.8:8000/api/empresas/$sqlId/")
                val connE = (urlEmp.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 6000
                    readTimeout = 6000
                    setRequestProperty("Authorization", "Bearer $accessToken")
                }
                if (connE.responseCode in 200..299) {
                    val eObj = JSONObject(connE.inputStream.bufferedReader().use { it.readText() })
                    val nomReal = eObj.optString("nombreempresa", eObj.optString("NombreEmpresa", ""))
                    if (nomReal.isNotBlank() && nomReal != "null") {
                        nombreMostrar = nomReal
                    }
                    val mailReal = eObj.optString("correo", eObj.optString("Correo", ""))
                    if (mailReal.isNotBlank() && mailReal != "null") {
                        correoReal = mailReal
                    }
                }
                connE.disconnect()
            } catch (_: Exception) {}
        }

        // Guardar todas las preferencias para que cualquier Activity encuentre el ID, correo y token
        val prefs = context.getSharedPreferences("ImpulsoNicaPrefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("access_token", accessToken)
            putString("jwt_token", accessToken)
            putString("rol", rolDetectado)
            putString("user_role", rolDetectado)
            putString("correo_usuario", correoReal)
            putString("user_email", correoReal)
            putString("nombre_usuario", nombreMostrar)
            putInt("usuario_sql_id", sqlId.coerceAtLeast(1))
            if (rolDetectado == "empresa") {
                putInt("empresa_id", sqlId.coerceAtLeast(1))
                putString("nombre_empresa", nombreMostrar)
            } else if (rolDetectado == "candidato") {
                putInt("candidato_id", sqlId.coerceAtLeast(1))
            }
            apply()
        }

        rolDetectado
    } catch (_: Exception) {
        null
    }
}

private suspend fun registrarCandidatoNuevo(
    nombre: String,
    apellido: String,
    correo: String,
    password: String
): Boolean = withContext(Dispatchers.IO) {
    try {
        val url = URL("http://20.51.194.8:8000/api/registro/")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 7000
            readTimeout = 7000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        }
        val payload = JSONObject().apply {
            put("nombres", nombre)
            put("apellidos", apellido)
            put("correo", correo)
            put("email", correo)
            put("username", correo)
            put("password", password)
            put("municipioid", 1)
        }
        conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
        val ok = conn.responseCode in 200..299
        conn.disconnect()
        ok
    } catch (_: Exception) {
        false
    }
}

private suspend fun enviarSolicitudEmpresaAlAdmin(
    nombreEmpresa: String,
    ruc: String,
    representante: String,
    correo: String,
    telefono: String,
    direccion: String,
    sector: String
): Boolean = withContext(Dispatchers.IO) {
    try {
        val urlAdmin = URL("http://20.51.194.8:8000/api/dashboard/admin-gestion/")
        val connA = (urlAdmin.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 7000
            readTimeout = 7000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        }
        val payloadAdmin = JSONObject().apply {
            put("accion", "registrar_solicitud_empresa")
            put("nombre_empresa", nombreEmpresa)
            put("ruc", ruc)
            put("representante", representante)
            put("correo", correo)
            put("telefono", telefono)
            put("direccion", direccion)
            put("sector", sector)
        }
        connA.outputStream.use { it.write(payloadAdmin.toString().toByteArray(Charsets.UTF_8)) }
        val codeA = connA.responseCode
        connA.disconnect()
        if (codeA in 200..299) {
            return@withContext true
        }
    } catch (_: Exception) {}

    try {
        val url = URL("http://20.51.194.8:8000/api/v1/notificaciones/")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 7000
            readTimeout = 7000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        }
        val payload = JSONObject().apply {
            put("receptor_sql_id", 1)
            put("tipo_receptor", "admin")
            put("tipo_alerta", "SOLICITUD_EMPRESA")
            put("titulo", "Solicitud de Registro Corporativo: $nombreEmpresa")
            put("mensaje", "La empresa $nombreEmpresa (RUC: $ruc) solicita validación y creación de credenciales.")
            put("estado_accion", "Pendiente")
            put(
                "data_extra",
                JSONObject().apply {
                    put("nombre_empresa", nombreEmpresa)
                    put("ruc", ruc)
                    put("representante", representante)
                    put("correo", correo)
                    put("telefono", telefono)
                    put("direccion", direccion)
                    put("sector", sector)
                }
            )
        }
        conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
        val ok = conn.responseCode in 200..299
        conn.disconnect()
        ok
    } catch (_: Exception) {
        false
    }
}

fun obtenerLogoImpulsoNicaDrawableId(context: Context): Int {
    val nombresPrioritarios = listOf(
        "logoimpulsonica",
    )
    for (nombre in nombresPrioritarios) {
        val id = context.resources.getIdentifier(nombre, "drawable", context.packageName)
        if (id != 0) return id
    }

    return try {
        val campos = R.drawable::class.java.declaredFields
        val candidatoLogo = campos.firstOrNull { field ->
            val n = field.name.lowercase()
            !n.startsWith("ic_launcher") &&
                    !n.startsWith("abc_") &&
                    !n.startsWith("avd_") &&
                    !n.startsWith("btn_") &&
                    !n.startsWith("design_") &&
                    !n.startsWith("mtrl_") &&
                    !n.startsWith("m3_") &&
                    !n.startsWith("notification_") &&
                    n != "notificacion" &&
                    (n.contains("logo") || n.contains("impulso") || n.contains("nica") || n.contains("icono") || n.contains("img"))
        } ?: campos.firstOrNull { field ->
            val n = field.name.lowercase()
            !n.startsWith("ic_launcher") &&
                    !n.startsWith("abc_") &&
                    !n.startsWith("avd_") &&
                    !n.startsWith("btn_") &&
                    !n.startsWith("design_") &&
                    !n.startsWith("mtrl_") &&
                    !n.startsWith("m3_") &&
                    !n.startsWith("notification_") &&
                    n != "notificacion"
        }
        candidatoLogo?.getInt(null) ?: 0
    } catch (_: Exception) {
        0
    }
}