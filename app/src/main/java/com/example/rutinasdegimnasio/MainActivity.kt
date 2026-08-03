package com.example.rutinasdegimnasio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
import com.example.rutinasdegimnasio.model.Exercise
import com.example.rutinasdegimnasio.model.ExerciseCategory
import com.example.rutinasdegimnasio.viewmodel.WorkoutViewModel
import com.example.rutinasdegimnasio.viewmodel.WorkoutUiState
import androidx.lifecycle.viewmodel.compose.viewModel
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

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentRoute != "welcome" && currentRoute?.startsWith("workout_session") == false,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                Text("GIMNASIO APP", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Black, fontSize = 24.sp)
                HorizontalDivider()
                NavigationDrawerItem(
                    label = { Text("Rutinas") },
                    selected = currentRoute == "home",
                    onClick = { 
                        scope.launch { drawerState.close() }
                        navController.navigate("home")
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Cerrar Sesión") },
                    selected = false,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        navController.navigate("welcome") {
                            popUpTo("home") { inclusive = true }
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
                composable("welcome") {
                    WelcomeScreen(
                        onLoginClick = { /* Lógica login */ },
                        onGuestClick = { navController.navigate("home") }
                    )
                }
                composable("home") {
                    WorkoutHomeScreen(
                        uiState = uiState,
                        onCategoryClick = { categoryName ->
                            navController.navigate("details/$categoryName")
                        },
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
                            onStartSession = { navController.navigate("workout_session/$categoryName") }
                        )
                    }
                }
                composable(
                    "workout_session/{categoryName}",
                    arguments = listOf(navArgument("categoryName") { type = NavType.StringType })
                ) { backStackEntry ->
                    val categoryName = backStackEntry.arguments?.getString("categoryName") ?: ""
                    val category = (uiState as? WorkoutUiState.Success)?.categories?.find { it.title == categoryName }
                    category?.let {
                        WorkoutSessionScreen(it, onFinish = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}

@Composable
fun WelcomeScreen(onLoginClick: () -> Unit, onGuestClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1B5E20))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "RUTINAS GYM", fontSize = 48.sp, fontWeight = FontWeight.Black, color = Color.White)
        Text(text = "Disciplina y Fuerza", fontSize = 18.sp, color = Color.White.copy(alpha = 0.8f), modifier = Modifier.padding(bottom = 48.dp))
        Button(
            onClick = onLoginClick,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp)
        ) { Text("INICIAR SESIÓN", fontWeight = FontWeight.Bold) }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = onGuestClick,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color.White)),
            shape = RoundedCornerShape(12.dp)
        ) { Text("ENTRAR COMO INVITADO", fontWeight = FontWeight.Bold) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutTopBar(onMenuClick: () -> Unit) {
    CenterAlignedTopAppBar(
        title = { Text("CAMPO DE ENTRENAMIENTO", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Menu, contentDescription = "Menú")
            }
        }
    )
}

@Composable
fun WorkoutHomeScreen(uiState: WorkoutUiState, onCategoryClick: (String) -> Unit, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (uiState) {
            is WorkoutUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is WorkoutUiState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = uiState.message, color = Color.Red, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onRetry) { Text("Reintentar") }
                }
            }
            is WorkoutUiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { Text("Selecciona tu rutina de entrenamiento", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
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
fun ExerciseDetailScreen(category: ExerciseCategory, onBack: () -> Unit, onStartSession: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        ConstraintLayout(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer).padding(16.dp)
        ) {
            val (btnBack, title, count) = createRefs()
            IconButton(onClick = onBack, modifier = Modifier.constrainAs(btnBack) { top.linkTo(parent.top); start.linkTo(parent.start) }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
            }
            Text(text = category.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black,
                modifier = Modifier.constrainAs(title) { top.linkTo(btnBack.bottom); start.linkTo(parent.start) })
            Text(text = "${category.exercises.size} EJERCICIOS", style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.constrainAs(count) { bottom.linkTo(title.bottom); end.linkTo(parent.end) })
        }

        LazyColumn(
            modifier = Modifier.weight(1f).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(category.exercises) { exercise ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF1B5E20)), contentAlignment = Alignment.Center) {
                                Text(text = "!", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = exercise.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(text = exercise.reps, style = MaterialTheme.typography.bodySmall, color = Color(0xFF1B5E20), fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = exercise.description, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    }
                }
            }
        }
        
        Button(
            onClick = onStartSession, 
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("¡COMENZAR MISIÓN!", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun WorkoutSessionScreen(category: ExerciseCategory, onFinish: () -> Unit) {
    var currentExerciseIndex by remember { mutableIntStateOf(0) }
    var timeLeft by remember { mutableIntStateOf(240) } // 4 minutos = 240 segundos
    var isResting by remember { mutableStateOf(false) }
    
    val currentExercise = if (currentExerciseIndex < category.exercises.size) category.exercises[currentExerciseIndex] else null

    LaunchedEffect(key1 = timeLeft, key2 = isResting) {
        if (timeLeft > 0) {
            delay(1000L)
            timeLeft--
        } else {
            if (!isResting) {
                isResting = true
                timeLeft = 40 // 40 segundos de descanso
            } else {
                isResting = false
                currentExerciseIndex++
                timeLeft = 240 // Volver a 4 minutos
            }
        }
    }

    if (currentExercise == null) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("¡MISIÓN CUMPLIDA!", fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color(0xFF1B5E20))
                Text("Has completado el entrenamiento de ${category.title}", textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = onFinish, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Text("VOLVER AL CAMPO")
                }
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize().background(if (isResting) Color(0xFFE8F5E9) else Color.White).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (isResting) "DESCANSO TÁCTICO" else "EJERCICIO ACTUAL",
                style = MaterialTheme.typography.labelLarge,
                color = if (isResting) Color(0xFF2E7D32) else Color.Gray
            )
            
            Text(
                text = if (isResting) "Prepárate para el siguiente" else currentExercise.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Box(
                modifier = Modifier.size(200.dp).clip(CircleShape).background(if (isResting) Color(0xFF2E7D32) else Color(0xFF1B5E20)),
                contentAlignment = Alignment.Center
            ) {
                val minutes = timeLeft / 60
                val seconds = timeLeft % 60
                Text(
                    text = String.format("%02d:%02d", minutes, seconds),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            if (!isResting) {
                Text(text = "OBJETIVO:", fontWeight = FontWeight.Bold)
                Text(text = currentExercise.reps, style = MaterialTheme.typography.titleLarge, color = Color(0xFF1B5E20))
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = currentExercise.description, textAlign = TextAlign.Center, color = Color.Gray)
            } else {
                val nextExercise = if (currentExerciseIndex + 1 < category.exercises.size) category.exercises[currentExerciseIndex + 1] else null
                nextExercise?.let {
                    Text(text = "SIGUIENTE:", fontWeight = FontWeight.Bold)
                    Text(text = it.name, style = MaterialTheme.typography.titleLarge)
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            TextButton(onClick = onFinish) {
                Text("ABANDONAR MISIÓN", color = Color.Red)
            }
        }
    }
}
