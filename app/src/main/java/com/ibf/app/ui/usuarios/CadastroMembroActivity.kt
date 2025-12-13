package com.ibf.app.ui.usuarios

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import com.ibf.app.R

class CadastroMembroActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var editTextNome: TextInputEditText
    private lateinit var editTextCpf: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cadastro_membro)

        firestore = FirebaseFirestore.getInstance()
        editTextNome = findViewById(R.id.edit_text_nome_membro)
        editTextCpf = findViewById(R.id.edit_text_cpf_membro)

        findViewById<Button>(R.id.button_cadastrar_membro).setOnClickListener {
            cadastrarNovoMembro()
        }
        
        findViewById<android.view.View>(R.id.button_back).setOnClickListener { finish() }
    }

    private fun cadastrarNovoMembro() {
        val nome = editTextNome.text.toString().trim()
        val cpf = editTextCpf.text.toString().trim()

        if (nome.isEmpty()) {
            Toast.makeText(this, "O nome é obrigatório.", Toast.LENGTH_SHORT).show()
            return
        }

        if (cpf.isEmpty() || cpf.length != 11) {
            Toast.makeText(this, "Informe um CPF válido (11 dígitos, apenas números).", Toast.LENGTH_SHORT).show()
            return
        }

        // Verificar se CPF já existe
        findViewById<Button>(R.id.button_cadastrar_membro).isEnabled = false
        firestore.collection("usuarios")
            .whereEqualTo("cpf", cpf)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    Toast.makeText(this, "Este CPF já está cadastrado!", Toast.LENGTH_LONG).show()
                    findViewById<Button>(R.id.button_cadastrar_membro).isEnabled = true
                } else {
                    salvarMembroNoFirestore(nome, cpf)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao verificar CPF: ${e.message}", Toast.LENGTH_SHORT).show()
                findViewById<Button>(R.id.button_cadastrar_membro).isEnabled = true
            }
    }

    private fun salvarMembroNoFirestore(nome: String, cpf: String) {
        val novoMembro = hashMapOf(
            "nome" to nome,
            "cpf" to cpf,
            "funcoes" to hashMapOf<String, String>()
        )

        firestore.collection("usuarios")
            .add(novoMembro)
            .addOnSuccessListener {
                Toast.makeText(this, "Membro cadastrado com sucesso!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao salvar: ${e.message}", Toast.LENGTH_SHORT).show()
                findViewById<Button>(R.id.button_cadastrar_membro).isEnabled = true
            }
    }
}