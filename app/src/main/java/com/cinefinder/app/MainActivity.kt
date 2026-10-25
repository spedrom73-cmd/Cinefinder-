package com.cinefinder.app

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    Toast.makeText(this, "Início selecionado", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_categories -> {
                    Toast.makeText(this, "Categorias selecionadas", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_identify -> {
                    Toast.makeText(this, "Identificar selecionado", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_explore -> {
                    Toast.makeText(this, "Explorar selecionado", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_account -> {
                    Toast.makeText(this, "Conta selecionada", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
    }
}
