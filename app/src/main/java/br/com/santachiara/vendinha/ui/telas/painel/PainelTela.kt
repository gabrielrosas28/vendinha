package br.com.santachiara.vendinha.ui.telas.painel

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.santachiara.vendinha.AppContainer
import br.com.santachiara.vendinha.R
import br.com.santachiara.vendinha.data.db.Caixa
import br.com.santachiara.vendinha.data.db.Grupo
import br.com.santachiara.vendinha.data.db.StatusCaixa
import br.com.santachiara.vendinha.domain.ContaCaixa
import br.com.santachiara.vendinha.domain.RegrasEstoque
import br.com.santachiara.vendinha.domain.SituacaoEstoque
import br.com.santachiara.vendinha.domain.ValorVenda
import br.com.santachiara.vendinha.ui.componentes.CadeadoOrganizador
import br.com.santachiara.vendinha.ui.componentes.CampoTexto
import br.com.santachiara.vendinha.ui.componentes.DialogoSairDoApp
import br.com.santachiara.vendinha.ui.componentes.FaixaAviso
import br.com.santachiara.vendinha.ui.componentes.JanelaDialogo
import br.com.santachiara.vendinha.ui.componentes.LinhaVisor
import br.com.santachiara.vendinha.ui.componentes.Tecla
import br.com.santachiara.vendinha.ui.componentes.TeclaIcone
import br.com.santachiara.vendinha.ui.componentes.TeclaSecundaria
import br.com.santachiara.vendinha.ui.componentes.TeclaTexto
import br.com.santachiara.vendinha.ui.componentes.Tela
import br.com.santachiara.vendinha.ui.componentes.TipoAviso
import br.com.santachiara.vendinha.ui.componentes.Visor
import br.com.santachiara.vendinha.ui.tema.Cores
import br.com.santachiara.vendinha.ui.vm
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AlertaEstoque(val nome: String, val situacao: SituacaoEstoque, val restam: Int)

data class PainelEstado(
    val carregado: Boolean = false,
    val grupo: Grupo? = null,
    val caixa: Caixa? = null,
    val totalGanho: Long = 0,
    val numVendas: Int = 0,
    val itensAtivos: Int = 0,
    val alertas: List<AlertaEstoque> = emptyList(),
)

class PainelViewModel(private val c: AppContainer) : ViewModel() {
    val estado: StateFlow<PainelEstado> = combine(
        c.grupos.observarAtivo(),
        c.caixa.observarCaixaAtual(),
        c.caixa.observarVendasAtuais(),
        c.produtos.observarAtivos(),
    ) { grupo, caixa, vendas, itens ->
        val valores = vendas.map { ValorVenda(it.venda.totalCentavos, it.venda.estornada) }
        PainelEstado(
            carregado = true,
            grupo = grupo,
            caixa = caixa,
            totalGanho = ContaCaixa.totalGanho(valores),
            numVendas = ContaCaixa.numeroVendasValidas(valores),
            itensAtivos = itens.size,
            alertas = itens.mapNotNull { i ->
                when (val s = RegrasEstoque.situacao(i.controlaEstoque, i.qtdDisponivel)) {
                    SituacaoEstoque.ACABANDO, SituacaoEstoque.ESGOTADO -> AlertaEstoque(i.nome, s, i.qtdDisponivel ?: 0)
                    else -> null
                }
            }.sortedBy { it.restam },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PainelEstado())

    fun abrirRelatorio(abrir: (Long) -> Unit) {
        val cx = estado.value.caixa ?: return
        viewModelScope.launch { c.caixa.fechamentoDoCaixa(cx.id)?.let(abrir) }
    }

    fun renomear(nome: String) {
        val g = estado.value.grupo ?: return
        if (nome.isBlank()) return
        viewModelScope.launch { c.grupos.renomear(g.id, nome) }
    }
}

@Composable
fun PainelTela(
    onVender: () -> Unit,
    onAbrirCaixa: () -> Unit,
    onEstoque: () -> Unit,
    onHistorico: () -> Unit,
    onProdutos: () -> Unit,
    onFecharCaixa: () -> Unit,
    onRelatorio: (Long) -> Unit,
    onOrganizador: () -> Unit,
    onSemGrupo: () -> Unit,
) {
    val viewModel = vm { PainelViewModel(it) }
    val e by viewModel.estado.collectAsStateWithLifecycle()
    var sair by rememberSaveable { mutableStateOf(false) }
    var editarNome by rememberSaveable { mutableStateOf(false) }

    BackHandler { sair = true }
    if (e.carregado && e.grupo == null) {
        androidx.compose.runtime.LaunchedEffect(Unit) { onSemGrupo() }
        return
    }
    val grupo = e.grupo ?: run { Box(Modifier.fillMaxSize().background(Cores.Fundo)); return }
    val fechado = e.caixa?.status == StatusCaixa.FECHADO
    val corGrupo = Cores.grupo(grupo.corIndex)

    Tela(titulo = null, onVoltar = null) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(64.dp).background(corGrupo, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (grupo.numeroGrupo > 0) grupo.numeroGrupo.toString() else grupo.nome.take(1).uppercase(),
                        style = MaterialTheme.typography.headlineLarge,
                        color = Cores.sobre(corGrupo),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        grupo.nome,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Cores.Tinta,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        if (grupo.numeroGrupo > 0) stringResource(R.string.turma_grupo, grupo.turma, grupo.numeroGrupo) else grupo.turma,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Cores.Apoio,
                    )
                }
                TeclaIcone(R.drawable.ic_editar, stringResource(R.string.painel_editar_nome), { editarNome = true }, tamanho = 52.dp)
                Spacer(Modifier.width(4.dp))
                CadeadoOrganizador(onOrganizador)
            }

            Visor {
                LinhaVisor(stringResource(R.string.painel_vendido_hoje), e.totalGanho, grande = true)
                Text(
                    pluralStringResource(R.plurals.vendas, e.numVendas, e.numVendas),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Cores.VisorRotulo,
                )
            }

            e.alertas.take(3).forEach { a ->
                FaixaAviso(
                    if (a.situacao == SituacaoEstoque.ESGOTADO) stringResource(R.string.alerta_esgotou, a.nome)
                    else pluralStringResource(R.plurals.alerta_acabando, a.restam, a.nome, a.restam),
                    if (a.situacao == SituacaoEstoque.ESGOTADO) TipoAviso.ERRO else TipoAviso.ALERTA,
                )
            }
            if (e.alertas.size > 3) {
                Text(
                    stringResource(R.string.alerta_mais, e.alertas.size - 3),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Cores.AlertaTexto,
                )
            }

            val semItens = e.itensAtivos == 0
            Tecla(
                onClick = when {
                    semItens -> onProdutos
                    e.caixa == null -> onAbrirCaixa
                    else -> onVender
                },
                habilitada = !fechado,
                alturaMinima = 168.dp,
                forma = RoundedCornerShape(28.dp),
                beico = 7.dp,
            ) {
                Icon(painterResource(R.drawable.ic_carrinho), null, Modifier.size(52.dp))
                Spacer(Modifier.width(16.dp))
                Text(stringResource(R.string.painel_vender), style = MaterialTheme.typography.displayMedium)
            }
            val dica = when {
                fechado -> stringResource(R.string.painel_dica_fechado)
                semItens -> stringResource(R.string.painel_dica_sem_itens)
                e.caixa == null -> stringResource(R.string.painel_dica_abrir)
                else -> null
            }
            if (dica != null) {
                Text(dica, style = MaterialTheme.typography.bodyLarge, color = Cores.Apoio, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Atalho(R.drawable.ic_estoque, stringResource(R.string.painel_estoque), Cores.Azul, onEstoque, Modifier.weight(1f))
                Atalho(R.drawable.ic_historico, stringResource(R.string.painel_historico), Cores.Grupos[3], onHistorico, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Atalho(R.drawable.ic_etiqueta, stringResource(R.string.painel_produtos), Cores.Verde, onProdutos, Modifier.weight(1f))
                if (fechado) {
                    Atalho(R.drawable.ic_recibo, stringResource(R.string.painel_relatorio), Cores.Tinta, { viewModel.abrirRelatorio(onRelatorio) }, Modifier.weight(1f))
                } else {
                    Atalho(
                        R.drawable.ic_cadeado, stringResource(R.string.painel_fechar_caixa), Cores.Erro, onFecharCaixa,
                        Modifier.weight(1f), habilitado = e.caixa != null,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (sair) DialogoSairDoApp(onFechar = { sair = false })
    if (editarNome) {
        var nome by rememberSaveable { mutableStateOf(grupo.nome) }
        JanelaDialogo(onFechar = { editarNome = false }) {
            Text(stringResource(R.string.painel_editar_nome), style = MaterialTheme.typography.headlineSmall, color = Cores.Tinta)
            Spacer(Modifier.height(16.dp))
            CampoTexto(
                valor = nome,
                onMudar = { nome = it.take(40) },
                rotulo = stringResource(R.string.painel_nome_grupo),
                teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TeclaSecundaria(stringResource(R.string.cancelar), { editarNome = false }, Modifier.weight(1f))
                TeclaTexto(
                    stringResource(R.string.salvar),
                    onClick = { viewModel.renomear(nome); editarNome = false },
                    modifier = Modifier.weight(1f),
                    cor = Cores.Verde,
                    corConteudo = Color.White,
                    habilitada = nome.isNotBlank(),
                )
            }
        }
    }
}

@Composable
private fun Atalho(
    @DrawableRes icone: Int,
    texto: String,
    corIcone: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
) {
    Tecla(
        onClick = onClick,
        modifier = modifier,
        cor = Cores.Superficie,
        corConteudo = Cores.Tinta,
        borda = Cores.Linha,
        habilitada = habilitado,
        alturaMinima = 132.dp,
        forma = RoundedCornerShape(24.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(painterResource(icone), null, tint = if (habilitado) corIcone else Cores.TeclaApagadaTexto, modifier = Modifier.size(36.dp))
            Spacer(Modifier.height(6.dp))
            Text(texto, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        }
    }
}
