package br.com.santachiara.vendinha.ui.componentes

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import br.com.santachiara.vendinha.R
import br.com.santachiara.vendinha.ui.tema.Cores
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object CacheDeFotos {
    private const val LADO_ALVO = 400
    private val cache = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 8).toInt()) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    fun naMemoria(path: String): Bitmap? = cache.get(path)

    suspend fun carregar(path: String): Bitmap? = cache.get(path) ?: withContext(Dispatchers.IO) {
        val f = File(path)
        if (!f.exists()) return@withContext null
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, limites)
        var amostra = 1
        while (minOf(limites.outWidth, limites.outHeight) / (amostra * 2) >= LADO_ALVO) amostra *= 2
        val bmp = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply {
            inSampleSize = amostra
            inPreferredConfig = Bitmap.Config.RGB_565
        })
        bmp?.also { cache.put(path, it) }
    }

    fun esquecer(path: String) {
        cache.remove(path)
    }
}

@Composable
fun FotoProduto(path: String?, descricao: String?, modifier: Modifier = Modifier) {
    val imagem by produceState<ImageBitmap?>(
        initialValue = path?.let { CacheDeFotos.naMemoria(it) }?.asImageBitmap(),
        key1 = path,
    ) {
        value = path?.let { CacheDeFotos.carregar(it) }?.asImageBitmap()
    }
    val img = imagem
    if (img != null) {
        Image(img, contentDescription = descricao, contentScale = ContentScale.Crop, modifier = modifier)
    } else {
        Box(modifier.background(Cores.Fundo), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_galeria), contentDescription = descricao, tint = Cores.Linha, modifier = Modifier.size(40.dp))
        }
    }
}
