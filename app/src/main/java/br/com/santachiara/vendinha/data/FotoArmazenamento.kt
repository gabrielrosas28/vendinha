package br.com.santachiara.vendinha.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.min

class FotoArmazenamento(private val context: Context) {

    private val pasta: File get() = File(context.filesDir, PASTA).apply { mkdirs() }

    fun novoUriCamera(): Uri {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val f = File(dir, "foto_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, autoridade(context), f)
    }

    suspend fun salvar(uri: Uri): String = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val entrada = resolver.openInputStream(uri) ?: error("Não abriu a imagem")
        entrada.use { BitmapFactory.decodeStream(it, null, limites) }
        val lado = min(limites.outWidth, limites.outHeight)
        require(lado > 0) { "Imagem inválida" }
        var amostra = 1
        while (lado / (amostra * 2) >= LADO_MAX) amostra *= 2

        val bruto = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = amostra })
        } ?: error("Não decodificou a imagem")

        val rotacao = runCatching {
            resolver.openInputStream(uri)?.use { ExifInterface(it).rotationDegrees } ?: 0
        }.getOrDefault(0)

        val q = min(bruto.width, bruto.height)
        val escala = if (q > LADO_MAX) LADO_MAX.toFloat() / q else 1f
        val m = Matrix().apply {
            postScale(escala, escala)
            if (rotacao != 0) postRotate(rotacao.toFloat())
        }
        val quadrado = Bitmap.createBitmap(bruto, (bruto.width - q) / 2, (bruto.height - q) / 2, q, q, m, true)
        if (quadrado !== bruto) bruto.recycle()

        var qualidade = 80
        var bytes: ByteArray
        do {
            bytes = ByteArrayOutputStream().use { out ->
                quadrado.compress(Bitmap.CompressFormat.JPEG, qualidade, out)
                out.toByteArray()
            }
            qualidade -= 10
        } while (bytes.size > ALVO_BYTES && qualidade >= 50)
        quadrado.recycle()

        val destino = File(pasta, "item_${System.currentTimeMillis()}_${(1000..9999).random()}.jpg")
        destino.writeBytes(bytes)
        destino.absolutePath
    }

    fun apagar(path: String) {
        runCatching { File(path).takeIf { it.parentFile == pasta }?.delete() }
    }

    suspend fun apagarTodas() = withContext(Dispatchers.IO) {
        File(context.filesDir, PASTA).deleteRecursively()
        File(context.cacheDir, "camera").deleteRecursively()
    }

    companion object {
        const val PASTA = "fotos"
        const val LADO_MAX = 800
        const val ALVO_BYTES = 250_000
        fun autoridade(context: Context) = "${context.packageName}.arquivos"
    }
}
