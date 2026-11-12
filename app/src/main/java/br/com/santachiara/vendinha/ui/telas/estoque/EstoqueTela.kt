package br.com.santachiara.vendinha.ui.telas.estoque

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.santachiara.vendinha.AppContainer
import br.com.santachiara.vendinha.R
import br.com.santachiara.vendinha.data.db.Item
import br.com.santachiara.vendinha.domain.RegrasEstoque
import br.com.santachiara.vendinha.domain.SituacaoEstoque
import br.com.santachiara.vendinha.ui.componentes.FotoProduto
import br.com.santachiara.vendinha.ui.componentes.Tela
import br.com.santachiara.vendinha.ui.tema.Cores
import br.com.santachiara.vendinha.ui.tema.NUMEROS_TABULARES
import br.com.santachiara.vendinha.ui.vm
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class LinhaEstoque(val item: Item, val vendidos: Int)

class EstoqueViewModel(c: AppContainer) : ViewModel() {
    val linhas: StateFlow<List<LinhaEstoque>?> = combine(c.produtos.observarTodos(), c.caixa.observarQtdVendida()) { itens, vendidos ->
        val porItem = vendidos.associate { it.itemId to it.quantidade }
        itens.map { LinhaEstoque(it, porItem[it.id] ?: 0) }
            .filter { it.item.ativo || it.vendidos > 0 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun EstoqueTela(onVoltar: () -> Unit) {
    val viewModel = vm { EstoqueViewModel(it) }
    val linhas by viewModel.linhas.collectAsStateWithLifecycle()

    Tela(titulo = stringResource(R.string.estoque_titulo), onVoltar = onVoltar) {
        val lista = linhas ?: return@Tela
        if (lista.isEmpty()) {
            Text(
                stringResource(R.string.estoque_vazio),
                style = MaterialTheme.typography.titleLarge,
                color = Cores.Apoio,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(32.dp),
            )
            return@Tela
        }
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                val total = lista.sumOf { it.vendidos }
                Text(
                    pluralStringResource(R.plurals.estoque_total_vendido, total, total),
                    style = MaterialTheme.typography.titleLarge,
                    color = Cores.Tinta,
                )
            }
            items(lista, key = { it.item.id }) { LinhaDeEstoque(it) }
        }
    }
}

@Composable
private fun LinhaDeEstoque(l: LinhaEstoque) {
    val i = l.item
    val situacao = RegrasEstoque.situacao(i.controlaEstoque, i.qtdDisponivel)
    Row(
        Modifier
            .fillMaxWidth()
            .background(Cores.Superficie, RoundedCornerShape(22.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FotoProduto(i.fotoPath, stringResource(R.string.foto_de, i.nome), Modifier.size(76.dp).clip(RoundedCornerShape(14.dp)))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(i.nome, style = MaterialTheme.typography.titleLarge, color = Cores.Tinta, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (i.controlaEstoque) {
                val inicial = (i.qtdInicial ?: 0).coerceAtLeast(1)
                val disponivel = (i.qtdDisponivel ?: 0).coerceAtLeast(0)
                Text(
                    stringResource(R.string.estoque_comecou_restam, i.qtdInicial ?: 0, disponivel),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Cores.Apoio,
                )
                Spacer(Modifier.height(8.dp))
                val corBarra = when (situacao) {
                    SituacaoEstoque.ESGOTADO -> Cores.Erro
                    SituacaoEstoque.ACABANDO -> Cores.Laranja
                    else -> Cores.Verde
                }
                val descricaoBarra = stringResource(R.string.estoque_barra, disponivel, i.qtdInicial ?: 0)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .background(Cores.Fundo, RoundedCornerShape(8.dp))
                        .semantics { contentDescription = descricaoBarra },
                ) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth((disponivel.toFloat() / inicial).coerceIn(0f, 1f))
                            .background(corBarra, RoundedCornerShape(8.dp)),
                    )
                }
                if (situacao == SituacaoEstoque.ACABANDO || situacao == SituacaoEstoque.ESGOTADO) {
                    Spacer(Modifier.height(6.dp))
                    Selo(
                        stringResource(if (situacao == SituacaoEstoque.ESGOTADO) R.string.esgotado else R.string.acabando),
                        if (situacao == SituacaoEstoque.ESGOTADO) Cores.Erro else Cores.AlertaTexto,
                        if (situacao == SituacaoEstoque.ESGOTADO) Cores.ErroFundo else Cores.AlertaFundo,
                    )
                }
            } else {
                Text(stringResource(R.string.sem_limite), style = MaterialTheme.typography.bodyLarge, color = Cores.Apoio)
            }
            if (!i.ativo) Text(stringResource(R.string.produto_desativado), style = MaterialTheme.typography.bodyMedium, color = Cores.Apoio)
        }
        Spacer(Modifier.width(14.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(96.dp)) {
            Text(
                l.vendidos.toString(),
                style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = NUMEROS_TABULARES),
                color = Cores.Tinta,
            )
            Text(stringResource(R.string.estoque_vendidos), style = MaterialTheme.typography.bodyMedium, color = Cores.Apoio)
        }
    }
}

@Composable
fun Selo(texto: String, cor: Color, fundo: Color, modifier: Modifier = Modifier) {
    Text(
        texto,
        style = MaterialTheme.typography.labelMedium,
        color = cor,
        modifier = modifier
            .background(fundo, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
