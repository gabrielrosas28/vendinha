package br.com.santachiara.vendinha.ui.tema

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.santachiara.vendinha.R
import br.com.santachiara.vendinha.domain.Denominacao

object Cores {
    val Fundo = Color(0xFFF4F7FB)
    val Superficie = Color(0xFFFFFFFF)
    val Tinta = Color(0xFF1B2340)
    val Apoio = Color(0xFF5A6280)
    val Linha = Color(0xFFDCE2EC)

    val Laranja = Color(0xFFFF7A1A)
    val Azul = Color(0xFF2B6FFF)
    val Verde = Color(0xFF0F9D6B)
    val AlertaTexto = Color(0xFF8A3C00)
    val AlertaFundo = Color(0xFFFFF1E5)
    val Erro = Color(0xFFB42318)
    val ErroFundo = Color(0xFFFDECEA)

    val VerdeTexto = Color(0xFF0A7A52)
    val AzulTexto = Color(0xFF1F56D6)

    val Visor = Color(0xFF1B2340)
    val VisorNumero = Color(0xFF7CF2B0)
    val VisorFalta = Color(0xFFFFB4A8)
    val VisorRotulo = Color(0xFFB7C0DA)

    val TeclaApagada = Color(0xFFE3E7EF)
    val TeclaApagadaTexto = Color(0xFF6B7391)

    val Grupos = listOf(Color(0xFFFF7A1A), Color(0xFF2B6FFF), Color(0xFF0F9D6B), Color(0xFF8A3FFC), Color(0xFFD6336C))

    fun grupo(corIndex: Int): Color = Grupos[corIndex.mod(Grupos.size)]

    fun sobre(cor: Color): Color = if (cor == Laranja || cor == Grupos[0]) Tinta else Color.White

    fun beico(cor: Color): Color = lerp(cor, Color.Black, 0.28f)
}

object CoresDinheiro {
    fun cedula(d: Denominacao): Color = when (d) {
        Denominacao.R200 -> Color(0xFF8D8072)
        Denominacao.R100 -> Color(0xFF1E88C7)
        Denominacao.R50 -> Color(0xFFB8732F)
        Denominacao.R20 -> Color(0xFFF2B01E)
        Denominacao.R10 -> Color(0xFFC8423B)
        Denominacao.R5 -> Color(0xFF8B5BA8)
        Denominacao.R2 -> Color(0xFF5B87A9)
        else -> Color.Gray
    }

    fun textoCedula(d: Denominacao): Color = if (d == Denominacao.R20) Color(0xFF3A2A00) else Color.White

    val Ouro = Color(0xFFD9A92B)
    val OuroBorda = Color(0xFFA67A0E)
    val Prata = Color(0xFFC7CCD3)
    val PrataBorda = Color(0xFF8E959F)
    val Cobre = Color(0xFFC07A44)
    val CobreBorda = Color(0xFF8C5226)
}

val Nunito = FontFamily(
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
    Font(R.font.nunito_black, FontWeight.Black),
)

const val NUMEROS_TABULARES = "tnum"

private fun estilo(tamanho: Int, peso: FontWeight, altura: Int = (tamanho * 1.22f).toInt()) = TextStyle(
    fontFamily = Nunito,
    fontWeight = peso,
    fontSize = tamanho.sp,
    lineHeight = altura.sp,
)

val TipografiaVendinha = Typography(
    displayLarge = estilo(56, FontWeight.Black),
    displayMedium = estilo(44, FontWeight.Black),
    displaySmall = estilo(36, FontWeight.Black),
    headlineLarge = estilo(32, FontWeight.Black),
    headlineMedium = estilo(28, FontWeight.Black),
    headlineSmall = estilo(24, FontWeight.ExtraBold),
    titleLarge = estilo(22, FontWeight.ExtraBold),
    titleMedium = estilo(20, FontWeight.ExtraBold),
    titleSmall = estilo(18, FontWeight.ExtraBold),
    bodyLarge = estilo(18, FontWeight.Bold),
    bodyMedium = estilo(16, FontWeight.Bold),
    bodySmall = estilo(14, FontWeight.Bold),
    labelLarge = estilo(20, FontWeight.ExtraBold),
    labelMedium = estilo(16, FontWeight.ExtraBold),
    labelSmall = estilo(14, FontWeight.ExtraBold),
)

val FormasVendinha = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun VendinhaTema(conteudo: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Cores.Laranja,
            onPrimary = Cores.Tinta,
            secondary = Cores.Azul,
            onSecondary = Color.White,
            tertiary = Cores.Verde,
            onTertiary = Color.White,
            background = Cores.Fundo,
            onBackground = Cores.Tinta,
            surface = Cores.Superficie,
            onSurface = Cores.Tinta,
            surfaceVariant = Cores.Fundo,
            onSurfaceVariant = Cores.Apoio,
            surfaceContainerHigh = Cores.Superficie,
            surfaceContainerHighest = Cores.Superficie,
            outline = Cores.Linha,
            error = Cores.Erro,
            onError = Color.White,
        ),
        typography = TipografiaVendinha,
        shapes = FormasVendinha,
        content = conteudo,
    )
}
