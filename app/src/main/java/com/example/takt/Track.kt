package com.example.takt

import android.provider.MediaStore

// этот файл нужен чтобы объяснить приложению, что такое "Трек"

// data class
data class Track(
    val title: String,
    val artist: String,
    val duration: String,
    val path: String // путь в памяти телефона
)