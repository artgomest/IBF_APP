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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cadastro_membro)

        firestore = FirebaseFirestore.getInstance()
        initializeViews()
        setupSpinner()
        setupListeners()
    }

    private fun initializeViews() {
        // Dados Pessoais
        editTextEmail = findViewById(R.id.edit_text_email)
        editTextNome = findViewById(R.id.edit_text_nome_membro)
        radioGroupSexo = findViewById(R.id.radio_group_sexo)
        editTextDataNascimento = findViewById(R.id.edit_text_data_nascimento)
        editTextNaturalidade = findViewById(R.id.edit_text_naturalidade)

        // Documentos
        editTextRg = findViewById(R.id.edit_text_rg)
        editTextOrgaoExpedidor = findViewById(R.id.edit_text_orgao_expedidor)
        editTextCpf = findViewById(R.id.edit_text_cpf)
        radioGroupEstadoCivil = findViewById(R.id.radio_group_estado_civil)
        layoutNomeConjuge = findViewById(R.id.layout_nome_conjuge)
        editTextNomeConjuge = findViewById(R.id.edit_text_nome_conjuge)

        // Endereço e Contato
        editTextEndereco = findViewById(R.id.edit_text_endereco)
        editTextNumero = findViewById(R.id.edit_text_numero)
        editTextBairro = findViewById(R.id.edit_text_bairro)
        editTextCidade = findViewById(R.id.edit_text_cidade)
        editTextCep = findViewById(R.id.edit_text_cep)
        editTextCelular = findViewById(R.id.edit_text_celular)
        editTextFixo = findViewById(R.id.edit_text_fixo)

        // Escolaridade
        spinnerEscolaridade = findViewById(R.id.spinner_escolaridade)
        layoutEscolaridadeOutro = findViewById(R.id.layout_escolaridade_outro)
        editTextEscolaridadeOutro = findViewById(R.id.edit_text_escolaridade_outro)

        // Filiação
        editTextNomePai = findViewById(R.id.edit_text_nome_pai)
        editTextNomeMae = findViewById(R.id.edit_text_nome_mae)

        // Botões
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

        // Date Picker
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDateInView()
        }
        editTextDataNascimento.setOnClickListener {
            DatePickerDialog(
                this,
                dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Lógica condicional para Estado Civil
        radioGroupEstadoCivil.setOnCheckedChangeListener { _, checkedId ->
            layoutNomeConjuge.isVisible = checkedId == R.id.radio_casado
        }

        // Lógica condicional para Escolaridade
        spinnerEscolaridade.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selection = parent?.getItemAtPosition(position).toString()
                layoutEscolaridadeOutro.isVisible = selection.equals("Outro", ignoreCase = true)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Input Masks
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

        fun validate(editText: TextInputEditText, errorMsg: String) {
            if (editText.text.toString().trim().isEmpty()) {
                editText.error = errorMsg
                isValid = false
            } else {
                editText.error = null
            }
        }

        validate(editTextNome, "Nome é obrigatório")
        validate(editTextEmail, "Email é obrigatório")
        validate(editTextDataNascimento, "Data de nascimento é obrigatória")
        validate(editTextCpf, "CPF é obrigatório")
        validate(editTextCelular, "Celular é obrigatório")
        validate(editTextEndereco, "Endereço é obrigatório")
        validate(editTextCidade, "Cidade é obrigatória")

        if (radioGroupSexo.checkedRadioButtonId == -1) {
            Toast.makeText(this, "Selecione o sexo", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        if (radioGroupEstadoCivil.checkedRadioButtonId == -1) {
            Toast.makeText(this, "Selecione o estado civil", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        if (spinnerEscolaridade.selectedItemPosition == 0) {
            Toast.makeText(this, "Selecione o nível de escolaridade", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(editTextEmail.text.toString().trim()).matches()) {
            editTextEmail.error = "E-mail inválido"
            isValid = false
        }

        return isValid
    }

    private fun cadastrarNovoMembro() {
        if (!validateFields()) {
            Toast.makeText(this, "Por favor, corrija os erros indicados.", Toast.LENGTH_LONG).show()
            return
        }

        // Coleta de dados
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
        if (escolaridade.equals("Outro", ignoreCase = true)) {
            escolaridade = editTextEscolaridadeOutro.text.toString().trim()
        }
        val nomePai = editTextNomePai.text.toString().trim()
        val nomeMae = editTextNomeMae.text.toString().trim()

        // Criação do mapa para o Firestore
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
            "funcoes" to hashMapOf<String, String>() // Inicia sem papéis definidos
        )

        // Adiciona o novo membro à coleção 'usuarios'
        firestore.collection("usuarios")
            .add(novoMembro)
            .addOnSuccessListener {
                Toast.makeText(this, "Membro '$nome' cadastrado com sucesso!", Toast.LENGTH_LONG).show()
                finish() // Volta para a tela anterior
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao cadastrar membro: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}