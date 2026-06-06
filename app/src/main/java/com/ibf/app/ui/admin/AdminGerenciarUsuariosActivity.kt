package com.ibf.app.ui.admin

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.ibf.app.R
import com.ibf.app.adapters.GerenciarUsuarioAdapter
import com.ibf.app.data.models.Usuario

class AdminGerenciarUsuariosActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var recyclerUsuarios: RecyclerView
    private lateinit var editSearch: EditText
    private lateinit var adapter: GerenciarUsuarioAdapter
    
    private val todosUsuarios = mutableListOf<Usuario>()
    private val redesDisponiveis = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_gerenciar_usuarios)

        firestore = FirebaseFirestore.getInstance()

        findViewById<ImageView>(R.id.btn_back).setOnClickListener { finish() }
        swipeRefresh = findViewById(R.id.swipe_refresh)
        recyclerUsuarios = findViewById(R.id.recycler_usuarios)
        editSearch = findViewById(R.id.edit_search)

        recyclerUsuarios.layoutManager = LinearLayoutManager(this)
        adapter = GerenciarUsuarioAdapter(todosUsuarios) { usuario ->
            abrirDialogEdicao(usuario)
        }
        recyclerUsuarios.adapter = adapter

        swipeRefresh.setOnRefreshListener { carregarUsuarios() }

        editSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filtrarLista(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        carregarRedes()
        carregarUsuarios()
    }

    private fun carregarRedes() {
        firestore.collection("redes").get().addOnSuccessListener { docs ->
            redesDisponiveis.clear()
            redesDisponiveis.add("geral") // Opção especial
            for (doc in docs) {
                doc.getString("nome")?.let { redesDisponiveis.add(it) }
            }
        }
    }

    private fun carregarUsuarios() {
        swipeRefresh.isRefreshing = true
        firestore.collection("usuarios").get()
            .addOnSuccessListener { docs ->
                todosUsuarios.clear()
                for (doc in docs) {
                    val uid = doc.id
                    val nome = doc.getString("nome") ?: ""
                    val email = doc.getString("email") ?: ""
                    val funcoes = doc.get("funcoes") as? Map<String, String> ?: emptyMap()
                    
                    val usuario = Usuario(
                        uid = uid,
                        nome = nome,
                        email = email,
                        funcoes = HashMap(funcoes)
                    )
                    todosUsuarios.add(usuario)
                }
                todosUsuarios.sortBy { it.nome }
                adapter.atualizarLista(todosUsuarios)
                swipeRefresh.isRefreshing = false
                
                // Reaplica o filtro se houver algo digitado
                filtrarLista(editSearch.text.toString())
            }
            .addOnFailureListener {
                swipeRefresh.isRefreshing = false
                Toast.makeText(this, "Erro ao carregar usuários", Toast.LENGTH_SHORT).show()
            }
    }

    private fun filtrarLista(query: String) {
        val lowerQuery = query.lowercase().trim()
        val filtrados = todosUsuarios.filter {
            it.nome.lowercase().contains(lowerQuery) || it.email.lowercase().contains(lowerQuery)
        }
        adapter.atualizarLista(filtrados)
    }

    private fun abrirDialogEdicao(usuario: Usuario) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_editar_usuario, null)
        val textTitulo = dialogView.findViewById<TextView>(R.id.text_titulo_dialog)
        val containerFuncoes = dialogView.findViewById<LinearLayout>(R.id.container_funcoes_atuais)
        val spinnerRede = dialogView.findViewById<Spinner>(R.id.spinner_nova_rede)
        val spinnerPapel = dialogView.findViewById<Spinner>(R.id.spinner_novo_papel)
        val btnAdicionar = dialogView.findViewById<Button>(R.id.btn_adicionar_funcao)
        val btnFechar = dialogView.findViewById<Button>(R.id.btn_fechar_dialog)

        textTitulo.text = "Gerenciar: ${usuario.nome}"

        val adapterRede = ArrayAdapter(this, android.R.layout.simple_spinner_item, redesDisponiveis)
        adapterRede.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRede.adapter = adapterRede

        val papeis = listOf("lider", "pastor", "secretario", "admin")
        val adapterPapel = ArrayAdapter(this, android.R.layout.simple_spinner_item, papeis)
        adapterPapel.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPapel.adapter = adapterPapel

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // Preencher lista atual de funções
        fun renderizarFuncoes() {
            containerFuncoes.removeAllViews()
            if (usuario.funcoes.isEmpty()) {
                val tv = TextView(this)
                tv.text = "Nenhuma função cadastrada."
                tv.setPadding(8, 8, 8, 8)
                containerFuncoes.addView(tv)
            } else {
                for ((rede, papel) in usuario.funcoes) {
                    val itemView = LayoutInflater.from(this).inflate(R.layout.item_funcao_usuario, containerFuncoes, false)
                    val tvDescricao = itemView.findViewById<TextView>(R.id.text_descricao_funcao)
                    val btnRemover = itemView.findViewById<ImageView>(R.id.btn_remover_funcao)
                    
                    tvDescricao.text = "$papel ($rede)"
                    
                    btnRemover.setOnClickListener {
                        removerFuncao(usuario, rede) { dialog.dismiss() }
                    }
                    
                    containerFuncoes.addView(itemView)
                }
            }
        }
        
        renderizarFuncoes()

        btnAdicionar.setOnClickListener {
            val novaRede = spinnerRede.selectedItem?.toString() ?: ""
            val novoPapel = spinnerPapel.selectedItem?.toString() ?: ""
            if (novaRede.isNotEmpty() && novoPapel.isNotEmpty()) {
                adicionarFuncao(usuario, novaRede, novoPapel) { dialog.dismiss() }
            }
        }

        btnFechar.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun adicionarFuncao(usuario: Usuario, rede: String, papel: String, onSuccess: () -> Unit) {
        val update = hashMapOf<String, Any>(
            "funcoes.$rede" to papel
        )
        firestore.collection("usuarios").document(usuario.uid)
            .update(update)
            .addOnSuccessListener {
                Toast.makeText(this, "Função adicionada!", Toast.LENGTH_SHORT).show()
                onSuccess()
                carregarUsuarios()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao adicionar função.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun removerFuncao(usuario: Usuario, rede: String, onSuccess: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("Remover Função")
            .setMessage("Deseja remover a função da rede '$rede' de ${usuario.nome}?")
            .setPositiveButton("Remover") { _, _ ->
                val update = hashMapOf<String, Any>(
                    "funcoes.$rede" to FieldValue.delete()
                )
                firestore.collection("usuarios").document(usuario.uid)
                    .update(update)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Função removida!", Toast.LENGTH_SHORT).show()
                        onSuccess()
                        carregarUsuarios()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Erro ao remover função.", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
