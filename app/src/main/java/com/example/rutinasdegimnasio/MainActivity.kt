package com.example.rutinasdegimnasio

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.rutinasdegimnasio.ui.theme.RutinasdegimnasioTheme
import com.example.rutinasdegimnasio.ui.components.ExerciseIllustration
import com.example.rutinasdegimnasio.model.Exercise
import com.example.rutinasdegimnasio.model.ExerciseCategory
import com.example.rutinasdegimnasio.viewmodel.WorkoutViewModel
import com.example.rutinasdegimnasio.viewmodel.WorkoutUiState
import com.example.rutinasdegimnasio.network.SupabaseClient
import com.example.rutinasdegimnasio.data.DatabaseHelper
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RutinasdegimnasioTheme {
                MainAppNavigation()
            }
        }
    }
}

@Composable
fun MainAppNavigation(viewModel: WorkoutViewModel = viewModel()) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val uiState by viewModel.uiState.collectAsState()
    val currentUser = SupabaseClient.client.auth.currentSessionOrNull()?.user

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentRoute != "welcome" && currentRoute?.startsWith("workout_session") == false,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("GIMNASIO APP", fontWeight = FontWeight.Black, fontSize = 24.sp)
                    Text(text = if (currentUser != null) "Sesión: ${currentUser.email}" else "Invitado", style = MaterialTheme.typography.bodySmall)
                }
                HorizontalDivider()
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Mis Rutinas") },
                    selected = currentRoute == "home",
                    onClick = { scope.launch { drawerState.close() }; navController.navigate("home") }
                )
                Spacer(modifier = Modifier.weight(1f))
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color.Red) },
                    label = { Text("Cerrar Sesión", color = Color.Red) },
                    selected = false,
                    onClick = { 
                        scope.launch { 
                            drawerState.close()
                            SupabaseClient.client.auth.signOut()
                            navController.navigate("welcome") { popUpTo("home") { inclusive = true } }
                        }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                if (currentRoute != "welcome" && currentRoute?.startsWith("workout_session") == false) {
                    WorkoutTopBar(onMenuClick = { scope.launch { drawerState.open() } })
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "welcome",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("welcome") { WelcomeScreen(onLoginSuccess = { navController.navigate("home") }) }
                composable("home") {
                    WorkoutHomeScreen(
                        uiState = uiState,
                        onCategoryClick = { navController.navigate("details/$it") },
                        onRetry = { viewModel.fetchWorkouts() }
                    )
                }
                composable(
                    "details/{categoryName}",
                    arguments = listOf(navArgument("categoryName") { type = NavType.StringType })
                ) { backStackEntry ->
                    val categoryName = backStackEntry.arguments?.getString("categoryName") ?: ""
                    val category = (uiState as? WorkoutUiState.Success)?.categories?.find { it.title == categoryName }
                    category?.let {
                        ExerciseDetailScreen(it, 
                            onBack = { navController.popBackStack() },
                            onStartSession = { navController.navigate("workout_session/$categoryName") },
                            onRefresh = { viewModel.fetchWorkouts() }
                        )
                    }
                }
                composable(
                    "workout_session/{categoryName}",
                    arguments = listOf(navArgument("categoryName") { type = NavType.StringType })
                ) { backStackEntry ->
                    val categoryName = backStackEntry.arguments?.getString("categoryName") ?: ""
                    val category = (uiState as? WorkoutUiState.Success)?.categories?.find { it.title == categoryName }
                    category?.let { WorkoutSessionScreen(it, onFinish = { navController.popBackStack() }) }
                }
            }
        }
    }
}

@Composable
fun WelcomeScreen(onLoginSuccess: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF1B5E20)).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "RUTINAS GYM", fontSize = 48.sp, fontWeight = FontWeight.Black, color = Color.White)
        Text(text = "Disciplina y Fuerza", fontSize = 18.sp, color = Color.White.copy(alpha = 0.8f))
        Spacer(modifier = Modifier.height(48.dp))
        OutlinedTextField(
            value = email, onValueChange = { email = it },
            label = { Text("Correo Electrónico", color = Color.White) },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color.White) },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color.White, unfocusedBorderColor = Color.White.copy(alpha = 0.5f))
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("Contraseña", color = Color.White) },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White) },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color.White, unfocusedBorderColor = Color.White.copy(alpha = 0.5f))
        )
        Spacer(modifier = Modifier.height(32.dp))
        if (isLoading) CircularProgressIndicator(color = Color.White)
        else {
            Button(
                onClick = { 
                    scope.launch {
                        isLoading = true
                        try {
                            SupabaseClient.client.auth.signInWith(Email) { this.email = email; this.password = password }
                            onLoginSuccess()
                        } catch (e: Exception) { Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show() }
                        finally { isLoading = false }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp)
            ) { Text("INICIAR SESIÓN", fontWeight = FontWeight.Bold) }
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onLoginSuccess) { Text("ENTRAR COMO INVITADO", color = Color.White, fontWeight = FontWeight.Bold) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutTopBar(onMenuClick: () -> Unit) {
    CenterAlignedTopAppBar(
        title = { Text("CAMPO DE ENTRENAMIENTO", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        navigationIcon = { IconButton(onClick = onMenuClick) { Icon(Icons.Default.Menu, contentDescription = "Menú") } }
    )
}

@Composable
fun WorkoutHomeScreen(uiState: WorkoutUiState, onCategoryClick: (String) -> Unit, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (uiState) {
            is WorkoutUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            is WorkoutUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "No se pudieron cargar las rutinas",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = uiState.message,
                        color = Color.Red,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(onClick = onRetry) {
                        Text("Reintentar")
                    }
                }
            }
            is WorkoutUiState.Success -> {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    item {Text("Selecciona tu rutina de entrenamiento", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)}
                    items(uiState.categories) { category ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onCategoryClick(category.title) },
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(category.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    Text("${category.exercises.size} ejercicios • ${category.level}", style = MaterialTheme.typography.bodyMedium)
                                }
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExerciseDetailScreen(category: ExerciseCategory, onBack: () -> Unit, onStartSession: () -> Unit, onRefresh: () -> Unit = {}) {
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper(context) }
    var showDialog by remember { mutableStateOf(false) }
    var exerciseToEdit by remember { mutableStateOf<Exercise?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<Exercise?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { exerciseToEdit = null; showDialog = true }, containerColor = Color(0xFF1B5E20), contentColor = Color.White) {
                Icon(Icons.Default.Add, contentDescription = "Añadir")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ConstraintLayout(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer).padding(16.dp)) {
                val (btnBack, title, count) = createRefs()
                IconButton(onClick = onBack, modifier = Modifier.constrainAs(btnBack) { top.linkTo(parent.top); start.linkTo(parent.start) }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                }
                Text(text = category.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black,
                    modifier = Modifier.constrainAs(title) { top.linkTo(btnBack.bottom); start.linkTo(parent.start) })
                Text(text = "${category.exercises.size} EJERCICIOS", style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.constrainAs(count) { bottom.linkTo(title.bottom); end.linkTo(parent.end) })
            }

            LazyColumn(modifier = Modifier.weight(1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(category.exercises) { exercise ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // MOSTRAR IMAGEN DINÁMICA O ICONO
                                val imageRes = when(exercise.name) {
                                    "Crunch Militar" -> context.resources.getIdentifier("crunch_militar", "drawable", context.packageName)
                                    else -> 0
                                }

                                if (imageRes != 0) {
                                    Image(
                                        painter = painterResource(id = imageRes),
                                        contentDescription = null,
                                        modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF1B5E20)), contentAlignment = Alignment.Center) {
                                        Text(text = "!", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = exercise.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(text = exercise.reps, style = MaterialTheme.typography.bodySmall, color = Color(0xFF1B5E20), fontWeight = FontWeight.Bold)
                                }
                                IconButton(onClick = { exerciseToEdit = exercise; showDialog = true }) { Icon(Icons.Default.Edit, contentDescription = null, tint = Color.Gray) }
                                IconButton(onClick = { showDeleteConfirm = exercise }) { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) }
                            }
                            Text(text = exercise.description, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        }
                    }
                }
            }
            Button(onClick = onStartSession, modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp), shape = RoundedCornerShape(12.dp)) {
                Text("¡COMENZAR MISIÓN!", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showDialog) {
        ExerciseFormDialog(
            exercise = exerciseToEdit,
            onDismiss = { showDialog = false },
            onSave = { name, desc, reps ->
                if (exerciseToEdit == null) {
                    dbHelper.insertExercise(name, desc, reps, category.id)
                    Toast.makeText(context, "Misión Guardada", Toast.LENGTH_SHORT).show()
                } else {
                    dbHelper.updateExercise(exerciseToEdit!!.id, name, desc, reps)
                    Toast.makeText(context, "Misión Actualizada", Toast.LENGTH_SHORT).show()
                }
                onRefresh()
                showDialog = false
            }
        )
    }

    if (showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("¿ELIMINAR?") },
            text = { Text("¿Borrar '${showDeleteConfirm?.name}'?") },
            confirmButton = {
                Button(onClick = { 
                    dbHelper.deleteExercise(showDeleteConfirm!!.id)
                    onRefresh()
                    showDeleteConfirm = null 
                }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("SÍ, ELIMINAR") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = null }) { Text("CANCELAR") } }
        )
    }
}

@Composable
fun ExerciseFormDialog(exercise: Exercise?, onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf(exercise?.name ?: "") }
    var desc by remember { mutableStateOf(exercise?.description ?: "") }
    var reps by remember { mutableStateOf(exercise?.reps ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (exercise == null) "NUEVO EJERCICIO" else "EDITAR EJERCICIO") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") })
                OutlinedTextField(value = reps, onValueChange = { reps = it }, label = { Text("Reps (ej: 4x20)") })
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Instrucciones") }, minLines = 2)
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onSave(name, desc, reps) }) { Text("GUARDAR") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR") } }
    )
}

@Composable
fun WorkoutSessionScreen(category: ExerciseCategory, onFinish: () -> Unit) {
    var currentExerciseIndex by remember { mutableIntStateOf(0) }
    var timeLeft by remember { mutableIntStateOf(240) }
    var isResting by remember { mutableStateOf(false) }
    val currentExercise = if (currentExerciseIndex < category.exercises.size) category.exercises[currentExerciseIndex] else null

    LaunchedEffect(key1 = timeLeft, key2 = isResting) {
        if (timeLeft > 0) { delay(1000L); timeLeft-- }
        else {
            if (!isResting) { isResting = true; timeLeft = 40 }
            else { isResting = false; currentExerciseIndex++; timeLeft = 240 }
        }
    }

    if (currentExercise == null) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("¡MISIÓN CUMPLIDA!", fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color(0xFF1B5E20))
                Text("Has completado el entrenamiento de ${category.title}")
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = onFinish, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("VOLVER AL CAMPO") }
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize().background(if (isResting) Color(0xFFE8F5E9) else Color.White).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = if (isResting) "DESCANSO TÁCTICO" else "EJERCICIO ACTUAL", color = if (isResting) Color(0xFF2E7D32) else Color.Gray)
            Text(text = if (isResting) "Prepárate para el siguiente" else currentExercise.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            
            if (!isResting) {
                val context = LocalContext.current
                val exerciseName = currentExercise.name.lowercase().replace(" ", "_")
                val startPhaseRes = context.resources.getIdentifier("${exerciseName}_start", "drawable", context.packageName)
                val finishPhaseRes = context.resources.getIdentifier("${exerciseName}_finish", "drawable", context.packageName)

                ExerciseIllustration(
                    modifier = Modifier.padding(vertical = 16.dp),
                    title = currentExercise.name,
                    startPhaseImageRes = if (startPhaseRes != 0) startPhaseRes else null,
                    finishPhaseImageRes = if (finishPhaseRes != 0) finishPhaseRes else null
                )
            } else {
                Spacer(modifier = Modifier.height(48.dp))
            }

            Box(modifier = Modifier.size(200.dp).clip(CircleShape).background(if (isResting) Color(0xFF2E7D32) else Color(0xFF1B5E20)), contentAlignment = Alignment.Center) {
                val min = timeLeft / 60; val sec = timeLeft % 60
                Text(text = String.format("%02d:%02d", min, sec), fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.height(48.dp))
            if (!isResting) {
                Text(text = "OBJETIVO:", fontWeight = FontWeight.Bold)
                Text(text = currentExercise.reps, style = MaterialTheme.typography.titleLarge, color = Color(0xFF1B5E20))
                Text(text = currentExercise.description, textAlign = TextAlign.Center, color = Color.Gray)
            }
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onFinish) {
                Text("SALIR DEL ENTRENAMIENTO", color = Color.Red)
            }
        }
    }
}
