// Em app/src/main/java/com/ibf/app/ui/usuarios/ListaUsuariosRedeActivity.kt
// (Substitua o arquivo inteiro)

package com.ibf.app.ui.usuarios

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.ibf.app.R
import com.ibf.app.adapters.ViewPagerAdapter
import com.ibf.app.ui.usuarios.fragments.SolicitacoesPendentesFragment
import com.ibf.app.ui.usuarios.fragments.UsuariosAtivosFragment

class ListaUsuariosRedeActivity : AppCompatActivity() {

    private var redeSelecionada: String? = null
    private var papelUsuarioLogado: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_usuarios_rede)

        findViewById<TextView>(R.id.text_page_title).text = getString(R.string.usuarios_da_rede)
        findViewById<ImageView>(R.id.button_back).setOnClickListener { finish() }

        redeSelecionada = intent.getStringExtra("REDE_SELECIONADA")
        papelUsuarioLogado = intent.getStringExtra("PAPEL_USUARIO_LOGADO")

        if (redeSelecionada == null || papelUsuarioLogado == null) {
            Toast.makeText(this, getString(R.string.erro_rede_ou_papel_nao_especificados), Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupViewPagerAndTabs()

        findViewById<FloatingActionButton>(R.id.fab_adicionar_usuario).setOnClickListener {
            val intent = Intent(this, CadastroUsuarioActivity::class.java)
            intent.putExtra("REDE_SELECIONADA", redeSelecionada)
            intent.putExtra("PAPEL_USUARIO_LOGADO", papelUsuarioLogado)
            startActivity(intent)
        }
    }

    private fun setupViewPagerAndTabs() {
        val viewPager: ViewPager2 = findViewById(R.id.view_pager)
        val tabLayout: TabLayout = findViewById(R.id.tab_layout)

        val adapter = ViewPagerAdapter(this)

        // Passando o papel do usuário logado para os fragments
        adapter.addFragment(UsuariosAtivosFragment.newInstance(redeSelecionada!!, papelUsuarioLogado!!), "Ativos")
        adapter.addFragment(SolicitacoesPendentesFragment.newInstance(redeSelecionada!!), "Pendentes")

        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = adapter.getPageTitle(position)
        }.attach()
    }
}