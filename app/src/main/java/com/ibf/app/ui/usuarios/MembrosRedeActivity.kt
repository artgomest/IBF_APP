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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.ibf.app.R
import com.ibf.app.adapters.UsuarioRedeAdapter
import com.ibf.app.data.models.UsuarioRede

class MembrosRedeActivity : AppCompatActivity(), UsuarioRedeAdapter.OnItemClickListener {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var adapter: UsuarioRedeAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
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
        val pageTitle = if (redeSelecionada == "TODAS") "Todos os Membros" else "Membros da $redeSelecionada"
        findViewById<TextView>(R.id.text_page_title).text = pageTitle
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout)

        setupRecyclerView()
        configurarBotoes()
        setupSwipeToRefresh()
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
            Log.d("MembrosRedeActivity", "FAB Adicionar Membro clicado")
            mostrarDialogoCadastroCpf()
        }

        findViewById<ImageView>(R.id.button_ver_agenda).setOnClickListener {
            val intent = Intent(this, com.ibf.app.ui.agenda.AgendaActivity::class.java)
            intent.putExtra("REDE_SELECIONADA", redeSelecionada)
            startActivity(intent)
        }
    }

    private fun setupSwipeToRefresh() {
        swipeRefreshLayout.setOnRefreshListener {
            Log.d("MembrosRedeActivity", "Refresh acionado.")
            carregarMembrosDaRede()
        }
    }

    private fun carregarMembrosDaRede() {
        if (redeSelecionada == null) {
            swipeRefreshLayout.isRefreshing = false
            return
        }

        val query = if (redeSelecionada == "TODAS") {
            firestore.collection("membros")
        } else {
            firestore.collection("membros")
                .whereGreaterThan(com.google.firebase.firestore.FieldPath.of("funcoes", redeSelecionada!!), "")
        }

        query.get()
            .addOnSuccessListener { documents ->
                val usuariosDaRede = mutableListOf<UsuarioRede>()
                for (document in documents) {
                    val uid = document.id
                    val nome = document.getString("nome") ?: "Nome Desconhecido"

                    val funcoesRaw = document.get("funcoes") as? Map<*, *>
                    
                    val papelNaRede = if (redeSelecionada == "TODAS") {
                        val papelGeral = document.getString("papel") ?: "Membro"
                        if (funcoesRaw != null && funcoesRaw.isNotEmpty()) {
                            val primeiraRede = funcoesRaw.keys.firstOrNull { it != "geral" }?.toString()
                            if (primeiraRede != null) {
                                "${funcoesRaw[primeiraRede]} ($primeiraRede)"
                            } else {
                                funcoesRaw["geral"]?.toString() ?: papelGeral
                            }
                        } else {
                            papelGeral
                        }
                    } else {
                        funcoesRaw?.get(redeSelecionada)?.toString()
                    }

                    if (papelNaRede != null) {
                        val cadastroCompleto = document.getBoolean("cadastroCompleto") ?: false
                        usuariosDaRede.add(UsuarioRede(uid, nome, papelNaRede, cadastroCompleto = cadastroCompleto))
                    }
                }
                adapter.atualizarLista(usuariosDaRede.sortedBy { it.nome })
                swipeRefreshLayout.isRefreshing = false // Para a animação de refresh
            }
            .addOnFailureListener { e ->
                Log.e("MembrosRedeActivity", "Falha ao carregar membros: ${e.message}", e)
                Toast.makeText(this, "Erro ao carregar membros.", Toast.LENGTH_SHORT).show()
                swipeRefreshLayout.isRefreshing = false // Para a animação de refresh também em caso de erro
            }
    }

    override fun onItemClick(usuario: UsuarioRede) {
        // Ação para abrir a "ficha" do membro, passando o seu ID
        val intent = Intent(this, FichaMembroActivity::class.java)
        intent.putExtra("MEMBRO_ID", usuario.uid)
        intent.putExtra("REDE_SELECIONADA", redeSelecionada)
        intent.putExtra("PAPEL_USUARIO_LOGADO", papelUsuarioLogado)
        startActivity(intent)
    }

    private fun mostrarDialogoCadastroCpf() {
        try {
            Log.d("MembrosRedeActivity", "Iniciando mostrarDialogoCadastroCpf")
            val builder = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            builder.setTitle("Cadastrar Novo Membro")
            builder.setMessage("Digite o CPF do membro para prosseguir:")
            
            val input = com.google.android.material.textfield.TextInputEditText(this)
            input.hint = "000.000.000-00"
            input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
            
            try {
                input.addTextChangedListener(com.ibf.app.utils.MaskUtils.applyMask(input, com.ibf.app.utils.MaskUtils.MASK_CPF))
            } catch (e: Exception) {
                Log.e("MembrosRedeActivity", "Erro ao aplicar máscara: ${e.message}")
            }
            
            val padding = (24 * resources.displayMetrics.density).toInt()
            val container = android.widget.FrameLayout(this)
            val lp = android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(padding, 0, padding, 0)
            input.layoutParams = lp
            container.addView(input)
            
            builder.setView(container)

            builder.setPositiveButton("Avançar") { _, _ ->
                val cpfComMascara = input.text.toString().trim()
                val cpf = cpfComMascara.replace(".", "").replace("-", "")
                if (cpf.length == 11) {
                    verificarCpfEGerarLink(cpf)
                } else {
                    Toast.makeText(this, "CPF inválido", Toast.LENGTH_SHORT).show()
                }
            }
            builder.setNegativeButton("Cancelar", null)
            
            Log.d("MembrosRedeActivity", "Exibindo o diálogo")
            builder.show()
        } catch (e: Exception) {
            Log.e("MembrosRedeActivity", "Erro ao criar diálogo: ${e.message}", e)
            Toast.makeText(this, "Erro ao abrir cadastro: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun verificarCpfEGerarLink(cpf: String) {
        firestore.collection("membros").whereEqualTo("cpf", cpf).get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    val novoMembro = hashMapOf(
                        "cpf" to cpf,
                        "nome" to "Novo Membro", // Provisório, será atualizado na web
                        "cadastroCompleto" to false,
                        "funcoes" to if (redeSelecionada != null) hashMapOf(redeSelecionada!! to "Membro") else hashMapOf<String, String>()
                    )
                    firestore.collection("membros").add(novoMembro).addOnSuccessListener {
                        mostrarDialogoWhatsApp()
                        carregarMembrosDaRede()
                    }
                } else {
                    // Já cadastrado
                    mostrarDialogoWhatsApp()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao verificar CPF", Toast.LENGTH_SHORT).show()
            }
    }

    private fun mostrarDialogoWhatsApp() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Enviar Link")
        builder.setMessage("Membro preparado! Envie o link do portal web pelo WhatsApp para que ele possa completar o cadastro.")

        builder.setPositiveButton("Enviar pelo WhatsApp") { _, _ ->
            val link = "https://ibfapp-c4078.web.app/cadastro"
            val mensagem = "Olá! Por favor, conclua seu cadastro acessando o link: $link \n\nUtilize seu CPF para acessar."
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "text/plain"
            intent.putExtra(Intent.EXTRA_TEXT, mensagem)
            intent.setPackage("com.whatsapp")
            
            try {
                startActivity(intent)
            } catch (e: Exception) {
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "text/plain"
                shareIntent.putExtra(Intent.EXTRA_TEXT, mensagem)
                startActivity(Intent.createChooser(shareIntent, "Compartilhar link via"))
            }
        }
        builder.setNegativeButton("Fechar", null)
        builder.show()
    }
}

