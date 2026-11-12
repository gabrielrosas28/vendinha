package br.com.santachiara.vendinha.ui.componentes

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.santachiara.vendinha.domain.Dinheiro
import br.com.santachiara.vendinha.ui.tema.Cores
import br.com.santachiara.vendinha.ui.tema.NUMEROS_TABULARES

@Composable
fun Visor(modifier: Modifier = Modifier, conteudo: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .background(Cores.Visor, RoundedCornerShape(24.dp))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        content = conteudo,
    )
}

@Composable
fun LinhaVisor(
    rotulo: String,
    centavos: Long,
    modifier: Modifier = Modifier,
    cor: Color = Cores.VisorNumero,
    grande: Boolean = false,
    animar: Boolean = grande,
) {
    val valor = Dinheiro.formatar(centavos)
    val estiloValor: TextStyle = if (grande) MaterialTheme.typography.displayMedium else MaterialTheme.typography.titleLarge
    Row(
        modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = "$rotulo: $valor"
                if (grande) liveRegion = LiveRegionMode.Polite
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            rotulo,
            style = if (grande) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
            color = Cores.VisorRotulo,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (animar) {
            AnimatedContent(
                targetState = valor,
                transitionSpec = {
                    (slideInVertically { it / 3 } + fadeIn()) togetherWith (slideOutVertically { -it / 3 } + fadeOut())
                },
                label = "visor",
            ) { v ->
                Text(v, style = estiloValor.copy(fontFeatureSettings = NUMEROS_TABULARES), color = cor, textAlign = TextAlign.End, maxLines = 1)
            }
        } else {
            Text(valor, style = estiloValor.copy(fontFeatureSettings = NUMEROS_TABULARES), color = cor, textAlign = TextAlign.End, maxLines = 1)
        }
    }
}
