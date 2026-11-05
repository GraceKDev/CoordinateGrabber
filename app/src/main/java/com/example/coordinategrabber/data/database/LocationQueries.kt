package com.example.coordinategrabber.data.database

import android.content.ContentValues
import android.content.Context
import com.example.coordinategrabber.data.export.CoordinateDTO
import com.example.coordinategrabber.data.export.RouteDTO
import com.example.coordinategrabber.data.model.Location
import com.example.coordinategrabber.data.model.Route

class UserQueries(context: Context) {
    private val dbHelper = MyDatabaseHelper(context)

    fun createRoute(name: String, date: String): Int {
        return dbHelper.writableDatabase.use { db ->
            val values = ContentValues().apply {
                put(MyDatabaseHelper.COLUMN_ROUTE_NAME, name)
                put(MyDatabaseHelper.COLUMN_ROUTE_DATE, date)
            }
            db.insert(MyDatabaseHelper.TABLE_ROUTES, null, values).toInt()
        }
    }

    fun addLocation(location: Location): Long {
        return dbHelper.writableDatabase.use { db ->
            val values = ContentValues().apply {
                put(MyDatabaseHelper.COLUMN_LAT, location.lat)
                put(MyDatabaseHelper.COLUMN_LON, location.lon)
                put(MyDatabaseHelper.COLUMN_DATE, location.date)
                location.routeId?.let {
                    put(MyDatabaseHelper.COLUMN_ROUTE_ID, it)
                }
                location.photoUri?.let {
                    put(MyDatabaseHelper.COLUMN_PHOTO_URI, it)
                }
            }
            db.insert(MyDatabaseHelper.TABLE_COORDINATES, null, values)
        }
    }

    fun deleteLocation(id: Int): Int {
        return dbHelper.writableDatabase.use { db ->
            db.delete(
                MyDatabaseHelper.TABLE_COORDINATES,
                "${MyDatabaseHelper.COLUMN_ID} = ?",
                arrayOf(id.toString())
            )
        }
    }

    fun deleteRoute(routeId: Int): Int {
        return dbHelper.writableDatabase.use { db ->
            db.delete(
                MyDatabaseHelper.TABLE_COORDINATES,
                "${MyDatabaseHelper.COLUMN_ROUTE_ID} = ?",
                arrayOf(routeId.toString())
            )
            db.delete(
                MyDatabaseHelper.TABLE_ROUTES,
                "${MyDatabaseHelper.COLUMN_ID} = ?",
                arrayOf(routeId.toString())
            )
        }
    }

    fun deleteAllLocalData() {
        dbHelper.writableDatabase.use { db ->
            db.delete(MyDatabaseHelper.TABLE_COORDINATES, null, null)
            db.delete(MyDatabaseHelper.TABLE_ROUTES, null, null)
        }
    }

    fun importData(coordinates: List<CoordinateDTO>, routes: List<RouteDTO>) {
        dbHelper.writableDatabase.use { db ->
            db.beginTransaction()
            try {
                coordinates.forEach { dto ->
                    val values = ContentValues().apply {
                        put(MyDatabaseHelper.COLUMN_LAT, dto.latitude)
                        put(MyDatabaseHelper.COLUMN_LON, dto.longitude)
                        put(MyDatabaseHelper.COLUMN_DATE, dto.date)
                    }
                    db.insert(MyDatabaseHelper.TABLE_COORDINATES, null, values)
                }

                routes.forEach { routeDto ->
                    val routeValues = ContentValues().apply {
                        put(MyDatabaseHelper.COLUMN_ROUTE_NAME, routeDto.name)
                        put(MyDatabaseHelper.COLUMN_ROUTE_DATE, routeDto.date)
                    }
                    val routeId = db.insert(MyDatabaseHelper.TABLE_ROUTES, null, routeValues).toInt()

                    routeDto.points.forEach { pointDto ->
                        val pointValues = ContentValues().apply {
                            put(MyDatabaseHelper.COLUMN_ROUTE_ID, routeId)
                            put(MyDatabaseHelper.COLUMN_LAT, pointDto.latitude)
                            put(MyDatabaseHelper.COLUMN_LON, pointDto.longitude)
                            put(MyDatabaseHelper.COLUMN_DATE, pointDto.date)
                        }
                        db.insert(MyDatabaseHelper.TABLE_COORDINATES, null, pointValues)
                    }
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
    }

    fun getManualLocations(): List<Location> {
        val locationList = mutableListOf<Location>()
        val db = dbHelper.readableDatabase

        db.query(
            MyDatabaseHelper.TABLE_COORDINATES,
            null,
            "${MyDatabaseHelper.COLUMN_ROUTE_ID} IS NULL OR ${MyDatabaseHelper.COLUMN_ROUTE_ID} = 0",
            null,
            null,
            null,
            "${MyDatabaseHelper.COLUMN_ID} DESC"
        ).use { cursor ->
            val photoUriIndex = cursor.getColumnIndex(MyDatabaseHelper.COLUMN_PHOTO_URI)
            while (cursor.moveToNext()) {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(MyDatabaseHelper.COLUMN_ID))
                val lat = cursor.getDouble(cursor.getColumnIndexOrThrow(MyDatabaseHelper.COLUMN_LAT))
                val lon = cursor.getDouble(cursor.getColumnIndexOrThrow(MyDatabaseHelper.COLUMN_LON))
                val date = cursor.getString(cursor.getColumnIndexOrThrow(MyDatabaseHelper.COLUMN_DATE))
                val photoUri = if (photoUriIndex != -1 && !cursor.isNull(photoUriIndex)) {
                    cursor.getString(photoUriIndex)
                } else null

                locationList.add(
                    Location(
                        id = id,
                        lat = lat,
                        lon = lon,
                        date = date,
                        photoUri = photoUri
                    )
                )
            }
        }
        return locationList
    }

    fun getAllRoutesWithPoints(): List<Route> {
        val routeList = mutableListOf<Route>()
        val db = dbHelper.readableDatabase

        db.query(
            MyDatabaseHelper.TABLE_ROUTES,
            null,
            null,
            null,
            null,
            null,
            "${MyDatabaseHelper.COLUMN_ID} DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(MyDatabaseHelper.COLUMN_ID))
                val name = cursor.getString(cursor.getColumnIndexOrThrow(MyDatabaseHelper.COLUMN_ROUTE_NAME))
                val date = cursor.getString(cursor.getColumnIndexOrThrow(MyDatabaseHelper.COLUMN_ROUTE_DATE))

                val points = getRoutePoints(id)
                routeList.add(
                    Route(
                        id = id,
                        name = name,
                        date = date,
                        points = points
                    )
                )
            }
        }
        return routeList
    }

    private fun getRoutePoints(routeId: Int): List<Location> {
        val points = mutableListOf<Location>()
        val db = dbHelper.readableDatabase

        db.query(
            MyDatabaseHelper.TABLE_COORDINATES,
            null,
            "${MyDatabaseHelper.COLUMN_ROUTE_ID} = ?",
            arrayOf(routeId.toString()),
            null,
            null,
            "${MyDatabaseHelper.COLUMN_ID} ASC"
        ).use { cursor ->
            val photoUriIndex = cursor.getColumnIndex(MyDatabaseHelper.COLUMN_PHOTO_URI)
            while (cursor.moveToNext()) {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(MyDatabaseHelper.COLUMN_ID))
                val lat = cursor.getDouble(cursor.getColumnIndexOrThrow(MyDatabaseHelper.COLUMN_LAT))
                val lon = cursor.getDouble(cursor.getColumnIndexOrThrow(MyDatabaseHelper.COLUMN_LON))
                val date = cursor.getString(cursor.getColumnIndexOrThrow(MyDatabaseHelper.COLUMN_DATE))
                val photoUri = if (photoUriIndex != -1 && !cursor.isNull(photoUriIndex)) {
                    cursor.getString(photoUriIndex)
                } else null

                points.add(
                    Location(
                        id = id,
                        routeId = routeId,
                        lat = lat,
                        lon = lon,
                        date = date,
                        photoUri = photoUri
                    )
                )
            }
        }
        return points
    }

    fun getAllLocations(): List<Location> {
        val locationList = mutableListOf<Location>()
        val db = dbHelper.readableDatabase

        db.query(
            MyDatabaseHelper.TABLE_COORDINATES,
            null,
            null,
            null,
            null,
            null,
            "${MyDatabaseHelper.COLUMN_ID} ASC"
        ).use { cursor ->
            val photoUriIndex = cursor.getColumnIndex(MyDatabaseHelper.COLUMN_PHOTO_URI)
            while (cursor.moveToNext()) {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(MyDatabaseHelper.COLUMN_ID))
                val routeId = if (!cursor.isNull(cursor.getColumnIndexOrThrow(MyDatabaseHelper.COLUMN_ROUTE_ID))) {
                    cursor.getInt(cursor.getColumnIndexOrThrow(MyDatabaseHelper.COLUMN_ROUTE_ID))
                } else null
                val lat = cursor.getDouble(cursor.getColumnIndexOrThrow(MyDatabaseHelper.COLUMN_LAT))
                val lon = cursor.getDouble(cursor.getColumnIndexOrThrow(MyDatabaseHelper.COLUMN_LON))
                val date = cursor.getString(cursor.getColumnIndexOrThrow(MyDatabaseHelper.COLUMN_DATE))
                val photoUri = if (photoUriIndex != -1 && !cursor.isNull(photoUriIndex)) {
                    cursor.getString(photoUriIndex)
                } else null

                locationList.add(
                    Location(
                        id = id,
                        routeId = routeId,
                        lat = lat,
                        lon = lon,
                        date = date,
                        photoUri = photoUri
                    )
                )
            }
        }
        return locationList
    }
}
