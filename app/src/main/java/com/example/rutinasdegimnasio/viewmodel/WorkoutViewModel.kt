package com.example.rutinasdegimnasio.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rutinasdegimnasio.data.DatabaseHelper
import com.example.rutinasdegimnasio.model.Exercise
import com.example.rutinasdegimnasio.model.ExerciseCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class WorkoutUiState {
    object Loading : WorkoutUiState()
    data class Success(val categories: List<ExerciseCategory>) : WorkoutUiState()
    data class Error(val message: String) : WorkoutUiState()
}

// Cambiamos a AndroidViewModel para poder acceder a la base de datos local
class WorkoutViewModel(application: Application) : AndroidViewModel(application) {
    private val dbHelper = DatabaseHelper(application)
    private val _uiState = MutableStateFlow<WorkoutUiState>(WorkoutUiState.Loading)
    val uiState: StateFlow<WorkoutUiState> = _uiState

    init {
        fetchWorkouts()
    }

    fun fetchWorkouts() {
        viewModelScope.launch {
            _uiState.value = WorkoutUiState.Loading
            try {
                // CARGA REAL DESDE SQLITE (Punto 2 del avance)
                val categories = mutableListOf<ExerciseCategory>()
                val db = dbHelper.readableDatabase
                
                val catCursor = db.rawQuery("SELECT * FROM ${DatabaseHelper.TABLE_CATEGORIES}", null)
                while (catCursor.moveToNext()) {
                    val catId = catCursor.getInt(catCursor.getColumnIndexOrThrow(DatabaseHelper.CAT_ID))
                    val catTitle = catCursor.getString(catCursor.getColumnIndexOrThrow(DatabaseHelper.CAT_TITLE))
                    val catLevel = catCursor.getString(catCursor.getColumnIndexOrThrow(DatabaseHelper.CAT_LEVEL))
                    
                    val exercises = mutableListOf<Exercise>()
                    val exCursor = db.rawQuery("SELECT * FROM ${DatabaseHelper.TABLE_EXERCISES} WHERE ${DatabaseHelper.EX_CAT_ID} = $catId", null)
                    while (exCursor.moveToNext()) {
                        exercises.add(Exercise(
                            id = exCursor.getInt(exCursor.getColumnIndexOrThrow(DatabaseHelper.EX_ID)),
                            name = exCursor.getString(exCursor.getColumnIndexOrThrow(DatabaseHelper.EX_NAME)),
                            description = exCursor.getString(exCursor.getColumnIndexOrThrow(DatabaseHelper.EX_DESC)),
                            reps = exCursor.getString(exCursor.getColumnIndexOrThrow(DatabaseHelper.EX_REPS))
                        ))
                    }
                    exCursor.close()
                    categories.add(ExerciseCategory(catId, catTitle, catLevel, exercises))
                }
                catCursor.close()
                
                _uiState.value = WorkoutUiState.Success(categories)
            } catch (e: Exception) {
                _uiState.value = WorkoutUiState.Error("Error al cargar base de datos: ${e.message}")
            }
        }
    }
}
