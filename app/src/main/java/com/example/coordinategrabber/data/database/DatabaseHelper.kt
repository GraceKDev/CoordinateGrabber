package com.example.coordinategrabber.data.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

public class MyDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VER) {

    override fun onCreate(db: SQLiteDatabase) {
        val createCoordinatesTable = """
            CREATE TABLE $TABLE_COORDINATES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_LAT DOUBLE,
                $COLUMN_LON DOUBLE,
                $COLUMN_DATE STRING,
                $COLUMN_ROUTE_ID INTEGER
            )
        """.trimIndent()

        val createRoutesTable = """
            CREATE TABLE $TABLE_ROUTES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_ROUTE_NAME STRING,
                $COLUMN_ROUTE_DATE STRING
            )
        """.trimIndent()

        db.execSQL(createCoordinatesTable)
        db.execSQL(createRoutesTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_COORDINATES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ROUTES")
        onCreate(db)
    }

    companion object {
        private const val DATABASE_NAME = "UserData.db"
        private const val DATABASE_VER = 3

        const val TABLE_COORDINATES = "Coordinates"
        const val COLUMN_ID = "id"
        const val COLUMN_LAT = "lat"
        const val COLUMN_LON = "lon"
        const val COLUMN_DATE = "date"
        const val COLUMN_ROUTE_ID = "route_id"

        const val TABLE_ROUTES = "Routes"
        const val COLUMN_ROUTE_NAME = "name"
        const val COLUMN_ROUTE_DATE = "date"
    }
}
