package com.ibf.app.ui.admin

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.ibf.app.R
import com.ibf.app.adapters.AprovacaoUsuarioAdapter
import com.ibf.app.data.models.SolicitacaoCadastro

class AdminAprovarUsuariosActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var textEmpty: TextView
    private lateinit var adapter: AprovacaoUsuarioAdapter
    private val solicitacoes = mutableListOf<SolicitacaoCadastro>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_aprovar_usuarios)

        firestore = FirebaseFirestore.getInstance()
        
        findViewById<ImageView>(R.id.btn_back).setOnClickListener { finish() }
        textEmpty = findViewById(R.id.text_empty)
        swipeRefresh = findViewById(R.id.swipe_refresh)
        recyclerView = findViewById(R.id.recycler_view)
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = AprovacaoUsuarioAdapter(solicitacoes, this::aprovarUsuario, this::rejeitarUsuario)
        recyclerView.adapter = adapter

        swipeRefresh.setOnRefreshListener { carregarSolicitacoes() }
        
        carregarSolicitacoes()
    }

    private fun carregarSolicitacoes() {
        swipeRefresh.isRefreshing = true
        firestore.collection("solicitacoesCadastro")
            .whereEqualTo("status", "pendente")
            .get()
            .addOnSuccessListener { docs ->
                solicitacoes.clear()
                for (doc in docs) {
                    val sol = doc.toObject(SolicitacaoCadastro::class.java).apply { id = doc.id }
                    // Vamos exibir todas as solicitações pendentes, tanto as de lideres quanto as do site
                    solicitacoes.add(sol)
                }
                
                adapter.notifyDataSetChanged()
                textEmpty.visibility = if (solicitacoes.isEmpty()) View.VISIBLE else View.GONE
                swipeRefresh.isRefreshing = false
            }
            .addOnFailureListener { e ->
                swipeRefresh.isRefreshing = false
                Toast.makeText(this, "Erro ao carregar: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("AdminAprovar", "Erro", e)
            }
    }

    private fun aprovarUsuario(sol: SolicitacaoCadastro) {
        if (sol.uid == "existente") {
            firestore.collection("usuarios").whereEqualTo("email", sol.email).get()
                .addOnSuccessListener { docs ->
                    if (!docs.isEmpty) {
                        val doc = docs.documents[0]
                        val funcoesAtuaisMap = doc.get("funcoes") as? Map<*, *> ?: emptyMap<String, String>()
                        val funcoesAtuaisStr = funcoesAtuaisMap.entries.joinToString(", ") { "${it.value} (${it.key})" }
                        
                        val novaId = doc.id // O verdadeiro UID do usuário
                        sol.uid = novaId // Substitui para o ID real para processarAprovacao
                        
                        val dialog = AlertDialog.Builder(this)
                            .setTitle("Aprovar Nova Função")
                            .setMessage("Este usuário já existe na base!\n\nFunções atuais: $funcoesAtuaisStr\n\nDeseja conceder também o acesso de ${sol.papelSolicitado} (${sol.redeId})?")
                            .setPositiveButton("Aprovar") { _, _ -> processarAprovacao(sol) }
                            .setNegativeButton("Cancelar", null)
                            .create()
                        dialog.show()
                    } else {
                        Toast.makeText(this, "Erro: Usuário não encontrado pelo e-mail.", Toast.LENGTH_LONG).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Erro ao buscar usuário.", Toast.LENGTH_SHORT).show()
                }
        } else {
            val dialog = AlertDialog.Builder(this)
                .setTitle("Aprovar Usuário")
                .setMessage("Deseja realmente aprovar ${sol.nome} como ${sol.papelSolicitado}?")
                .setPositiveButton("Aprovar") { _, _ ->
                    processarAprovacao(sol)
                }
                .setNegativeButton("Cancelar", null)
                .create()
            dialog.show()
        }
    }

    private fun processarAprovacao(sol: SolicitacaoCadastro) {
        swipeRefresh.isRefreshing = true
        
        val chaveRede = if (sol.redeId.isNotEmpty()) sol.redeId else "geral"
        
        val updateData = hashMapOf<String, Any>(
            "nome" to sol.nome,
            "email" to sol.email
        )
        
        // Vamos adicionar ou mesclar a função
        val funcoesMap = hashMapOf<String, String>(
            chaveRede to sol.papelSolicitado
        )
        updateData["funcoes"] = funcoesMap

        // Atualizar/criar em usuários
        firestore.collection("usuarios").document(sol.uid)
            .set(updateData, SetOptions.merge())
            .addOnSuccessListener {
                // Atualizar status na solicitação
                firestore.collection("solicitacoesCadastro").document(sol.id)
                    .update("status", "aprovado")
                    .addOnSuccessListener {
                        Toast.makeText(this, "Usuário aprovado com sucesso!", Toast.LENGTH_SHORT).show()
                        carregarSolicitacoes()
                    }
            }
            .addOnFailureListener {
                swipeRefresh.isRefreshing = false
                Toast.makeText(this, "Falha ao aprovar usuário.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun rejeitarUsuario(sol: SolicitacaoCadastro) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Rejeitar Solicitação")
            .setMessage("Deseja rejeitar o cadastro de ${sol.nome}? Isso não apagará o usuário do Auth, apenas marcará a solicitação como rejeitada.")
            .setPositiveButton("Rejeitar") { _, _ ->
                firestore.collection("solicitacoesCadastro").document(sol.id)
                    .update("status", "rejeitado")
                    .addOnSuccessListener {
                        Toast.makeText(this, "Solicitação rejeitada.", Toast.LENGTH_SHORT).show()
                        carregarSolicitacoes()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .create()
        dialog.show()
    }
}
