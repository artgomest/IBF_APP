package com.ibf.app.ui.dashboard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import com.ibf.app.ui.relatorios.FormularioRedeActivity
import com.ibf.app.ui.relatorios.SelecionarRelatorioSheet
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_secretario_dashboard)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        greetingText = findViewById(R.id.text_greeting)

        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val redeInPrefs = sharedPref.getString("REDE_SELECIONADA", null)

        redeSelecionada = redeInPrefs ?: intent.getStringExtra("REDE_SELECIONADA")

        if (redeSelecionada == null) {
            Toast.makeText(this, getString(R.string.erro_rede_nao_selecionada_logout), Toast.LENGTH_LONG).show()
            fazerLogout()
            return
        }

        greetingText.text = getString(R.string.relatorios_da_rede, redeSelecionada)

        setupRecyclerView()
        setupNavigation()

        carregarStatusDosRelatorios()
    }

    override fun onResume() {
        super.onResume()
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val currentRedeInPrefs = sharedPref.getString("REDE_SELECIONADA", null)

        if (currentRedeInPrefs != null && currentRedeInPrefs != redeSelecionada) {
            redeSelecionada = currentRedeInPrefs
            greetingText.text = getString(R.string.relatorios_da_rede, redeSelecionada)
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
                greetingText.text = getString(R.string.relatorios_da_rede, redeSelecionada)
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
        val redeAtiva = redeSelecionada ?: run {
            Log.e("SecretarioDashboard", "redeSelecionada é nula em carregarStatusDosRelatorios()")
            return
        }
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        firestore.collection("redes").whereEqualTo("nome", redeAtiva).get().addOnSuccessListener { redesDocs ->
            if (redesDocs.isEmpty) {
                Log.e("FirestoreError", "Nenhuma rede encontrada com o nome: $redeAtiva")
                listaDeStatus.clear()
                relatorioAdapter.notifyDataSetChanged()
                return@addOnSuccessListener
            }
            val diaDaSemana = redesDocs.documents.first().getLong("diaDaSemana")?.toInt() ?: run {
                Log.e("FirestoreError", "Dia da semana não encontrado para a rede: $redeAtiva")
                return@addOnSuccessListener
            }

            firestore.collection("relatorios")
                .whereEqualTo("autorUid", usuarioAtual.uid) // No cast needed, as uid is String
                .whereEqualTo("idRede", redeAtiva)
                .get().addOnSuccessListener { relatoriosDocs ->
                    val relatoriosEnviados = relatoriosDocs.mapNotNull { doc ->
                        val rel = doc.toObject(Relatorio::class.java).apply { id = doc.id }
                        Log.d("SecretarioDashboard", "Relatório do Firestore: ID: ${rel.id}, Data: ${rel.dataReuniao}, Autor: ${rel.autorUid}, Rede: ${rel.idRede}")
                        rel
                    }
                    Log.d("SecretarioDashboard", "Total de relatórios enviados encontrados para UID ${usuarioAtual.uid} e rede $redeAtiva: ${relatoriosEnviados.size}")

                    val statusFinal = mutableListOf<StatusRelatorio>()
                    val semanasParaVerificar = 8

                    for (i in 0 until semanasParaVerificar) {
                        val dataEsperadaCal = Calendar.getInstance()
                        dataEsperadaCal.add(Calendar.WEEK_OF_YEAR, -i)
                        dataEsperadaCal.set(Calendar.DAY_OF_WEEK, diaDaSemana)

                        if (dataEsperadaCal.time.after(Date())) continue

                        val dataEsperadaStr = sdf.format(dataEsperadaCal.time)
                        Log.d("SecretarioDashboard", "Verificando semana ${i+1}. Data esperada: $dataEsperadaStr (Dia da semana: ${dataEsperadaCal.get(Calendar.DAY_OF_WEEK)})")

                        val relatorioEncontrado = relatoriosEnviados.find { it.dataReuniao == dataEsperadaStr }

                        if (relatorioEncontrado != null) {
                            statusFinal.add(StatusRelatorio.Enviado(relatorioEncontrado))
                            Log.d("SecretarioDashboard", "Relatório ENVIADO encontrado para $dataEsperadaStr")
                        } else {
                            statusFinal.add(StatusRelatorio.Faltante(dataEsperadaStr, redeAtiva))
                            Log.d("SecretarioDashboard", "Relatório FALTANTE para $dataEsperadaStr")
                        }
                    }

                    listaDeStatus.clear()
                    listaDeStatus.addAll(statusFinal.sortedByDescending {
                        when(it) {
                            is StatusRelatorio.Enviado -> sdf.parse(it.relatorio.dataReuniao)
                            is StatusRelatorio.Faltante -> sdf.parse(it.dataEsperada)
                        }
                    })
                    relatorioAdapter.notifyDataSetChanged()
                    Log.d("SecretarioDashboard", "Adapter notificado. Total de itens na lista: ${listaDeStatus.size}")
                }.addOnFailureListener { e -> Log.e("FirestoreError", "Falha ao buscar relatorios", e) }
        }.addOnFailureListener { e -> Log.e("FirestoreError", "Falha ao buscar redes", e) }
    }

    private fun abrirSeletorDePerfil() {
        val user = auth.currentUser ?: return
        firestore.collection("usuarios").document(user.uid).get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val nomeUsuario = document.getString("nome") ?: getString(R.string.usuario_padrao)
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
                if (rede != redeSelecionada) {
                    val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    sharedPref.edit {
                        putString("REDE_SELECIONADA", rede)
                    }
                    this.redeSelecionada = rede
                    greetingText.text = getString(R.string.relatorios_da_rede, redeSelecionada)
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