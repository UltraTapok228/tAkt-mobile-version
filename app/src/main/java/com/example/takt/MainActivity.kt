package com.example.takt

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        if (savedInstanceState == null) {
            loadFragment(NowPlayingFragment())
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_listen -> {
                    loadFragment(NowPlayingFragment())
                    true
                }
                R.id.nav_library -> {
                    loadFragment(LibraryFragment())
                    true
                }
                R.id.nav_crzc -> {
                    // Пока временно грузим экран Библиотеки как заглушку.
                    // Позже мы создадим под это дело отдельный CrzcFragment
                    loadFragment(LibraryFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
