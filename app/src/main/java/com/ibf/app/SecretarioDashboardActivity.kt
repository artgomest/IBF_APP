package com.ibf.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SecretarioDashboardActivity : AppCompatActivity(), RelatorioAdapter.OnItemClickListener, SelecionarPerfilSheet.PerfilSelecionadoListener {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var relatorioAdapter: RelatorioAdapter
    private val listaDeStatus = mutableListOf<StatusRelatorio>()

    private var redeSelecionada: String? = null
    private lateinit var greetingText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_secretario_dashboard)

        // Pega a rede que foi selecionada ao fazer login ou ao trocar de perfil
        redeSelecionada = intent.getStringExtra("REDE_SELECIONADA")
        if (redeSelecionada == null) {
            Toast.makeText(this, "Erro: Nenhuma rede selecionada. Fazendo logout.", Toast.LENGTH_LONG).show()
            fazerLogout()
            return
        }

        // Inicializa o Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Inicializa os componentes da UI
        greetingText = findViewById(R.id.text_greeting)
        greetingText.text = "$redeSelecionada"

        // Configura as funcionalidades da tela
        setupRecyclerView()
        setupNavigation()
    }

    override fun onResume() {
        super.onResume()
        // onResume é chamado toda vez que a tela volta a ficar ativa.
        // É o lugar perfeito para garantir que os dados estejam sempre atualizados.
        carregarStatusDosRelatorios()
    }

    // Chamado pelo Adapter quando um item da lista de relatórios é clicado
    override fun onItemClick(status: StatusRelatorio) {
        val intent = Intent(this, FormularioRedeActivity::class.java)

        // Sempre passamos a rede ativa para o formulário, para o caso de ser um novo relatório
        intent.putExtra("REDE_SELECIONADA", redeSelecionada)

        when (status) {
            is StatusRelatorio.Enviado -> {
                // Se está editando, também passa o ID do relatório existente
                intent.putExtra("RELATORIO_ID", status.relatorio.id)
            }
            is StatusRelatorio.Faltante -> {
                // Se está preenchendo um pendente, passa a data e a rede da pendência
                Toast.makeText(this, "Preenchendo relatório pendente", Toast.LENGTH_SHORT).show()
                intent.putExtra("DATA_PENDENTE", status.dataEsperada)
                intent.putExtra("REDE_SELECIONADA", status.nomeRede) // Passa a rede específica do item pendente
            }
        }
        startActivity(intent)
    }

    // Chamado pela Bandeja de Perfil quando um perfil é escolhido
    override fun onPerfilSelecionado(rede: String, papel: String) {
        if (papel != "secretario") {
            navegarParaTelaCorreta(rede, papel)
        } else {
            // Se apenas trocou para outra rede de secretário, atualiza a tela atual
            Toast.makeText(this, "Exibindo relatórios da $rede", Toast.LENGTH_SHORT).show()
            this.redeSelecionada = rede
            greetingText.text = "Relatórios da $redeSelecionada"
            carregarStatusDosRelatorios() // Recarrega a lista para a nova rede
        }
    }

    // Chamado pela Bandeja de Perfil quando o botão de Sair é clicado
    override fun onLogoutSelecionado() {
        fazerLogout()
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerViewMeusRelatorios)
        recyclerView.layoutManager = LinearLayoutManager(this)
        relatorioAdapter = RelatorioAdapter(listaDeStatus, this)
        recyclerView.adapter = relatorioAdapter
    }

    private fun setupNavigation() {
        val profileImage = findViewById<ImageView>(R.id.image_profile)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.navigation_home

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> true
                R.id.navigation_reports -> {
                    val bottomSheet = SelecionarRelatorioSheet()
                    bottomSheet.show(supportFragmentManager, "SelecionarRelatorioSheet")
                    true
                }
                R.id.navigation_profile -> {
                    abrirSeletorDePerfil()
                    true
                }
                else -> false
            }
        }
        profileImage.setOnClickListener { abrirSeletorDePerfil() }
    }

    private fun carregarStatusDosRelatorios() {
        val usuarioAtual = auth.currentUser ?: return
        val redeAtiva = redeSelecionada ?: return
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        firestore.collection("redes").whereEqualTo("nome", redeAtiva).get().addOnSuccessListener { redesDocs ->
            if (redesDocs.isEmpty) {
                Log.e("FirestoreError", "Nenhuma rede encontrada com o nome: $redeAtiva")
                return@addOnSuccessListener
            }
            val diaDaSemana = redesDocs.documents.first().getLong("diaDaSemana")?.toInt() ?: return@addOnSuccessListener

            firestore.collection("relatorios")
                .whereEqualTo("autorUid", usuarioAtual.uid)
                .whereEqualTo("idRede", redeAtiva)
                .get().addOnSuccessListener { relatoriosDocs ->
                    val relatoriosEnviados = relatoriosDocs.mapNotNull { doc -> doc.toObject(Relatorio::class.java).apply { id = doc.id } }
                    val statusFinal = mutableListOf<StatusRelatorio>()
                    val semanasParaVerificar = 8

                    for (i in 0 until semanasParaVerificar) {
                        val dataEsperadaCal = Calendar.getInstance()
                        dataEsperadaCal.add(Calendar.WEEK_OF_YEAR, -i)
                        dataEsperadaCal.set(Calendar.DAY_OF_WEEK, diaDaSemana)

                        if (dataEsperadaCal.time.after(Date())) continue

                        val dataEsperadaStr = sdf.format(dataEsperadaCal.time)
                        val relatorioEncontrado = relatoriosEnviados.find { it.dataReuniao == dataEsperadaStr }

                        if (relatorioEncontrado != null) {
                            statusFinal.add(StatusRelatorio.Enviado(relatorioEncontrado))
                        } else {
                            statusFinal.add(StatusRelatorio.Faltante(dataEsperadaStr, redeAtiva))
                        }
                    }

                    listaDeStatus.clear()
                    statusFinal.sortByDescending {
                        when(it) {
                            is StatusRelatorio.Enviado -> sdf.parse(it.relatorio.dataReuniao)
                            is StatusRelatorio.Faltante -> sdf.parse(it.dataEsperada)
                        }
                    }
                    listaDeStatus.addAll(statusFinal)
                    relatorioAdapter.notifyDataSetChanged()
                }
                .addOnFailureListener { e -> Log.e("FirestoreError", "Falha ao buscar relatorios", e) }
        }.addOnFailureListener { e -> Log.e("FirestoreError", "Falha ao buscar redes", e) }
    }

    private fun abrirSeletorDePerfil() {
        val user = auth.currentUser ?: return
        firestore.collection("usuarios").document(user.uid).get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val nomeUsuario = document.getString("nome") ?: "Usuário"
                @Suppress("UNCHECKED_CAST")
                val funcoes = document.get("funcoes") as? HashMap<String, String>
                if (!funcoes.isNullOrEmpty()) {
                    val bottomSheet = SelecionarPerfilSheet.newInstance(funcoes, nomeUsuario)
                    bottomSheet.show(supportFragmentManager, "SelecionarPerfilSheet")
                }
            }
        }
    }

    private fun fazerLogout() {
        auth.signOut()
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun navegarParaTelaCorreta(rede: String, papel: String?) {
        val intent = when (papel) {
            "pastor" -> Intent(this, PastorDashboardActivity::class.java)
            "lider" -> Intent(this, LiderDashboardActivity::class.java)
            "secretario" -> Intent(this, SecretarioDashboardActivity::class.java)
            else -> null
        }

        if (intent != null) {
            if (this::class.java.simpleName == intent.component?.shortClassName?.removePrefix(".")) {
                return
            }
            intent.putExtra("REDE_SELECIONADA", rede)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}