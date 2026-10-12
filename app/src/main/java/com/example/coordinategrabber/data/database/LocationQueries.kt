package com.example.coordinategrabber.data.database

import android.content.ContentValues
import android.content.Context
import com.example.coordinategrabber.data.model.Location

class UserQueries(context: Context) {
    private val dbHelper = MyDatabaseHelper(context)

    fun addLocation(location: Location): Long {
        return dbHelper.writableDatabase.use { db ->
            val values = ContentValues().apply {
                put(MyDatabaseHelper.COLUMN_LAT,location.lat)
                put(MyDatabaseHelper.COLUMN_LON,location.lon)
                put(MyDatabaseHelper.COLUMN_DATE,location.date)
            }
            db.insert(MyDatabaseHelper.TABLE_NAME,null, values)
        }
    }

    fun getAllLocations() :List<Location> {
        val locationList = mutableListOf<Location>()
        val db = dbHelper.readableDatabase


        db.query(
            MyDatabaseHelper.TABLE_NAME,
            null,
            null,
            null,
            null,
            null,
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val lat = cursor.getDouble(
                    cursor.getColumnIndexOrThrow(MyDatabaseHelper.COLUMN_LAT)
                )

                val lon = cursor.getDouble(
                    cursor.getColumnIndexOrThrow(MyDatabaseHelper.COLUMN_LON)
                )

                val date = cursor.getString(
                    cursor.getColumnIndexOrThrow(MyDatabaseHelper.COLUMN_DATE)
                )

                locationList.add(
                    Location(
                        lat = lat,
                        lon = lon,
                        date = date
                    )
                )
            }
        }

        return locationList

        return locationList
    }
}
