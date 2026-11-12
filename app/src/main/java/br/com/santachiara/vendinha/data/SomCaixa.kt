package br.com.santachiara.vendinha.data

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import br.com.santachiara.vendinha.R

class SomCaixa(context: Context) {
    private val pool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()
    private val somId = pool.load(context.applicationContext, R.raw.caixa_registradora, 1)

    fun tocar() {
        pool.play(somId, 0.9f, 0.9f, 1, 0, 1f)
    }
}
