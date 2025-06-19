package com.ibf.app.ui.perfil

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ibf.app.R

class PerfilActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var nomeEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var editarSalvarButton: Button

    private var modoEdicao = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        nomeEditText = findViewById(R.id.edit_text_nome_perfil)
        emailEditText = findViewById(R.id.edit_text_email_perfil)
        editarSalvarButton = findViewById(R.id.button_editar_salvar)

        findViewById<ImageView>(R.id.button_back).setOnClickListener { finish() }

        carregarDadosUsuario()

        editarSalvarButton.setOnClickListener {
            if (modoEdicao) {
                salvarAlteracoes()
            } else {
                ativarModoEdicao()
            }
        }

        // Listeners para os botões futuros que ainda não têm função
        findViewById<Button>(R.id.button_estatisticas).setOnClickListener {
            startActivity(Intent(this, EstatisticasActivity::class.java))
        }

        findViewById<Button>(R.id.button_notificacoes).setOnClickListener { Toast.makeText(this, getString(R.string.em_breve), Toast.LENGTH_SHORT).show() }
        findViewById<Button>(R.id.button_ajuda).setOnClickListener { Toast.makeText(this, getString(R.string.em_breve), Toast.LENGTH_SHORT).show() }
    }

    private fun carregarDadosUsuario() {
        val user = auth.currentUser
        if (user != null) {
            firestore.collection("usuarios").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        nomeEditText.setText(document.getString("nome"))
                        emailEditText.setText(document.getString("email"))
                    }
                }
        }
    }

    private fun ativarModoEdicao() {
        modoEdicao = true
        nomeEditText.isEnabled = true
        nomeEditText.requestFocus()
        editarSalvarButton.text = getString(R.string.perfil_salvar)
    }

    private fun desativarModoEdicao() {
        modoEdicao = false
        nomeEditText.isEnabled = false
        editarSalvarButton.text = getString(R.string.perfil_editar)
    }

    private fun salvarAlteracoes() {
        val novoNome = nomeEditText.text.toString().trim()
        if (novoNome.isEmpty()) {
            nomeEditText.error = "O nome não pode ficar vazio"
            return
        }

        val user = auth.currentUser
        if (user != null) {
            editarSalvarButton.isEnabled = false
            firestore.collection("usuarios").document(user.uid)
                .update("nome", novoNome)
                .addOnSuccessListener {
                    Toast.makeText(this, getString(R.string.perfil_info_atualizadas), Toast.LENGTH_SHORT).show()
                    desativarModoEdicao()
                    editarSalvarButton.isEnabled = true
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "${getString(R.string.perfil_falha_atualizar)}: ${e.message}", Toast.LENGTH_LONG).show()
                    editarSalvarButton.isEnabled = true
                }
        }
    }
}