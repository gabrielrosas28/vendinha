package br.com.santachiara.vendinha.ui.telas.organizador

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.santachiara.vendinha.AppContainer
import br.com.santachiara.vendinha.R
import br.com.santachiara.vendinha.data.Compartilhar
import br.com.santachiara.vendinha.data.RelatorioJson
import br.com.santachiara.vendinha.data.db.Caixa
import br.com.santachiara.vendinha.data.db.Fechamento
import br.com.santachiara.vendinha.data.db.Grupo
import br.com.santachiara.vendinha.data.db.StatusCaixa
import br.com.santachiara.vendinha.domain.Dinheiro
import br.com.santachiara.vendinha.domain.nomeCompletoGrupo
import br.com.santachiara.vendinha.ui.Formatos
import br.com.santachiara.vendinha.ui.componentes.DialogoConfirmacao
import br.com.santachiara.vendinha.ui.componentes.DialogoSenha
import br.com.santachiara.vendinha.ui.componentes.FaixaAviso
import br.com.santachiara.vendinha.ui.componentes.Tecla
import br.com.santachiara.vendinha.ui.componentes.TeclaSecundaria
import br.com.santachiara.vendinha.ui.componentes.TeclaTexto
import br.com.santachiara.vendinha.ui.componentes.Tela
import br.com.santachiara.vendinha.ui.componentes.TipoAviso
import br.com.santachiara.vendinha.ui.componentes.TituloSecao
import br.com.santachiara.vendinha.ui.tema.Cores
import br.com.santachiara.vendinha.ui.tema.NUMEROS_TABULARES
import br.com.santachiara.vendinha.ui.vm
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class OrganizadorEstado(
    val grupo: Grupo? = null,
    val caixa: Caixa? = null,
    val fechamentos: List<Fechamento> = emptyList(),
    val somAtivo: Boolean = true,
)

class OrganizadorViewModel(private val c: AppContainer) : ViewModel() {
    var liberado by mutableStateOf(false)

    val estado: StateFlow<OrganizadorEstado> = combine(
        c.grupos.observarAtivo(),
        c.caixa.observarCaixaAtual(),
        c.caixa.observarFechamentos(),
        c.config.observar(),
    ) { g, cx, f, cfg -> OrganizadorEstado(g, cx, f, cfg?.somAtivo ?: true) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OrganizadorEstado())

    suspend fun conferirSenha(s: String) = c.config.conferir(s)

    fun reabrir() {
        val cx = estado.value.caixa ?: return
        viewModelScope.launch { c.caixa.reabrir(cx.id) }
    }

    fun zerar(pronto: () -> Unit) {
        viewModelScope.launch {
            c.zerarTablet()
            pronto()
        }
    }

    fun apagarHistorico() {
        viewModelScope.launch { c.caixa.apagarHistorico() }
    }

    fun definirSom(ativo: Boolean) {
        viewModelScope.launch { c.config.definirSom(ativo) }
    }

    suspend fun gerarBackup(context: android.content.Context): java.io.File {
        val agora = System.currentTimeMillis()
        val json = RelatorioJson.backup(c.caixa.listarFechamentos(), agora, c.caixa.listarRegistro())
        return Compartilhar.salvarTexto(context, "vendinha_backup_${Formatos.arquivo(agora)}.json", json)
    }
}

private enum class Confirmacao { REABRIR, ZERAR, APAGAR_1, APAGAR_2 }

@Composable
fun OrganizadorTela(
    onVoltar: () -> Unit,
    onAbrirRelatorio: (Long) -> Unit,
    onZerado: () -> Unit,
    onTurmas: () -> Unit,
    onRegistro: () -> Unit,
) {
    val viewModel = vm { OrganizadorViewModel(it) }
    val e by viewModel.estado.collectAsStateWithLifecycle()
    var confirmar by rememberSaveable { mutableStateOf<Confirmacao?>(null) }
    var semApp by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val escopo = rememberCoroutineScope()
    val tituloBackup = stringResource(R.string.org_backup)

    if (!viewModel.liberado) {
        Tela(titulo = stringResource(R.string.org_titulo), onVoltar = onVoltar) {}
        DialogoSenha(
            titulo = stringResource(R.string.org_titulo),
            explicacao = stringResource(R.string.org_senha_explicacao),
            onCancelar = onVoltar,
            verificar = viewModel::conferirSenha,
            onSucesso = { viewModel.liberado = true },
        )
        return
    }

    Tela(titulo = stringResource(R.string.org_titulo), onVoltar = onVoltar) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { TituloSecao(stringResource(R.string.org_este_tablet)) }
            item {
                Column(
                    Modifier.fillMaxWidth().background(Cores.Superficie, RoundedCornerShape(22.dp)).padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val g = e.grupo
                    Text(
                        if (g == null) stringResource(R.string.org_sem_grupo) else nomeCompletoGrupo(g.nome, g.turma, g.numeroGrupo),
                        style = MaterialTheme.typography.titleLarge,
                        color = Cores.Tinta,
                    )
                    Text(
                        when (e.caixa?.status) {
                            null -> stringResource(R.string.org_caixa_nao_aberto)
                            StatusCaixa.ABERTO -> stringResource(R.string.org_caixa_aberto)
                            StatusCaixa.FECHADO -> stringResource(R.string.org_caixa_fechado)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = Cores.Apoio,
                    )
                }
            }
            if (e.caixa?.status == StatusCaixa.FECHADO) {
                item {
                    TeclaTexto(
                        stringResource(R.string.org_reabrir),
                        onClick = { confirmar = Confirmacao.REABRIR },
                        icone = R.drawable.ic_reabrir,
                        cor = Cores.Azul,
                        corConteudo = Color.White,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item {
                TeclaTexto(
                    stringResource(R.string.org_zerar),
                    onClick = { confirmar = Confirmacao.ZERAR },
                    icone = R.drawable.ic_lixeira,
                    cor = Cores.Erro,
                    corConteudo = Color.White,
                    habilitada = e.grupo != null,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Text(stringResource(R.string.org_zerar_ajuda), style = MaterialTheme.typography.bodyMedium, color = Cores.Apoio)
            }
            if (e.caixa?.status == StatusCaixa.ABERTO) {
                item { FaixaAviso(stringResource(R.string.org_zerar_aviso_aberto), TipoAviso.ALERTA) }
            }

            item { Spacer(Modifier.size(4.dp)); TituloSecao(stringResource(R.string.org_feira)) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TeclaTexto(
                        stringResource(R.string.turmas_titulo),
                        onClick = onTurmas,
                        icone = R.drawable.ic_pessoas,
                        cor = Cores.Grupos[3],
                        corConteudo = Color.White,
                        alturaMinima = 80.dp,
                        modifier = Modifier.weight(1f),
                    )
                    TeclaTexto(
                        stringResource(R.string.registro_titulo),
                        onClick = onRegistro,
                        icone = R.drawable.ic_recibo,
                        cor = Cores.Azul,
                        corConteudo = Color.White,
                        alturaMinima = 80.dp,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                val estadoSom = stringResource(if (e.somAtivo) R.string.ligado else R.string.desligado)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Cores.Superficie, RoundedCornerShape(22.dp))
                        .clickable(role = Role.Switch) { viewModel.definirSom(!e.somAtivo) }
                        .semantics { stateDescription = estadoSom }
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painterResource(if (e.somAtivo) R.drawable.ic_som_ligado else R.drawable.ic_som_desligado),
                        null,
                        tint = Cores.Tinta,
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(Modifier.width(14.dp))
                    Text(stringResource(R.string.org_som), style = MaterialTheme.typography.titleLarge, color = Cores.Tinta, modifier = Modifier.weight(1f))
                    Switch(
                        checked = e.somAtivo,
                        onCheckedChange = null,
                        colors = SwitchDefaults.colors(checkedTrackColor = Cores.Verde, checkedThumbColor = Color.White),
                    )
                }
            }

            item { Spacer(Modifier.size(4.dp)); TituloSecao(stringResource(R.string.org_relatorios)) }
            if (e.fechamentos.isEmpty()) {
                item { Text(stringResource(R.string.org_relatorios_vazio), style = MaterialTheme.typography.bodyLarge, color = Cores.Apoio) }
            }
            items(e.fechamentos, key = { it.id }) { f -> LinhaFechamento(f) { onAbrirRelatorio(f.id) } }
            if (semApp) item { FaixaAviso(stringResource(R.string.relatorio_sem_app), TipoAviso.ERRO) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TeclaSecundaria(
                        tituloBackup,
                        onClick = {
                            escopo.launch {
                                val arquivo = viewModel.gerarBackup(context)
                                semApp = !Compartilhar.arquivo(context, arquivo, "application/json", tituloBackup)
                            }
                        },
                        icone = R.drawable.ic_exportar,
                        habilitada = e.fechamentos.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                    )
                    TeclaSecundaria(
                        stringResource(R.string.org_apagar_relatorios),
                        onClick = { confirmar = Confirmacao.APAGAR_1 },
                        icone = R.drawable.ic_lixeira,
                        corTexto = Cores.Erro,
                        habilitada = e.fechamentos.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item { Spacer(Modifier.size(24.dp)) }
        }
    }

    when (confirmar) {
        Confirmacao.REABRIR -> DialogoConfirmacao(
            titulo = stringResource(R.string.org_reabrir),
            texto = stringResource(R.string.org_reabrir_texto),
            textoConfirmar = stringResource(R.string.org_reabrir_confirmar),
            onConfirmar = { confirmar = null; viewModel.reabrir() },
            onCancelar = { confirmar = null },
        )
        Confirmacao.ZERAR -> DialogoConfirmacao(
            titulo = stringResource(R.string.org_zerar_titulo),
            texto = stringResource(R.string.org_zerar_texto),
            textoConfirmar = stringResource(R.string.org_zerar_confirmar),
            perigoso = true,
            onConfirmar = { confirmar = null; viewModel.zerar(onZerado) },
            onCancelar = { confirmar = null },
        )
        Confirmacao.APAGAR_1 -> DialogoConfirmacao(
            titulo = stringResource(R.string.org_apagar_titulo),
            texto = stringResource(R.string.org_apagar_texto, e.fechamentos.size),
            textoConfirmar = stringResource(R.string.continuar),
            perigoso = true,
            onConfirmar = { confirmar = Confirmacao.APAGAR_2 },
            onCancelar = { confirmar = null },
        )
        Confirmacao.APAGAR_2 -> DialogoConfirmacao(
            titulo = stringResource(R.string.org_apagar_titulo2),
            texto = stringResource(R.string.org_apagar_texto2),
            textoConfirmar = stringResource(R.string.org_apagar_confirmar),
            perigoso = true,
            onConfirmar = { confirmar = null; viewModel.apagarHistorico() },
            onCancelar = { confirmar = null },
        )
        null -> Unit
    }
}

@Composable
private fun LinhaFechamento(f: Fechamento, onClick: () -> Unit) {
    Tecla(
        onClick = onClick,
        cor = Cores.Superficie,
        corConteudo = Cores.Tinta,
        borda = Cores.Linha,
        alturaMinima = 84.dp,
        forma = RoundedCornerShape(22.dp),
        recheio = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(f.nomeGrupo, style = MaterialTheme.typography.titleLarge)
            Text(
                (if (f.numeroGrupo > 0) stringResource(R.string.turma_grupo, f.turma, f.numeroGrupo) else f.turma) + " · " + Formatos.dataHora(f.data),
                style = MaterialTheme.typography.bodyMedium,
                color = Cores.Apoio,
            )
        }
        Text(
            Dinheiro.formatar(f.totalCentavos),
            style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = NUMEROS_TABULARES),
        )
    }
}
