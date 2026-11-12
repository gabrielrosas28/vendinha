package br.com.santachiara.vendinha.ui.telas.organizador

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.santachiara.vendinha.R
import br.com.santachiara.vendinha.data.ResultadoTurma
import br.com.santachiara.vendinha.data.TurmaRepositorio
import br.com.santachiara.vendinha.data.db.GrupoCatalogo
import br.com.santachiara.vendinha.data.db.TurmaComGrupos
import br.com.santachiara.vendinha.domain.corDoNumero
import br.com.santachiara.vendinha.ui.componentes.CampoTexto
import br.com.santachiara.vendinha.ui.componentes.DialogoConfirmacao
import br.com.santachiara.vendinha.ui.componentes.JanelaDialogo
import br.com.santachiara.vendinha.ui.componentes.Tecla
import br.com.santachiara.vendinha.ui.componentes.TeclaSecundaria
import br.com.santachiara.vendinha.ui.componentes.TeclaTexto
import br.com.santachiara.vendinha.ui.componentes.Tela
import br.com.santachiara.vendinha.ui.tema.Cores
import br.com.santachiara.vendinha.ui.vm
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TurmasViewModel(private val repo: TurmaRepositorio) : ViewModel() {
    val turmas: StateFlow<List<TurmaComGrupos>?> = repo.observarTodas()
        .map<List<TurmaComGrupos>, List<TurmaComGrupos>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    suspend fun criar(nome: String) = repo.criar(nome)
}

@Composable
fun TurmasTela(onVoltar: () -> Unit, onAbrirTurma: (Long) -> Unit) {
    val viewModel = vm { TurmasViewModel(it.turmas) }
    val turmas by viewModel.turmas.collectAsStateWithLifecycle()
    var nova by rememberSaveable { mutableStateOf(false) }

    Tela(titulo = stringResource(R.string.turmas_titulo), onVoltar = onVoltar) {
        val lista = turmas ?: return@Tela
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                TeclaTexto(
                    stringResource(R.string.turmas_nova),
                    onClick = { nova = true },
                    icone = R.drawable.ic_mais,
                    cor = Cores.Verde,
                    corConteudo = Color.White,
                    alturaMinima = 76.dp,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Text(stringResource(R.string.turmas_ajuda), style = MaterialTheme.typography.bodyMedium, color = Cores.Apoio)
            }
            if (lista.isEmpty()) {
                item { Text(stringResource(R.string.turmas_vazio), style = MaterialTheme.typography.titleMedium, color = Cores.Apoio) }
            }
            items(lista, key = { it.turma.id }) { t ->
                Tecla(
                    onClick = { onAbrirTurma(t.turma.id) },
                    cor = Cores.Superficie,
                    corConteudo = Cores.Tinta,
                    borda = Cores.Linha,
                    alturaMinima = 84.dp,
                    forma = RoundedCornerShape(22.dp),
                    recheio = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                ) {
                    Text(t.turma.nome, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.width(120.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            pluralStringResource(R.plurals.turmas_n_grupos, t.grupos.size, t.grupos.size),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            t.gruposOrdenados.joinToString(" · ") { it.produto },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Cores.Apoio,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Icon(painterResource(R.drawable.ic_editar), stringResource(R.string.editar), tint = Cores.Apoio, modifier = Modifier.size(26.dp))
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (nova) {
        DialogoNome(
            titulo = stringResource(R.string.turmas_nova),
            rotulo = stringResource(R.string.turma_nome),
            exemplo = stringResource(R.string.turma_nome_exemplo),
            inicial = "",
            textoSalvar = stringResource(R.string.turmas_criar),
            onCancelar = { nova = false },
            salvar = { nome ->
                when (viewModel.criar(nome)) {
                    ResultadoTurma.OK -> { nova = false; null }
                    ResultadoTurma.JA_EXISTE -> R.string.turma_erro_existe
                    ResultadoTurma.NOME_INVALIDO -> R.string.turma_erro_nome
                }
            },
        )
    }
}

class TurmaViewModel(private val id: Long, private val repo: TurmaRepositorio) : ViewModel() {
    val turma: StateFlow<TurmaComGrupos?> = repo.observar(id)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    suspend fun renomear(nome: String) = repo.renomear(id, nome)
    suspend fun adicionarGrupo(produto: String) = repo.adicionarGrupo(id, produto)
    suspend fun renomearGrupo(grupoId: Long, produto: String) = repo.renomearGrupo(grupoId, produto)
    fun apagarGrupo(grupoId: Long) { viewModelScope.launch { repo.apagarGrupo(grupoId) } }
    fun apagar() {
        viewModelScope.launch { repo.apagar(id) }
    }
}

private sealed interface Edicao {
    data object RenomearTurma : Edicao
    data object ApagarTurma : Edicao
    data object NovoGrupo : Edicao
    data class Grupo(val grupo: GrupoCatalogo) : Edicao
    data class ApagarGrupo(val grupo: GrupoCatalogo) : Edicao
}

@Composable
fun TurmaTela(turmaId: Long, onVoltar: () -> Unit) {
    val viewModel = vm(chave = "turma-$turmaId") { TurmaViewModel(turmaId, it.turmas) }
    val t by viewModel.turma.collectAsStateWithLifecycle()
    var edicao by remember { mutableStateOf<Edicao?>(null) }
    var carregou by remember { mutableStateOf(false) }
    LaunchedEffect(t) { if (t != null) carregou = true }
    LaunchedEffect(t, carregou) { if (carregou && t == null) onVoltar() }

    Tela(titulo = t?.turma?.nome ?: "", onVoltar = onVoltar) {
        val turma = t ?: return@Tela
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TeclaSecundaria(
                        stringResource(R.string.turma_renomear),
                        onClick = { edicao = Edicao.RenomearTurma },
                        icone = R.drawable.ic_editar,
                        modifier = Modifier.weight(1f),
                    )
                    TeclaSecundaria(
                        stringResource(R.string.turma_apagar),
                        onClick = { edicao = Edicao.ApagarTurma },
                        icone = R.drawable.ic_lixeira,
                        corTexto = Cores.Erro,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                Text(stringResource(R.string.turma_grupos), style = MaterialTheme.typography.titleLarge, color = Cores.Tinta)
            }
            if (turma.grupos.isEmpty()) {
                item { Text(stringResource(R.string.inicio_turma_sem_grupos), style = MaterialTheme.typography.bodyLarge, color = Cores.Apoio) }
            }
            items(turma.gruposOrdenados, key = { it.id }) { g ->
                val cor = Cores.grupo(corDoNumero(g.numero))
                Tecla(
                    onClick = { edicao = Edicao.Grupo(g) },
                    cor = Cores.Superficie,
                    corConteudo = Cores.Tinta,
                    borda = Cores.Linha,
                    alturaMinima = 84.dp,
                    forma = RoundedCornerShape(22.dp),
                    recheio = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Box(Modifier.size(52.dp).background(cor, CircleShape), contentAlignment = Alignment.Center) {
                        Text(g.numero.toString(), style = MaterialTheme.typography.headlineSmall, color = Cores.sobre(cor))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.grupo_numero, g.numero), style = MaterialTheme.typography.bodyMedium, color = Cores.Apoio)
                        Text(g.produto, style = MaterialTheme.typography.titleLarge)
                    }
                    Icon(painterResource(R.drawable.ic_editar), stringResource(R.string.editar), tint = Cores.Apoio, modifier = Modifier.size(26.dp))
                }
            }
            item {
                TeclaTexto(
                    stringResource(R.string.turma_novo_grupo),
                    onClick = { edicao = Edicao.NovoGrupo },
                    icone = R.drawable.ic_mais,
                    cor = Cores.Verde,
                    corConteudo = Color.White,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    val turma = t ?: return
    when (val e = edicao) {
        Edicao.RenomearTurma -> DialogoNome(
            titulo = stringResource(R.string.turma_renomear),
            rotulo = stringResource(R.string.turma_nome),
            exemplo = stringResource(R.string.turma_nome_exemplo),
            inicial = turma.turma.nome,
            textoSalvar = stringResource(R.string.salvar),
            onCancelar = { edicao = null },
            salvar = { nome ->
                when (viewModel.renomear(nome)) {
                    ResultadoTurma.OK -> { edicao = null; null }
                    ResultadoTurma.JA_EXISTE -> R.string.turma_erro_existe
                    ResultadoTurma.NOME_INVALIDO -> R.string.turma_erro_nome
                }
            },
        )
        Edicao.ApagarTurma -> DialogoConfirmacao(
            titulo = stringResource(R.string.turma_apagar_titulo, turma.turma.nome),
            texto = stringResource(R.string.turma_apagar_texto),
            textoConfirmar = stringResource(R.string.turma_apagar),
            perigoso = true,
            onConfirmar = { edicao = null; viewModel.apagar() },
            onCancelar = { edicao = null },
        )
        Edicao.NovoGrupo -> DialogoNome(
            titulo = stringResource(R.string.turma_novo_grupo_titulo, (turma.grupos.maxOfOrNull { it.numero } ?: 0) + 1),
            rotulo = stringResource(R.string.turma_grupo_produto),
            exemplo = stringResource(R.string.turma_grupo_produto_exemplo),
            inicial = "",
            textoSalvar = stringResource(R.string.turma_adicionar),
            onCancelar = { edicao = null },
            salvar = { nome -> if (viewModel.adicionarGrupo(nome)) { edicao = null; null } else R.string.turma_grupo_erro },
        )
        is Edicao.Grupo -> DialogoNome(
            titulo = stringResource(R.string.grupo_numero, e.grupo.numero),
            rotulo = stringResource(R.string.turma_grupo_produto),
            exemplo = stringResource(R.string.turma_grupo_produto_exemplo),
            inicial = e.grupo.produto,
            textoSalvar = stringResource(R.string.salvar),
            onCancelar = { edicao = null },
            salvar = { nome -> if (viewModel.renomearGrupo(e.grupo.id, nome)) { edicao = null; null } else R.string.turma_grupo_erro },
            textoApagar = stringResource(R.string.turma_apagar_grupo),
            onApagar = { edicao = Edicao.ApagarGrupo(e.grupo) },
        )
        is Edicao.ApagarGrupo -> DialogoConfirmacao(
            titulo = stringResource(R.string.turma_apagar_grupo_titulo, e.grupo.numero, e.grupo.produto),
            texto = stringResource(R.string.turma_apagar_grupo_texto),
            textoConfirmar = stringResource(R.string.turma_apagar_grupo),
            perigoso = true,
            onConfirmar = { edicao = null; viewModel.apagarGrupo(e.grupo.id) },
            onCancelar = { edicao = null },
        )
        null -> Unit
    }
}

@Composable
private fun DialogoNome(
    titulo: String,
    rotulo: String,
    exemplo: String,
    inicial: String,
    textoSalvar: String,
    onCancelar: () -> Unit,
    salvar: suspend (String) -> Int?,
    textoApagar: String? = null,
    onApagar: (() -> Unit)? = null,
) {
    var nome by rememberSaveable { mutableStateOf(inicial) }
    var erro by remember { mutableStateOf<Int?>(null) }
    var salvando by remember { mutableStateOf(false) }
    val escopo = rememberCoroutineScope()
    JanelaDialogo(onFechar = onCancelar) {
        Text(titulo, style = MaterialTheme.typography.headlineSmall, color = Cores.Tinta)
        Spacer(Modifier.height(16.dp))
        CampoTexto(
            valor = nome,
            onMudar = { nome = it.take(40); erro = null },
            rotulo = rotulo,
            exemplo = exemplo,
            erro = erro?.let { stringResource(it) },
            teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TeclaSecundaria(stringResource(R.string.cancelar), onCancelar, Modifier.weight(1f))
            TeclaTexto(
                textoSalvar,
                onClick = {
                    salvando = true
                    escopo.launch {
                        erro = salvar(nome)
                        salvando = false
                    }
                },
                modifier = Modifier.weight(1f),
                cor = Cores.Verde,
                corConteudo = Color.White,
                habilitada = nome.isNotBlank() && !salvando,
            )
        }
        if (textoApagar != null && onApagar != null) {
            Spacer(Modifier.height(12.dp))
            TeclaSecundaria(textoApagar, onApagar, Modifier.fillMaxWidth(), icone = R.drawable.ic_lixeira, corTexto = Cores.Erro)
        }
    }
}
