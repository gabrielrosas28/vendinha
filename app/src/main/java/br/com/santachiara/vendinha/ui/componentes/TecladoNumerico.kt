package br.com.santachiara.vendinha.ui.componentes

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import br.com.santachiara.vendinha.R
import br.com.santachiara.vendinha.domain.Senha
import br.com.santachiara.vendinha.ui.tema.Cores
import kotlinx.coroutines.launch

@Composable
fun TecladoNumerico(
    onDigito: (Char) -> Unit,
    onApagar: () -> Unit,
    modifier: Modifier = Modifier,
    alturaTecla: androidx.compose.ui.unit.Dp = 64.dp,
    extra: (@Composable (Modifier) -> Unit)? = null,
) {
    val apagar = stringResource(R.string.apagar_digito)
    Column(modifier.widthIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("123", "456", "789").forEach { linha ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                linha.forEach { d -> TeclaDigito(d, alturaTecla, Modifier.weight(1f)) { onDigito(d) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (extra != null) extra(Modifier.weight(1f)) else Spacer(Modifier.weight(1f))
            TeclaDigito('0', alturaTecla, Modifier.weight(1f)) { onDigito('0') }
            Tecla(
                onClick = onApagar,
                modifier = Modifier.weight(1f),
                cor = Cores.Superficie,
                corConteudo = Cores.Tinta,
                borda = Cores.Linha,
                alturaMinima = alturaTecla,
                descricao = apagar,
            ) { Icon(painterResource(R.drawable.ic_apagar_tecla), null, Modifier.size(30.dp)) }
        }
    }
}

@Composable
private fun TeclaDigito(d: Char, altura: androidx.compose.ui.unit.Dp, modifier: Modifier, onClick: () -> Unit) {
    Tecla(onClick = onClick, modifier = modifier, cor = Cores.Superficie, corConteudo = Cores.Tinta, borda = Cores.Linha, alturaMinima = altura) {
        Text(d.toString(), style = MaterialTheme.typography.headlineLarge)
    }
}

@Composable
fun PontosSenha(quantidade: Int, maximo: Int, erroTick: Int, modifier: Modifier = Modifier) {
    val tremor = remember { Animatable(0f) }
    LaunchedEffect(erroTick) {
        if (erroTick > 0) {
            for (x in listOf(18f, -16f, 12f, -8f, 4f, 0f)) tremor.animateTo(x, spring(stiffness = 4000f))
        }
    }
    val descricao = stringResource(R.string.digitos_digitados, quantidade)
    Row(
        modifier
            .offset { IntOffset(tremor.value.dp.roundToPx(), 0) }
            .clearAndSetSemantics { contentDescription = descricao },
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        repeat(maximo) { i ->
            val cheio = i < quantidade
            Box(
                Modifier
                    .size(20.dp)
                    .then(
                        if (cheio) Modifier.background(Cores.Tinta, CircleShape)
                        else Modifier.border(2.dp, Cores.Linha, CircleShape)
                    )
            )
        }
    }
}

@Composable
fun DialogoSenha(
    titulo: String,
    onCancelar: () -> Unit,
    verificar: suspend (String) -> Boolean,
    onSucesso: () -> Unit,
    explicacao: String? = null,
) {
    var senha by remember { mutableStateOf("") }
    var erro by remember { mutableStateOf(false) }
    var erroTick by remember { mutableStateOf(0) }
    var conferindo by remember { mutableStateOf(false) }
    val escopo = rememberCoroutineScope()

    Dialog(onDismissRequest = onCancelar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(16.dp)
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .background(Cores.Fundo, RoundedCornerShape(28.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(painterResource(R.drawable.ic_cadeado), null, tint = Cores.Azul, modifier = Modifier.size(36.dp))
            Spacer(Modifier.height(8.dp))
            Text(titulo, style = MaterialTheme.typography.headlineSmall, color = Cores.Tinta, textAlign = TextAlign.Center)
            if (explicacao != null) {
                Spacer(Modifier.height(4.dp))
                Text(explicacao, style = MaterialTheme.typography.bodyLarge, color = Cores.Apoio, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(16.dp))
            PontosSenha(senha.length, Senha.MAX_DIGITOS, erroTick)
            Spacer(Modifier.height(8.dp))
            Text(
                if (erro) stringResource(R.string.senha_errada) else " ",
                style = MaterialTheme.typography.bodyLarge,
                color = Cores.Erro,
            )
            Spacer(Modifier.height(8.dp))
            TecladoNumerico(
                onDigito = { d ->
                    if (senha.length < Senha.MAX_DIGITOS) { senha += d; erro = false }
                },
                onApagar = { senha = senha.dropLast(1); erro = false },
                alturaTecla = 60.dp,
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TeclaSecundaria(stringResource(R.string.cancelar), onCancelar, Modifier.weight(1f))
                TeclaTexto(
                    stringResource(R.string.entrar),
                    onClick = {
                        conferindo = true
                        escopo.launch {
                            val ok = verificar(senha)
                            conferindo = false
                            if (ok) onSucesso() else {
                                erro = true; erroTick++; senha = ""
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    cor = Cores.Azul,
                    habilitada = senha.length >= Senha.MIN_DIGITOS && !conferindo,
                )
            }
        }
    }
}

@Composable
fun DialogoConfirmacao(
    titulo: String,
    texto: String,
    textoConfirmar: String,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
    perigoso: Boolean = false,
) {
    Dialog(onDismissRequest = onCancelar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(16.dp)
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .background(Cores.Fundo, RoundedCornerShape(28.dp))
                .padding(24.dp),
        ) {
            Text(titulo, style = MaterialTheme.typography.headlineSmall, color = if (perigoso) Cores.Erro else Cores.Tinta)
            Spacer(Modifier.height(10.dp))
            Text(texto, style = MaterialTheme.typography.bodyLarge, color = Cores.Tinta)
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TeclaSecundaria(stringResource(R.string.cancelar), onCancelar, Modifier.weight(1f))
                TeclaTexto(
                    textoConfirmar,
                    onConfirmar,
                    Modifier.weight(1f),
                    cor = if (perigoso) Cores.Erro else Cores.Verde,
                    corConteudo = androidx.compose.ui.graphics.Color.White,
                )
            }
        }
    }
}
