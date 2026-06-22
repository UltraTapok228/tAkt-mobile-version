package com.example.takt

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager // импорт для очистки истории
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    // 1. Создаем экземпляры всех экранов ОДИН РАЗ, чтобы они не уничтожались
    private val fragmentListen = NowPlayingFragment()
    private val fragmentLibrary = LibraryFragment()
    private val fragmentCrzc = CrzcFragment()

    // Переменная, которая хранит текущий открытый экран
    private var activeFragment: Fragment = fragmentLibrary

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // 2. Добавляем все фрагменты в систему разом, но прячем два из них
        supportFragmentManager.beginTransaction().apply {
            add(R.id.fragmentContainer, fragmentCrzc, "CRZC").hide(fragmentCrzc)
            add(R.id.fragmentContainer, fragmentListen, "LISTEN").hide(fragmentListen)
            // Библиотеку не прячем, она будет открыта по умолчанию
            add(R.id.fragmentContainer, fragmentLibrary, "LIBRARY")
            commit()
        }

        // Устанавливаем вкладку по умолчанию для визуального выделения иконки
        bottomNav.selectedItemId = R.id.nav_library

        // 3. Умное переключение через .hide() и .show()
        bottomNav.setOnItemSelectedListener { item ->

            // Принудительно очищаем BackStack (закрываем открытый альбом, если он есть)
            supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)

            when (item.itemId) {
                R.id.nav_listen -> {
                    supportFragmentManager.beginTransaction().hide(activeFragment).show(fragmentListen).commit()
                    activeFragment = fragmentListen
                    true
                }
                R.id.nav_library -> {
                    supportFragmentManager.beginTransaction().hide(activeFragment).show(fragmentLibrary).commit()
                    activeFragment = fragmentLibrary
                    true
                }
                R.id.nav_crzc -> {
                    supportFragmentManager.beginTransaction().hide(activeFragment).show(fragmentCrzc).commit()
                    activeFragment = fragmentCrzc
                    true
                }
                else -> false
            }
        }
    }
}