package com.ibf.app.ui.admin

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ibf.app.R

class AdminCadastroUsuarioActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var editNome: TextInputEditText
    private lateinit var editEmail: TextInputEditText
    private lateinit var editSenha: TextInputEditText
    private lateinit var spinnerPapel: Spinner
    private lateinit var spinnerRede: Spinner
    private lateinit var btnCadastrar: Button

    private val redes = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_cadastro_usuario)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        findViewById<ImageView>(R.id.btn_back).setOnClickListener { finish() }

        editNome = findViewById(R.id.edit_nome)
        editEmail = findViewById(R.id.edit_email)
        editSenha = findViewById(R.id.edit_senha)
        spinnerPapel = findViewById(R.id.spinner_papel)
        spinnerRede = findViewById(R.id.spinner_rede)
        btnCadastrar = findViewById(R.id.btn_cadastrar)

        configurarSpinners()

        btnCadastrar.setOnClickListener { processarCadastro() }
    }

    private fun configurarSpinners() {
        val papeis = listOf("secretario", "lider", "pastor", "admin")
        val adapterPapel = ArrayAdapter(this, android.R.layout.simple_spinner_item, papeis)
        adapterPapel.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPapel.adapter = adapterPapel

        firestore.collection("redes").get().addOnSuccessListener { docs ->
            redes.clear()
            redes.add("Geral (Todas as Redes)") // Opção especial
            for (doc in docs) {
                doc.getString("nome")?.let { redes.add(it) }
            }
            val adapterRede = ArrayAdapter(this, android.R.layout.simple_spinner_item, redes)
            adapterRede.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerRede.adapter = adapterRede
        }.addOnFailureListener {
            Toast.makeText(this, "Erro ao carregar redes.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun processarCadastro() {
        val nome = editNome.text.toString().trim()
        val email = editEmail.text.toString().trim()
        val senha = editSenha.text.toString().trim()
        val papel = spinnerPapel.selectedItem?.toString() ?: ""
        var rede = spinnerRede.selectedItem?.toString() ?: ""

        if (nome.isEmpty() || email.isEmpty() || senha.isEmpty() || papel.isEmpty() || rede.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos.", Toast.LENGTH_SHORT).show()
            return
        }

        if (senha.length < 6) {
            Toast.makeText(this, "A senha deve ter pelo menos 6 caracteres.", Toast.LENGTH_SHORT).show()
            return
        }

        if (rede == "Geral (Todas as Redes)") {
            rede = "geral"
        }

        btnCadastrar.isEnabled = false

        // Em vez de usar auth.createUserWithEmailAndPassword (que desloga o admin),
        // vamos enviar uma requisição para a Cloud Function através do Firestore
        val requisicao = hashMapOf(
            "nome" to nome,
            "email" to email,
            "senha" to senha,
            "papel" to papel,
            "rede" to rede,
            "status" to "pendente",
            "adminUid" to (auth.currentUser?.uid ?: "")
        )

        firestore.collection("admin_cadastros_pendentes").add(requisicao)
            .addOnSuccessListener {
                Toast.makeText(this, "Requisição enviada! O usuário será criado no servidor em breve.", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener { e ->
                btnCadastrar.isEnabled = true
                Toast.makeText(this, "Erro ao solicitar cadastro: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}
