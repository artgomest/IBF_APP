package com.ibf.app.ui.admin

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ibf.app.R
import com.ibf.app.ui.dashboard.LiderDashboardActivity
import com.ibf.app.ui.dashboard.PastorDashboardActivity
import com.ibf.app.ui.main.MainActivity
import com.ibf.app.ui.shared.SelecionarPerfilSheet

class AdminDashboardActivity : AppCompatActivity(), SelecionarPerfilSheet.PerfilSelecionadoListener {

    private lateinit var btnModoPastor: Button
    private lateinit var btnSelecionarRede: Button
    private lateinit var btnAprovarUsuarios: Button
    private lateinit var btnCadastrarManual: Button
    private lateinit var btnGerenciarUsuarios: Button
    private lateinit var btnLogout: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        btnModoPastor = findViewById(R.id.btn_modo_pastor)
        btnSelecionarRede = findViewById(R.id.btn_selecionar_rede)
        btnAprovarUsuarios = findViewById(R.id.btn_aprovar_usuarios)
        btnCadastrarManual = findViewById(R.id.btn_cadastrar_manual)
        btnGerenciarUsuarios = findViewById(R.id.btn_gerenciar_usuarios)
        btnLogout = findViewById(R.id.btn_logout)

        btnModoPastor.setOnClickListener {
            // Entrar no modo pastor (visão geral) sem sobrescrever o login admin
            val intent = Intent(this, PastorDashboardActivity::class.java)
            startActivity(intent)
        }

        btnSelecionarRede.setOnClickListener {
            // Mostrar todas as redes para o admin escolher uma para atuar como líder
            FirebaseFirestore.getInstance().collection("redes").get().addOnSuccessListener { docs ->
                val funcoesDisponiveis = HashMap<String, String>()
                for (doc in docs) {
                    val nomeRede = doc.getString("nome")
                    if (nomeRede != null) {
                        funcoesDisponiveis[nomeRede] = "lider"
                    }
                }
                
                if (funcoesDisponiveis.isEmpty()) {
                    Toast.makeText(this, "Nenhuma rede encontrada no sistema.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val bottomSheet = SelecionarPerfilSheet.newInstance(funcoesDisponiveis, "Administrador")
                bottomSheet.show(supportFragmentManager, "SelecionarPerfilSheet")
            }.addOnFailureListener {
                Toast.makeText(this, "Erro ao carregar redes.", Toast.LENGTH_SHORT).show()
            }
        }

        btnAprovarUsuarios.setOnClickListener {
            val intent = Intent(this, AdminAprovarUsuariosActivity::class.java)
            startActivity(intent)
        }

        btnCadastrarManual.setOnClickListener {
            val intent = Intent(this, AdminCadastroUsuarioActivity::class.java)
            startActivity(intent)
        }

        btnGerenciarUsuarios.setOnClickListener {
            val intent = Intent(this, AdminGerenciarUsuariosActivity::class.java)
            startActivity(intent)
        }

        btnLogout.setOnClickListener {
            onLogoutSelecionado()
        }
        
        com.ibf.app.utils.UpdateChecker.verificar(this)
    }

    override fun onPerfilSelecionado(rede: String, papel: String) {
        // Entrar como Líder sem perder o login de admin
        val intent = Intent(this, LiderDashboardActivity::class.java)
        intent.putExtra("REDE_SELECIONADA", rede)
        startActivity(intent)
    }

    override fun onLogoutSelecionado() {
        FirebaseAuth.getInstance().signOut()
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        sharedPref.edit().clear().apply()
        
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
