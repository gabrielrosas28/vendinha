package br.com.santachiara.vendinha.ui.telas.historico

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.santachiara.vendinha.AppContainer
import br.com.santachiara.vendinha.R
import br.com.santachiara.vendinha.data.db.Caixa
import br.com.santachiara.vendinha.data.db.StatusCaixa
import br.com.santachiara.vendinha.data.db.VendaComItens
import br.com.santachiara.vendinha.domain.Dinheiro
import br.com.santachiara.vendinha.ui.Formatos
import br.com.santachiara.vendinha.ui.componentes.DialogoSenha
import br.com.santachiara.vendinha.ui.componentes.FaixaAviso
import br.com.santachiara.vendinha.ui.componentes.LinhaVisor
import br.com.santachiara.vendinha.ui.componentes.Tecla
import br.com.santachiara.vendinha.ui.componentes.TeclaTexto
import br.com.santachiara.vendinha.ui.componentes.Tela
import br.com.santachiara.vendinha.ui.componentes.TipoAviso
import br.com.santachiara.vendinha.ui.componentes.Visor
import br.com.santachiara.vendinha.ui.tema.Cores
import br.com.santachiara.vendinha.ui.tema.NUMEROS_TABULARES
import br.com.santachiara.vendinha.ui.telas.estoque.Selo
import br.com.santachiara.vendinha.ui.vm
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoricoViewModel(c: AppContainer) : ViewModel() {
    val vendas: StateFlow<List<VendaComItens>?> = c.caixa.observarVendasAtuais()
        .map<List<VendaComItens>, List<VendaComItens>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

private fun resumoItens(v: VendaComItens) = v.itens.joinToString(", ") { "${it.quantidade}× ${it.nomeItem}" }

@Composable
fun HistoricoTela(onVoltar: () -> Unit, onAbrirVenda: (Long) -> Unit) {
    val viewModel = vm { HistoricoViewModel(it) }
    val vendas by viewModel.vendas.collectAsStateWithLifecycle()

    Tela(titulo = stringResource(R.string.historico_titulo), onVoltar = onVoltar) {
        val lista = vendas ?: return@Tela
        if (lista.isEmpty()) {
            Text(
                stringResource(R.string.historico_vazio),
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
            items(lista, key = { it.venda.id }) { v -> CartaoVenda(v) { onAbrirVenda(v.venda.id) } }
        }
    }
}

@Composable
private fun CartaoVenda(v: VendaComItens, onClick: () -> Unit) {
    val estornada = v.venda.estornada
    Tecla(
        onClick = onClick,
        cor = Cores.Superficie,
        corConteudo = Cores.Tinta,
        borda = Cores.Linha,
        alturaMinima = 96.dp,
        forma = RoundedCornerShape(22.dp),
        recheio = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
    ) {
        Text(
            Formatos.hora(v.venda.dataHora),
            style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = NUMEROS_TABULARES),
            color = Cores.Apoio,
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(resumoItens(v), style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Start)
            Text(
                stringResource(R.string.historico_recebeu_troco, Dinheiro.formatar(v.venda.recebidoCentavos), Dinheiro.formatar(v.venda.trocoCentavos)),
                style = MaterialTheme.typography.bodyMedium,
                color = Cores.Apoio,
                textAlign = TextAlign.Start,
            )
            if (estornada) {
                Spacer(Modifier.height(4.dp))
                Selo(stringResource(R.string.estornada), Cores.Erro, Cores.ErroFundo)
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            Dinheiro.formatar(v.venda.totalCentavos),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFeatureSettings = NUMEROS_TABULARES,
                textDecoration = if (estornada) TextDecoration.LineThrough else null,
            ),
            color = if (estornada) Cores.Apoio else Cores.Tinta,
        )
    }
}

class VendaDetalheViewModel(private val vendaId: Long, private val c: AppContainer) : ViewModel() {
    val venda: StateFlow<VendaComItens?> = c.caixa.observarVenda(vendaId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val caixa: StateFlow<Caixa?> = c.caixa.observarCaixaAtual()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    suspend fun conferirSenha(s: String) = c.config.conferir(s)

    fun estornar() {
        viewModelScope.launch { c.caixa.estornar(vendaId) }
    }
}

@Composable
fun VendaDetalheTela(vendaId: Long, onVoltar: () -> Unit) {
    val viewModel = vm(chave = "venda-$vendaId") { VendaDetalheViewModel(vendaId, it) }
    val v by viewModel.venda.collectAsStateWithLifecycle()
    val caixa by viewModel.caixa.collectAsStateWithLifecycle()
    var pedirSenha by rememberSaveable { mutableStateOf(false) }

    Tela(titulo = stringResource(R.string.venda_detalhe_titulo), onVoltar = onVoltar) {
        val venda = v ?: return@Tela
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(Formatos.dataHora(venda.venda.dataHora), style = MaterialTheme.typography.titleLarge, color = Cores.Apoio)
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                venda.itens.forEach { iv ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${iv.quantidade}×", style = MaterialTheme.typography.headlineSmall, color = Cores.Tinta, modifier = Modifier.width(64.dp))
                        Column(Modifier.weight(1f)) {
                            Text(iv.nomeItem, style = MaterialTheme.typography.titleLarge, color = Cores.Tinta)
                            Text(stringResource(R.string.venda_cada, Dinheiro.formatar(iv.precoUnitCentavos)), style = MaterialTheme.typography.bodyMedium, color = Cores.Apoio)
                        }
                        Text(
                            Dinheiro.formatar(iv.precoUnitCentavos * iv.quantidade),
                            style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = NUMEROS_TABULARES),
                            color = Cores.Tinta,
                        )
                    }
                }
            }
            Visor {
                LinhaVisor(stringResource(R.string.total), venda.venda.totalCentavos, cor = Color.White)
                LinhaVisor(stringResource(R.string.recebido), venda.venda.recebidoCentavos, cor = Color.White)
                LinhaVisor(stringResource(R.string.troco), venda.venda.trocoCentavos)
            }
            when {
                venda.venda.estornada -> FaixaAviso(
                    stringResource(R.string.venda_estornada_em, Formatos.hora(venda.venda.estornadaEm ?: 0L)),
                    TipoAviso.ERRO,
                )
                caixa?.status == StatusCaixa.FECHADO -> FaixaAviso(stringResource(R.string.venda_estorno_caixa_fechado), TipoAviso.INFO)
                else -> {
                    TeclaTexto(
                        stringResource(R.string.venda_estornar),
                        onClick = { pedirSenha = true },
                        icone = R.drawable.ic_desfazer,
                        cor = Cores.Erro,
                        corConteudo = Color.White,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(stringResource(R.string.venda_estornar_ajuda), style = MaterialTheme.typography.bodyMedium, color = Cores.Apoio)
                }
            }
        }
    }

    if (pedirSenha) {
        DialogoSenha(
            titulo = stringResource(R.string.venda_estornar),
            explicacao = stringResource(R.string.venda_estornar_senha),
            onCancelar = { pedirSenha = false },
            verificar = viewModel::conferirSenha,
            onSucesso = { pedirSenha = false; viewModel.estornar() },
        )
    }
}
