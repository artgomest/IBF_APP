package com.ibf.app.utils

import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Utilitário centralizado para cálculo de rede com base no perfil do membro.
 *
 * Regras de classificação:
 *   - < 9 anos          → "Rede de Crianças"
 *   - 9 a 13 anos       → "Rede de Juniores"
 *   - 14 a 17 anos      → "Rede de Jovens"
 *   - 18+ solteiro(a)   → "Rede de Jovens"
 *   - 18+ casado, masc  → "Rede de Homens"
 *   - 18+ casado, fem   → "Rede de Mulheres"
 *   - 18+ viúvo/div, m  → "Rede de Homens"
 *   - 18+ viúvo/div, f  → "Rede de Mulheres"
 */
object RedeRules {

    data class RedeResult(
        val rede: String?,
        val motivo: String? = null
    )

    /**
     * Calcula a rede do membro com base em data de nascimento, sexo e estado civil.
     *
     * @param dataNascimento Data no formato "yyyy-MM-dd" ou "dd/MM/yyyy"
     * @param sexo "Masculino" ou "Feminino"
     * @param estadoCivil "Solteiro(a)", "Casado(a)", "Viúvo(a)", "Divorciado(a)", "União Estável"
     */
    fun calcularRedePorPerfil(
        dataNascimento: String?,
        sexo: String?,
        estadoCivil: String?
    ): RedeResult {
        val idade = calcularIdade(dataNascimento) ?: return RedeResult(null, "Data de nascimento inválida")
        val sexoNorm = normalizar(sexo)
        val civilNorm = normalizar(estadoCivil)

        return when {
            idade < 9 -> RedeResult("Rede de Crianças")
            idade in 9..13 -> RedeResult("Rede de Juniores")
            idade in 14..17 -> RedeResult("Rede de Jovens")
            civilNorm.contains("solteir") -> RedeResult("Rede de Jovens")
            sexoNorm.contains("masculino") -> RedeResult("Rede de Homens")
            sexoNorm.contains("feminino") -> RedeResult("Rede de Mulheres")
            else -> RedeResult("Rede de Jovens", "Sexo não identificado, classificado como jovens")
        }
    }

    /**
     * Calcula a rede para filhos (até 18 anos usa só a idade).
     */
    fun calcularRedeFilho(dataNascimento: String?, sexo: String?): String {
        val idade = calcularIdade(dataNascimento)
        val sexoNorm = normalizar(sexo)

        return when {
            idade == null -> "Rede de Jovens"
            idade < 9 -> "Rede de Crianças"
            idade in 9..13 -> "Rede de Juniores"
            idade < 18 -> "Rede de Jovens"
            sexoNorm.contains("masculino") -> "Rede de Homens"
            sexoNorm.contains("feminino") -> "Rede de Mulheres"
            else -> "Rede de Jovens"
        }
    }

    /**
     * Calcula a idade a partir de uma data de nascimento.
     * Suporta formatos: "yyyy-MM-dd" e "dd/MM/yyyy".
     */
    fun calcularIdade(dataNascimento: String?): Int? {
        if (dataNascimento.isNullOrBlank()) return null
        val date = parseDate(dataNascimento) ?: return null

        val today = Calendar.getInstance()
        val birth = Calendar.getInstance().apply { time = date }

        var age = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR)
        val birthdayThisYear = Calendar.getInstance().apply {
            set(Calendar.YEAR, today.get(Calendar.YEAR))
            set(Calendar.MONTH, birth.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, birth.get(Calendar.DAY_OF_MONTH))
        }
        if (today.before(birthdayThisYear)) age--

        return if (age >= 0) age else null
    }

    /**
     * Normaliza um CPF removendo pontuação (apenas dígitos).
     */
    fun normalizarCpf(cpf: String?): String {
        return (cpf ?: "").replace(Regex("\\D"), "")
    }

    /**
     * Valida CPF com dígitos verificadores (algoritmo oficial).
     */
    fun validarCpf(cpfRaw: String?): Boolean {
        val cpf = normalizarCpf(cpfRaw)
        if (cpf.length != 11) return false
        if (cpf.all { it == cpf[0] }) return false

        // Primeiro dígito verificador
        var soma = 0
        for (i in 0..8) {
            soma += Character.getNumericValue(cpf[i]) * (10 - i)
        }
        var resto = (soma * 10) % 11
        if (resto == 10) resto = 0
        if (resto != Character.getNumericValue(cpf[9])) return false

        // Segundo dígito verificador
        soma = 0
        for (i in 0..9) {
            soma += Character.getNumericValue(cpf[i]) * (11 - i)
        }
        resto = (soma * 10) % 11
        if (resto == 10) resto = 0
        if (resto != Character.getNumericValue(cpf[10])) return false

        return true
    }

    // --- Helpers Internos ---

    private fun parseDate(value: String): Date? {
        val formats = arrayOf("yyyy-MM-dd", "dd/MM/yyyy")
        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.getDefault())
                sdf.isLenient = false
                return sdf.parse(value)
            } catch (_: Exception) {
                // tenta o próximo formato
            }
        }
        return null
    }

    private fun normalizar(value: String?): String {
        if (value.isNullOrBlank()) return ""
        val normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
        return normalized.replace(Regex("[\\p{InCombiningDiacriticalMarks}]"), "").lowercase()
    }
}
