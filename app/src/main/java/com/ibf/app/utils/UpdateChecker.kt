package com.ibf.app.utils

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.appcompat.app.AlertDialog
import com.ibf.app.BuildConfig
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {
    fun verificar(activity: Activity) {
        Thread {
            try {
                // Adicionando timestamp para evitar cache no Android
                val url = URL("https://ibfapp-c4078.web.app/version.json?t=" + System.currentTimeMillis())
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                if (connection.responseCode == 200) {
                    val stream = connection.inputStream
                    val reader = BufferedReader(InputStreamReader(stream))
                    val response = reader.readText()
                    reader.close()

                    val jsonObject = JSONObject(response)
                    val versaoNoBanco = jsonObject.optLong("versionCode", 0)
                    val linkDownload = jsonObject.optString("linkDownload", "https://ibfapp-c4078.web.app/app-release.apk")
                    val obrigatorio = jsonObject.optBoolean("obrigatorio", false)

                    if (versaoNoBanco > BuildConfig.VERSION_CODE) {
                        activity.runOnUiThread {
                            if (!activity.isFinishing && !activity.isDestroyed) {
                                mostrarDialogoDeAtualizacao(activity, linkDownload, obrigatorio)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("UpdateChecker", "Erro ao verificar atualizacao HTTP", e)
            }
        }.start()
    }

    private fun mostrarDialogoDeAtualizacao(activity: Activity, link: String, obrigatorio: Boolean) {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle("Nova Atualização Disponível")
        builder.setMessage("Uma nova versão do aplicativo está disponível. Por favor, atualize para continuar tendo acesso às novidades e correções.")
        builder.setPositiveButton("Baixar Agora") { _, _ ->
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
            activity.startActivity(intent)
            if (obrigatorio) {
                activity.finishAffinity()
            }
        }
        
        if (!obrigatorio) {
            builder.setNegativeButton("Mais Tarde") { dialog, _ ->
                dialog.dismiss()
            }
        }
        
        builder.setCancelable(!obrigatorio)
        builder.show()
    }
}
