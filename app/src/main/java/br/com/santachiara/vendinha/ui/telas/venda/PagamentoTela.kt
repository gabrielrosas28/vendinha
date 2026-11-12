package br.com.santachiara.vendinha.ui.telas.venda

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.santachiara.vendinha.R
import br.com.santachiara.vendinha.domain.ContaCaixa
import br.com.santachiara.vendinha.domain.Dinheiro
import br.com.santachiara.vendinha.ui.componentes.BandejaPecas
import br.com.santachiara.vendinha.ui.componentes.FaixaAviso
import br.com.santachiara.vendinha.ui.componentes.LinhaVisor
import br.com.santachiara.vendinha.ui.componentes.ManterTelaLigada
import br.com.santachiara.vendinha.ui.componentes.Tecla
import br.com.santachiara.vendinha.ui.componentes.TeclaSecundaria
import br.com.santachiara.vendinha.ui.componentes.TeclaTexto
import br.com.santachiara.vendinha.ui.componentes.TecladoDinheiro
import br.com.santachiara.vendinha.ui.componentes.Tela
import br.com.santachiara.vendinha.ui.componentes.TipoAviso
import br.com.santachiara.vendinha.ui.componentes.Visor
import br.com.santachiara.vendinha.ui.tema.Cores

@Composable
fun PagamentoTela(viewModel: VendaViewModel, onVoltar: () -> Unit, onVendaFeita: () -> Unit) {
    ManterTelaLigada()
    val total by viewModel.total.collectAsStateWithLifecycle()
    val contagem by viewModel.recebido.contagem.collectAsStateWithLifecycle()
    val confirmando by viewModel.confirmando.collectAsStateWithLifecycle()
    val aviso by viewModel.aviso.collectAsStateWithLifecycle()
    val recebido = Dinheiro.somar(contagem)
    val troco = ContaCaixa.troco(total, recebido)
    val pode = ContaCaixa.podeConfirmar(total, recebido)

    Tela(titulo = stringResource(R.string.pagamento_titulo), onVoltar = onVoltar) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Visor {
                LinhaVisor(stringResource(R.string.total), total, cor = Color.White)
                LinhaVisor(stringResource(R.string.recebido), recebido, cor = Color.White)
                Box(Modifier.fillMaxWidth().padding(vertical = 6.dp).height(2.dp).background(Color.White.copy(alpha = 0.15f)))
                if (troco >= 0) {
                    LinhaVisor(stringResource(R.string.troco), troco, grande = true)
                } else {
                    LinhaVisor(stringResource(R.string.falta), -troco, cor = Cores.VisorFalta, grande = true)
                }
            }
            Text(
                stringResource(R.string.pagamento_pergunta),
                style = MaterialTheme.typography.headlineSmall,
                color = Cores.Tinta,
                modifier = Modifier.semantics { heading() },
            )
            BandejaPecas(
                Dinheiro.emPecas(contagem),
                textoVazio = stringResource(R.string.pagamento_bandeja_vazia),
                tamanho = 84.dp,
                onTirar = viewModel::tirar,
            )
            TecladoDinheiro(onToque = viewModel::receber)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TeclaSecundaria(
                    stringResource(R.string.apagar),
                    onClick = viewModel::apagarRecebido,
                    corTexto = Cores.Erro,
                    habilitada = recebido > 0,
                    modifier = Modifier.weight(1f),
                )
                TeclaTexto(
                    stringResource(R.string.pagamento_valor_certinho),
                    onClick = viewModel::valorCertinho,
                    cor = Cores.Azul,
                    corConteudo = Color.White,
                    habilitada = total > 0,
                    modifier = Modifier.weight(1f),
                )
            }
            aviso?.let { a -> FaixaAviso(stringResource(a.texto, *a.args.toTypedArray()), TipoAviso.ERRO) }
            TeclaTexto(
                stringResource(R.string.pagamento_confirmar),
                onClick = { viewModel.confirmar(onVendaFeita) },
                icone = R.drawable.ic_ok,
                cor = Cores.Verde,
                corConteudo = Color.White,
                habilitada = pode && !confirmando,
                alturaMinima = 88.dp,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            )
        }
    }
}

@Composable
fun ProntoTela(viewModel: VendaViewModel, onProximo: () -> Unit) {
    ManterTelaLigada()
    BackHandler(onBack = onProximo)
    val venda = viewModel.ultimaVenda
    if (venda == null) {
        LaunchedEffect(Unit) { onProximo() }
        return
    }
    val troco = venda.trocoCentavos
    val pecas = remember(troco) { Dinheiro.decomporTroco(troco).pecas }
    val pop = remember { Animatable(0.3f) }
    val entrada = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    LaunchedEffect(Unit) { entrada.animateTo(1f, tween(450, delayMillis = 150)) }

    Tela(titulo = null, onVoltar = null, fundo = Cores.Verde) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        ) {
            Box(
                Modifier
                    .scale(pop.value)
                    .size(120.dp)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_ok), null, tint = Cores.Verde, modifier = Modifier.size(80.dp))
            }
            Text(
                stringResource(R.string.pronto_titulo),
                style = MaterialTheme.typography.displayLarge,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Visor(Modifier.widthIn(max = 620.dp)) {
                LinhaVisor(stringResource(R.string.troco), troco, grande = true, animar = false)
            }
            Column(
                Modifier
                    .widthIn(max = 620.dp)
                    .graphicsLayer {
                        alpha = entrada.value
                        translationY = (1f - entrada.value) * 40.dp.toPx()
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (troco > 0) {
                    Text(stringResource(R.string.pronto_devolva), style = MaterialTheme.typography.headlineSmall, color = Color.White)
                    Spacer(Modifier.height(14.dp))
                    BandejaPecas(pecas, tamanho = 160.dp, corTexto = Color.White, centralizar = true)
                } else {
                    Text(
                        stringResource(R.string.pronto_sem_troco),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Tecla(
                onClick = onProximo,
                cor = Color.White,
                corConteudo = Cores.VerdeTexto,
                borda = Color(0xFF0A6E4B),
                alturaMinima = 96.dp,
                beico = 7.dp,
                modifier = Modifier.widthIn(max = 620.dp).fillMaxWidth(),
            ) {
                Text(stringResource(R.string.pronto_proximo), style = MaterialTheme.typography.headlineMedium)
            }
        }
    }
}
