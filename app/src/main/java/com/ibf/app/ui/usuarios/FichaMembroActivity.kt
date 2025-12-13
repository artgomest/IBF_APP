package com.ibf.app.ui.usuarios

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.ibf.app.R

class FichaMembroActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private var membroId: String? = null
    private var isEditing = false

    // Componentes da UI
    private lateinit var textPageTitle: TextView
    private lateinit var fabEditarMembro: FloatingActionButton
    private lateinit var fabSalvarMembro: FloatingActionButton

    // Inputs
    private lateinit var inputs: List<View> // Lista para facilitar manipular todos de uma vez

    // Dados Pessoais
    private lateinit var textNomeMembro: TextInputEditText
    private lateinit var textDataNascimento: TextInputEditText
    private lateinit var textSexo: TextInputEditText
    private lateinit var textRg: TextInputEditText
    private lateinit var textOrgaoExpeditor: TextInputEditText
    private lateinit var textCpf: TextInputEditText
    private lateinit var textNaturalidade: TextInputEditText
    private lateinit var textEscolaridade: TextInputEditText

    // Contato
    private lateinit var textEmail: TextInputEditText
    private lateinit var textCelular: TextInputEditText
    private lateinit var textFixo: TextInputEditText
    private lateinit var textCep: TextInputEditText
    private lateinit var textCidade: TextInputEditText
    private lateinit var textRua: TextInputEditText
    private lateinit var textNumero: TextInputEditText
    private lateinit var textBairro: TextInputEditText

    // Família
    private lateinit var textEstadoCivil: TextInputEditText
    private lateinit var textDataCasamento: TextInputEditText
    private lateinit var textConjuge: TextInputEditText
    private lateinit var textMae: TextInputEditText
    private lateinit var textPai: TextInputEditText
    private lateinit var checkTemFilhos: MaterialCheckBox
    private lateinit var textFilhos: TextInputEditText

    // Eclesiástico
    private lateinit var checkBatizado: MaterialCheckBox
    private lateinit var textDataBatismo: TextInputEditText
    private lateinit var textIgrejaBatismo: TextInputEditText
    private lateinit var textTipoMembro: TextInputEditText
    private lateinit var textAceitoPor: TextInputEditText
    private lateinit var textIgrejaAnterior: TextInputEditText
    private lateinit var textPastorAnterior: TextInputEditText
    private lateinit var textCargos: TextInputEditText
    private lateinit var textTalentos: TextInputEditText
    private lateinit var textDesejaFuncao: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ficha_membro)

        firestore = FirebaseFirestore.getInstance()
        membroId = intent.getStringExtra("MEMBRO_ID")

        inicializarComponentes()

        if (membroId == null) {
            Toast.makeText(this, "Erro: ID do membro não fornecido.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        configurarBotoes()
        carregarDadosDoMembro()
    }

    private fun inicializarComponentes() {
        textPageTitle = findViewById(R.id.text_page_title)
        fabEditarMembro = findViewById(R.id.fab_editar_membro)
        fabSalvarMembro = findViewById(R.id.fab_salvar_membro)

        // Pessoais
        textNomeMembro = findViewById(R.id.text_nome_membro)
        textDataNascimento = findViewById(R.id.text_data_nascimento)
        textSexo = findViewById(R.id.text_sexo)
        textRg = findViewById(R.id.text_rg)
        textOrgaoExpeditor = findViewById(R.id.text_orgao_expeditor)
        textCpf = findViewById(R.id.text_cpf)
        textNaturalidade = findViewById(R.id.text_naturalidade)
        textEscolaridade = findViewById(R.id.text_escolaridade)

        // Contato
        textEmail = findViewById(R.id.text_email)
        textCelular = findViewById(R.id.text_celular)
        textFixo = findViewById(R.id.text_fixo)
        textCep = findViewById(R.id.text_cep)
        textCidade = findViewById(R.id.text_cidade)
        textRua = findViewById(R.id.text_rua)
        textNumero = findViewById(R.id.text_numero)
        textBairro = findViewById(R.id.text_bairro)

        // Família
        textEstadoCivil = findViewById(R.id.text_estado_civil)
        textDataCasamento = findViewById(R.id.text_data_casamento)
        textConjuge = findViewById(R.id.text_conjuge)
        textMae = findViewById(R.id.text_mae)
        textPai = findViewById(R.id.text_pai)
        checkTemFilhos = findViewById(R.id.check_tem_filhos)
        textFilhos = findViewById(R.id.text_filhos)

        // Eclesiástico
        checkBatizado = findViewById(R.id.check_batizado)
        textDataBatismo = findViewById(R.id.text_data_batismo)
        textIgrejaBatismo = findViewById(R.id.text_igreja_batismo)
        textTipoMembro = findViewById(R.id.text_tipo_membro)
        textAceitoPor = findViewById(R.id.text_aceito_por)
        textIgrejaAnterior = findViewById(R.id.text_igreja_anterior)
        textPastorAnterior = findViewById(R.id.text_pastor_anterior)
        textCargos = findViewById(R.id.text_cargos)
        textTalentos = findViewById(R.id.text_talentos)
        textDesejaFuncao = findViewById(R.id.text_deseja_funcao)

        // Lista de todos os inputs para atalhos
        inputs = listOf(
            textNomeMembro, textDataNascimento, textSexo, textRg, textOrgaoExpeditor, textCpf,
            textNaturalidade, textEscolaridade, textEmail, textCelular, textFixo, textCep,
            textCidade, textRua, textNumero, textBairro, textEstadoCivil, textDataCasamento,
            textConjuge, textMae, textPai, checkTemFilhos, textFilhos, checkBatizado,
            textDataBatismo, textIgrejaBatismo, textTipoMembro, textAceitoPor, textIgrejaAnterior,
            textPastorAnterior, textCargos, textTalentos, textDesejaFuncao
        )
    }

    private fun configurarBotoes() {
        findViewById<ImageView>(R.id.button_back).setOnClickListener { finish() }

        fabEditarMembro.setOnClickListener {
            alternarModoEdicao(true)
        }

        fabSalvarMembro.setOnClickListener {
            salvarAlteracoes()
        }
    }

    private fun alternarModoEdicao(editar: Boolean) {
        isEditing = editar
        
        inputs.forEach { view ->
            view.isEnabled = editar
        }

        if (editar) {
            fabEditarMembro.visibility = View.GONE
            fabSalvarMembro.visibility = View.VISIBLE
            textNomeMembro.requestFocus()
        } else {
            fabEditarMembro.visibility = View.VISIBLE
            fabSalvarMembro.visibility = View.GONE
        }
    }

    private fun salvarAlteracoes() {
        val nome = textNomeMembro.text.toString().trim()
        
        if (nome.isEmpty()) {
            textNomeMembro.error = "O nome é obrigatório."
            return
        }

        val dadosMap = hashMapOf(
            // Pessoais
            "nome" to nome,
            "dataNascimento" to textDataNascimento.text.toString().trim(),
            "sexo" to textSexo.text.toString().trim(),
            "rg" to textRg.text.toString().trim(),
            "orgaoExpeditor" to textOrgaoExpeditor.text.toString().trim(),
            "cpf" to textCpf.text.toString().trim(),
            "naturalidade" to textNaturalidade.text.toString().trim(),
            "escolaridade" to textEscolaridade.text.toString().trim(),

            // Contato
            "email" to textEmail.text.toString().trim(),
            "celular" to textCelular.text.toString().trim(),
            "telefoneFixo" to textFixo.text.toString().trim(),
            "cep" to textCep.text.toString().trim(),
            "cidade" to textCidade.text.toString().trim(),
            "rua" to textRua.text.toString().trim(),
            "numero" to textNumero.text.toString().trim(),
            "bairro" to textBairro.text.toString().trim(),

            // Família
            "estadoCivil" to textEstadoCivil.text.toString().trim(),
            "dataCasamento" to textDataCasamento.text.toString().trim(),
            "conjuge" to textConjuge.text.toString().trim(),
            "mae" to textMae.text.toString().trim(),
            "pai" to textPai.text.toString().trim(),
            "temFilhos" to checkTemFilhos.isChecked,
            "filhosDetalhes" to textFilhos.text.toString().trim(),

            // Eclesiástico
            "batizado" to checkBatizado.isChecked,
            "dataBatismo" to textDataBatismo.text.toString().trim(),
            "igrejaBatismo" to textIgrejaBatismo.text.toString().trim(),
            "tipoMembro" to textTipoMembro.text.toString().trim(),
            "aceitoPor" to textAceitoPor.text.toString().trim(),
            "igrejaAnterior" to textIgrejaAnterior.text.toString().trim(),
            "pastorAnterior" to textPastorAnterior.text.toString().trim(),
            "cargosExercidos" to textCargos.text.toString().trim(),
            "talentosMinisterios" to textTalentos.text.toString().trim(),
            "desejaFuncao" to textDesejaFuncao.text.toString().trim()
        )

        membroId?.let { id ->
            // Usando SetOptions.merge() para não sobrescrever campos que não estão no form se houverem
            firestore.collection("usuarios").document(id)
                .set(dadosMap, SetOptions.merge())
                .addOnSuccessListener {
                    Toast.makeText(this, "Dados atualizados com sucesso!", Toast.LENGTH_SHORT).show()
                    alternarModoEdicao(false)
                    textPageTitle.text = nome
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Falha ao atualizar os dados.", Toast.LENGTH_SHORT).show()
                    Log.e("FichaMembroActivity", "Erro ao salvar: ${e.message}", e)
                }
        }
    }

    private fun carregarDadosDoMembro() {
        membroId?.let { id ->
            firestore.collection("usuarios").document(id).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        
                        textPageTitle.text = document.getString("nome") ?: "Nome não encontrado"

                        // Pessoais
                        textNomeMembro.setText(document.getString("nome"))
                        textDataNascimento.setText(document.getString("dataNascimento"))
                        textSexo.setText(document.getString("sexo"))
                        textRg.setText(document.getString("rg"))
                        textOrgaoExpeditor.setText(document.getString("orgaoExpeditor"))
                        textCpf.setText(document.getString("cpf"))
                        textNaturalidade.setText(document.getString("naturalidade"))
                        textEscolaridade.setText(document.getString("escolaridade"))

                        // Contato
                        textEmail.setText(document.getString("email"))
                        textCelular.setText(document.getString("celular"))
                        textFixo.setText(document.getString("telefoneFixo"))
                        textCep.setText(document.getString("cep"))
                        textCidade.setText(document.getString("cidade"))
                        textRua.setText(document.getString("rua"))
                        textNumero.setText(document.getString("numero"))
                        textBairro.setText(document.getString("bairro"))

                        // Família
                        textEstadoCivil.setText(document.getString("estadoCivil"))
                        textDataCasamento.setText(document.getString("dataCasamento"))
                        textConjuge.setText(document.getString("conjuge"))
                        textMae.setText(document.getString("mae"))
                        textPai.setText(document.getString("pai"))
                        checkTemFilhos.isChecked = document.getBoolean("temFilhos") ?: false
                        textFilhos.setText(document.getString("filhosDetalhes"))

                        // Eclesiástico
                        checkBatizado.isChecked = document.getBoolean("batizado") ?: false
                        textDataBatismo.setText(document.getString("dataBatismo"))
                        textIgrejaBatismo.setText(document.getString("igrejaBatismo"))
                        textTipoMembro.setText(document.getString("tipoMembro"))
                        textAceitoPor.setText(document.getString("aceitoPor"))
                        textIgrejaAnterior.setText(document.getString("igrejaAnterior"))
                        textPastorAnterior.setText(document.getString("pastorAnterior"))
                        textCargos.setText(document.getString("cargosExercidos"))
                        textTalentos.setText(document.getString("talentosMinisterios"))
                        textDesejaFuncao.setText(document.getString("desejaFuncao"))

                    } else {
                        Toast.makeText(this, "Membro não encontrado.", Toast.LENGTH_LONG).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Erro ao carregar dados do membro.", Toast.LENGTH_SHORT).show()
                    Log.e("FichaMembroActivity", "Erro ao buscar documento: ${e.message}", e)
                }
        }
    }
}

