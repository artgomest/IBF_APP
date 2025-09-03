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

    // Declare outras EditTexts para os novos campos aqui

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cadastro_membro)

        firestore = FirebaseFirestore.getInstance()
        editTextNome = findViewById(R.id.edit_text_nome_membro)
        // ... inicialize suas outras EditTexts aqui

        findViewById<Button>(R.id.button_cadastrar_membro).setOnClickListener {
            cadastrarNovoMembro()
        }
    }

    private fun cadastrarNovoMembro() {
        val nome = editTextNome.text.toString().trim()

        if (nome.isEmpty()) {
            Toast.makeText(this, "O nome é obrigatório.", Toast.LENGTH_SHORT).show()
            return
        }

        // Crie um mapa com todos os dados do membro
        val novoMembro = hashMapOf(
            "nome" to nome,
            // Adicione os outros campos aqui
            "funcoes" to hashMapOf<String, String>() // Inicia sem papéis definidos
        )

        // Adiciona o novo membro à coleção 'usuarios'
        firestore.collection("usuarios")
            .add(novoMembro)
            .addOnSuccessListener {
                Toast.makeText(this, "Membro '$nome' cadastrado com sucesso!", Toast.LENGTH_SHORT).show()
                finish() // Volta para a tela anterior
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao cadastrar membro: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}