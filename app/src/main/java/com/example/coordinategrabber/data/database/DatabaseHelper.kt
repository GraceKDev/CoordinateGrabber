package com.example.coordinategrabber.data.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class MyDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VER) {

    override fun onCreate(db: SQLiteDatabase) {
        val createCoordinatesTable = """
            CREATE TABLE $TABLE_COORDINATES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_LAT DOUBLE,
                $COLUMN_LON DOUBLE,
                $COLUMN_DATE TEXT,
                $COLUMN_ROUTE_ID INTEGER,
                $COLUMN_PHOTO_URI TEXT,
                $COLUMN_IMAGE_URL TEXT,
                $COLUMN_BACKED_UP INTEGER DEFAULT 0
            )
        """.trimIndent()

        val createRoutesTable = """
            CREATE TABLE $TABLE_ROUTES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_ROUTE_NAME TEXT,
                $COLUMN_ROUTE_DATE TEXT
            )
        """.trimIndent()

        db.execSQL(createCoordinatesTable)
        db.execSQL(createRoutesTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        try {
            db.execSQL("ALTER TABLE $TABLE_COORDINATES ADD COLUMN $COLUMN_PHOTO_URI TEXT")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            db.execSQL("ALTER TABLE $TABLE_COORDINATES ADD COLUMN $COLUMN_IMAGE_URL TEXT")
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            db.execSQL("ALTER TABLE $TABLE_COORDINATES ADD COLUMN $COLUMN_BACKED_UP INTEGER DEFAULT 0")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val DATABASE_NAME = "UserData.db"
        private const val DATABASE_VER = 9
        const val TABLE_COORDINATES = "Coordinates"
        const val COLUMN_ID = "id"
        const val COLUMN_LAT = "lat"
        const val COLUMN_LON = "lon"
        const val COLUMN_DATE = "date"
        const val COLUMN_ROUTE_ID = "route_id"
        const val COLUMN_PHOTO_URI = "photo_uri"
        const val COLUMN_IMAGE_URL = "image_url"
        const val COLUMN_BACKED_UP = "backed_up"
        const val TABLE_ROUTES = "Routes"
        const val COLUMN_ROUTE_NAME = "name"
        const val COLUMN_ROUTE_DATE = "date"
    }
}
