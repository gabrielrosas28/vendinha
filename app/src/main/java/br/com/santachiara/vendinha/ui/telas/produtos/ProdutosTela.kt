package br.com.santachiara.vendinha.ui.telas.produtos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.santachiara.vendinha.R
import br.com.santachiara.vendinha.data.ProdutoRepositorio
import br.com.santachiara.vendinha.data.db.Item
import br.com.santachiara.vendinha.domain.Dinheiro
import br.com.santachiara.vendinha.domain.RegrasEstoque
import br.com.santachiara.vendinha.domain.SituacaoEstoque
import br.com.santachiara.vendinha.ui.componentes.FotoProduto
import br.com.santachiara.vendinha.ui.componentes.Tecla
import br.com.santachiara.vendinha.ui.componentes.TeclaTexto
import br.com.santachiara.vendinha.ui.componentes.Tela
import br.com.santachiara.vendinha.ui.tema.Cores
import br.com.santachiara.vendinha.ui.vm
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ProdutosViewModel(produtos: ProdutoRepositorio) : ViewModel() {
    val itens: StateFlow<List<Item>?> = produtos.observarTodos()
        .map<List<Item>, List<Item>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun ProdutosTela(onVoltar: () -> Unit, onNovo: () -> Unit, onEditar: (Long) -> Unit) {
    val viewModel = vm { ProdutosViewModel(it.produtos) }
    val itens by viewModel.itens.collectAsStateWithLifecycle()

    Tela(titulo = stringResource(R.string.produtos_titulo), onVoltar = onVoltar) {
        val lista = itens ?: return@Tela
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                TeclaTexto(
                    stringResource(R.string.produtos_novo),
                    onClick = onNovo,
                    icone = R.drawable.ic_mais,
                    cor = Cores.Verde,
                    corConteudo = Color.White,
                    alturaMinima = 80.dp,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (lista.isEmpty()) {
                item {
                    Column(Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(painterResource(R.drawable.ic_camera), null, tint = Cores.Apoio, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.produtos_vazio_titulo), style = MaterialTheme.typography.headlineSmall, color = Cores.Tinta, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(6.dp))
                        Text(stringResource(R.string.produtos_vazio_texto), style = MaterialTheme.typography.bodyLarge, color = Cores.Apoio, textAlign = TextAlign.Center)
                    }
                }
            }
            items(lista, key = { it.id }) { item -> LinhaProduto(item) { onEditar(item.id) } }
        }
    }
}

@Composable
private fun LinhaProduto(item: Item, onClick: () -> Unit) {
    val situacao = RegrasEstoque.situacao(item.controlaEstoque, item.qtdDisponivel)
    Tecla(
        onClick = onClick,
        cor = Cores.Superficie,
        corConteudo = Cores.Tinta,
        borda = Cores.Linha,
        alturaMinima = 104.dp,
        forma = RoundedCornerShape(22.dp),
        descricao = null,
    ) {
        FotoProduto(
            item.fotoPath,
            descricao = stringResource(R.string.foto_de, item.nome),
            modifier = Modifier.size(84.dp).clip(RoundedCornerShape(16.dp)).alpha(if (item.ativo) 1f else 0.4f),
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(item.nome, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Start)
            Text(Dinheiro.formatar(item.precoCentavos), style = MaterialTheme.typography.titleMedium, color = Cores.Tinta, textAlign = TextAlign.Start)
            val (texto, cor) = when {
                !item.ativo -> stringResource(R.string.produto_desativado) to Cores.Apoio
                situacao == SituacaoEstoque.SEM_LIMITE -> stringResource(R.string.sem_limite) to Cores.Apoio
                situacao == SituacaoEstoque.ESGOTADO -> stringResource(R.string.esgotado) to Cores.Erro
                situacao == SituacaoEstoque.ACABANDO -> stringResource(R.string.restam_n, item.qtdDisponivel ?: 0) to Cores.AlertaTexto
                else -> stringResource(R.string.restam_n, item.qtdDisponivel ?: 0) to Cores.Apoio
            }
            Text(texto, style = MaterialTheme.typography.bodyLarge, color = cor, textAlign = TextAlign.Start)
        }
        Icon(painterResource(R.drawable.ic_editar), stringResource(R.string.editar), tint = Cores.Apoio, modifier = Modifier.size(28.dp))
    }
}

internal fun Modifier.selo(cor: Color) = this.background(cor, RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 4.dp)
