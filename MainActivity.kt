package com.example.rutinasdegimnasio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.rutinasdegimnasio.ui.theme.RutinasdegimnasioTheme
import kotlinx.coroutines.launch

// Modelo de datos
data class ExerciseCategory(val id: Int, val title: String, val level: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RutinasdegimnasioTheme {
                MainAppStructure()
            }
        }
    }
}

@Composable
fun MainAppStructure() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text("Menú de Rutinas", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
                HorizontalDivider()
                NavigationDrawerItem(
                    label = { Text("Inicio") },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() } }
                )
                NavigationDrawerItem(
                    label = { Text("Mi Perfil") },
                    selected = false,
                    onClick = { /* Ir a perfil */ }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                WorkoutTopBar(onMenuClick = { scope.launch { drawerState.open() } })
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("home") {
                    WorkoutHomeScreen(onCategoryClick = { navController.navigate("details") })
                }
                composable("details") {
                    ExerciseDetailScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutTopBar(onMenuClick: () -> Unit) {
    CenterAlignedTopAppBar(
        title = { Text("GYM EN CASA", fontWeight = FontWeight.Bold) },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Menu, contentDescription = "Menú")
            }
        }
    )
}

@Composable
fun WorkoutHomeScreen(onCategoryClick: () -> Unit) {
    val categories = listOf(
        ExerciseCategory(1, "Abdominales", "Principiante"),
        ExerciseCategory(2, "Pecho", "Intermedio"),
        ExerciseCategory(3, "Brazo", "Avanzado")
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Text("Elige tu rutina", style = MaterialTheme.typography.headlineSmall) }
        items(categories) { category ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onCategoryClick() },
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(category.title, modifier = Modifier.padding(24.dp), style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@Composable
fun ExerciseDetailScreen(onBack: () -> Unit) {
    // Aquí implementaremos ConstraintLayout para tu entrega
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Lista de Ejercicios", style = MaterialTheme.typography.headlineMedium)
        Button(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) {
            Text("Volver")
        }
    }
}
