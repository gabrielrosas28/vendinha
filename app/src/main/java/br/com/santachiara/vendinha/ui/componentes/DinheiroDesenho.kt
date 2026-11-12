package br.com.santachiara.vendinha.ui.componentes

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import br.com.santachiara.vendinha.R
import br.com.santachiara.vendinha.domain.Denominacao
import br.com.santachiara.vendinha.domain.Dinheiro
import br.com.santachiara.vendinha.domain.Pecas
import br.com.santachiara.vendinha.domain.TipoDinheiro
import br.com.santachiara.vendinha.ui.tema.Cores
import br.com.santachiara.vendinha.ui.tema.CoresDinheiro
import br.com.santachiara.vendinha.ui.tema.Nunito

private const val PROPORCAO_CEDULA = 2.15f

private fun Denominacao.numero(): String =
    if (centavos >= 100) (centavos / 100).toString() else centavos.toString()

@Composable
fun descricaoPeca(d: Denominacao): String =
    stringResource(
        if (d.tipo == TipoDinheiro.CEDULA) R.string.nota_de else R.string.moeda_de,
        Dinheiro.formatar(d.centavos),
    )

@Composable
fun Cedula(d: Denominacao, largura: Dp, modifier: Modifier = Modifier) {
    val altura = largura / PROPORCAO_CEDULA
    val cor = CoresDinheiro.cedula(d)
    val texto = CoresDinheiro.textoCedula(d)
    val densidade = LocalDensity.current
    val raio = largura * 0.07f
    Box(
        modifier
            .size(largura, altura)
            .background(cor, RoundedCornerShape(raio))
            .padding(altura * 0.07f)
            .border(1.5.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(raio * 0.7f)),
    ) {
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .padding(start = altura * 0.08f)
                .size(altura * 0.62f)
                .background(lerp(cor, Color.White, 0.32f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "R$",
                style = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Black, fontSize = with(densidade) { (altura * 0.2f).toSp() }),
                color = lerp(cor, Color.Black, 0.45f),
            )
        }
        Column(
            Modifier
                .align(Alignment.CenterEnd)
                .padding(end = altura * 0.1f),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                d.numero(),
                style = TextStyle(
                    fontFamily = Nunito,
                    fontWeight = FontWeight.Black,
                    fontSize = with(densidade) { (altura * 0.5f).toSp() },
                    lineHeight = with(densidade) { (altura * 0.52f).toSp() },
                ),
                color = texto,
            )
            Text(
                "REAIS",
                style = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = with(densidade) { (altura * 0.13f).toSp() }),
                color = texto.copy(alpha = 0.9f),
            )
        }
    }
}

@Composable
fun Moeda(d: Denominacao, diametro: Dp, modifier: Modifier = Modifier) {
    val (anel, miolo, borda) = when (d) {
        Denominacao.M1 -> Triple(CoresDinheiro.Ouro, CoresDinheiro.Prata, CoresDinheiro.OuroBorda)
        Denominacao.M050 -> Triple(CoresDinheiro.Prata, CoresDinheiro.Prata, CoresDinheiro.PrataBorda)
        Denominacao.M005 -> Triple(CoresDinheiro.Cobre, CoresDinheiro.Cobre, CoresDinheiro.CobreBorda)
        else -> Triple(CoresDinheiro.Ouro, CoresDinheiro.Ouro, CoresDinheiro.OuroBorda)
    }
    val densidade = LocalDensity.current
    val tinta = Color(0xFF2A1E05)
    Box(modifier.size(diametro), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val r = size.minDimension / 2
            drawCircle(anel, r)
            drawCircle(borda, r - 1.dp.toPx(), style = Stroke(2.dp.toPx()))
            drawCircle(miolo, r * 0.7f)
            drawCircle(borda.copy(alpha = 0.5f), r * 0.7f, style = Stroke(1.5.dp.toPx()))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                d.numero(),
                style = TextStyle(
                    fontFamily = Nunito,
                    fontWeight = FontWeight.Black,
                    fontSize = with(densidade) { (diametro * 0.36f).toSp() },
                    lineHeight = with(densidade) { (diametro * 0.38f).toSp() },
                ),
                color = tinta,
            )
            Text(
                if (d == Denominacao.M1) "REAL" else "CENTAVOS",
                style = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = with(densidade) { (diametro * 0.1f).toSp() }),
                color = tinta.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
fun Peca(d: Denominacao, tamanho: Dp, modifier: Modifier = Modifier) {
    if (d.tipo == TipoDinheiro.CEDULA) Cedula(d, tamanho, modifier) else Moeda(d, tamanho * 0.5f, modifier)
}

@Composable
private fun PecaTocavel(d: Denominacao, onToque: () -> Unit, conteudo: @Composable () -> Unit) {
    val interacao = remember { MutableInteractionSource() }
    val apertada by interacao.collectIsPressedAsState()
    val escala by animateFloatAsState(if (apertada) 0.92f else 1f, spring(dampingRatio = 0.5f, stiffness = 900f), label = "peca")
    val descricao = descricaoPeca(d)
    Box(
        Modifier
            .scale(escala)
            .clickable(interacao, indication = null, role = Role.Button, onClick = onToque)
            .clearAndSetSemantics {
                contentDescription = descricao
                role = Role.Button
            },
    ) { conteudo() }
}

private val ORDEM_CEDULAS = listOf(Denominacao.R2, Denominacao.R5, Denominacao.R10, Denominacao.R20, Denominacao.R50, Denominacao.R100, Denominacao.R200)
private val ORDEM_MOEDAS = listOf(Denominacao.M005, Denominacao.M010, Denominacao.M025, Denominacao.M050, Denominacao.M1)

@Composable
fun TecladoDinheiro(onToque: (Denominacao) -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val vao = 10.dp
        val colunas = 4
        val largCedula = (maxWidth - vao * (colunas - 1)) / colunas
        val diamMoeda = min((maxWidth - vao * 4) / 5, 96.dp)
        Column(verticalArrangement = Arrangement.spacedBy(vao)) {
            ORDEM_CEDULAS.chunked(colunas).forEach { linha ->
                Row(horizontalArrangement = Arrangement.spacedBy(vao)) {
                    linha.forEach { d -> PecaTocavel(d, { onToque(d) }) { Cedula(d, largCedula) } }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ORDEM_MOEDAS.forEach { d -> PecaTocavel(d, { onToque(d) }) { Moeda(d, diamMoeda) } }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BandejaPecas(
    pecas: List<Pecas>,
    modifier: Modifier = Modifier,
    tamanho: Dp = 92.dp,
    textoVazio: String? = null,
    corTexto: Color = Cores.Tinta,
    centralizar: Boolean = false,
    onTirar: ((Denominacao) -> Unit)? = null,
) {
    if (pecas.isEmpty()) {
        if (textoVazio != null) {
            Box(
                modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp)
                    .border(2.dp, Cores.Linha, RoundedCornerShape(18.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(textoVazio, style = MaterialTheme.typography.bodyLarge, color = Cores.Apoio, textAlign = TextAlign.Center)
            }
        }
        return
    }
    val tirarRotulo = stringResource(R.string.tirar_uma)
    FlowRow(
        modifier.fillMaxWidth(),
        horizontalArrangement = if (centralizar) Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally) else Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        pecas.forEach { p ->
            val descricao = "${p.quantidade} × ${descricaoPeca(p.denominacao)}"
            Row(
                Modifier
                    .then(
                        if (onTirar != null) Modifier.clickable(role = Role.Button, onClickLabel = tirarRotulo) { onTirar(p.denominacao) }
                        else Modifier
                    )
                    .semantics(mergeDescendants = true) { contentDescription = descricao }
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box {
                    repeat(minOf(p.quantidade, 3)) { i ->
                        Peca(p.denominacao, tamanho, Modifier.offset(x = (i * 5).dp, y = (i * 4).dp))
                    }
                }
                if (p.quantidade > 1) {
                    Spacer(Modifier.size(width = (10 + (minOf(p.quantidade, 3) - 1) * 5).dp, height = 1.dp))
                    Text(
                        "×${p.quantidade}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = corTexto,
                        modifier = Modifier.clearAndSetSemantics { },
                    )
                }
            }
        }
    }
}
