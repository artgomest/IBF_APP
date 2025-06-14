package com.ibf.app.util

import android.util.Log
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Converte uma String de data para um objeto Date.
 * @param format O formato da string de data (ex: "yyyy-MM-dd" ou "dd/MM/yyyy"). Padrão é "yyyy-MM-dd".
 * @return Um objeto Date se a conversão for bem-sucedida, ou null em caso de erro.
 */
fun String.toDate(format: String = "yyyy-MM-dd"): Date? {
    return try {
        // SimpleDateFormat não é thread-safe, então crie uma nova instância a cada vez.
        SimpleDateFormat(format, Locale.getDefault()).parse(this)
    } catch (e: ParseException) {
        // Loga o erro para depuração
        Log.e("DateConverter", "Erro ao converter data '$this' com formato '$format': ${e.message}")
        null
    } catch (e: IllegalArgumentException) {
        // Loga erro para formatos inválidos
        Log.e("DateConverter", "Formato de data inválido '$format': ${e.message}")
        null
    }
}