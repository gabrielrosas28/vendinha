package br.com.santachiara.vendinha.ui.telas.inicio

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.santachiara.vendinha.R
import br.com.santachiara.vendinha.data.GrupoRepositorio
import br.com.santachiara.vendinha.domain.GrupoPredefinido
import br.com.santachiara.vendinha.data.TurmaRepositorio
import br.com.santachiara.vendinha.data.db.TurmaComGrupos
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import br.com.santachiara.vendinha.domain.TOTAL_CORES_GRUPO
import br.com.santachiara.vendinha.domain.nomeCompletoGrupo
import br.com.santachiara.vendinha.ui.componentes.CadeadoOrganizador
import br.com.santachiara.vendinha.ui.componentes.DialogoConfirmacao
import br.com.santachiara.vendinha.ui.componentes.Tecla
import br.com.santachiara.vendinha.ui.componentes.TeclaSecundaria
import br.com.santachiara.vendinha.ui.componentes.TeclaTexto
import br.com.santachiara.vendinha.ui.componentes.Tela
import br.com.santachiara.vendinha.ui.componentes.CampoTexto
import br.com.santachiara.vendinha.ui.componentes.JanelaDialogo
import br.com.santachiara.vendinha.ui.componentes.DialogoSairDoApp
import br.com.santachiara.vendinha.ui.tema.Cores
import br.com.santachiara.vendinha.ui.vm
import kotlinx.coroutines.launch

class InicioViewModel(private val grupos: GrupoRepositorio, turmas: TurmaRepositorio) : ViewModel() {
    val turmas: StateFlow<List<TurmaComGrupos>?> = turmas.observarTodas()
        .map<List<TurmaComGrupos>, List<TurmaComGrupos>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun iniciar(turma: String, numero: Int, nome: String, cor: Int, pronto: () -> Unit) {
        viewModelScope.launch {
            grupos.iniciar(turma, numero, nome, cor)
            pronto()
        }
    }
}

@Composable
fun IniciarGrupoTela(onIniciado: () -> Unit, onOrganizador: () -> Unit) {
    val viewModel = vm { InicioViewModel(it.grupos, it.turmas) }
    val turmas by viewModel.turmas.collectAsStateWithLifecycle()
    var turmaEscolhida by rememberSaveable { mutableStateOf<String?>(null) }
    val lista = turmas.orEmpty()
    val atual = lista.firstOrNull { it.turma.nome == turmaEscolhida } ?: lista.firstOrNull()
    var escolhido by rememberSaveable(stateSaver = GrupoSaver) { mutableStateOf<GrupoPredefinido?>(null) }
    var outro by rememberSaveable { mutableStateOf(false) }
    var sair by rememberSaveable { mutableStateOf(false) }

    BackHandler { sair = true }

    Tela(titulo = null, onVoltar = null) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 260.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Row(verticalAlignment = Alignment.Top) {
                        Image(
                            painterResource(R.drawable.logo_feirinha),
                            contentDescription = stringResource(R.string.logo_feirinha),
                            modifier = Modifier.weight(1f).widthIn(max = 520.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        CadeadoOrganizador(onOrganizador)
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        stringResource(R.string.inicio_titulo),
                        style = MaterialTheme.typography.displaySmall,
                        color = Cores.Tinta,
                        modifier = Modifier.semantics { heading() },
                    )
                    Spacer(Modifier.height(16.dp))
                    if (lista.isNotEmpty()) AbasTurma(lista.map { it.turma.nome }, atual?.turma?.nome) { turmaEscolhida = it }
                }
            }
            val grupos = atual?.gruposOrdenados.orEmpty().map { GrupoPredefinido(atual!!.turma.nome, it.numero, it.produto) }
            items(grupos, key = { "${it.turma}-${it.numero}" }) { g ->
                CardGrupo(g) { escolhido = g }
            }
            if (turmas != null && grupos.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        stringResource(if (lista.isEmpty()) R.string.inicio_sem_turmas else R.string.inicio_turma_sem_grupos),
                        style = MaterialTheme.typography.titleMedium,
                        color = Cores.Apoio,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                TeclaSecundaria(
                    stringResource(R.string.inicio_outro),
                    onClick = { outro = true },
                    icone = R.drawable.ic_editar,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
                )
            }
        }
    }

    escolhido?.let { g ->
        DialogoConfirmacao(
            titulo = stringResource(R.string.inicio_confirmar_titulo),
            texto = nomeCompletoGrupo(g.produto, g.turma, g.numero),
            textoConfirmar = stringResource(R.string.inicio_comecar),
            onConfirmar = {
                escolhido = null
                viewModel.iniciar(g.turma, g.numero, g.produto, g.corIndex, onIniciado)
            },
            onCancelar = { escolhido = null },
        )
    }
    if (outro) {
        DialogoOutroGrupo(
            onCancelar = { outro = false },
            onComecar = { t, nome, cor ->
                outro = false
                viewModel.iniciar(t, 0, nome, cor, onIniciado)
            },
        )
    }
    if (sair) DialogoSairDoApp(onFechar = { sair = false })
}

@Composable
private fun AbasTurma(turmas: List<String>, selecionada: String?, onEscolher: (String) -> Unit) {
    LazyRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(turmas, key = { it }) { t ->
            val ativa = t == selecionada
            Tecla(
                onClick = { onEscolher(t) },
                modifier = Modifier.width(120.dp).semantics { selected = ativa },
                cor = if (ativa) Cores.Tinta else Cores.Superficie,
                corConteudo = if (ativa) Color.White else Cores.Tinta,
                borda = if (ativa) null else Cores.Linha,
                alturaMinima = 60.dp,
                forma = RoundedCornerShape(18.dp),
            ) {
                Text(t.replace(" ", " "), maxLines = 1, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun CardGrupo(g: GrupoPredefinido, onClick: () -> Unit) {
    val cor = Cores.grupo(g.corIndex)
    val texto = Cores.sobre(cor)
    val rotuloGrupo = stringResource(R.string.grupo_numero, g.numero)
    Tecla(
        onClick = onClick,
        cor = cor,
        corConteudo = texto,
        alturaMinima = 132.dp,
        forma = RoundedCornerShape(24.dp),
        beico = 6.dp,
        descricao = "${g.produto}, $rotuloGrupo",
    ) {
        Box(
            Modifier
                .size(64.dp)
                .background(texto.copy(alpha = 0.16f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(g.numero.toString(), style = MaterialTheme.typography.displaySmall, color = texto)
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(rotuloGrupo, style = MaterialTheme.typography.titleSmall, color = texto.copy(alpha = 0.85f), textAlign = TextAlign.Start)
            Text(g.produto, style = MaterialTheme.typography.headlineSmall, color = texto, textAlign = TextAlign.Start)
        }
    }
}

@Composable
private fun DialogoOutroGrupo(onCancelar: () -> Unit, onComecar: (String, String, Int) -> Unit) {
    var turma by rememberSaveable { mutableStateOf("") }
    var nome by rememberSaveable { mutableStateOf("") }
    var cor by rememberSaveable { mutableStateOf(0) }
    JanelaDialogo(onFechar = onCancelar) {
        Text(stringResource(R.string.inicio_outro_titulo), style = MaterialTheme.typography.headlineSmall, color = Cores.Tinta)
        Spacer(Modifier.height(16.dp))
        CampoTexto(
            valor = turma,
            onMudar = { turma = it.take(20) },
            rotulo = stringResource(R.string.inicio_outro_turma),
            exemplo = stringResource(R.string.inicio_outro_turma_exemplo),
            teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
        Spacer(Modifier.height(12.dp))
        CampoTexto(
            valor = nome,
            onMudar = { nome = it.take(40) },
            rotulo = stringResource(R.string.inicio_outro_nome),
            exemplo = stringResource(R.string.inicio_outro_nome_exemplo),
            teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.inicio_outro_cor), style = MaterialTheme.typography.titleSmall, color = Cores.Tinta)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(TOTAL_CORES_GRUPO) { i ->
                val c = Cores.grupo(i)
                val nomeCor = stringResource(NOMES_CORES[i])
                Box(
                    Modifier
                        .size(56.dp)
                        .background(c, CircleShape)
                        .then(if (i == cor) Modifier.border(4.dp, Cores.Tinta, CircleShape) else Modifier)
                        .clickable(role = Role.RadioButton) { cor = i }
                        .semantics { contentDescription = nomeCor; selected = i == cor },
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TeclaSecundaria(stringResource(R.string.cancelar), onCancelar, Modifier.weight(1f))
            TeclaTexto(
                stringResource(R.string.inicio_comecar),
                onClick = { onComecar(turma.trim(), nome.trim(), cor) },
                modifier = Modifier.weight(1f),
                cor = Cores.Verde,
                corConteudo = Color.White,
                habilitada = turma.isNotBlank() && nome.isNotBlank(),
            )
        }
    }
}

private val NOMES_CORES = listOf(R.string.cor_laranja, R.string.cor_azul, R.string.cor_verde, R.string.cor_roxo, R.string.cor_rosa)

private val GrupoSaver = androidx.compose.runtime.saveable.Saver<GrupoPredefinido?, List<Any>>(
    save = { g -> g?.let { listOf(it.turma, it.numero, it.produto) } ?: emptyList() },
    restore = { l -> if (l.size == 3) GrupoPredefinido(l[0] as String, l[1] as Int, l[2] as String) else null },
)
