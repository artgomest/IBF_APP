package com.ibf.app.ui.dashboard

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ibf.app.R
import com.ibf.app.data.models.Relatorio
import com.ibf.app.ui.agenda.AgendaActivity
import com.ibf.app.ui.configuracoes.ConfiguracoesRedeActivity
import com.ibf.app.ui.graficos.LiderGraficosActivity
import com.ibf.app.ui.main.MainActivity
import com.ibf.app.ui.perfil.PerfilActivity
import com.ibf.app.ui.relatorios.PastorRelatoriosActivity
import com.ibf.app.ui.shared.SelecionarPerfilSheet
import com.ibf.app.ui.usuarios.MembrosRedeActivity
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PastorDashboardActivity : AppCompatActivity(), SelecionarPerfilSheet.PerfilSelecionadoListener {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var greetingText: TextView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var containerRedes: LinearLayout

    // Views de métricas
    private lateinit var textTotalRedes: TextView
    private lateinit var textTotalMembros: TextView
    private lateinit var textPresencaMedia: TextView
    private lateinit var textTotalOfertas: TextView
    private lateinit var textTotalVisitantes: TextView

    // Dados auxiliares
    private data class RedeInfo(
        val id: String,
        val nome: String,
        val diaDaSemana: Int?,
        var totalMembros: Int = 0,
        var ultimoRelatorio: String? = null,
        var relatorioEmDia: Boolean = false
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pastor_dashboard)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        greetingText = findViewById(R.id.text_greeting)
        swipeRefresh = findViewById(R.id.swipe_refresh)
        containerRedes = findViewById(R.id.container_redes)
        textTotalRedes = findViewById(R.id.text_total_redes)
        textTotalMembros = findViewById(R.id.text_total_membros)
        textPresencaMedia = findViewById(R.id.text_presenca_media)
        textTotalOfertas = findViewById(R.id.text_total_ofertas)
        textTotalVisitantes = findViewById(R.id.text_total_visitantes)

        swipeRefresh.setOnRefreshListener { carregarDados() }

        setupNavigation()
        setupAcoes()
        carregarNomePastor()
        carregarDados()
    }

    private fun carregarNomePastor() {
        auth.currentUser?.uid?.let { uid ->
            firestore.collection("usuarios").document(uid).get()
                .addOnSuccessListener { doc ->
                    val nome = doc.getString("nome") ?: getString(R.string.pastor_padrao)
                    greetingText.text = getString(R.string.ola_pastor_nome, nome)
                }
                .addOnFailureListener {
                    greetingText.text = getString(R.string.ola_pastor_nome_placeholder)
                }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun carregarDados() {
        swipeRefresh.isRefreshing = true

        // Passo 1: Carregar todas as redes
        firestore.collection("redes").get()
            .addOnSuccessListener { redesDocs ->
                val redes = redesDocs.map { doc ->
                    RedeInfo(
                        id = doc.id,
                        nome = doc.getString("nome") ?: "Sem nome",
                        diaDaSemana = doc.getLong("diaDaSemana")?.toInt()
                    )
                }

                textTotalRedes.text = redes.size.toString()

                // Passo 2: Carregar todos os relatórios (últimas 8 semanas)
                firestore.collection("relatorios").get()
                    .addOnSuccessListener { relatoriosDocs ->
                        val todosRelatorios = relatoriosDocs.mapNotNull { doc ->
                            doc.toObject(Relatorio::class.java).apply { id = doc.id }
                        }

                        // Passo 3: Carregar todos os membros
                        firestore.collection("usuarios").get()
                            .addOnSuccessListener { usuariosDocs ->
                                processarDados(redes, todosRelatorios, usuariosDocs.size())
                                swipeRefresh.isRefreshing = false
                            }
                            .addOnFailureListener {
                                processarDados(redes, todosRelatorios, 0)
                                swipeRefresh.isRefreshing = false
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e("PastorDashboard", "Erro ao carregar relatórios: ${e.message}")
                        swipeRefresh.isRefreshing = false
                        Toast.makeText(this, "Erro ao carregar dados.", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Log.e("PastorDashboard", "Erro ao carregar redes: ${e.message}")
                swipeRefresh.isRefreshing = false
                Toast.makeText(this, "Erro ao carregar redes.", Toast.LENGTH_SHORT).show()
            }
    }

    @SuppressLint("SetTextI18n")
    private fun processarDados(
        redes: List<RedeInfo>,
        todosRelatorios: List<Relatorio>,
        totalUsuarios: Int
    ) {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val currencyFormat = DecimalFormat("R$ #,##0.00")

        val dataInicio = Calendar.getInstance().apply {
            set(2025, Calendar.JULY, 1, 0, 0, 0)
        }.time

        // Métricas agregadas
        var somaPresencas = 0
        var totalRelatoriosRecentes = 0
        var somaOfertas = 0.0
        var somaVisitantes = 0

        // Filtrar relatórios das últimas 8 semanas
        val oitoSemanasAtras = Calendar.getInstance().apply {
            add(Calendar.WEEK_OF_YEAR, -8)
        }.time

        for (relatorio in todosRelatorios) {
            try {
                val dataRelatorio = sdf.parse(relatorio.dataReuniao)
                if (dataRelatorio != null && dataRelatorio.after(oitoSemanasAtras)) {
                    somaPresencas += relatorio.totalPessoas
                    somaOfertas += relatorio.valorOferta
                    somaVisitantes += relatorio.totalVisitantes
                    totalRelatoriosRecentes++
                }
            } catch (_: Exception) { }
        }

        val presencaMedia = if (totalRelatoriosRecentes > 0) somaPresencas / totalRelatoriosRecentes else 0

        // Atualizar cards de métricas
        textTotalMembros.text = totalUsuarios.toString()
        textPresencaMedia.text = presencaMedia.toString()
        textTotalOfertas.text = currencyFormat.format(somaOfertas)
        textTotalVisitantes.text = somaVisitantes.toString()

        // Processar status por rede
        val redesComStatus = redes.map { rede ->
            val relatoriosDaRede = todosRelatorios.filter { it.idRede == rede.nome }

            // Verificar se o relatório da semana atual está em dia
            var emDia = false
            var ultimaData: String? = null

            if (rede.diaDaSemana != null) {
                val dataEsperadaCal = Calendar.getInstance()
                dataEsperadaCal.set(Calendar.DAY_OF_WEEK, rede.diaDaSemana)
                if (dataEsperadaCal.time.after(Date())) {
                    dataEsperadaCal.add(Calendar.WEEK_OF_YEAR, -1)
                }

                if (!dataEsperadaCal.time.before(dataInicio)) {
                    val dataEsperadaStr = sdf.format(dataEsperadaCal.time)
                    emDia = relatoriosDaRede.any { it.dataReuniao == dataEsperadaStr }
                }
            }

            // Último relatório enviado
            val ultimoEnviado = relatoriosDaRede.maxByOrNull {
                try { sdf.parse(it.dataReuniao)?.time ?: 0L } catch (_: Exception) { 0L }
            }
            ultimaData = ultimoEnviado?.dataReuniao

            // Contar membros da rede
            val totalMembrosRede = relatoriosDaRede.map { it.autorUid }.distinct().size

            rede.copy(
                relatorioEmDia = emDia,
                ultimoRelatorio = ultimaData,
                totalMembros = totalMembrosRede
            )
        }

        // Popular cards de rede
        montarCardsDeRede(redesComStatus)
    }

    @SuppressLint("InflateParams")
    private fun montarCardsDeRede(redes: List<RedeInfo>) {
        containerRedes.removeAllViews()

        for (rede in redes) {
            val cardView = LayoutInflater.from(this)
                .inflate(R.layout.item_rede_status_pastor, containerRedes, false)

            val textNome = cardView.findViewById<TextView>(R.id.text_nome_rede)
            val textStatus = cardView.findViewById<TextView>(R.id.text_status_rede)
            val textMembros = cardView.findViewById<TextView>(R.id.text_membros_count)
            val textUltimo = cardView.findViewById<TextView>(R.id.text_ultimo_relatorio)
            val statusDot = cardView.findViewById<View>(R.id.view_status_indicator)

            textNome.text = rede.nome

            if (rede.relatorioEmDia) {
                textStatus.text = "Relatório em dia"
                textStatus.setTextColor(ContextCompat.getColor(this, R.color.ibf_success))
                val bg = statusDot.background as? GradientDrawable
                bg?.setColor(ContextCompat.getColor(this, R.color.ibf_success))
            } else {
                textStatus.text = "Relatório pendente"
                textStatus.setTextColor(ContextCompat.getColor(this, R.color.ibf_error))
                val bg = statusDot.background as? GradientDrawable
                bg?.setColor(ContextCompat.getColor(this, R.color.ibf_error))
            }

            if (rede.ultimoRelatorio != null) {
                textUltimo.text = "Último: ${rede.ultimoRelatorio}"
            } else {
                textUltimo.text = "Sem relatórios"
            }

            textMembros.visibility = View.GONE // Simplificar por agora

            // Clique no card → navegar para detalhes da rede
            cardView.setOnClickListener {
                mostrarOpcoesRede(rede.nome)
            }

            containerRedes.addView(cardView)
        }
    }

    private fun mostrarOpcoesRede(nomeRede: String) {
        val opcoes = arrayOf("Relatórios", "Gráficos", "Membros", "Configurações")
        android.app.AlertDialog.Builder(this)
            .setTitle(nomeRede)
            .setItems(opcoes) { _, which ->
                when (which) {
                    0 -> {
                        val intent = Intent(this, PastorRelatoriosActivity::class.java)
                        intent.putExtra("REDE_SELECIONADA", nomeRede)
                        startActivity(intent)
                    }
                    1 -> {
                        val intent = Intent(this, LiderGraficosActivity::class.java)
                        intent.putExtra("REDE_SELECIONADA", nomeRede)
                        startActivity(intent)
                    }
                    2 -> {
                        val intent = Intent(this, MembrosRedeActivity::class.java)
                        intent.putExtra("REDE_SELECIONADA", nomeRede)
                        intent.putExtra("PAPEL_USUARIO_LOGADO", "pastor")
                        startActivity(intent)
                    }
                    3 -> {
                        val intent = Intent(this, ConfiguracoesRedeActivity::class.java)
                        intent.putExtra("REDE_SELECIONADA", nomeRede)
                        intent.putExtra("PAPEL_USUARIO_LOGADO", "pastor")
                        startActivity(intent)
                    }
                }
            }
            .show()
    }

    private fun setupAcoes() {
        findViewById<MaterialCardView>(R.id.card_agenda).setOnClickListener {
            // Agenda geral — usar primeira rede disponível ou buscar dinâmico
            firestore.collection("redes").get().addOnSuccessListener { docs ->
                val primeiraRede = docs.documents.firstOrNull()?.getString("nome")
                if (primeiraRede != null) {
                    val intent = Intent(this, AgendaActivity::class.java)
                    intent.putExtra("REDE_SELECIONADA", primeiraRede)
                    startActivity(intent)
                }
            }
        }

        findViewById<MaterialCardView>(R.id.card_mudar_perfil).setOnClickListener {
            abrirSeletorDePerfil()
        }
    }

    private fun setupNavigation() {
        val profileImage = findViewById<ImageView>(R.id.image_profile)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.navigation_home

        profileImage.setOnClickListener { abrirSeletorDePerfil() }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> true
                R.id.navigation_profile -> {
                    val intent = Intent(this, PerfilActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }

    private fun abrirSeletorDePerfil() {
        val user = auth.currentUser ?: return
        firestore.collection("usuarios").document(user.uid).get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val nomeUsuario = document.getString("nome") ?: getString(R.string.usuario_padrao)
                val funcoesRaw = document.get("funcoes") as? Map<*, *>
                val funcoes = HashMap<String, String>()
                funcoesRaw?.forEach { (k, v) -> if (k is String && v != null) funcoes[k] = v.toString() }
                if (funcoes.isNotEmpty()) {
                    if (funcoes.containsKey("geral") && funcoes["geral"] == "pastor") {
                        expandirRedesParaPastor(funcoes, nomeUsuario)
                    } else {
                        val bottomSheet = SelecionarPerfilSheet.newInstance(funcoes, nomeUsuario)
                        bottomSheet.show(supportFragmentManager, "SelecionarPerfilSheet")
                    }
                }
            }
        }
    }

    private fun expandirRedesParaPastor(funcoesOriginais: HashMap<String, String>, nomeUsuario: String) {
        firestore.collection("redes").get()
            .addOnSuccessListener { redesDocs ->
                val funcoesExpandidas = HashMap<String, String>()

                funcoesOriginais.forEach { (rede, papel) ->
                    if (rede != "geral") {
                        funcoesExpandidas[rede] = papel
                    }
                }

                for (doc in redesDocs) {
                    val nomeRede = doc.getString("nome")
                    if (nomeRede != null && !funcoesExpandidas.containsKey(nomeRede)) {
                        funcoesExpandidas[nomeRede] = "pastor"
                    }
                }

                if (funcoesExpandidas.isNotEmpty()) {
                    val bottomSheet = SelecionarPerfilSheet.newInstance(funcoesExpandidas, nomeUsuario)
                    bottomSheet.show(supportFragmentManager, "SelecionarPerfilSheet")
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao carregar redes.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fazerLogout() {
        auth.signOut()
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        sharedPref.edit { clear() }
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    @SuppressLint("StringFormatMatches")
    override fun onPerfilSelecionado(rede: String, papel: String) {
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        sharedPref.edit {
            putString("REDE_SELECIONADA", rede)
            putString("PAPEL_SELECIONADO", papel)
        }

        val intent = when (papel) {
            "pastor" -> Intent(this, PastorDashboardActivity::class.java)
            "lider" -> Intent(this, LiderDashboardActivity::class.java)
            "secretario" -> Intent(this, SecretarioDashboardActivity::class.java)
            else -> null
        }

        if (intent != null) {
            intent.putExtra("REDE_SELECIONADA", rede)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        } else {
            Toast.makeText(this, getString(R.string.papel_desconhecido, papel), Toast.LENGTH_LONG).show()
            fazerLogout()
        }
    }

    override fun onLogoutSelecionado() {
        fazerLogout()
    }
}
