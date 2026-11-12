package br.com.santachiara.vendinha.ui.componentes

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.santachiara.vendinha.ui.tema.Cores

@Composable
fun Tecla(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cor: Color = Cores.Laranja,
    corConteudo: Color = Cores.sobre(cor),
    habilitada: Boolean = true,
    alturaMinima: Dp = 64.dp,
    forma: Shape = RoundedCornerShape(20.dp),
    borda: Color? = null,
    beico: Dp = 5.dp,
    descricao: String? = null,
    recheio: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    conteudo: @Composable RowScope.() -> Unit,
) {
    val interacao = remember { MutableInteractionSource() }
    val apertada by interacao.collectIsPressedAsState()
    val afundar by animateDpAsState(if (apertada && habilitada) beico else 0.dp, tween(70), label = "tecla")
    val face = if (habilitada) cor else Cores.TeclaApagada
    val corBeico = when {
        !habilitada -> Color(0xFFCDD3DE)
        borda != null -> borda
        else -> Cores.beico(cor)
    }
    val texto = if (habilitada) corConteudo else Cores.TeclaApagadaTexto

    Box(
        modifier
            .semantics { if (descricao != null) contentDescription = descricao }
            .clickable(
                interactionSource = interacao,
                indication = null,
                enabled = habilitada,
                role = Role.Button,
                onClick = onClick,
            )
    ) {
        Box(
            Modifier
                .matchParentSize()
                .padding(top = beico)
                .background(corBeico, forma)
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = beico)
                .offset(y = afundar)
                .heightIn(min = alturaMinima)
                .clip(forma)
                .background(face)
                .then(if (borda != null && habilitada) Modifier.border(2.dp, borda, forma) else Modifier)
                .padding(recheio),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(LocalContentColor provides texto) {
                ProvideTextStyle(MaterialTheme.typography.labelLarge.copy(color = texto, textAlign = TextAlign.Center)) {
                    conteudo()
                }
            }
        }
    }
}

@Composable
fun TeclaTexto(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icone: Int? = null,
    cor: Color = Cores.Laranja,
    corConteudo: Color = Cores.sobre(cor),
    habilitada: Boolean = true,
    alturaMinima: Dp = 64.dp,
    borda: Color? = null,
) {
    Tecla(
        onClick = onClick,
        modifier = modifier,
        cor = cor,
        corConteudo = corConteudo,
        habilitada = habilitada,
        alturaMinima = alturaMinima,
        borda = borda,
    ) {
        if (icone != null) {
            Icon(painterResource(icone), contentDescription = null, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(texto)
    }
}

@Composable
fun TeclaSecundaria(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icone: Int? = null,
    corTexto: Color = Cores.Tinta,
    habilitada: Boolean = true,
    alturaMinima: Dp = 64.dp,
) {
    TeclaTexto(
        texto = texto,
        onClick = onClick,
        modifier = modifier,
        icone = icone,
        cor = Cores.Superficie,
        corConteudo = corTexto,
        habilitada = habilitada,
        alturaMinima = alturaMinima,
        borda = Cores.Linha,
    )
}

@Composable
fun TeclaIcone(
    @DrawableRes icone: Int,
    descricao: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cor: Color = Cores.Superficie,
    corIcone: Color = Cores.Tinta,
    tamanho: Dp = 56.dp,
    habilitada: Boolean = true,
) {
    Tecla(
        onClick = onClick,
        modifier = modifier.width(tamanho),
        cor = cor,
        corConteudo = corIcone,
        habilitada = habilitada,
        alturaMinima = tamanho - 4.dp,
        forma = CircleShape,
        borda = if (cor == Cores.Superficie) Cores.Linha else null,
        beico = 4.dp,
        descricao = descricao,
    ) {
        Icon(painterResource(icone), contentDescription = null, modifier = Modifier.size(26.dp))
    }
}
