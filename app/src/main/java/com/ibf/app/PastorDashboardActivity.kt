package com.ibf.app // Seu package name correto

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class PastorDashboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pastor_dashboard)

        // Encontra o ImageView do perfil no novo layout
        val profileImage = findViewById<ImageView>(R.id.image_profile)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Define a ação de clique para a IMAGEM DE PERFIL
        profileImage.setOnClickListener {
            fazerLogout()
        }

        // Define a lógica para a barra de navegação inferior
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    // Lógica para o item "Início"
                    Toast.makeText(this, "Clicou em Início", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.navigation_reports -> {
                    // Lógica para o item "Relatórios"
                    Toast.makeText(this, "Clicou em Relatórios", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.navigation_profile -> {
                    // Poderíamos também colocar o logout aqui
                    Toast.makeText(this, "Clicou em Perfil", Toast.LENGTH_SHORT).show()
                    fazerLogout() // Exemplo: sair ao clicar no perfil
                    true
                }
                else -> false
            }
        }
    }

    private fun fazerLogout() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, MainActivity::class.java)
        // Limpa todas as telas anteriores para que o usuário não possa "voltar"
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish() // Fecha a tela do painel
    }
}