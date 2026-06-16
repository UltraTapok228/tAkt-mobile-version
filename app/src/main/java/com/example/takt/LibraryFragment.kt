package com.example.takt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class LibraryFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Загружаем наш терминальный XML-экран
        val view = inflater.inflate(R.layout.fragment_library, container, false)

        // Создаем тестовую базу данных треков
        val myTracks = listOf(
            Track("Mein Teil", "Rammstein", "04:32"),
            Track("ДА ПОШЛА ТЫ...", "Crazy Cucumber", "00:56"),
            Track("God Save The Queen", "Sex Pistols", "03:17"),
            Track("Dig Up Her Bones", "Misfits", "03:01"),
            Track("Fuck The System", "The Exploited", "04:15")
        )

        // Находим наш список на экране
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewTracks)

        // Обязательно говорим списку, что элементы должны идти друг за другом сверху вниз
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Подключаем наш Адаптер с музыкой к списку
        recyclerView.adapter = TrackAdapter(myTracks)

        return view
    }
}