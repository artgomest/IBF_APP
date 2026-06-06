package com.ibf.app.ui.dashboard

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ibf.app.R
import com.ibf.app.adapters.RelatorioAdapter
import com.ibf.app.data.models.Relatorio
import com.ibf.app.data.models.StatusRelatorio
import com.ibf.app.ui.main.MainActivity
import com.ibf.app.ui.perfil.PerfilActivity
import com.ibf.app.ui.relatorios.FormularioRedeActivity
import com.ibf.app.ui.shared.SelecionarPerfilSheet
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
    private lateinit var textRedeAtual: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_secretario_dashboard)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        greetingText = findViewById(R.id.text_greeting)
        textRedeAtual = findViewById(R.id.text_rede_dashboard)

        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val redeInPrefs = sharedPref.getString("REDE_SELECIONADA", null)

        redeSelecionada = intent.getStringExtra("REDE_SELECIONADA") ?: redeInPrefs

        if (redeSelecionada == null) {
            Toast.makeText(this, getString(R.string.erro_rede_nao_selecionada_logout), Toast.LENGTH_LONG).show()
            fazerLogout()
            return
        }

        textRedeAtual.text = getString(R.string.rede_dashboard_label, redeSelecionada)

        setupRecyclerView()
        setupNavigation()

        buscarDadosUsuario()
        carregarStatusDosRelatorios()
        com.ibf.app.utils.UpdateChecker.verificar(this)
    }

    private fun buscarDadosUsuario() {
        val user = auth.currentUser ?: return
        firestore.collection("usuarios").document(user.uid).get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val nomeCompleto = document.getString("nome") ?: getString(R.string.usuario_padrao)
                val primeiroNome = nomeCompleto.split(" ").firstOrNull() ?: nomeCompleto
                greetingText.text = primeiroNome

                val funcoes = extrairFuncoes(document.get("funcoes"))
                // Fallback legado
                if (funcoes.isEmpty()) {
                    document.getString("papel")?.let { funcoes["geral"] = it }
                }

                // Segurança: Se a rede do cache não existir mais no perfil e não tiver acesso geral
                if (funcoes[redeSelecionada] == null && funcoes["geral"] == null) {
                    Toast.makeText(this@SecretarioDashboardActivity, "Sua permissão para esta rede foi revogada ou alterada.", Toast.LENGTH_LONG).show()
                    val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    sharedPref.edit().clear().apply()
                    
                    val intent = Intent(this@SecretarioDashboardActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val currentRedeInPrefs = sharedPref.getString("REDE_SELECIONADA", null)

        if (redeSelecionada == null && currentRedeInPrefs != null) {
            redeSelecionada = currentRedeInPrefs
            textRedeAtual.text = getString(R.string.rede_dashboard_label, redeSelecionada)
            carregarStatusDosRelatorios()
        } else {
            carregarStatusDosRelatorios()
        }
    }

    override fun onItemClick(status: StatusRelatorio) {
        val intent = Intent(this, FormularioRedeActivity::class.java)

        intent.putExtra("REDE_SELECIONADA", redeSelecionada)

        when (status) {
            is StatusRelatorio.Enviado -> {
                Toast.makeText(this, getString(R.string.editando_relatorio, status.relatorio.dataReuniao), Toast.LENGTH_SHORT).show()
                intent.putExtra("RELATORIO_ID", status.relatorio.id)
                intent.putExtra("DATA_PENDENTE", status.relatorio.dataReuniao)
            }
            is StatusRelatorio.Faltante -> {
                Toast.makeText(this, getString(R.string.preenchendo_relatorio_pendente, status.dataEsperada), Toast.LENGTH_SHORT).show()
                intent.putExtra("DATA_PENDENTE", status.dataEsperada)
                intent.putExtra("REDE_SELECIONADA", status.nomeRede)
            }
        }
        startActivity(intent)
    }

    override fun onPerfilSelecionado(rede: String, papel: String) {
        if (papel != "secretario") {
            navegarParaTelaCorreta(rede, papel)
        } else {
            if (rede != redeSelecionada) {
                val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                sharedPref.edit {
                    putString("REDE_SELECIONADA", rede)
                }
                this.redeSelecionada = rede
                textRedeAtual.text = getString(R.string.rede_dashboard_label, redeSelecionada)
                carregarStatusDosRelatorios()
                Toast.makeText(this, getString(R.string.exibindo_relatorios_da_rede, rede), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, getString(R.string.ja_exibindo_relatorios_da_rede, rede), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onLogoutSelecionado() {
        fazerLogout()
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerViewMeusRelatorios)
        recyclerView.layoutManager = LinearLayoutManager(this)
        relatorioAdapter = RelatorioAdapter(listaDeStatus, this)
        recyclerView.adapter = relatorioAdapter

        val swipeRefresh = findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipe_refresh)
        swipeRefresh.setOnRefreshListener {
            carregarStatusDosRelatorios()
        }
    }

    private fun setupNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.navigation_home

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> true
                R.id.navigation_profile -> {
                    val intent = Intent(this, PerfilActivity::class.java)
                    // --- ADIÇÃO IMPORTANTE AQUI ---
                    // Avisa a tela de perfil qual é a rede ativa
                    intent.putExtra("REDE_SELECIONADA", redeSelecionada)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun carregarStatusDosRelatorios() {
        val usuarioAtual = auth.currentUser ?: return
        val redeAtiva = redeSelecionada ?: return
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        // --- CORREÇÃO APLICADA AQUI ---
        // 1. Definimos a nossa data de início
        val dataInicio = Calendar.getInstance().apply {
            set(2026, Calendar.JUNE, 1, 0, 0, 0)
        }.time

        firestore.collection("redes").whereEqualTo("nome", redeAtiva).get().addOnSuccessListener { redesDocs ->
            if (redesDocs.isEmpty) {
                findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipe_refresh)?.isRefreshing = false
                return@addOnSuccessListener
            }
            val diaDaSemana = redesDocs.documents.first().getLong("diaDaSemana")?.toInt() ?: return@addOnSuccessListener

            firestore.collection("relatorios")
                .whereEqualTo("idRede", redeAtiva)
                .get().addOnSuccessListener { relatoriosDocs ->
                    val relatoriosEnviados = relatoriosDocs.mapNotNull { doc -> doc.toObject(Relatorio::class.java).apply { id = doc.id } }
                    val statusFinal = mutableListOf<StatusRelatorio>()
                    val calLoop = Calendar.getInstance()
                    calLoop.set(Calendar.DAY_OF_WEEK, diaDaSemana)
                    
                    if (calLoop.time.after(Date())) {
                        calLoop.add(Calendar.WEEK_OF_YEAR, -1)
                    }

                    while (!calLoop.time.before(dataInicio)) {
                        val dataEsperadaStr = sdf.format(calLoop.time)
                        val relatorioEncontrado = relatoriosEnviados.find { it.dataReuniao == dataEsperadaStr }

                        if (relatorioEncontrado != null) {
                            statusFinal.add(StatusRelatorio.Enviado(relatorioEncontrado))
                        } else {
                            statusFinal.add(StatusRelatorio.Faltante(dataEsperadaStr, redeAtiva))
                        }
                        
                        calLoop.add(Calendar.WEEK_OF_YEAR, -1)
                    }

                    listaDeStatus.clear()
                    val sortedList = statusFinal.sortedByDescending {
                        try {
                            when(it) {
                                is StatusRelatorio.Enviado -> sdf.parse(it.relatorio.dataReuniao)
                                is StatusRelatorio.Faltante -> sdf.parse(it.dataEsperada)
                            }
                        } catch (e: Exception) {
                            Date(0)
                        }
                    }
                    listaDeStatus.addAll(sortedList)
                    relatorioAdapter.notifyDataSetChanged()
                    findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipe_refresh)?.isRefreshing = false
                }
        }
    }

    private fun abrirSeletorDePerfil() {
        val user = auth.currentUser ?: return
        firestore.collection("usuarios").document(user.uid).get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val nomeUsuario = document.getString("nome") ?: getString(R.string.usuario_padrao)
                val funcoes = extrairFuncoes(document.get("funcoes"))
                
                // Fallback legado
                if (funcoes.isEmpty()) {
                    document.getString("papel")?.let { funcoes["geral"] = it }
                }

                if (funcoes.isNotEmpty()) {
                    val bottomSheet = SelecionarPerfilSheet.newInstance(funcoes, nomeUsuario)
                    bottomSheet.show(supportFragmentManager, "SelecionarPerfilSheet")
                } else {
                    Toast.makeText(this, "Nenhum papel definido para este usuário.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun extrairFuncoes(raw: Any?): HashMap<String, String> {
        val result = HashMap<String, String>()
        if (raw == null) return result
        
        when (raw) {
            is Map<*, *> -> {
                for ((key, value) in raw) {
                    if (key is String && value != null) {
                        result[key] = value.toString()
                    }
                }
            }
            is String -> {
                if (raw.isNotEmpty()) {
                    result["geral"] = raw
                }
            }
        }
        return result
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
            "admin" -> Intent(this, com.ibf.app.ui.admin.AdminDashboardActivity::class.java)
            "pastor" -> Intent(this, PastorDashboardActivity::class.java)
            "lider" -> Intent(this, LiderDashboardActivity::class.java)
            "secretario" -> Intent(this, SecretarioDashboardActivity::class.java)
            else -> null
        }

        if (intent != null) {
            if (this::class.java.simpleName == intent.component?.shortClassName?.removePrefix(".")) {
                if (rede != redeSelecionada) {
                    val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    sharedPref.edit {
                        putString("REDE_SELECIONADA", rede)
                    }
                    this.redeSelecionada = rede
                    textRedeAtual.text = getString(R.string.rede_dashboard_label, redeSelecionada)
                    carregarStatusDosRelatorios()
                    Toast.makeText(this, getString(R.string.exibindo_relatorios_da_rede, rede), Toast.LENGTH_SHORT).show()
                }
                return
            }

            intent.putExtra("REDE_SELECIONADA", rede)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}