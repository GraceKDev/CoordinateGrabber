package com.example.coordinategrabber.data.model

data class Route(
    val id: Int,
    val name: String,
    val date: String,
    val points: List<Location> = emptyList()
)
