package br.com.santachiara.vendinha.ui.telas.venda

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.santachiara.vendinha.R
import br.com.santachiara.vendinha.data.db.Item
import br.com.santachiara.vendinha.domain.Dinheiro
import br.com.santachiara.vendinha.domain.RegrasEstoque
import br.com.santachiara.vendinha.domain.SituacaoEstoque
import br.com.santachiara.vendinha.ui.componentes.FaixaAviso
import br.com.santachiara.vendinha.ui.componentes.FotoProduto
import br.com.santachiara.vendinha.ui.componentes.LinhaVisor
import br.com.santachiara.vendinha.ui.componentes.ManterTelaLigada
import br.com.santachiara.vendinha.ui.componentes.Tecla
import br.com.santachiara.vendinha.ui.componentes.TeclaIcone
import br.com.santachiara.vendinha.ui.componentes.TeclaSecundaria
import br.com.santachiara.vendinha.ui.componentes.Tela
import br.com.santachiara.vendinha.ui.componentes.TipoAviso
import br.com.santachiara.vendinha.ui.componentes.Visor
import br.com.santachiara.vendinha.ui.tema.Cores
import br.com.santachiara.vendinha.ui.tema.NUMEROS_TABULARES

@Composable
fun VendaTela(viewModel: VendaViewModel, onVoltar: () -> Unit, onCobrar: () -> Unit) {
    ManterTelaLigada()
    val itens by viewModel.itens.collectAsStateWithLifecycle()
    val linhas by viewModel.linhas.collectAsStateWithLifecycle()
    val total by viewModel.total.collectAsStateWithLifecycle()
    val qtds by viewModel.quantidades.collectAsStateWithLifecycle()
    val aviso by viewModel.aviso.collectAsStateWithLifecycle()

    Tela(titulo = stringResource(R.string.venda_titulo), onVoltar = onVoltar) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Visor { LinhaVisor(stringResource(R.string.total), total, grande = true) }
            Spacer(Modifier.height(12.dp))

            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val colunas = when {
                    maxWidth < 480.dp -> 2
                    maxWidth < 900.dp -> 3
                    else -> 4
                }
                val lista = itens.orEmpty()
                if (itens != null && lista.isEmpty()) {
                    Text(
                        stringResource(R.string.venda_sem_itens),
                        style = MaterialTheme.typography.titleLarge,
                        color = Cores.Apoio,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    )
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(colunas),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 12.dp),
                ) {
                    items(lista, key = { it.id }, contentType = { "item" }) { item ->
                        CardItem(item, qtds[item.id] ?: 0) { viewModel.tocarItem(item) }
                    }
                }
            }

            AnimatedVisibility(aviso != null) {
                aviso?.let { a ->
                    FaixaAviso(
                        stringResource(a.texto, *a.args.toTypedArray()),
                        TipoAviso.ALERTA,
                        Modifier.padding(bottom = 8.dp),
                    )
                }
            }

            PainelPedido(linhas, onMais = viewModel::mais, onMenos = viewModel::menos)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                TeclaSecundaria(
                    stringResource(R.string.venda_limpar),
                    onClick = viewModel::limparPedido,
                    corTexto = Cores.Erro,
                    habilitada = linhas.isNotEmpty(),
                    alturaMinima = 76.dp,
                    modifier = Modifier.weight(1f),
                )
                Tecla(
                    onClick = onCobrar,
                    habilitada = total > 0,
                    alturaMinima = 76.dp,
                    modifier = Modifier.weight(2f),
                ) {
                    Text(stringResource(R.string.venda_cobrar), style = MaterialTheme.typography.headlineMedium)
                }
            }
        }
    }
}

@Composable
private fun CardItem(item: Item, noPedido: Int, onToque: () -> Unit) {
    val situacao = RegrasEstoque.situacao(item.controlaEstoque, item.qtdDisponivel)
    val esgotado = situacao == SituacaoEstoque.ESGOTADO
    val restam = item.qtdDisponivel ?: 0
    val pulo by animateFloatAsState(if (noPedido > 0) 1f else 0.6f, spring(dampingRatio = 0.4f, stiffness = 700f), label = "badge")
    Tecla(
        onClick = onToque,
        cor = Cores.Superficie,
        corConteudo = Cores.Tinta,
        borda = Cores.Linha,
        habilitada = !esgotado,
        forma = RoundedCornerShape(22.dp),
        recheio = PaddingValues(10.dp),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Box {
                FotoProduto(
                    item.fotoPath,
                    descricao = stringResource(R.string.foto_de, item.nome),
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .alpha(if (esgotado) 0.35f else 1f),
                )
                if (noPedido > 0) {
                    val descricao = stringResource(R.string.venda_no_pedido, noPedido)
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .scale(pulo)
                            .size(52.dp)
                            .background(Cores.Laranja, CircleShape)
                            .clearAndSetSemantics { contentDescription = descricao },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(noPedido.toString(), style = MaterialTheme.typography.headlineSmall, color = Cores.Tinta)
                    }
                }
                if (esgotado) {
                    Text(
                        stringResource(R.string.esgotado),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(Cores.Erro, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                item.nome,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start,
            )
            Text(
                Dinheiro.formatar(item.precoCentavos),
                style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = NUMEROS_TABULARES),
                textAlign = TextAlign.Start,
            )
            val (texto, cor, fundo) = when (situacao) {
                SituacaoEstoque.SEM_LIMITE -> Triple(stringResource(R.string.sem_limite), Cores.Apoio, Color.Transparent)
                SituacaoEstoque.NORMAL -> Triple(stringResource(R.string.restam_n, restam), Cores.Apoio, Color.Transparent)
                SituacaoEstoque.ACABANDO -> Triple(stringResource(R.string.acabando_restam, restam), Cores.AlertaTexto, Cores.AlertaFundo)
                SituacaoEstoque.ESGOTADO -> Triple(stringResource(R.string.esgotado), Cores.Erro, Color.Transparent)
            }
            Text(
                texto,
                style = MaterialTheme.typography.bodyMedium,
                color = cor,
                maxLines = 1,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .background(fundo, RoundedCornerShape(8.dp))
                    .padding(horizontal = if (fundo == Color.Transparent) 0.dp else 8.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun PainelPedido(linhas: List<LinhaPedidoUi>, onMais: (Item) -> Unit, onMenos: (Item) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Cores.Superficie, RoundedCornerShape(22.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        if (linhas.isEmpty()) {
            Text(
                stringResource(R.string.venda_vazia),
                style = MaterialTheme.typography.titleMedium,
                color = Cores.Apoio,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
            )
        } else {
            LazyColumn(Modifier.heightIn(max = 236.dp)) {
                items(linhas, key = { it.item.id }) { l -> LinhaDoPedido(l, onMais, onMenos) }
            }
        }
    }
}

@Composable
private fun LinhaDoPedido(l: LinhaPedidoUi, onMais: (Item) -> Unit, onMenos: (Item) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 78.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FotoProduto(l.item.fotoPath, null, Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(l.item.nome, style = MaterialTheme.typography.titleMedium, color = Cores.Tinta, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(stringResource(R.string.venda_cada, Dinheiro.formatar(l.item.precoCentavos)), style = MaterialTheme.typography.bodyMedium, color = Cores.Apoio)
        }
        TeclaIcone(R.drawable.ic_menos, stringResource(R.string.venda_menos, l.item.nome), { onMenos(l.item) }, tamanho = 64.dp)
        Text(
            l.quantidade.toString(),
            style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = NUMEROS_TABULARES),
            color = Cores.Tinta,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 48.dp).semantics { contentDescription = l.quantidade.toString() },
        )
        TeclaIcone(R.drawable.ic_mais, stringResource(R.string.venda_mais, l.item.nome), { onMais(l.item) }, tamanho = 64.dp, cor = Cores.Laranja)
        Text(
            Dinheiro.formatar(l.subtotal),
            style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = NUMEROS_TABULARES),
            color = Cores.Tinta,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(min = 112.dp),
        )
    }
}
