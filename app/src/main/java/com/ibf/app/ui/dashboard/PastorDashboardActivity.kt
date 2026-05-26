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
        var relatorioEmDia: Boolean = false,
        var presencaMedia: Int = 0,
        var totalOfertas: Double = 0.0,
        var totalVisitantes: Int = 0
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
                    val nomeCompleto = doc.getString("nome") ?: getString(R.string.pastor_padrao)
                    val sexo = doc.getString("sexo") ?: ""
                    val prefixo = if (sexo.contains("feminino", ignoreCase = true)) "Pra." else "Pr."
                    val primeiroNome = nomeCompleto.split(" ").firstOrNull() ?: nomeCompleto
                    greetingText.text = "$prefixo $primeiroNome"

                    val funcoes = extrairFuncoes(doc.get("funcoes"))
                    // Fallback legado
                    if (funcoes.isEmpty()) {
                        doc.getString("papel")?.let { funcoes["geral"] = it }
                    }

                    // Segurança: se não tiver o papel de pastor geral
                    val papelGeral = funcoes["geral"]
                    if (papelGeral != "pastor" && papelGeral != "admin") {
                        Toast.makeText(this@PastorDashboardActivity, "Sua permissão de pastor foi revogada ou alterada.", Toast.LENGTH_LONG).show()
                        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                        sharedPref.edit().clear().apply()
                        
                        val intent = Intent(this@PastorDashboardActivity, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                }
                .addOnFailureListener {
                    greetingText.text = "Pastor"
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

                        // Passo 3: Carregar todos os usuários para métricas de membros
                        firestore.collection("usuarios").get()
                            .addOnSuccessListener { usuariosDocs ->
                                processarDados(redes, todosRelatorios, usuariosDocs)
                                swipeRefresh.isRefreshing = false
                            }
                            .addOnFailureListener {
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
        usuariosDocs: com.google.firebase.firestore.QuerySnapshot
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

        // Contar quantos não são pastores
        var totalMembrosSemPastores = 0
        for (userDoc in usuariosDocs) {
            val papelGeral = userDoc.getString("papel")
            val funcoesRaw = userDoc.get("funcoes") as? Map<*, *>
            val papelNoMapa = funcoesRaw?.get("geral")?.toString()
            
            if (papelGeral != "pastor" && papelNoMapa != "pastor") {
                totalMembrosSemPastores++
            }
        }

        // Atualizar cards de métricas
        textTotalMembros.text = totalMembrosSemPastores.toString()
        textPresencaMedia.text = presencaMedia.toString()
        textTotalOfertas.text = currencyFormat.format(somaOfertas)
        textTotalVisitantes.text = somaVisitantes.toString()

        // Mapear usuários por rede para contagem eficiente
        val usuariosPorRede = mutableMapOf<String, Int>()
        for (userDoc in usuariosDocs) {
            val funcoesRaw = userDoc.get("funcoes") as? Map<*, *>
            if (funcoesRaw != null) {
                for (redeKey in funcoesRaw.keys) {
                    if (redeKey is String && redeKey != "geral") {
                        usuariosPorRede[redeKey] = (usuariosPorRede[redeKey] ?: 0) + 1
                    }
                }
            } else {
                // Fallback para quem ainda tiver o campo "rede" legado
                val redeNome = userDoc.getString("rede") ?: ""
                if (redeNome.isNotEmpty()) {
                    usuariosPorRede[redeNome] = (usuariosPorRede[redeNome] ?: 0) + 1
                }
            }
        }

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

            // Contar membros da rede (usando o mapa de usuários)
            val countMembros = usuariosPorRede[rede.nome] ?: 0

            // Calcular métricas das últimas 8 semanas para a rede
            val relatoriosRecentesRede = relatoriosDaRede.filter {
                try {
                    val dt = sdf.parse(it.dataReuniao)
                    dt != null && dt.after(oitoSemanasAtras)
                } catch (_: Exception) { false }
            }

            var somaPresencasRede = 0
            var somaOfertasRede = 0.0
            var somaVisitantesRede = 0

            for (r in relatoriosRecentesRede) {
                somaPresencasRede += r.totalPessoas
                somaOfertasRede += r.valorOferta
                somaVisitantesRede += r.totalVisitantes
            }

            val mediaPresencaRede = if (relatoriosRecentesRede.isNotEmpty()) somaPresencasRede / relatoriosRecentesRede.size else 0

            rede.copy(
                relatorioEmDia = emDia,
                ultimoRelatorio = ultimaData,
                totalMembros = countMembros,
                presencaMedia = mediaPresencaRede,
                totalOfertas = somaOfertasRede,
                totalVisitantes = somaVisitantesRede
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
            
            val textPresencaMediaRede = cardView.findViewById<TextView>(R.id.text_presenca_media_rede)
            val textVisitantesRede = cardView.findViewById<TextView>(R.id.text_visitantes_rede)
            val textOfertasRede = cardView.findViewById<TextView>(R.id.text_ofertas_rede)
            val btnAcessarRede = cardView.findViewById<View>(R.id.btn_acessar_rede)

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

            textMembros.text = rede.totalMembros.toString()
            textPresencaMediaRede.text = rede.presencaMedia.toString()
            textVisitantesRede.text = rede.totalVisitantes.toString()
            
            val currencyFormat = DecimalFormat("R$ #,##0.00")
            textOfertasRede.text = currencyFormat.format(rede.totalOfertas)

            // Clique no card e botão → navegar para detalhes da rede
            cardView.setOnClickListener {
                mostrarOpcoesRede(rede.nome)
            }
            btnAcessarRede.setOnClickListener {
                mostrarOpcoesRede(rede.nome)
            }

            containerRedes.addView(cardView)
        }
    }

    private fun mostrarOpcoesRede(nomeRede: String) {
        val opcoes = arrayOf("Relatórios", "Gráficos", "Membros", "Agenda", "Configurações")
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
                        val intent = Intent(this, AgendaActivity::class.java)
                        intent.putExtra("REDE_SELECIONADA", nomeRede)
                        intent.putExtra("MODO_PASTOR", true)
                        startActivity(intent)
                    }
                    4 -> {
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
            val intent = Intent(this, AgendaActivity::class.java)
            intent.putExtra("MODO_PASTOR", true)
            startActivity(intent)
        }

        findViewById<MaterialCardView>(R.id.card_todos_membros).setOnClickListener {
            val intent = Intent(this, MembrosRedeActivity::class.java)
            intent.putExtra("REDE_SELECIONADA", "TODAS")
            intent.putExtra("PAPEL_USUARIO_LOGADO", "pastor")
            startActivity(intent)
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
                val funcoes = extrairFuncoes(document.get("funcoes"))
                
                // Fallback para campo legado 'papel'
                if (funcoes.isEmpty()) {
                    document.getString("papel")?.let { funcoes["geral"] = it }
                }

                if (funcoes.isNotEmpty()) {
                    if (funcoes.containsKey("geral") && (funcoes["geral"] == "pastor" || funcoes["geral"] == "admin")) {
                        expandirRedesParaPastor(funcoes, nomeUsuario)
                    } else {
                        val bottomSheet = SelecionarPerfilSheet.newInstance(funcoes, nomeUsuario)
                        bottomSheet.show(supportFragmentManager, "SelecionarPerfilSheet")
                    }
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
