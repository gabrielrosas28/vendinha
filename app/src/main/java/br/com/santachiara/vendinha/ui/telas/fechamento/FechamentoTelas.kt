package br.com.santachiara.vendinha.ui.telas.fechamento

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.santachiara.vendinha.AppContainer
import br.com.santachiara.vendinha.R
import br.com.santachiara.vendinha.data.db.Caixa
import br.com.santachiara.vendinha.domain.ContaCaixa
import br.com.santachiara.vendinha.domain.Dinheiro
import br.com.santachiara.vendinha.domain.LinhaVendida
import br.com.santachiara.vendinha.domain.Resumo
import br.com.santachiara.vendinha.domain.ResultadoConferencia
import br.com.santachiara.vendinha.domain.ResumoCaixa
import br.com.santachiara.vendinha.domain.ValorVenda
import br.com.santachiara.vendinha.ui.componentes.BandejaPecas
import br.com.santachiara.vendinha.ui.componentes.DialogoConfirmacao
import br.com.santachiara.vendinha.ui.componentes.FaixaAviso
import br.com.santachiara.vendinha.ui.componentes.LinhaVisor
import br.com.santachiara.vendinha.ui.componentes.Tecla
import br.com.santachiara.vendinha.ui.componentes.TeclaSecundaria
import br.com.santachiara.vendinha.ui.componentes.TeclaTexto
import br.com.santachiara.vendinha.ui.componentes.TecladoDinheiro
import br.com.santachiara.vendinha.ui.componentes.TecladoNumerico
import br.com.santachiara.vendinha.ui.componentes.Tela
import br.com.santachiara.vendinha.ui.componentes.TipoAviso
import br.com.santachiara.vendinha.ui.componentes.TituloSecao
import br.com.santachiara.vendinha.ui.componentes.Visor
import br.com.santachiara.vendinha.ui.tema.Cores
import br.com.santachiara.vendinha.ui.tema.NUMEROS_TABULARES
import br.com.santachiara.vendinha.ui.telas.caixa.ContadorDinheiro
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FechamentoEstado(val caixa: Caixa? = null, val resumo: ResumoCaixa? = null) {
    val esperado: Long get() = ContaCaixa.dinheiroEsperado(caixa?.trocoInicialCentavos ?: 0, resumo?.totalGanhoCentavos ?: 0)
}

enum class ModoConferencia { CONTAR, DIGITAR }

class FechamentoViewModel(private val c: AppContainer) : ViewModel() {

    val estado: StateFlow<FechamentoEstado> = combine(c.caixa.observarCaixaAtual(), c.caixa.observarVendasAtuais()) { caixa, vendas ->
        FechamentoEstado(
            caixa = caixa,
            resumo = Resumo.calcular(
                vendas.map { ValorVenda(it.venda.totalCentavos, it.venda.estornada) },
                vendas.flatMap { v -> v.itens.map { LinhaVendida(it.itemId, it.nomeItem, it.quantidade, it.precoUnitCentavos, v.venda.estornada) } },
            ),
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, FechamentoEstado())

    val contador = ContadorDinheiro()
    val modo = MutableStateFlow(ModoConferencia.CONTAR)
    val digitado = MutableStateFlow(0L)
    private val _fechando = MutableStateFlow(false)
    val fechando: StateFlow<Boolean> = _fechando

    fun digitar(d: Char) {
        val novo = digitado.value * 10 + (d - '0')
        if (novo <= Dinheiro.MAXIMO_CENTAVOS) digitado.value = novo
    }

    fun apagarDigito() { digitado.value = digitado.value / 10 }

    fun contado(contagem: Map<br.com.santachiara.vendinha.domain.Denominacao, Int>, digitado: Long, modo: ModoConferencia): Long =
        if (modo == ModoConferencia.CONTAR) Dinheiro.somar(contagem) else digitado

    fun fechar(contado: Long?, pronto: (Long) -> Unit) {
        val caixa = estado.value.caixa ?: return
        if (_fechando.value) return
        _fechando.value = true
        viewModelScope.launch {
            val id = c.caixa.fechar(caixa.id, contado)
            pronto(id)
        }
    }
}

@Composable
fun FecharCaixaTela(viewModel: FechamentoViewModel, onVoltar: () -> Unit, onContar: () -> Unit, onFechado: (Long) -> Unit) {
    val e by viewModel.estado.collectAsStateWithLifecycle()
    val resumo = e.resumo
    var confirmarPular by rememberSaveable { mutableStateOf(false) }

    Tela(titulo = stringResource(R.string.fechar_titulo), onVoltar = onVoltar) {
        if (resumo == null) return@Tela
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Visor {
                LinhaVisor(stringResource(R.string.total_ganho), resumo.totalGanhoCentavos, grande = true)
                Text(
                    pluralStringResource(R.plurals.vendas, resumo.numeroVendas, resumo.numeroVendas),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Cores.VisorRotulo,
                )
            }
            resumo.campeao?.let { c ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Cores.AlertaFundo, RoundedCornerShape(22.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(60.dp).background(Cores.Laranja, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(painterResource(R.drawable.ic_estrela), null, tint = Cores.Tinta, modifier = Modifier.size(36.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(stringResource(R.string.campeao), style = MaterialTheme.typography.bodyLarge, color = Cores.AlertaTexto)
                        Text(c.nome, style = MaterialTheme.typography.headlineSmall, color = Cores.Tinta)
                        Text(pluralStringResource(R.plurals.vendidos, c.quantidade, c.quantidade), style = MaterialTheme.typography.bodyLarge, color = Cores.AlertaTexto)
                    }
                }
            }
            if (resumo.itens.isNotEmpty()) {
                TituloSecao(stringResource(R.string.fechar_por_item))
                TabelaItens(resumo)
            }
            if (resumo.numeroEstornadas > 0) {
                Text(
                    pluralStringResource(R.plurals.estornadas, resumo.numeroEstornadas, resumo.numeroEstornadas),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Cores.Apoio,
                )
            }
            TeclaTexto(
                stringResource(R.string.fechar_contar),
                onClick = onContar,
                cor = Cores.Azul,
                corConteudo = Color.White,
                alturaMinima = 80.dp,
                modifier = Modifier.fillMaxWidth(),
            )
            TeclaSecundaria(
                stringResource(R.string.fechar_pular),
                onClick = { confirmarPular = true },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            )
        }
    }
    if (confirmarPular) {
        DialogoConfirmacao(
            titulo = stringResource(R.string.fechar_confirmar_titulo),
            texto = stringResource(R.string.fechar_confirmar_texto),
            textoConfirmar = stringResource(R.string.fechar_confirmar),
            onConfirmar = { confirmarPular = false; viewModel.fechar(null, onFechado) },
            onCancelar = { confirmarPular = false },
        )
    }
}

@Composable
fun TabelaItens(resumo: ResumoCaixa) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Cores.Superficie, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        resumo.itens.forEachIndexed { i, it ->
            if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(Cores.Linha))
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(it.nome, style = MaterialTheme.typography.titleMedium, color = Cores.Tinta, modifier = Modifier.weight(1f))
                Text(
                    "${it.quantidade}×",
                    style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = NUMEROS_TABULARES),
                    color = Cores.Apoio,
                    modifier = Modifier.width(64.dp),
                    textAlign = TextAlign.End,
                )
                Text(
                    Dinheiro.formatar(it.totalCentavos),
                    style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = NUMEROS_TABULARES),
                    color = Cores.Tinta,
                    modifier = Modifier.width(128.dp),
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

@Composable
fun ConferirTela(viewModel: FechamentoViewModel, onVoltar: () -> Unit, onFechado: (Long) -> Unit) {
    val e by viewModel.estado.collectAsStateWithLifecycle()
    val modo by viewModel.modo.collectAsStateWithLifecycle()
    val contagem by viewModel.contador.contagem.collectAsStateWithLifecycle()
    val digitado by viewModel.digitado.collectAsStateWithLifecycle()
    val fechando by viewModel.fechando.collectAsStateWithLifecycle()
    var confirmar by rememberSaveable { mutableStateOf(false) }
    var confirmarPular by rememberSaveable { mutableStateOf(false) }

    val contado = viewModel.contado(contagem, digitado, modo)
    val diferenca = ContaCaixa.diferenca(contado, e.esperado)
    val comecou = contado > 0

    Tela(titulo = stringResource(R.string.conferir_titulo), onVoltar = onVoltar) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Visor {
                LinhaVisor(stringResource(R.string.abrir_troco_inicial), e.caixa?.trocoInicialCentavos ?: 0, cor = Color.White)
                LinhaVisor(stringResource(R.string.total_ganho), e.resumo?.totalGanhoCentavos ?: 0, cor = Color.White)
                LinhaVisor(stringResource(R.string.conferir_esperado), e.esperado, cor = Color.White)
                Box(Modifier.fillMaxWidth().padding(vertical = 6.dp).height(2.dp).background(Color.White.copy(alpha = 0.15f)))
                LinhaVisor(stringResource(R.string.conferir_contado), contado, grande = true)
            }
            if (comecou) {
                when (ContaCaixa.resultado(diferenca)) {
                    ResultadoConferencia.BATEU -> FaixaAviso(stringResource(R.string.conferir_bateu), TipoAviso.SUCESSO)
                    ResultadoConferencia.SOBROU -> FaixaAviso(stringResource(R.string.conferir_sobrou, Dinheiro.formatar(diferenca)), TipoAviso.INFO)
                    ResultadoConferencia.FALTOU -> FaixaAviso(stringResource(R.string.conferir_faltou, Dinheiro.formatar(-diferenca)), TipoAviso.ERRO)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AbaModo(stringResource(R.string.conferir_modo_contar), modo == ModoConferencia.CONTAR, Modifier.weight(1f)) { viewModel.modo.value = ModoConferencia.CONTAR }
                AbaModo(stringResource(R.string.conferir_modo_digitar), modo == ModoConferencia.DIGITAR, Modifier.weight(1f)) { viewModel.modo.value = ModoConferencia.DIGITAR }
            }
            if (modo == ModoConferencia.CONTAR) {
                BandejaPecas(
                    Dinheiro.emPecas(contagem),
                    textoVazio = stringResource(R.string.conferir_bandeja_vazia),
                    tamanho = 84.dp,
                    onTirar = viewModel.contador::tirar,
                )
                TecladoDinheiro(onToque = viewModel.contador::somar)
            } else {
                Text(
                    Dinheiro.formatar(digitado),
                    style = MaterialTheme.typography.displayMedium.copy(fontFeatureSettings = NUMEROS_TABULARES),
                    color = Cores.Tinta,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                TecladoNumerico(
                    onDigito = viewModel::digitar,
                    onApagar = viewModel::apagarDigito,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    extra = { m ->
                        Tecla(onClick = { viewModel.digitar('0'); viewModel.digitar('0') }, modifier = m, cor = Cores.Superficie, corConteudo = Cores.Tinta, borda = Cores.Linha) {
                            Text("00", style = MaterialTheme.typography.headlineLarge)
                        }
                    },
                )
            }
            TeclaTexto(
                stringResource(R.string.conferir_fechar),
                onClick = { confirmar = true },
                icone = R.drawable.ic_cadeado,
                cor = Cores.Verde,
                corConteudo = Color.White,
                habilitada = !fechando,
                alturaMinima = 84.dp,
                modifier = Modifier.fillMaxWidth(),
            )
            TeclaSecundaria(
                stringResource(R.string.fechar_pular),
                onClick = { confirmarPular = true },
                habilitada = !fechando,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            )
        }
    }
    if (confirmar) {
        DialogoConfirmacao(
            titulo = stringResource(R.string.fechar_confirmar_titulo),
            texto = stringResource(R.string.fechar_confirmar_texto_contado, Dinheiro.formatar(contado)),
            textoConfirmar = stringResource(R.string.fechar_confirmar),
            onConfirmar = { confirmar = false; viewModel.fechar(contado, onFechado) },
            onCancelar = { confirmar = false },
        )
    }
    if (confirmarPular) {
        DialogoConfirmacao(
            titulo = stringResource(R.string.fechar_confirmar_titulo),
            texto = stringResource(R.string.fechar_confirmar_texto),
            textoConfirmar = stringResource(R.string.fechar_confirmar),
            onConfirmar = { confirmarPular = false; viewModel.fechar(null, onFechado) },
            onCancelar = { confirmarPular = false },
        )
    }
}

@Composable
private fun AbaModo(texto: String, ativa: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Tecla(
        onClick = onClick,
        modifier = modifier.semantics { selected = ativa },
        cor = if (ativa) Cores.Tinta else Cores.Superficie,
        corConteudo = if (ativa) Color.White else Cores.Tinta,
        borda = if (ativa) null else Cores.Linha,
        alturaMinima = 60.dp,
    ) { Text(texto, style = MaterialTheme.typography.titleMedium) }
}
