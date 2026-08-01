package com.example.rutinasdegimnasio.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rutinasdegimnasio.model.Exercise
import com.example.rutinasdegimnasio.model.ExerciseCategory
import com.example.rutinasdegimnasio.network.RetrofitInstance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class WorkoutUiState {
    object Loading : WorkoutUiState()
    data class Success(val categories: List<ExerciseCategory>) : WorkoutUiState()
    data class Error(val message: String) : WorkoutUiState()
}

class WorkoutViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<WorkoutUiState>(WorkoutUiState.Loading)
    val uiState: StateFlow<WorkoutUiState> = _uiState

    init {
        fetchWorkouts()
    }

    fun fetchWorkouts() {
        viewModelScope.launch {
            _uiState.value = WorkoutUiState.Loading
            try {
                // En un escenario real con una API funcional:
                 // val response = RetrofitInstance.api.getWorkouts()
                 // _uiState.value = WorkoutUiState.Success(response)
                
                // Simulación de carga de API
                kotlinx.coroutines.delay(1500)
                _uiState.value = WorkoutUiState.Success(militaryWorkouts)
            } catch (e: Exception) {
                _uiState.value = WorkoutUiState.Error("Error de conexión: ${e.message}")
            }
        }
    }
}

private val militaryWorkouts = listOf(
    ExerciseCategory(1, "Abdominales", "Militar", listOf(
        Exercise("Crunch Militar", "Manos en la nuca, subida explosiva.", "4 Series x 25"),
        Exercise("Elevación de Piernas", "Mantener cuerpo recto, subir pies a 90°.", "4 Series x 15"),
        Exercise("Plancha Táctica", "Apoyo en antebrazos, abdomen contraído.", "3 Series x 1 min"),
        Exercise("V-Ups", "Tocar puntas de pies en el aire.", "3 Series x 12")
    )),
    ExerciseCategory(2, "Pecho", "Fuerza Especial", listOf(
        Exercise("Flexiones Militares", "Codos pegados al cuerpo, bajar completo.", "4 Series x 20"),
        Exercise("Flexiones Diamante", "Manos juntas formando un diamante.", "3 Series x 12"),
        Exercise("Flexiones Explosivas", "Despegar manos del suelo al subir.", "3 Series x 10"),
        Exercise("Fondos en Banco", "Cuerpo recto, bajar glúteos cerca del suelo.", "4 Series x 15")
    )),
    ExerciseCategory(3, "Brazo", "Infante", listOf(
        Exercise("Dominadas (Barra)", "Agarre prono, barbilla sobre la barra.", "4 Series x Máximo"),
        Exercise("Curl con Mochila", "Usa una mochila con peso como resistencia.", "4 Series x 12"),
        Exercise("Flexiones Cerradas", "Enfocadas en tríceps.", "3 Series x 15"),
        Exercise("Dominadas Supinas", "Palmas hacia ti, barbilla sobre barra.", "3 Series x 8")
    )),
    ExerciseCategory(4, "Pierna", "Ranger", listOf(
        Exercise("Sentadilla con Salto", "Bajar profundo y salto explosivo.", "4 Series x 15"),
        Exercise("Zancadas Militares", "Caminar dando pasos largos hacia abajo.", "4 Series x 20m"),
        Exercise("Sentadilla Isométrica", "Espalda contra pared en 90°.", "3 Series x 45 seg"),
        Exercise("Burpees Tácticos", "Cuerpo a tierra y salto.", "4 Series x 10")
    ))
)
