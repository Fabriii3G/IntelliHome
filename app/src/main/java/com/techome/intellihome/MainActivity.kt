package com.techome.intellihome

import androidx.compose.material3.MaterialTheme
import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Asegurarse de que el usuario Admin esté en el archivo
        addDefaultAdminUser()

        setContent {
            MaterialTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LoginScreen(
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }

    // Función para agregar el usuario Admin con la contraseña predeterminada si no existe en el archivo
    private fun addDefaultAdminUser() {
        val file = File(filesDir, "users.txt")
        if (!file.exists() || !file.readText().contains("Admin")) {
            // Si no existe el usuario Admin, lo agregamos
            file.appendText("Admin,Administrador,admin@tec.com,TEC2024,30\n")
        }
    }
}

// Clase usuario
data class User(
    val alias: String,
    val fullName: String,
    val email: String,
    var password: String,
    val age: Int,
)

var isAppEnabled by mutableStateOf(true) // Variable para habilitar/deshabilitar el aplicativo

@Composable
fun LoginScreen(modifier: Modifier = Modifier) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isLoggedIn by rememberSaveable { mutableStateOf(false) }
    var isAdmin by rememberSaveable { mutableStateOf(false) }
    var showRegister by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    if (isLoggedIn) {
        if (isAdmin) {
            AdminMenuScreen(context = context)
        } else {
            UserMenuScreen()
        }
    } else if (showRegister) {
        RegisterUserScreen(onBack = { showRegister = false })
    } else {
        if (!isAppEnabled) {
            Toast.makeText(context, "El aplicativo está deshabilitado", Toast.LENGTH_SHORT).show()
            return
        }
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BasicTextField(
                value = username,
                onValueChange = { username = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.padding(16.dp)) {
                        if (username.isEmpty()) Text("Usuario")
                        innerTextField()
                    }
                }
            )
            BasicTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                visualTransformation = PasswordVisualTransformation(),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.padding(16.dp)) {
                        if (password.isEmpty()) Text("Contraseña")
                        innerTextField()
                    }
                }
            )
            Button(onClick = {
                val userData = readUserDataFromFile(context, username)
                if (username == "Admin" && password == "TEC2024") {
                    isLoggedIn = true
                    isAdmin = true
                    Toast.makeText(context, "Ingreso exitoso como Admin", Toast.LENGTH_SHORT).show()
                    startPasswordReminder(context) // Inicia recordatorio de contraseña
                } else if (userData != null && userData.password == password) {
                    isLoggedIn = true
                    isAdmin = false
                    Toast.makeText(context, "Ingreso exitoso", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                        context,
                        "Contraseña o usuario incorrectos",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }) {
                Text("Login")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón para registrar nuevo usuario
            Button(onClick = {
                showRegister = true
            }) {
                Text("Registrar nuevo usuario")
            }
        }
    }
}

// Pantalla del menú de administrador
@Composable
fun AdminMenuScreen(context: android.content.Context) {
    var newPassword by rememberSaveable { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Bienvenido, Admin")

        // Botón para habilitar/deshabilitar el aplicativo
        Button(onClick = {
            isAppEnabled = !isAppEnabled
            Toast.makeText(context, if (isAppEnabled) "Aplicativo habilitado" else "Aplicativo deshabilitado", Toast.LENGTH_SHORT).show()
        }) {
            Text(if (isAppEnabled) "Deshabilitar aplicativo" else "Habilitar aplicativo")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Campo para cambiar la contraseña
        BasicTextField(
            value = newPassword,
            onValueChange = { newPassword = it },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.padding(16.dp)) {
                    if (newPassword.isEmpty()) Text("Nueva contraseña")
                    innerTextField()
                }
            }
        )

        // Botón para actualizar la contraseña
        Button(onClick = {
            if (newPassword.isNotEmpty()) {
                updateAdminPassword(context, newPassword)
                Toast.makeText(context, "Contraseña actualizada", Toast.LENGTH_SHORT).show()
                newPassword = "" // Limpiar el campo después de actualizar
            } else {
                Toast.makeText(context, "Por favor ingrese una nueva contraseña", Toast.LENGTH_SHORT).show()
            }
        }) {
            Text("Cambiar Contraseña")
        }
    }
}

// Pantalla del menú de usuario común
@Composable
fun UserMenuScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Bienvenido, Usuario")
        // Funcionalidades para usuarios comunes
    }
}

// Pantalla de registro de usuario
@Composable
fun RegisterUserScreen(onBack: () -> Unit) {
    var alias by rememberSaveable { mutableStateOf("") }
    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var age by rememberSaveable { mutableStateOf(18) } // Nueva variable para la edad
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Registrar nuevo usuario", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(16.dp))

        BasicTextField(
            value = alias,
            onValueChange = { alias = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.padding(16.dp)) {
                    if (alias.isEmpty()) Text("Usuario (alias)")
                    innerTextField()
                }
            }
        )

        BasicTextField(
            value = fullName,
            onValueChange = { fullName = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.padding(16.dp)) {
                    if (fullName.isEmpty()) Text("Nombre completo")
                    innerTextField()
                }
            }
        )

        BasicTextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.padding(16.dp)) {
                    if (email.isEmpty()) Text("Correo electrónico")
                    innerTextField()
                }
            }
        )

        BasicTextField(
            value = password,
            onValueChange = { password = it },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.padding(16.dp)) {
                    if (password.isEmpty()) Text("Contraseña")
                    innerTextField()
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Selector de edad
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text("Edad:")
            BasicTextField(
                value = age.toString(),
                onValueChange = { newValue ->
                    val parsedValue = newValue.toIntOrNull() ?: 18
                    age = parsedValue.coerceIn(1, 120) // Limitar la edad entre 1 y 120
                },
                modifier = Modifier
                    .padding(start = 8.dp)
                    .width(50.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            if (alias.isNotEmpty() && fullName.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                val newUser = User(alias, fullName, email, password, age)
                saveUserToFile(context, newUser)
                Toast.makeText(context, "Usuario registrado con éxito", Toast.LENGTH_SHORT).show()
                onBack() // Regresar a la pantalla de inicio de sesión
            } else {
                Toast.makeText(context, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show()
            }
        }) {
            Text("Registrar")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onBack) {
            Text("Volver")
        }
    }
}

// Función para leer los datos de usuario desde el archivo
fun readUserDataFromFile(context: android.content.Context, alias: String): User? {
    val file = File(context.filesDir, "users.txt")
    if (file.exists()) {
        val lines = file.readLines()
        for (line in lines) {
            val parts = line.split(",")
            if (parts.size == 5 && parts[0] == alias) {
                return User(
                    alias = parts[0],
                    fullName = parts[1],
                    email = parts[2],
                    password = parts[3],
                    age = parts[4].toInt() // Convertir el campo de edad a entero
                )
            }
        }
    }
    return null
}

// Función para guardar los datos de usuario en un archivo
fun saveUserToFile(context: android.content.Context, user: User) {
    val file = File(context.filesDir, "users.txt")
    file.appendText("${user.alias},${user.fullName},${user.email},${user.password},${user.age}\n")
}

// Función para actualizar la contraseña del Admin en el archivo
fun updateAdminPassword(context: android.content.Context, newPassword: String) {
    val file = File(context.filesDir, "users.txt")
    if (file.exists()) {
        val lines = file.readLines()
        val newLines = lines.map { line ->
            if (line.startsWith("Admin")) {
                val parts = line.split(",")
                "Admin,${parts[1]},${parts[2]},$newPassword,${parts[4]}"
            } else {
                line
            }
        }
        file.writeText(newLines.joinToString("\n"))
    }
}

// Función para recordar al admin que cambie la contraseña cada 2 minutos usando notificaciones
fun startPasswordReminder(context: android.content.Context) {
    val handler = Handler(Looper.getMainLooper())

    // Crear el canal de notificaciones
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            "PASSWORD_REMINDER",
            "Recordatorio de Contraseña",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Recordatorio para cambiar la contraseña predeterminada"
        }
        val notificationManager: NotificationManager =
            context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    val reminderRunnable = object : Runnable {
        @SuppressLint("MissingPermission")
        override fun run() {
            val builder = NotificationCompat.Builder(context, "PASSWORD_REMINDER")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Recordatorio de contraseña")
                .setContentText("Por favor cambie su contraseña predeterminada")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

            with(NotificationManagerCompat.from(context)) {
                notify(1, builder.build())
            }

            // Repetir la llamada cada 2 minutos (120000 ms)
            handler.postDelayed(this, 120000)
        }
    }

    // Iniciar el recordatorio
    handler.post(reminderRunnable)
}
