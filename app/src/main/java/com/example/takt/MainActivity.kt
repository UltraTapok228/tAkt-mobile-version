package com.example.takt

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Говорим приложению отрисовать наш каркас с нижней панелью
        setContentView(R.layout.activity_main)

        // Находим нашу нижнюю панель по ID
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // При первом запуске сразу показываем экран плеера (NowPlayingFragment)
        if (savedInstanceState == null) {
            loadFragment(NowPlayingFragment())
        }

        // Вешаем слушатель на нажатия кнопок в меню
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_listen -> {
                    // Если нажали LISTEN, грузим фрагмент плеера
                    loadFragment(NowPlayingFragment())
                    true
                }
                R.id.nav_library -> {
                    // Если нажали LIBRARY, грузим фрагмент библиотеки
                    loadFragment(LibraryFragment())
                    true
                }
                else -> false
            }
        }
    }

    // Вспомогательная функция, которая делает всю грязную работу по замене экранов
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}