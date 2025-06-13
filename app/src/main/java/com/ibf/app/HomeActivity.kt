package com.ibf.app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.google.firebase.auth.FirebaseAuth

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val logoutButton = findViewById<Button>(R.id.buttonLogout)

        logoutButton.setOnClickListener {
            // Desloga o usuário do Firebase
            FirebaseAuth.getInstance().signOut()

            // Cria a intenção de voltar para a tela de login
            val intent = Intent(this, MainActivity::class.java)
            // Flags para limpar o histórico de telas e não deixar o usuário voltar para a Home
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }
}
