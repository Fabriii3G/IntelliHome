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
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.io.File
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }
        // Asegurarse de que el usuario Admin esté en el archivo
        addDefaultAdminUser()

        setContent {
            MaterialTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    var currentScreen by rememberSaveable { mutableStateOf("login") }

                    when (currentScreen) {
                        "login" -> LoginScreen(
                            modifier = Modifier.padding(innerPadding),
                            onForgotPassword = { currentScreen = "requestEmail" }
                        )
                        "requestEmail" -> RequestPasswordResetScreen(
                            onEmailSent = { currentScreen = "verifyCode" }
                        )
                        "verifyCode" -> VerifyRecoveryCodeScreen(
                            onCodeVerified = { currentScreen = "resetPassword" },
                            onError = { currentScreen = "requestEmail" }
                        )
                        "resetPassword" -> ResetPasswordScreen(
                            onPasswordReset = { currentScreen = "login" }
                        )
                    }
                }
            }
        }
    }
    // Función para agregar el usuario Admin con la contraseña predeterminada si no existe en el archivo
    private fun addDefaultAdminUser() {
        val file = File(filesDir, "users.txt")
        if (!file.exists() || !file.readText().contains("Admin")) {
            // Si no existe el usuario Admin, lo agregamos
            file.appendText("Admin,Administrador,admin@tec.com,TEC2024,30,Apartamento Inteligente,Carro\n")
        }
    }
}

// Clase usuario actualizada para incluir tipo de casa y tipo de vehículo
data class User(
    var alias: String,
    var fullName: String,
    var email: String,
    var password: String,
    val age: Int,
    var houseType: String,   // Nuevo campo para el tipo de casa
    var vehicleType: String  // Nuevo campo para el tipo de vehículo
)

var isAppEnabled by mutableStateOf(true) // Variable para habilitar/deshabilitar el aplicativo
var isLoggedIn by mutableStateOf(false)  // Variable global para estado de sesión
var isAdmin by mutableStateOf(false)     // Variable global para saber si el usuario es Admin
var currentUser by mutableStateOf(User("A", "B", "C", "D", 1, "F", "G"))

// Función para validar contraseñas
fun isPasswordValid(password: String): Boolean {
    val passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#\$%^&+=!])(?=\\S+\$).{8,}\$"
    val passwordMatcher = Regex(passwordPattern)
    return passwordMatcher.matches(password)
}

@Composable
fun LoginScreen(modifier: Modifier = Modifier, onForgotPassword: () -> Unit) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
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
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Mostrar mensaje si el aplicativo está deshabilitado
            if (!isAppEnabled) {
                Text("El aplicativo está deshabilitado. Solo el administrador puede iniciar sesión.", Modifier.padding(16.dp))
            }

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
                currentUser = readUserDataFromFile(context, username)

                // Permitir solo al administrador iniciar sesión si la app está deshabilitada
                if (!isAppEnabled && username != "Admin") {
                    Toast.makeText(context, "El aplicativo está deshabilitado. Solo el administrador puede iniciar sesión.", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (username == "Admin" && password == "TEC2024") {
                    isLoggedIn = true
                    isAdmin = true
                    Toast.makeText(context, "Ingreso exitoso como Admin", Toast.LENGTH_SHORT).show()
                    startPasswordReminder(context) // Inicia recordatorio de contraseña
                } else if (userData.password == password) {
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

            // Mostrar el botón de registro solo si la app está habilitada
            if (isAppEnabled) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    showRegister = true
                }) {
                    Text("Registrar nuevo usuario")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onForgotPassword) {
                Text("¿Olvidaste tu contraseña?")
            }
        }
    }
}

//Funcion para poder previsualizar el codigo
@Preview(showBackground = true)
@Composable
fun PreviewLoginScreen() {
    LoginScreen(onForgotPassword = {})
}

@Composable
fun RequestPasswordResetScreen(onEmailSent: () -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Recuperar contraseña")

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

        Button(onClick = {
            val user = readUserDataByEmail(context, email)
            if (user != null) {
                sendRecoveryCode(context, email)
                onEmailSent() // Navegar a la pantalla de verificación del código
            } else {
                Toast.makeText(context, "Correo no encontrado", Toast.LENGTH_SHORT).show()
            }
        }) {
            Text("Enviar código")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewRequestPasswordResetScreen() {
    RequestPasswordResetScreen(onEmailSent = {})
}

@Composable
fun VerifyRecoveryCodeScreen(onCodeVerified: () -> Unit, onError: () -> Unit) {
    var code by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Ingrese el código de recuperación")

        BasicTextField(
            value = code,
            onValueChange = { code = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.padding(16.dp)) {
                    if (code.isEmpty()) Text("Código de recuperación")
                    innerTextField()
                }
            }
        )

        Button(onClick = {
            val sharedPreferences = context.getSharedPreferences("RecoveryPrefs", android.content.Context.MODE_PRIVATE)
            val savedCode = sharedPreferences.getString("recovery_code", "")

            if (code == savedCode) {
                onCodeVerified() // Navegar a la pantalla para cambiar contraseña
            } else {
                onError() // Manejar el error
            }
        }) {
            Text("Verificar código")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewVerifyRecoveryCodeScreen() {
    VerifyRecoveryCodeScreen(onCodeVerified = {}, onError = {})
}

@Composable
fun ResetPasswordScreen(onPasswordReset: () -> Unit) {
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Cambiar Contraseña")

        BasicTextField(
            value = newPassword,
            onValueChange = { newPassword = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            visualTransformation = PasswordVisualTransformation(),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.padding(16.dp)) {
                    if (newPassword.isEmpty()) Text("Nueva contraseña")
                    innerTextField()
                }
            }
        )

        BasicTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            visualTransformation = PasswordVisualTransformation(),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.padding(16.dp)) {
                    if (confirmPassword.isEmpty()) Text("Confirmar contraseña")
                    innerTextField()
                }
            }
        )

        Button(onClick = {
            if (newPassword == confirmPassword) {
                saveNewPassword(context, newPassword)
                onPasswordReset() // Navegar de vuelta al login
            } else {
                Toast.makeText(context, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
            }
        }) {
            Text("Cambiar contraseña")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewResetPasswordScreen() {
    ResetPasswordScreen(onPasswordReset = {})
}


// Pantalla del menú de administrador
@Composable
fun AdminMenuScreen(context: android.content.Context) {
    var newPassword by rememberSaveable { mutableStateOf("") }
    val users = remember { mutableStateListOf<User>() }

    // Leer todos los usuarios al iniciar la pantalla
    LaunchedEffect(Unit) {
        users.clear()
        users.addAll(readAllUsers(context))
    }

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

        Spacer(modifier = Modifier.height(16.dp))

        // Lista de usuarios
        Text("Lista de usuarios:")
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(users) { user ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${user.alias} - ${user.fullName}", modifier = Modifier.weight(1f))
                    Button(onClick = {
                        // Función para actualizar a Admin
                        if (user.alias != "Admin") {
                            makeUserAdmin(context, user)
                            Toast.makeText(context, "${user.alias} ahora es administrador", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Text("Hacer Admin")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botón para cerrar sesión
        Button(onClick = {
            // Cerrar sesión (resetear el estado)
            isLoggedIn = false
            isAdmin = false
        }) {
            Text("Cerrar sesión")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewAdminMenuScreen() {
    val context = LocalContext.current // Necesario para el contexto
    AdminMenuScreen(context = context)
}


// Pantalla del menú de usuario común
@Composable
fun UserMenuScreen() {
    // Estado para controlar si el usuario está editando los datos
    var isEditing by rememberSaveable { mutableStateOf(false) }

    // Pantalla principal o pantalla de edición basada en el estado
    if (isEditing) {
        // Mostrar pantalla de edición de usuario
        EditUserScreen(onSave = {
            isEditing = false // Volver al menú del usuario una vez que se guardan los datos
        })
    } else {
        // Mostrar pantalla del menú de usuario
        UserMenuContent(onEdit = {
            isEditing = true // Cambiar el estado a edición
        })
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewUserMenuScreen() {
    UserMenuScreen()
}


@Composable
fun UserMenuContent(onEdit: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Bienvenido, ${currentUser.alias}")

        Spacer(modifier = Modifier.height(16.dp))

        // Botón para cerrar sesión
        Button(onClick = {
            isLoggedIn = false
            isAdmin = false
        }) {
            Text("Cerrar sesión")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botón para editar datos
        Button(onClick = {
            onEdit() // Llama a la función de edición pasada por parámetro
        }) {
            Text("Editar datos")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewUserMenuContent() {
    UserMenuContent(onEdit = {})
}


@Composable
fun EditUserScreen(onSave: () -> Unit) {
    val context = LocalContext.current
    var alias by rememberSaveable { mutableStateOf(currentUser.alias) }
    var fullName by rememberSaveable { mutableStateOf(currentUser.fullName) }
    var email by rememberSaveable { mutableStateOf(currentUser.email) }

    // Selector de tipo de casa
    val houseTypes = listOf("Apartamento Inteligente", "Apartamento Normal", "Casa Normal", "Casa con Apartamento")
    var selectedHouseType by rememberSaveable { mutableStateOf(currentUser.houseType) }
    var expandedHouseType by rememberSaveable { mutableStateOf(false) }

    // Selector de tipo de vehículo
    val vehicleTypes = listOf("Bicicleta", "Carro", "Moto", "Otro")
    var selectedVehicleType by rememberSaveable { mutableStateOf(currentUser.vehicleType) }
    var expandedVehicleType by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Editar datos", style = MaterialTheme.typography.headlineSmall)

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

        Spacer(modifier = Modifier.height(16.dp))

        // Selector de tipo de casa
        Text("Seleccione el tipo de casa:")
        Box {
            Button(onClick = { expandedHouseType = !expandedHouseType }) {
                Text(selectedHouseType)
            }
            DropdownMenu(
                expanded = expandedHouseType,
                onDismissRequest = { expandedHouseType = false }
            ) {
                houseTypes.forEach { houseType ->
                    DropdownMenuItem(
                        text = { Text(houseType) },
                        onClick = {
                            selectedHouseType = houseType
                            expandedHouseType = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Selector de tipo de vehículo
        Text("Seleccione el tipo de vehículo:")
        Box {
            Button(onClick = { expandedVehicleType = !expandedVehicleType }) {
                Text(selectedVehicleType)
            }
            DropdownMenu(
                expanded = expandedVehicleType,
                onDismissRequest = { expandedVehicleType = false }
            ) {
                vehicleTypes.forEach { vehicleType ->
                    DropdownMenuItem(
                        text = { Text(vehicleType) },
                        onClick = {
                            selectedVehicleType = vehicleType
                            expandedVehicleType = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            val oldUser = currentUser.alias
            // Guardar los cambios
            currentUser.alias = alias
            currentUser.fullName = fullName
            currentUser.email = email
            currentUser.houseType = selectedHouseType
            currentUser.vehicleType = selectedVehicleType

            onSave()  // Llamar al callback de guardado
            saveUserToFile(context, currentUser) //Guarda informacion nueva
            deleteUserFromFile(context, oldUser) //Elimina informacion anterior
        }) {
            Text("Guardar cambios")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onSave) {
            Text("Cancelar")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewEditUserScreen() {
    EditUserScreen(onSave = {})
}


// Pantalla de registro de usuario actualizada con validación de contraseña
@Composable
fun RegisterUserScreen(onBack: () -> Unit) {
    var alias by rememberSaveable { mutableStateOf("") }
    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var age by rememberSaveable { mutableStateOf(18) }
    val context = LocalContext.current

    // Nueva variable para el tipo de casa
    val houseTypes = listOf("Apartamento Inteligente", "Apartamento Normal", "Casa Normal", "Casa con Apartamento")
    var selectedHouseType by rememberSaveable { mutableStateOf(houseTypes[0]) }
    var expandedHouseType by rememberSaveable { mutableStateOf(false) }

    // Nueva variable para el tipo de vehículo
    val vehicleTypes = listOf("Bicicleta", "Carro", "Moto", "Otro")
    var selectedVehicleType by rememberSaveable { mutableStateOf(vehicleTypes[0]) }
    var expandedVehicleType by rememberSaveable { mutableStateOf(false) }

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

        Spacer(modifier = Modifier.height(8.dp))

        // Mostrar requisitos de contraseña
        Text(
            text = "La contraseña debe tener al menos 8 caracteres, una letra mayúscula, una letra minúscula, un número y un carácter especial.",
            modifier = Modifier.padding(16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Selector de tipo de casa
        Text("Seleccione el tipo de casa:")
        Box {
            Button(onClick = { expandedHouseType = !expandedHouseType }) {
                Text(selectedHouseType)
            }
            DropdownMenu(
                expanded = expandedHouseType,
                onDismissRequest = { expandedHouseType = false }
            ) {
                houseTypes.forEach { houseType ->
                    DropdownMenuItem(
                        text = { Text(houseType) },
                        onClick = {
                            selectedHouseType = houseType
                            expandedHouseType = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Selector de tipo de vehículo
        Text("Seleccione el tipo de vehículo:")
        Box {
            Button(onClick = { expandedVehicleType = !expandedVehicleType }) {
                Text(selectedVehicleType)
            }
            DropdownMenu(
                expanded = expandedVehicleType,
                onDismissRequest = { expandedVehicleType = false }
            ) {
                vehicleTypes.forEach { vehicleType ->
                    DropdownMenuItem(
                        text = { Text(vehicleType) },
                        onClick = {
                            selectedVehicleType = vehicleType
                            expandedVehicleType = false
                        }
                    )
                }
            }
        }

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
                    age = parsedValue.coerceIn(1, 120)
                },
                modifier = Modifier
                    .padding(start = 8.dp)
                    .width(50.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            if (alias.isNotEmpty() && fullName.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                if (!isPasswordValid(password)) {
                    Toast.makeText(
                        context,
                        "La contraseña no cumple con los requisitos.",
                        Toast.LENGTH_LONG
                    ).show()
                } else if (isUserAlreadyRegistered(context, alias, email)) {
                    Toast.makeText(context, "El alias o correo ya están en uso, elija otros", Toast.LENGTH_SHORT).show()
                } else {
                    val newUser = User(alias, fullName, email, password, age, selectedHouseType, selectedVehicleType)
                    saveUserToFile(context, newUser)
                    Toast.makeText(context, "Usuario registrado con éxito", Toast.LENGTH_SHORT).show()
                    onBack()
                }
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

@Preview(showBackground = true)
@Composable
fun PreviewRegisterUserScreen() {
    RegisterUserScreen(onBack = {})
}

@Composable
fun PaymentScreen() {
    var cardholderName by rememberSaveable { mutableStateOf("") }
    var cardNumber by rememberSaveable { mutableStateOf("") }
    var expiryDate by rememberSaveable { mutableStateOf("") }
    var securityCode by rememberSaveable { mutableStateOf("") }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var cardBrand by rememberSaveable { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Nombre del Tarjetahabiente
        BasicTextField(
            value = cardholderName,
            onValueChange = { cardholderName = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.padding(16.dp)) {
                    if (cardholderName.isEmpty()) Text("Nombre del Tarjetahabiente")
                    innerTextField()
                }
            }
        )

        // Número de tarjeta
        BasicTextField(
            value = cardNumber,
            onValueChange = {
                if (it.length <= 16) {
                    cardNumber = it
                    validateCardNumber(it)?.let { brand ->
                        cardBrand = brand
                        errorMessage = null
                    } ?: run {
                        errorMessage = "Número de tarjeta inválido"
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.padding(16.dp)) {
                    if (cardNumber.isEmpty()) Text("Número de tarjeta (16 dígitos)")
                    innerTextField()
                }
            }
        )

        // Fecha de validez (Mes/Año)
        BasicTextField(
            value = expiryDate,
            onValueChange = { expiryDate = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.padding(16.dp)) {
                    if (expiryDate.isEmpty()) Text("Fecha de Validez (MM/AA)")
                    innerTextField()
                }
            }
        )

        // Número verificador
        BasicTextField(
            value = securityCode,
            onValueChange = {
                if (it.length <= 4) {
                    securityCode = it
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.padding(16.dp)) {
                    if (securityCode.isEmpty()) Text("Número Verificador (4 dígitos)")
                    innerTextField()
                }
            }
        )

        // Mensajes de error y marca de la tarjeta
        if (errorMessage != null) {
            Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
        } else if (cardBrand != null) {
            Text(text = "Marca de la tarjeta: $cardBrand", modifier = Modifier.padding(8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botón para confirmar el pago
        Button(onClick = {
            if (cardNumber.length == 16 && securityCode.length == 4 && cardBrand != null) {
                Toast.makeText(context, "Pago procesado con éxito", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Verifica la información ingresada", Toast.LENGTH_LONG).show()
            }
        }) {
            Text("Confirmar Pago")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewPaymentScreen() {
    PaymentScreen()
}

// Función para validar el número de tarjeta y determinar la marca
fun validateCardNumber(cardNumber: String): String? {
    return when (cardNumber.firstOrNull()) {
        '1' -> "Visca"
        '2' -> "MasterChef"
        '3' -> "AmericanCity"
        '5' -> "TicaPay"
        else -> null
    }
}

// Función para leer los datos de usuario desde el archivo
fun readUserDataFromFile(context: android.content.Context, alias: String): User {
    val file = File(context.filesDir, "users.txt")
    if (file.exists()) {
        val lines = file.readLines()
        for (line in lines) {
            val parts = line.split(",")
            if (parts.size == 7 && parts[0] == alias) {
                return User(
                    alias = parts[0],
                    fullName = parts[1],
                    email = parts[2],
                    password = parts[3],
                    age = parts[4].toInt(), // Convertir el campo de edad a entero
                    houseType = parts[5],
                    vehicleType = parts[6]
                )
            }
        }
    }
    return User("A", "B", "C", "D", 1, "F", "G")
}

fun readUserDataByEmail(context: android.content.Context, email: String): User? {
    val file = File(context.filesDir, "users.txt")
    if (file.exists()) {
        val lines = file.readLines()
        for (line in lines) {
            val parts = line.split(",")
            if (parts.size == 7 && parts[2] == email) {
                return User(
                    alias = parts[0],
                    fullName = parts[1],
                    email = parts[2],
                    password = parts[3],
                    age = parts[4].toInt(),
                    houseType = parts[5],
                    vehicleType = parts[6]
                )
            }
        }
    }
    return null
}

fun sendRecoveryCode(context: android.content.Context, email: String) {
    val code = (10000..99999).random().toString()
    val sharedPreferences = context.getSharedPreferences("RecoveryPrefs", android.content.Context.MODE_PRIVATE)
    sharedPreferences.edit().putString("recovery_code", code).putString("recovery_email", email).apply()

    // Simulación de envío del código
    Toast.makeText(context, "Código enviado: $code", Toast.LENGTH_LONG).show()
}

fun saveNewPassword(context: android.content.Context, newPassword: String) {
    val sharedPreferences = context.getSharedPreferences("RecoveryPrefs", android.content.Context.MODE_PRIVATE)
    val email = sharedPreferences.getString("recovery_email", "")

    if (email != null && email.isNotEmpty()) {
        val file = File(context.filesDir, "users.txt")
        if (file.exists()) {
            val lines = file.readLines().toMutableList()
            val updatedLines = lines.map { line ->
                val parts = line.split(",")
                if (parts.size == 7 && parts[2] == email) {
                    "${parts[0]},${parts[1]},$email,$newPassword,${parts[4]},${parts[5]},${parts[6]}"
                } else {
                    line
                }
            }
            file.writeText(updatedLines.joinToString("\n"))
            Toast.makeText(context, "Contraseña actualizada exitosamente", Toast.LENGTH_SHORT).show()
        }
    }
}

// Función para leer todos los usuarios
fun readAllUsers(context: android.content.Context): List<User> {
    val file = File(context.filesDir, "users.txt")
    val users = mutableListOf<User>()
    if (file.exists()) {
        val lines = file.readLines()
        for (line in lines) {
            val parts = line.split(",")
            if (parts.size == 7) {
                users.add(
                    User(
                        alias = parts[0],
                        fullName = parts[1],
                        email = parts[2],
                        password = parts[3],
                        age = parts[4].toInt(),
                        houseType = parts[5],
                        vehicleType = parts[6]
                    )
                )
            }
        }
    }
    return users
}

// Función para verificar si el alias o el correo ya existen
fun isUserAlreadyRegistered(context: android.content.Context, alias: String, email: String): Boolean {
    val file = File(context.filesDir, "users.txt")
    if (file.exists()) {
        val lines = file.readLines()
        for (line in lines) {
            val parts = line.split(",")
            if (parts.size == 7) {
                val existingAlias = parts[0]
                val existingEmail = parts[2]
                if (existingAlias == alias || existingEmail == email) {
                    return true // Alias o correo ya están en uso
                }
            }
        }
    }
    return false
}

// Función para guardar los datos de usuario en un archivo
fun saveUserToFile(context: android.content.Context, user: User) {
    val file = File(context.filesDir, "users.txt")
    file.appendText("${user.alias},${user.fullName},${user.email},${user.password},${user.age},${user.houseType},${user.vehicleType}\n")
}

fun deleteUserFromFile(context: android.content.Context, userAlias: String) {
    val file = File(context.filesDir, "users.txt")

    if (file.exists()) {
        // Leer todas las líneas del archivo
        val lines = file.readLines().toMutableList()

        // Filtrar las líneas que no corresponden al usuario que deseas eliminar
        val updatedLines = lines.filter { !it.startsWith("$userAlias,") }

        // Sobrescribir el archivo con las líneas actualizadas
        file.writeText(updatedLines.joinToString("\n"))
    }
}

// Función para actualizar la contraseña del Admin en el archivo
fun updateAdminPassword(context: android.content.Context, newPassword: String) {
    val file = File(context.filesDir, "users.txt")
    if (file.exists()) {
        val lines = file.readLines()
        val newLines = lines.map { line ->
            if (line.startsWith("Admin")) {
                val parts = line.split(",")
                "Admin,${parts[1]},${parts[2]},$newPassword,${parts[4]},${parts[5]},${parts[6]}"
            } else {
                line
            }
        }
        file.writeText(newLines.joinToString("\n"))
    }
}

// Función para hacer a un usuario administrador
fun makeUserAdmin(context: android.content.Context, user: User) {
    val file = File(context.filesDir, "users.txt")
    if (file.exists()) {
        val lines = file.readLines()
        val newLines = lines.map { line ->
            val parts = line.split(",")
            if (parts[0] == user.alias) {
                "${user.alias},Administrador,${user.email},${user.password},${user.age},${user.houseType},${user.vehicleType}"
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