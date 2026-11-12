package br.com.santachiara.vendinha

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import br.com.santachiara.vendinha.ui.AppVendinha
import br.com.santachiara.vendinha.ui.tema.VendinhaTema

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val fundo = Color.rgb(0xF4, 0xF7, 0xFB)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(fundo, fundo),
            navigationBarStyle = SystemBarStyle.light(fundo, fundo),
        )
        super.onCreate(savedInstanceState)
        setContent {
            VendinhaTema { AppVendinha() }
        }
    }
}
