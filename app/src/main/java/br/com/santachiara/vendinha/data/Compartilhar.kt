package br.com.santachiara.vendinha.data

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object Compartilhar {

    suspend fun salvarRelatorioPng(context: Context, fechamentoId: Long, bitmap: Bitmap): File =
        withContext(Dispatchers.IO) {
            val dir = File(context.filesDir, "relatorios").apply { mkdirs() }
            val f = File(dir, "relatorio_$fechamentoId.png")
            f.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            f
        }

    suspend fun salvarTexto(context: Context, nome: String, conteudo: String): File =
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "compartilhar").apply { mkdirs() }
            File(dir, nome).apply { writeText(conteudo, Charsets.UTF_8) }
        }

    fun arquivo(context: Context, arquivo: File, mime: String, titulo: String): Boolean {
        val uri = FileProvider.getUriForFile(context, FotoArmazenamento.autoridade(context), arquivo)
        val envio = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri(arquivo.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return try {
            context.startActivity(Intent.createChooser(envio, titulo))
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }
}
