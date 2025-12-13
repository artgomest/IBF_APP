package com.ibf.app.ui.agenda

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.ibf.app.R
import com.ibf.app.data.models.Reuniao

class AgendaActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var adapter: ReuniaoAdapter
    private val listaReunioes = mutableListOf<Reuniao>()

    private var redeSelecionada: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agenda)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        redeSelecionada = intent.getStringExtra("REDE_SELECIONADA")

        findViewById<ImageView>(R.id.button_back).setOnClickListener { finish() }
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout)
        
        setupRecyclerView()
        setupFab()
        setupSwipeRefresh()
    }

    override fun onResume() {
        super.onResume()
        carregarReunioes()
    }

    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view_agenda)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ReuniaoAdapter(listaReunioes)
        recyclerView.adapter = adapter
    }

    private fun setupFab() {
        findViewById<FloatingActionButton>(R.id.fab_adicionar_reuniao).setOnClickListener {
            val intent = Intent(this, AgendarReuniaoActivity::class.java)
            intent.putExtra("REDE_SELECIONADA", redeSelecionada)
            startActivity(intent)
        }
    }

    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener {
            carregarReunioes()
        }
    }

    private fun carregarReunioes() {
        val uid = auth.currentUser?.uid ?: return
        
        // Query: Meetings where I am the leader OR the member
        // For simplicity in MVP for Leader Dashboard: Meetings where I created (liderUid == myUid)
        
        firestore.collection("reunioes")
            .whereEqualTo("liderUid", uid)
            .orderBy("dataHora", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { documents ->
                listaReunioes.clear()
                for (doc in documents) {
                    val reuniao = doc.toObject(Reuniao::class.java)
                    reuniao.id = doc.id
                    listaReunioes.add(reuniao)
                }
                adapter.updateList(listaReunioes)
                swipeRefreshLayout.isRefreshing = false
            }
            .addOnFailureListener { e ->
                Log.e("AgendaActivity", "Erro ao carregar agenda", e)
                Toast.makeText(this, "Erro ao carregar agenda.", Toast.LENGTH_SHORT).show()
                swipeRefreshLayout.isRefreshing = false
            }
    }
}
