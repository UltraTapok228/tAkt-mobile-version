package com.example.takt

import android.provider.MediaStore
import java.io.Serializable

// этот файл нужен чтобы объяснить приложению, что такое "Трек"

// data class
data class Track(
    val title: String,
    val artist: String,
    val duration: String,
    val path: String, // путь в памяти телефона
    val album: String // для сортировки треков Crazy Cucumber по альбомам
) : Serializable