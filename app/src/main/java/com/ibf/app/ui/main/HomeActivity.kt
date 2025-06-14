package com.ibf.app.ui.main

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.ibf.app.R

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home) // Garanta que você tem activity_home.xml

        val logoutButton = findViewById<Button>(R.id.buttonLogout) // Assumindo R.id.buttonLogout

        logoutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()

            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}