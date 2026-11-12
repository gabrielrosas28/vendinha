package br.com.santachiara.vendinha.ui.componentes

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.santachiara.vendinha.R
import br.com.santachiara.vendinha.ui.tema.Cores
import kotlinx.coroutines.launch

val LARGURA_MAXIMA: Dp = 840.dp

@Composable
fun Tela(
    titulo: String?,
    onVoltar: (() -> Unit)?,
    modifier: Modifier = Modifier,
    fundo: Color = Cores.Fundo,
    acoes: @Composable RowScope.() -> Unit = {},
    conteudo: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier
            .fillMaxSize()
            .background(fundo)
            .safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(Modifier.widthIn(max = LARGURA_MAXIMA).fillMaxSize()) {
            if (titulo != null || onVoltar != null) Cabecalho(titulo, onVoltar, acoes)
            conteudo()
        }
    }
}

@Composable
fun Cabecalho(titulo: String?, onVoltar: (() -> Unit)?, acoes: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onVoltar != null) {
            TeclaIcone(R.drawable.ic_voltar, stringResource(R.string.voltar), onVoltar)
            Spacer(Modifier.width(14.dp))
        }
        if (titulo != null) {
            Text(
                titulo,
                style = MaterialTheme.typography.headlineMedium,
                color = Cores.Tinta,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
            )
        } else {
            Spacer(Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically, content = acoes)
    }
}

enum class TipoAviso { ALERTA, ERRO, SUCESSO, INFO }

@Composable
fun FaixaAviso(texto: String, tipo: TipoAviso = TipoAviso.ALERTA, modifier: Modifier = Modifier) {
    val (fundo, cor) = when (tipo) {
        TipoAviso.ALERTA -> Cores.AlertaFundo to Cores.AlertaTexto
        TipoAviso.ERRO -> Cores.ErroFundo to Cores.Erro
        TipoAviso.SUCESSO -> Color(0xFFE3F6EE) to Cores.VerdeTexto
        TipoAviso.INFO -> Color(0xFFE6EEFF) to Cores.AzulTexto
    }
    Row(
        modifier
            .fillMaxWidth()
            .background(fundo, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (tipo == TipoAviso.ALERTA || tipo == TipoAviso.ERRO) {
            Icon(painterResource(R.drawable.ic_alerta), null, tint = cor, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(texto, style = MaterialTheme.typography.bodyLarge, color = cor)
    }
}

@Composable
fun ManterTelaLigada() {
    val context = LocalContext.current
    DisposableEffect(context) {
        val janela = context.acharActivity()?.window
        janela?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { janela?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
}

fun Context.acharActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

@Composable
fun CadeadoOrganizador(onAbrir: () -> Unit, modifier: Modifier = Modifier) {
    val progresso = remember { Animatable(0f) }
    val escopo = rememberCoroutineScope()
    val descricao = stringResource(R.string.cadeado_descricao)
    Box(
        modifier
            .size(52.dp)
            .semantics {
                contentDescription = descricao
                onLongClick { onAbrir(); true }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    val anim = escopo.launch {
                        progresso.snapTo(0f)
                        progresso.animateTo(1f, tween(2000, easing = LinearEasing))
                        onAbrir()
                    }
                    waitForUpOrCancellation()
                    anim.cancel()
                    escopo.launch { progresso.animateTo(0f, tween(150)) }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        val p = progresso.value
        Canvas(Modifier.size(44.dp)) {
            if (p > 0f) {
                drawArc(
                    color = Cores.Azul,
                    startAngle = -90f,
                    sweepAngle = 360f * p,
                    useCenter = false,
                    topLeft = Offset(3.dp.toPx(), 3.dp.toPx()),
                    size = Size(size.width - 6.dp.toPx(), size.height - 6.dp.toPx()),
                    style = Stroke(4.dp.toPx(), cap = StrokeCap.Round),
                )
            }
        }
        Icon(
            painterResource(R.drawable.ic_cadeado),
            contentDescription = null,
            tint = Cores.Apoio.copy(alpha = 0.55f + 0.45f * p),
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
fun TituloSecao(texto: String, modifier: Modifier = Modifier) {
    Text(
        texto,
        style = MaterialTheme.typography.titleLarge,
        color = Cores.Tinta,
        modifier = modifier.semantics { heading() },
    )
}

@Composable
fun Cartao(modifier: Modifier = Modifier, cor: Color = Cores.Superficie, conteudo: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .background(cor, RoundedCornerShape(20.dp))
            .padding(16.dp),
        content = conteudo,
    )
}
