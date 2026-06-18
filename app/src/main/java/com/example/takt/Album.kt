package com.example.takt

data class Album(
    val title: String,
    val artist: String,
    val coverPath: String, // путь к mp3 файлу
    val tracks: List<Track>
)