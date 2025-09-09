package com.ibf.app.ui.usuarios

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.ibf.app.R
import com.ibf.app.adapters.UsuarioRedeAdapter
import com.ibf.app.data.models.UsuarioRede

class MembrosRedeActivity : AppCompatActivity(), UsuarioRedeAdapter.OnItemClickListener {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var adapter: UsuarioRedeAdapter
    private val listaUsuarios = mutableListOf<UsuarioRede>()

    private var redeSelecionada: String? = null
    private var papelUsuarioLogado: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_membros_rede)

        firestore = FirebaseFirestore.getInstance()
        redeSelecionada = intent.getStringExtra("REDE_SELECIONADA")
        papelUsuarioLogado = intent.getStringExtra("PAPEL_USUARIO_LOGADO")

        if (redeSelecionada == null) {
            Toast.makeText(this, "Erro: Rede não especificada.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        findViewById<ImageView>(R.id.button_back).setOnClickListener { finish() }
        findViewById<TextView>(R.id.text_page_title).text = "Membros da $redeSelecionada"

        setupRecyclerView()
        configurarBotoes()
    }

    override fun onResume() {
        super.onResume()
        carregarMembrosDaRede()
    }

    private fun setupRecyclerView() {
        val recyclerView: RecyclerView = findViewById(R.id.recycler_view_membros)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = UsuarioRedeAdapter(listaUsuarios, this)
        recyclerView.adapter = adapter
    }

    private fun configurarBotoes() {
        findViewById<FloatingActionButton>(R.id.fab_adicionar_membro).setOnClickListener {
            // CORREÇÃO: Aponta para a nova tela de cadastro de membro
            val intent = Intent(this, CadastroMembroActivity::class.java)
            startActivity(intent)
        }

        findViewById<ImageView>(R.id.button_ver_agenda).setOnClickListener {
            Toast.makeText(this, "Tela de agenda de discipulados em breve!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun carregarMembrosDaRede() {
        firestore.collection("usuarios")
            .whereEqualTo("funcoes.$redeSelecionada", "membro") // Exemplo, pode precisar de ajuste
            .get()
            .addOnSuccessListener { documents ->
                val usuariosDaRede = mutableListOf<UsuarioRede>()
                for (document in documents) {
                    val uid = document.id
                    val nome = document.getString("nome") ?: "Nome Desconhecido"
                    val dataNascimento = document.getString("dataNascimento")
                    val statusAprovacao = document.getString("statusAprovacao") ?: "pendente"
                    val funcoes = document.get("funcoes") as? HashMap<String, String>
                    val papelNaRede = funcoes?.get(redeSelecionada)
                    if (papelNaRede != null) {
                        usuariosDaRede.add(UsuarioRede(uid, nome, papelNaRede, statusAprovacao, dataNascimento))
                    }
                }
                adapter.atualizarLista(usuariosDaRede.sortedBy { it.nome })
            }
            .addOnFailureListener { e ->
                Log.e("MembrosRedeActivity", "Falha ao carregar membros: ${e.message}", e)
            }
    }

    override fun onItemClick(usuario: UsuarioRede) {
        // Ação para abrir a "ficha" do membro
        Toast.makeText(this, "Abrindo ficha de ${usuario.nome}", Toast.LENGTH_SHORT).show()
        // No futuro:
        // val intent = Intent(this, FichaMembroActivity::class.java)
        // intent.putExtra("MEMBRO_ID", usuario.uid)
        // startActivity(intent)
    }
}