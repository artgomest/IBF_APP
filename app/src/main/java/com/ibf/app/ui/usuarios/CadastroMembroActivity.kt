package com.ibf.app.ui.usuarios

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore
import com.ibf.app.R
import com.redmadrobot.inputmask.MaskedTextChangedListener
import java.text.SimpleDateFormat
import java.util.*

class CadastroMembroActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore

    // Views
    private lateinit var editTextNome: TextInputEditText
    private lateinit var editTextEmail: TextInputEditText
    private lateinit var radioGroupSexo: RadioGroup
    private lateinit var editTextDataNascimento: TextInputEditText
    private lateinit var editTextNaturalidade: TextInputEditText
    private lateinit var editTextRg: TextInputEditText
    private lateinit var editTextOrgaoExpedidor: TextInputEditText
    private lateinit var editTextCpf: TextInputEditText
    private lateinit var radioGroupEstadoCivil: RadioGroup
    private lateinit var layoutNomeConjuge: TextInputLayout
    private lateinit var editTextNomeConjuge: TextInputEditText
    private lateinit var editTextEndereco: TextInputEditText
    private lateinit var editTextNumero: TextInputEditText
    private lateinit var editTextBairro: TextInputEditText
    private lateinit var editTextCidade: TextInputEditText
    private lateinit var editTextCep: TextInputEditText
    private lateinit var editTextCelular: TextInputEditText
    private lateinit var editTextFixo: TextInputEditText
    private lateinit var spinnerEscolaridade: Spinner
    private lateinit var layoutEscolaridadeOutro: TextInputLayout
    private lateinit var editTextEscolaridadeOutro: TextInputEditText
    private lateinit var editTextNomePai: TextInputEditText
    private lateinit var editTextNomeMae: TextInputEditText
    private lateinit var buttonCadastrarMembro: Button
    private lateinit var buttonBack: ImageView

    private val calendar = Calendar.getInstance()
    private var redeSelecionada: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cadastro_membro)

        firestore = FirebaseFirestore.getInstance()
        redeSelecionada = intent.getStringExtra("REDE_SELECIONADA")

        if (redeSelecionada == null) {
            Toast.makeText(this, getString(R.string.erro_rede_nao_especificada_cadastro), Toast.LENGTH_LONG).show()
            finish()
            return
        }

        initializeViews()
        setupSpinner()
        setupListeners()
    }

    private fun initializeViews() {
        editTextEmail = findViewById(R.id.edit_text_email)
        editTextNome = findViewById(R.id.edit_text_nome_membro)
        radioGroupSexo = findViewById(R.id.radio_group_sexo)
        editTextDataNascimento = findViewById(R.id.edit_text_data_nascimento)
        editTextNaturalidade = findViewById(R.id.edit_text_naturalidade)
        editTextRg = findViewById(R.id.edit_text_rg)
        editTextOrgaoExpedidor = findViewById(R.id.edit_text_orgao_expedidor)
        editTextCpf = findViewById(R.id.edit_text_cpf)
        radioGroupEstadoCivil = findViewById(R.id.radio_group_estado_civil)
        layoutNomeConjuge = findViewById(R.id.layout_nome_conjuge)
        editTextNomeConjuge = findViewById(R.id.edit_text_nome_conjuge)
        editTextEndereco = findViewById(R.id.edit_text_endereco)
        editTextNumero = findViewById(R.id.edit_text_numero)
        editTextBairro = findViewById(R.id.edit_text_bairro)
        editTextCidade = findViewById(R.id.edit_text_cidade)
        editTextCep = findViewById(R.id.edit_text_cep)
        editTextCelular = findViewById(R.id.edit_text_celular)
        editTextFixo = findViewById(R.id.edit_text_fixo)
        spinnerEscolaridade = findViewById(R.id.spinner_escolaridade)
        layoutEscolaridadeOutro = findViewById(R.id.layout_escolaridade_outro)
        editTextEscolaridadeOutro = findViewById(R.id.edit_text_escolaridade_outro)
        editTextNomePai = findViewById(R.id.edit_text_nome_pai)
        editTextNomeMae = findViewById(R.id.edit_text_nome_mae)
        buttonCadastrarMembro = findViewById(R.id.button_cadastrar_membro)
        buttonBack = findViewById(R.id.button_back)
    }

    private fun setupSpinner() {
        ArrayAdapter.createFromResource(
            this,
            R.array.niveis_escolaridade,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerEscolaridade.adapter = adapter
        }
    }

    private fun setupListeners() {
        buttonBack.setOnClickListener { finish() }
        buttonCadastrarMembro.setOnClickListener { cadastrarNovoMembro() }

        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDateInView()
        }
        editTextDataNascimento.setOnClickListener {
            DatePickerDialog(this, dateSetListener, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        radioGroupEstadoCivil.setOnCheckedChangeListener { _, checkedId ->
            layoutNomeConjuge.isVisible = checkedId == R.id.radio_casado
        }

        spinnerEscolaridade.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selection = parent?.getItemAtPosition(position).toString()
                layoutEscolaridadeOutro.isVisible = selection == getString(R.string.form_opcao_escolaridade_outro)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        MaskedTextChangedListener.installOn(editTextCpf, "[000].[000].[000]-[00]")
        MaskedTextChangedListener.installOn(editTextCep, "[00000]-[000]")
        MaskedTextChangedListener.installOn(editTextCelular, "([00]) [00000]-[0000]")
        MaskedTextChangedListener.installOn(editTextFixo, "([00]) [0000]-[0000]")
    }

    private fun updateDateInView() {
        val myFormat = "dd/MM/yyyy"
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
        editTextDataNascimento.setText(sdf.format(calendar.time))
    }

    private fun validateFields(): Boolean {
        var isValid = true

        fun validate(editText: TextInputEditText, errorMsgResId: Int) {
            if (editText.text.toString().trim().isEmpty()) {
                editText.error = getString(errorMsgResId)
                isValid = false
            } else {
                editText.error = null
            }
        }

        validate(editTextNome, R.string.erro_validacao_nome_obrigatorio)
        validate(editTextEmail, R.string.erro_validacao_email_obrigatorio)
        validate(editTextDataNascimento, R.string.erro_validacao_data_nascimento_obrigatoria)
        validate(editTextCpf, R.string.erro_validacao_cpf_obrigatorio)
        validate(editTextCelular, R.string.erro_validacao_celular_obrigatorio)
        validate(editTextEndereco, R.string.erro_validacao_endereco_obrigatorio)
        validate(editTextCidade, R.string.erro_validacao_cidade_obrigatoria)

        if (radioGroupSexo.checkedRadioButtonId == -1) {
            Toast.makeText(this, getString(R.string.erro_validacao_selecione_sexo), Toast.LENGTH_SHORT).show()
            isValid = false
        }

        if (radioGroupEstadoCivil.checkedRadioButtonId == -1) {
            Toast.makeText(this, getString(R.string.erro_validacao_selecione_estado_civil), Toast.LENGTH_SHORT).show()
            isValid = false
        }

        if (spinnerEscolaridade.selectedItemPosition == 0) {
            Toast.makeText(this, getString(R.string.erro_validacao_selecione_escolaridade), Toast.LENGTH_SHORT).show()
            isValid = false
        }

        if (editTextEmail.text.toString().trim().isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(editTextEmail.text.toString().trim()).matches()) {
            editTextEmail.error = getString(R.string.erro_validacao_email_invalido)
            isValid = false
        }

        return isValid
    }

    private fun cadastrarNovoMembro() {
        if (!validateFields()) {
            Toast.makeText(this, getString(R.string.erro_validacao_corrija_erros), Toast.LENGTH_LONG).show()
            return
        }

        val email = editTextEmail.text.toString().trim()
        val nome = editTextNome.text.toString().trim()
        val sexo = findViewById<RadioButton>(radioGroupSexo.checkedRadioButtonId).text.toString()
        val dataNascimento = editTextDataNascimento.text.toString().trim()
        val naturalidade = editTextNaturalidade.text.toString().trim()
        val rg = editTextRg.text.toString().trim()
        val orgaoExpedidor = editTextOrgaoExpedidor.text.toString().trim()
        val cpf = editTextCpf.text.toString().trim()
        val estadoCivil = findViewById<RadioButton>(radioGroupEstadoCivil.checkedRadioButtonId).text.toString()
        val nomeConjuge = if (radioGroupEstadoCivil.checkedRadioButtonId == R.id.radio_casado) editTextNomeConjuge.text.toString().trim() else null
        val endereco = editTextEndereco.text.toString().trim()
        val numero = editTextNumero.text.toString().trim()
        val bairro = editTextBairro.text.toString().trim()
        val cidade = editTextCidade.text.toString().trim()
        val cep = editTextCep.text.toString().trim()
        val celular = editTextCelular.text.toString().trim()
        val fixo = editTextFixo.text.toString().trim()
        var escolaridade = spinnerEscolaridade.selectedItem.toString()
        if (escolaridade == getString(R.string.form_opcao_escolaridade_outro)) {
            escolaridade = editTextEscolaridadeOutro.text.toString().trim()
        }
        val nomePai = editTextNomePai.text.toString().trim()
        val nomeMae = editTextNomeMae.text.toString().trim()

        val novoMembro = hashMapOf(
            "email" to email,
            "nome" to nome,
            "sexo" to sexo,
            "dataNascimento" to dataNascimento,
            "naturalidade" to naturalidade,
            "rg" to rg,
            "orgaoExpedidor" to orgaoExpedidor,
            "cpf" to cpf,
            "estadoCivil" to estadoCivil,
            "nomeConjuge" to nomeConjuge,
            "endereco" to endereco,
            "numero" to numero,
            "bairro" to bairro,
            "cidade" to cidade,
            "cep" to cep,
            "celular" to celular,
            "telefoneFixo" to fixo,
            "escolaridade" to escolaridade,
            "nomePai" to nomePai,
            "nomeMae" to nomeMae,
            "funcoes" to hashMapOf(redeSelecionada!! to getString(R.string.papel_membro))
        )

        firestore.collection("usuarios")
            .add(novoMembro)
            .addOnSuccessListener {
                Toast.makeText(this, getString(R.string.sucesso_membro_cadastrado, nome), Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, getString(R.string.erro_cadastrar_membro, e.message), Toast.LENGTH_LONG).show()
            }
    }
}