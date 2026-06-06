package com.ibf.app.ui.usuarios

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ibf.app.data.models.Reuniao
import com.ibf.app.ui.agenda.AgendarReuniaoActivity
import com.ibf.app.ui.agenda.ReuniaoAdapter
import com.ibf.app.utils.MaskUtils
import com.ibf.app.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.text.Normalizer
import java.util.Locale

class FichaMembroActivity : AppCompatActivity() {

    private data class CampoObrigatorio(
        val input: TextView,
        val rotulo: String
    )

    private data class FilhoCadastro(
        val id: String? = null,
        val nome: String,
        val dataNascimento: String,
        val sexo: String
    )

    private lateinit var firestore: FirebaseFirestore
    private var membroId: String? = null
    private var redeSelecionada: String? = null
    private var papelUsuarioLogado: String? = null
    private var membroNome: String = ""
    private var membroEmail: String = ""
    private var isEditing = false

    // Componentes da UI
    private lateinit var textPageTitle: TextView
    private lateinit var fabEditarMembro: FloatingActionButton
    private lateinit var fabSalvarMembro: FloatingActionButton
    private lateinit var fabExcluirMembro: FloatingActionButton
    private lateinit var fabMarcarDiscipulado: ExtendedFloatingActionButton
    private lateinit var recyclerHistorico: RecyclerView
    private lateinit var adapterHistorico: ReuniaoAdapter
    private val listaHistorico = mutableListOf<Reuniao>()

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
    private lateinit var textEstadoCivil: AutoCompleteTextView
    private lateinit var textDataCasamento: TextInputEditText
    private lateinit var textConjuge: TextInputEditText
    private lateinit var textMae: TextInputEditText
    private lateinit var textPai: TextInputEditText
    private lateinit var checkTemFilhos: MaterialCheckBox
    private lateinit var textFilhos: TextInputEditText
    private lateinit var layoutFilhosLegado: TextInputLayout
    private lateinit var layoutCadastroFilhos: LinearLayout
    private lateinit var containerFilhos: LinearLayout
    private lateinit var buttonAdicionarFilho: MaterialButton
    private val filhosCadastro = mutableListOf<FilhoCadastro>()

    // Eclesiástico
    private lateinit var checkBatizado: MaterialCheckBox
    private lateinit var textDataBatismo: TextInputEditText
    private lateinit var textIgrejaBatismo: TextInputEditText
    private lateinit var textTipoMembro: AutoCompleteTextView
    private lateinit var textAceitoPor: AutoCompleteTextView
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
        redeSelecionada = intent.getStringExtra("REDE_SELECIONADA")
        papelUsuarioLogado = intent.getStringExtra("PAPEL_USUARIO_LOGADO")

        inicializarComponentes()

        if (membroId == null) {
            Toast.makeText(this, "Erro: ID do membro não fornecido.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        configurarBotoes()
        setupHistorico()
        carregarDadosDoMembro()
        simplificarFormularioParaCriancas()
    }

    private fun simplificarFormularioParaCriancas() {
        val isCrianca = redeSelecionada == "Rede de Crianças"
        if (!isCrianca) return

        val viewsToHide = listOf(
            textRg, textOrgaoExpeditor, textCpf, textNaturalidade, textEscolaridade,
            textEmail, textCelular, textFixo, textCep, textCidade, textRua, textNumero, textBairro,
            textEstadoCivil, textDataCasamento, textConjuge, checkTemFilhos, layoutFilhosLegado, layoutCadastroFilhos,
            textCargos, textTalentos, textDesejaFuncao
        )

        viewsToHide.forEach { view ->
            // Try to hide the TextInputLayout wrapping the EditText
            val parent = view.parent as? View
            if (parent != null && parent.javaClass.simpleName.contains("TextInputLayout")) {
                parent.visibility = View.GONE
            } else {
                view.visibility = View.GONE
            }
        }
        
        // Hide the title for contact if needed
        findViewById<TextView>(R.id.title_contato)?.visibility = View.GONE
    }

    private fun setupHistorico() {
        recyclerHistorico = findViewById(R.id.recycler_historico_discipulado)
        recyclerHistorico.layoutManager = LinearLayoutManager(this)
        adapterHistorico = ReuniaoAdapter(listaHistorico)
        recyclerHistorico.adapter = adapterHistorico
    }

    private fun inicializarComponentes() {
        textPageTitle = findViewById(R.id.text_page_title)
        fabEditarMembro = findViewById(R.id.fab_editar_membro)
        fabSalvarMembro = findViewById(R.id.fab_salvar_membro)
        fabExcluirMembro = findViewById(R.id.fab_excluir_membro)
        fabMarcarDiscipulado = findViewById(R.id.fab_marcar_discipulado)

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
        layoutFilhosLegado = findViewById(R.id.layout_filhos_legado)
        layoutCadastroFilhos = findViewById(R.id.layout_cadastro_filhos)
        containerFilhos = findViewById(R.id.container_filhos)
        buttonAdicionarFilho = findViewById(R.id.button_adicionar_filho)

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

        // Aplicar Máscaras
        textCpf.addTextChangedListener(MaskUtils.applyMask(textCpf, MaskUtils.MASK_CPF))
        textCelular.addTextChangedListener(MaskUtils.applyMask(textCelular, MaskUtils.MASK_PHONE))
        textFixo.addTextChangedListener(MaskUtils.applyMask(textFixo, MaskUtils.MASK_PHONE))
        textDataNascimento.addTextChangedListener(MaskUtils.applyMask(textDataNascimento, MaskUtils.MASK_DATE))
        textDataCasamento.addTextChangedListener(MaskUtils.applyMask(textDataCasamento, MaskUtils.MASK_DATE))
        textDataBatismo.addTextChangedListener(MaskUtils.applyMask(textDataBatismo, MaskUtils.MASK_DATE))

        configurarListasDeSelecao()
        configurarDependenciasDoFormulario()
        configurarCadastroDeFilhos()
    }

    private fun configurarCadastroDeFilhos() {
        buttonAdicionarFilho.setOnClickListener {
            mostrarDialogoFilho()
        }
        atualizarListaFilhos()
    }

    private fun configurarListasDeSelecao() {
        configurarLista(
            textEstadoCivil,
            listOf("Solteiro(a)", "Casado(a)", "União Estável", "Divorciado(a)", "Viúvo(a)")
        )
        configurarLista(
            textTipoMembro,
            listOf("Membro", "Visitante", "Congregado")
        )
        configurarLista(
            textAceitoPor,
            listOf("Batismo", "Aclamação", "Transferência", "Reconciliação", "Restauração")
        )
    }

    private fun configurarLista(campo: AutoCompleteTextView, opcoes: List<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, opcoes)
        campo.setAdapter(adapter)
        campo.threshold = 0
        campo.setOnClickListener {
            if (campo.isEnabled) {
                campo.showDropDown()
            }
        }
        campo.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && campo.isEnabled) {
                campo.showDropDown()
            }
        }
    }

    private fun configurarDependenciasDoFormulario() {
        val atualizarComLimpeza: () -> Unit = {
            atualizarCamposDependentes(limparInativos = isEditing)
        }

        textEstadoCivil.afterTextChanged(atualizarComLimpeza)
        textTipoMembro.afterTextChanged(atualizarComLimpeza)
        textAceitoPor.afterTextChanged(atualizarComLimpeza)
        textIgrejaAnterior.afterTextChanged {
            atualizarCamposDependentes(limparInativos = false)
        }
        checkTemFilhos.setOnCheckedChangeListener { _, _ -> atualizarComLimpeza() }
        checkBatizado.setOnCheckedChangeListener { _, _ -> atualizarComLimpeza() }

        atualizarCamposDependentes(limparInativos = false)
    }

    private fun atualizarCamposDependentes(limparInativos: Boolean) {
        val casado = estadoCivilExigeDadosDeCasamento()
        val batizado = checkBatizado.isChecked
        val precisaAceite = tipoMembroExigeFormaDeAceite()
        val precisaIgrejaAnterior = aceiteExigeIgrejaAnterior()

        configurarCampoDependente(textDataCasamento, casado, limparInativos)
        configurarCampoDependente(textConjuge, casado, limparInativos)
        configurarAreaFilhos(checkTemFilhos.isChecked, limparInativos)
        configurarCampoDependente(textDataBatismo, batizado, limparInativos)
        configurarCampoDependente(textIgrejaBatismo, batizado, limparInativos)
        configurarCampoDependente(textAceitoPor, precisaAceite, limparInativos)
        configurarCampoDependente(textIgrejaAnterior, precisaIgrejaAnterior, limparInativos)
        configurarCampoDependente(textPastorAnterior, precisaIgrejaAnterior, limparInativos)
    }

    private fun configurarAreaFilhos(ativo: Boolean, limparInativo: Boolean) {
        layoutFilhosLegado.visibility = View.GONE
        layoutCadastroFilhos.visibility = if (ativo) View.VISIBLE else View.GONE
        buttonAdicionarFilho.isEnabled = isEditing && ativo

        if (!ativo) {
            textFilhos.error = null
            if (limparInativo && filhosCadastro.isNotEmpty()) {
                filhosCadastro.clear()
                atualizarListaFilhos()
            }
        }
    }

    private fun mostrarDialogoFilho(filhoExistente: FilhoCadastro? = null, indice: Int? = null) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (24 * resources.displayMetrics.density).toInt()
            setPadding(padding, 0, padding, 0)
        }

        val inputNome = TextInputEditText(this).apply {
            hint = "Nome do filho"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            setText(filhoExistente?.nome.orEmpty())
        }
        val inputNascimento = TextInputEditText(this).apply {
            hint = "Data de nascimento"
            inputType = InputType.TYPE_CLASS_DATETIME
            setText(filhoExistente?.dataNascimento.orEmpty())
            addTextChangedListener(MaskUtils.applyMask(this, MaskUtils.MASK_DATE))
        }
        val inputSexo = AutoCompleteTextView(this).apply {
            hint = "Sexo"
            inputType = InputType.TYPE_NULL
            setAdapter(ArrayAdapter(
                this@FichaMembroActivity,
                android.R.layout.simple_dropdown_item_1line,
                listOf("Masculino", "Feminino")
            ))
            setText(filhoExistente?.sexo.orEmpty(), false)
            setOnClickListener { showDropDown() }
        }
        container.addView(TextInputLayout(this).apply {
            hint = "Nome do filho"
            addView(inputNome)
        })
        container.addView(TextInputLayout(this).apply {
            hint = "Data de nascimento"
            helperText = "Formato: dd/mm/aaaa"
            addView(inputNascimento)
        })
        container.addView(TextInputLayout(this).apply {
            hint = "Sexo"
            endIconMode = TextInputLayout.END_ICON_DROPDOWN_MENU
            addView(inputSexo)
        })

        MaterialAlertDialogBuilder(this)
            .setTitle(if (filhoExistente == null) "Adicionar filho" else "Editar filho")
            .setView(container)
            .setPositiveButton("Salvar", null)
            .setNegativeButton("Cancelar", null)
            .show()
            .also { dialog ->
                dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val nome = inputNome.valor()
                    val dataNascimento = inputNascimento.valor()
                    val idade = calcularIdade(dataNascimento)
                    val sexo = inputSexo.valor()

                    when {
                        nome.isBlank() -> inputNome.error = "Nome é obrigatório."
                        idade == null -> inputNascimento.error = "Data inválida."
                        sexo.isBlank() -> inputSexo.error = "Sexo é obrigatório."
                        else -> {
                            val filho = FilhoCadastro(
                                filhoExistente?.id,
                                nome,
                                dataNascimento,
                                sexo
                            )
                            if (indice != null) {
                                filhosCadastro[indice] = filho
                            } else {
                                filhosCadastro += filho
                            }
                            atualizarListaFilhos()
                            dialog.dismiss()
                        }
                    }
                }
            }
    }

    private fun atualizarListaFilhos() {
        containerFilhos.removeAllViews()

        if (filhosCadastro.isEmpty()) {
            containerFilhos.addView(TextView(this).apply {
                text = "Nenhum filho cadastrado."
                setPadding(0, 8, 0, 8)
            })
            return
        }

        filhosCadastro.forEachIndexed { index, filho ->
            val idade = calcularIdade(filho.dataNascimento)
            val rede = calcularRedeDoFilho(filho)
            val resumo = "${filho.nome} - ${filho.dataNascimento} (${idade ?: "-"} anos) - $rede"

            val linha = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, 8, 0, 8)
            }

            linha.addView(TextView(this).apply {
                text = resumo
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })

            linha.addView(MaterialButton(this).apply {
                text = "Editar"
                isEnabled = isEditing
                setOnClickListener { mostrarDialogoFilho(filho, index) }
            })

            linha.addView(MaterialButton(this).apply {
                text = "Remover"
                isEnabled = isEditing
                setOnClickListener {
                    filhosCadastro.removeAt(index)
                    atualizarListaFilhos()
                }
            })

            containerFilhos.addView(linha)
        }
    }

    private fun configurarCampoDependente(
        input: TextView,
        ativo: Boolean,
        limparInativo: Boolean
    ) {
        val layout = input.parent as? TextInputLayout
        layout?.visibility = if (ativo) View.VISIBLE else View.GONE
        input.isEnabled = isEditing && ativo

        if (!ativo) {
            input.error = null
            layout?.error = null
            if (limparInativo && input.valor().isNotEmpty()) {
                input.setText("")
            }
        }
    }

    private fun TextView.afterTextChanged(action: () -> Unit) {
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                action()
            }
        })
    }

    private fun configurarBotoes() {
        findViewById<ImageView>(R.id.button_back).setOnClickListener { finish() }

        fabEditarMembro.setOnClickListener {
            alternarModoEdicao(true)
        }

        fabSalvarMembro.setOnClickListener {
            salvarAlteracoes()
        }

        fabMarcarDiscipulado.setOnClickListener {
            val intent = Intent(this, AgendarReuniaoActivity::class.java)
            intent.putExtra("REDE_SELECIONADA", redeSelecionada)
            intent.putExtra("MEMBRO_PRE_SELECIONADO_ID", membroId)
            intent.putExtra("MEMBRO_PRE_SELECIONADO_NOME", membroNome)
            intent.putExtra("MEMBRO_PRE_SELECIONADO_EMAIL", membroEmail)
            startActivity(intent)
        }

        // Botão de exclusão — visível apenas para o pastor
        if (papelUsuarioLogado == "pastor") {
            fabExcluirMembro.visibility = View.VISIBLE
        }
        fabExcluirMembro.setOnClickListener {
            confirmarExclusaoMembro()
        }
    }

    private fun alternarModoEdicao(editar: Boolean) {
        isEditing = editar
        
        inputs.forEach { view ->
            view.isEnabled = editar
        }

        atualizarCamposDependentes(limparInativos = false)
        atualizarListaFilhos()

        if (editar) {
            fabEditarMembro.visibility = View.GONE
            fabSalvarMembro.visibility = View.VISIBLE
            fabMarcarDiscipulado.visibility = View.GONE
            textNomeMembro.requestFocus()
        } else {
            fabEditarMembro.visibility = View.VISIBLE
            fabSalvarMembro.visibility = View.GONE
            fabMarcarDiscipulado.visibility = View.VISIBLE
        }
    }

    private fun salvarAlteracoes() {
        val nome = textNomeMembro.text.toString().trim()
        
        if (!validarFormulario()) {
            return
        }

        val dadosMap = hashMapOf(
            // Pessoais
            "nome" to nome,
            "dataNascimento" to textDataNascimento.text.toString().trim(),
            "sexo" to textSexo.text.toString().trim(),
            "rg" to textRg.text.toString().trim(),
            "orgaoExpeditor" to textOrgaoExpeditor.text.toString().trim(),
            "cpf" to textCpf.text.toString().trim().replace(".", "").replace("-", ""),
            "naturalidade" to textNaturalidade.text.toString().trim(),
            "escolaridade" to textEscolaridade.text.toString().trim(),

            // Contato
            "email" to textEmail.text.toString().trim(),
            "celular" to textCelular.text.toString().trim().replace("(", "").replace(")", "").replace("-", "").replace(" ", ""),
            "telefoneFixo" to textFixo.text.toString().trim().replace("(", "").replace(")", "").replace("-", "").replace(" ", ""),
            "cep" to textCep.text.toString().trim(),
            "cidade" to textCidade.text.toString().trim(),
            "rua" to textRua.text.toString().trim(),
            "numero" to textNumero.text.toString().trim(),
            "bairro" to textBairro.text.toString().trim(),

            // Família
            "estadoCivil" to textEstadoCivil.text.toString().trim(),
            "dataCasamento" to if (estadoCivilExigeDadosDeCasamento()) textDataCasamento.valor() else "",
            "conjuge" to if (estadoCivilExigeDadosDeCasamento()) textConjuge.valor() else "",
            "mae" to textMae.text.toString().trim(),
            "pai" to textPai.text.toString().trim(),
            "temFilhos" to checkTemFilhos.isChecked,
            "filhosDetalhes" to if (checkTemFilhos.isChecked) resumoFilhos() else "",
            "filhos" to if (checkTemFilhos.isChecked) dadosFilhosParaResponsavel() else emptyList<Map<String, Any?>>(),

            // Eclesiástico
            "batizado" to checkBatizado.isChecked,
            "dataBatismo" to if (checkBatizado.isChecked) textDataBatismo.valor() else "",
            "igrejaBatismo" to if (checkBatizado.isChecked) textIgrejaBatismo.valor() else "",
            "tipoMembro" to textTipoMembro.text.toString().trim(),
            "aceitoPor" to if (tipoMembroExigeFormaDeAceite()) textAceitoPor.valor() else "",
            "igrejaAnterior" to if (aceiteExigeIgrejaAnterior()) textIgrejaAnterior.valor() else "",
            "pastorAnterior" to if (aceiteExigeIgrejaAnterior()) textPastorAnterior.valor() else "",
            "cargosExercidos" to textCargos.text.toString().trim(),
            "talentosMinisterios" to textTalentos.text.toString().trim(),
            "desejaFuncao" to textDesejaFuncao.text.toString().trim(),
            "cadastroCompleto" to true
        )

        membroId?.let { id ->
            salvarMembroEFilhos(id, nome, dadosMap)
        }
    }

    private fun salvarMembroEFilhos(
        id: String,
        nome: String,
        dadosMap: HashMap<String, Any>
    ) {
        val responsavelRef = firestore.collection("membros").document(id)
        val batch = firestore.batch()
        val filhosPersistidos = mutableListOf<Map<String, Any?>>()

        if (checkTemFilhos.isChecked) {
            filhosCadastro.forEach { filho ->
                val filhoRef = filho.id
                    ?.takeIf { it.isNotBlank() }
                    ?.let { firestore.collection("membros").document(it) }
                    ?: firestore.collection("membros").document()

                val idade = calcularIdade(filho.dataNascimento) ?: 0
                val redeFilho = calcularRedeDoFilho(filho)
                val dadosFilho = hashMapOf(
                    "nome" to filho.nome,
                    "dataNascimento" to filho.dataNascimento,
                    "sexo" to filho.sexo,
                    "estadoCivil" to FieldValue.delete(),
                    "idade" to idade,
                    "responsavelUid" to id,
                    "responsavelNome" to nome,
                    "tipoCadastro" to "filho",
                    "cadastroCompleto" to true,
                    "funcoes" to hashMapOf(redeFilho to "Membro")
                )

                filhosPersistidos += mapOf(
                    "id" to filhoRef.id,
                    "nome" to filho.nome,
                    "dataNascimento" to filho.dataNascimento,
                    "sexo" to filho.sexo,
                    "idade" to idade,
                    "rede" to redeFilho
                )
                batch.set(filhoRef, dadosFilho, SetOptions.merge())
            }
        }

        dadosMap["filhos"] = filhosPersistidos
        dadosMap["filhosDetalhes"] = filhosPersistidos.joinToString("\n") {
            "${it["nome"]} (${it["dataNascimento"]}) - ${it["rede"]}"
        }

        batch.set(responsavelRef, dadosMap, SetOptions.merge())
        batch.commit()
            .addOnSuccessListener {
                filhosCadastro.clear()
                filhosPersistidos.forEach { filho ->
                    filhosCadastro += FilhoCadastro(
                        id = filho["id"]?.toString(),
                        nome = filho["nome"]?.toString().orEmpty(),
                        dataNascimento = filho["dataNascimento"]?.toString().orEmpty(),
                        sexo = filho["sexo"]?.toString().orEmpty()
                    )
                }
                Toast.makeText(this, "Dados atualizados com sucesso!", Toast.LENGTH_SHORT).show()
                alternarModoEdicao(false)
                textPageTitle.text = nome
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Falha ao atualizar os dados.", Toast.LENGTH_SHORT).show()
                Log.e("FichaMembroActivity", "Erro ao salvar: ${e.message}", e)
            }
    }

    private fun carregarDadosDoMembro() {
        membroId?.let { id ->
            firestore.collection("membros").document(id).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        
                        membroNome = document.getString("nome") ?: "Nome não encontrado"
                        membroEmail = document.getString("email") ?: ""
                        textPageTitle.text = membroNome

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
                        
                        // Auto-fill parent name if registered by a parent and both fields are empty
                        val isCrianca = redeSelecionada == "Rede de Crianças" || document.getString("tipoCadastro") == "filho"
                        val responsavelNome = document.getString("responsavelNome")
                        if (isCrianca && textPai.text.isNullOrEmpty() && textMae.text.isNullOrEmpty() && !responsavelNome.isNullOrEmpty()) {
                            textPai.setText(responsavelNome)
                        }

                        checkTemFilhos.isChecked = document.getBoolean("temFilhos") ?: false
                        textFilhos.setText(document.getString("filhosDetalhes"))
                        carregarFilhosDoDocumento(document.get("filhos"))

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

                        atualizarCamposDependentes(limparInativos = false)
                        atualizarListaFilhos()

                    } else {
                        Toast.makeText(this, "Membro não encontrado.", Toast.LENGTH_LONG).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Erro ao carregar dados do membro.", Toast.LENGTH_SHORT).show()
                        Log.e("FichaMembroActivity", "Erro ao buscar documento: ${e.message}", e)
                }
            carregarHistoricoDiscipulado()
        }
    }

    private fun validarFormulario(): Boolean {
        atualizarCamposDependentes(limparInativos = false)
        limparErrosDeValidacao()

        val camposObrigatorios = mutableListOf(
            CampoObrigatorio(textNomeMembro, "Nome completo"),
            CampoObrigatorio(textDataNascimento, "Data de nascimento"),
            CampoObrigatorio(textSexo, "Sexo"),
            CampoObrigatorio(textRg, "RG"),
            CampoObrigatorio(textOrgaoExpeditor, "Órgão expedidor"),
            CampoObrigatorio(textCpf, "CPF"),
            CampoObrigatorio(textNaturalidade, "Naturalidade"),
            CampoObrigatorio(textEscolaridade, "Escolaridade"),
            CampoObrigatorio(textEmail, "Email"),
            CampoObrigatorio(textCelular, "Celular"),
            CampoObrigatorio(textFixo, "Telefone fixo"),
            CampoObrigatorio(textCep, "CEP"),
            CampoObrigatorio(textCidade, "Cidade"),
            CampoObrigatorio(textRua, "Rua"),
            CampoObrigatorio(textNumero, "Número"),
            CampoObrigatorio(textBairro, "Bairro"),
            CampoObrigatorio(textEstadoCivil, "Estado civil"),
            CampoObrigatorio(textMae, "Nome da mãe"),
            CampoObrigatorio(textPai, "Nome do pai"),
            CampoObrigatorio(textTipoMembro, "Tipo do membro"),
            CampoObrigatorio(textCargos, "Cargos exercidos"),
            CampoObrigatorio(textTalentos, "Talentos / Ministérios"),
            CampoObrigatorio(textDesejaFuncao, "Deseja função")
        )

        if (estadoCivilExigeDadosDeCasamento()) {
            camposObrigatorios += CampoObrigatorio(textDataCasamento, "Data de casamento")
            camposObrigatorios += CampoObrigatorio(textConjuge, "Nome do cônjuge")
        }

        if (checkBatizado.isChecked) {
            camposObrigatorios += CampoObrigatorio(textDataBatismo, "Data de batismo")
            camposObrigatorios += CampoObrigatorio(textIgrejaBatismo, "Igreja de batismo")
        }

        if (tipoMembroExigeFormaDeAceite()) {
            camposObrigatorios += CampoObrigatorio(textAceitoPor, "Aceito por")
        }

        if (aceiteExigeIgrejaAnterior()) {
            camposObrigatorios += CampoObrigatorio(textIgrejaAnterior, "Igreja anterior")
            camposObrigatorios += CampoObrigatorio(textPastorAnterior, "Pastor")
        }

        val primeiroInvalido = camposObrigatorios.firstOrNull { campo ->
            if (campo.input.valor().isBlank()) {
                campo.input.error = "${campo.rotulo} é obrigatório."
                true
            } else {
                false
            }
        }

        if (primeiroInvalido != null) {
            primeiroInvalido.input.requestFocus()
            Toast.makeText(this, "Preencha os campos obrigatórios.", Toast.LENGTH_SHORT).show()
            return false
        }

        if (checkTemFilhos.isChecked && filhosCadastro.isEmpty()) {
            Toast.makeText(this, "Cadastre pelo menos um filho.", Toast.LENGTH_SHORT).show()
            buttonAdicionarFilho.requestFocus()
            return false
        }

        val cpf = textCpf.valor().replace(".", "").replace("-", "")
        if (cpf.length != 11) {
            textCpf.error = "Informe um CPF válido com 11 dígitos."
            textCpf.requestFocus()
            Toast.makeText(this, "CPF inválido.", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun limparErrosDeValidacao() {
        inputs.filterIsInstance<TextView>().forEach { input ->
            input.error = null
            (input.parent as? TextInputLayout)?.error = null
        }
    }

    private fun TextView.valor(): String = text?.toString()?.trim().orEmpty()

    private fun estadoCivilExigeDadosDeCasamento(): Boolean {
        val estadoCivil = textEstadoCivil.valor().normalizado()
        return estadoCivil.contains("casad") || estadoCivil.contains("uniao estavel")
    }

    private fun tipoMembroExigeFormaDeAceite(): Boolean {
        val tipoMembro = textTipoMembro.valor().normalizado()
        return tipoMembro.contains("membro")
    }

    private fun aceiteExigeIgrejaAnterior(): Boolean {
        if (!tipoMembroExigeFormaDeAceite()) {
            return false
        }

        val aceite = textAceitoPor.valor().normalizado()
        val igrejaAnteriorJaPreenchida = textIgrejaAnterior.valor().isNotBlank()
        return igrejaAnteriorJaPreenchida ||
            aceite.contains("transfer") ||
            aceite.contains("reconcili") ||
            aceite.contains("restaur")
    }

    private fun String.normalizado(): String {
        val semAcentos = Normalizer.normalize(this, Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
        return semAcentos.lowercase(Locale.ROOT)
    }

    private fun carregarFilhosDoDocumento(raw: Any?) {
        filhosCadastro.clear()
        val lista = raw as? List<*> ?: emptyList<Any>()

        lista.forEach { item ->
            val dados = item as? Map<*, *> ?: return@forEach
            val nome = dados["nome"]?.toString().orEmpty()
            val dataNascimento = dados["dataNascimento"]?.toString().orEmpty()
            if (nome.isNotBlank() && dataNascimento.isNotBlank()) {
                filhosCadastro += FilhoCadastro(
                    id = dados["id"]?.toString(),
                    nome = nome,
                    dataNascimento = dataNascimento,
                    sexo = dados["sexo"]?.toString().orEmpty()
                )
            }
        }
    }

    private fun dadosFilhosParaResponsavel(): List<Map<String, Any?>> {
        return filhosCadastro.map { filho ->
            val idade = calcularIdade(filho.dataNascimento)
            mapOf(
                "id" to filho.id,
                "nome" to filho.nome,
                "dataNascimento" to filho.dataNascimento,
                "sexo" to filho.sexo,
                "idade" to idade,
                "rede" to calcularRedeDoFilho(filho)
            )
        }
    }

    private fun resumoFilhos(): String {
        return filhosCadastro.joinToString("\n") { filho ->
            "${filho.nome} (${filho.dataNascimento}) - ${calcularRedeDoFilho(filho)}"
        }
    }

    private fun calcularRedeDoFilho(filho: FilhoCadastro): String {
        val idade = calcularIdade(filho.dataNascimento)
        val sexo = filho.sexo.normalizado()

        return when {
            idade == null -> redeSelecionada ?: "Membros"
            idade < 9 -> "Crianças"
            idade in 9..13 -> "Juniores"
            idade < 18 -> "Rede de jovens"
            sexo.contains("masculino") -> "Rede de homens"
            sexo.contains("feminino") -> "Rede de mulheres"
            else -> "Rede de jovens"
        }
    }

    private fun calcularIdade(dataNascimento: String): Int? {
        val nascimento = parseData(dataNascimento) ?: return null
        val hoje = Calendar.getInstance()
        val data = Calendar.getInstance().apply { time = nascimento }

        var idade = hoje.get(Calendar.YEAR) - data.get(Calendar.YEAR)
        val aniversarioJaPassou =
            hoje.get(Calendar.DAY_OF_YEAR) >= data.get(Calendar.DAY_OF_YEAR)
        if (!aniversarioJaPassou) {
            idade--
        }

        return idade.takeIf { it >= 0 }
    }

    private fun parseData(valor: String): Date? {
        return runCatching {
            SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).apply {
                isLenient = false
            }.parse(valor)
        }.getOrNull()
    }

    private fun carregarHistoricoDiscipulado() {
        membroId?.let { id ->
            firestore.collection("reunioes")
                .whereEqualTo("membroUid", id)
                .get()
                .addOnSuccessListener { documents ->
                    listaHistorico.clear()
                    for (doc in documents) {
                        val r = doc.toObject(Reuniao::class.java)
                        r.id = doc.id
                        listaHistorico.add(r)
                    }
                    listaHistorico.sortByDescending { it.dataHora?.toDate() }
                    adapterHistorico.updateList(listaHistorico)
                }
                .addOnFailureListener { e ->
                    Log.e("FichaMembroActivity", "Erro ao carregar histórico: ${e.message}", e)
                }
        }
    }
    private fun confirmarExclusaoMembro() {
        val nome = textNomeMembro.text.toString().trim().ifEmpty { "este membro" }
        MaterialAlertDialogBuilder(this)
            .setTitle("Excluir Membro")
            .setMessage("Tem certeza que deseja excluir \"$nome\" da igreja?\n\nEsta ação é irreversível e também removerá os filhos cadastrados sob este membro.")
            .setIcon(R.drawable.ic_warning)
            .setPositiveButton("Excluir") { _, _ ->
                excluirMembro()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun excluirMembro() {
        val id = membroId ?: return

        firestore.collection("membros")
            .whereEqualTo("responsavelUid", id)
            .get()
            .addOnSuccessListener { filhosDocs ->
                val batch = firestore.batch()

                for (filhoDoc in filhosDocs) {
                    batch.delete(filhoDoc.reference)
                }

                batch.delete(firestore.collection("membros").document(id))

                batch.commit()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Membro excluído com sucesso.", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Log.e("FichaMembroActivity", "Erro ao excluir membro: ${e.message}", e)
                        Toast.makeText(this, "Falha ao excluir o membro.", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Log.e("FichaMembroActivity", "Erro ao buscar filhos: ${e.message}", e)
                Toast.makeText(this, "Falha ao excluir o membro.", Toast.LENGTH_SHORT).show()
            }
    }
}

