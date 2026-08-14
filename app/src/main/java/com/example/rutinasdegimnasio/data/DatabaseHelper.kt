package com.example.rutinasdegimnasio.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "RutinasGym.db"
        const val DATABASE_VERSION = 2 // Subimos la versión para aplicar cambios

        const val TABLE_CATEGORIES = "categories"
        const val TABLE_EXERCISES = "exercises"

        const val CAT_ID = "id"
        const val CAT_TITLE = "title"
        const val CAT_LEVEL = "level"

        const val EX_ID = "id"
        const val EX_CAT_ID = "category_id"
        const val EX_NAME = "name"
        const val EX_DESC = "description"
        const val EX_REPS = "reps"
        const val EX_IMAGE = "image_uri" // Nueva columna
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE $TABLE_CATEGORIES ($CAT_ID INTEGER PRIMARY KEY AUTOINCREMENT, $CAT_TITLE TEXT, $CAT_LEVEL TEXT)")
        db.execSQL("CREATE TABLE $TABLE_EXERCISES ($EX_ID INTEGER PRIMARY KEY AUTOINCREMENT, $EX_CAT_ID INTEGER, $EX_NAME TEXT, $EX_DESC TEXT, $EX_REPS TEXT, $EX_IMAGE TEXT, FOREIGN KEY($EX_CAT_ID) REFERENCES $TABLE_CATEGORIES($CAT_ID))")

        // Datos de prueba
        db.execSQL("INSERT INTO $TABLE_CATEGORIES (title, level) VALUES ('Abdominales', 'Militar'), ('Pecho', 'Fuerza Especial'), ('Brazo', 'Infante'), ('Pierna', 'Ranger')")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE_EXERCISES ADD COLUMN $EX_IMAGE TEXT")
        }
    }

    fun insertExercise(name: String, desc: String, reps: String, catId: Int, imageUri: String? = null): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(EX_NAME, name)
            put(EX_DESC, desc)
            put(EX_REPS, reps)
            put(EX_CAT_ID, catId)
            put(EX_IMAGE, imageUri)
        }
        return db.insert(TABLE_EXERCISES, null, values)
    }

    fun updateExercise(id: Int, name: String, desc: String, reps: String, imageUri: String? = null): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(EX_NAME, name)
            put(EX_DESC, desc)
            put(EX_REPS, reps)
            put(EX_IMAGE, imageUri)
        }
        return db.update(TABLE_EXERCISES, values, "$EX_ID = ?", arrayOf(id.toString()))
    }

    fun deleteExercise(id: Int): Int {
        return this.writableDatabase.delete(TABLE_EXERCISES, "$EX_ID = ?", arrayOf(id.toString()))
    }
}
