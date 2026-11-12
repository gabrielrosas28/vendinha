package br.com.santachiara.vendinha.ui.telas.organizador

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.santachiara.vendinha.AppContainer
import br.com.santachiara.vendinha.R
import br.com.santachiara.vendinha.data.db.RegistroVenda
import br.com.santachiara.vendinha.domain.Dinheiro
import br.com.santachiara.vendinha.domain.Registro
import br.com.santachiara.vendinha.domain.ValorRegistro
import br.com.santachiara.vendinha.domain.nomeCompletoGrupo
import br.com.santachiara.vendinha.ui.Formatos
import br.com.santachiara.vendinha.ui.componentes.LinhaVisor
import br.com.santachiara.vendinha.ui.componentes.Tecla
import br.com.santachiara.vendinha.ui.componentes.Tela
import br.com.santachiara.vendinha.ui.componentes.Visor
import br.com.santachiara.vendinha.ui.tema.Cores
import br.com.santachiara.vendinha.ui.tema.NUMEROS_TABULARES
import br.com.santachiara.vendinha.ui.telas.estoque.Selo
import br.com.santachiara.vendinha.ui.vm
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class RegistroViewModel(c: AppContainer) : ViewModel() {
    val registro: StateFlow<List<RegistroVenda>?> = c.caixa.observarRegistro()
        .map<List<RegistroVenda>, List<RegistroVenda>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

private fun RegistroVenda.chaveGrupo() = "$turma|$numeroGrupo|$nomeGrupo"
private fun RegistroVenda.rotuloGrupo() = nomeCompletoGrupo(nomeGrupo, turma, numeroGrupo)

@Composable
fun RegistroVendasTela(onVoltar: () -> Unit) {
    val viewModel = vm { RegistroViewModel(it) }
    val registro by viewModel.registro.collectAsStateWithLifecycle()
    var filtro by rememberSaveable { mutableStateOf<String?>(null) }

    Tela(titulo = stringResource(R.string.registro_titulo), onVoltar = onVoltar) {
        val todos = registro ?: return@Tela
        val grupos = remember(todos) { todos.distinctBy { it.chaveGrupo() } }
        val lista = if (filtro == null) todos else todos.filter { it.chaveGrupo() == filtro }
        val totais = remember(lista) {
            Registro.totais(lista.map { ValorRegistro(it.totalCentavos, it.recebidoCentavos, it.trocoCentavos, it.estornada) })
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (grupos.size > 1) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { Filtro(stringResource(R.string.registro_todos), filtro == null) { filtro = null } }
                        items(grupos, key = { it.chaveGrupo() }) { r ->
                            Filtro(r.rotuloGrupo(), filtro == r.chaveGrupo()) { filtro = r.chaveGrupo() }
                        }
                    }
                }
            }
            item {
                Visor {
                    LinhaVisor(stringResource(R.string.registro_vendido), totais.vendidoCentavos, grande = true, animar = false)
                    LinhaVisor(stringResource(R.string.registro_recebido_total), totais.recebidoCentavos, cor = Color.White)
                    LinhaVisor(stringResource(R.string.registro_troco_total), totais.trocoCentavos, cor = Color.White)
                    Text(
                        pluralStringResource(R.plurals.registro_compras, totais.numeroVendas, totais.numeroVendas) +
                            if (totais.numeroEstornadas > 0) " · " + pluralStringResource(R.plurals.registro_estornadas, totais.numeroEstornadas, totais.numeroEstornadas) else "",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Cores.VisorRotulo,
                    )
                }
            }
            if (lista.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.registro_vazio),
                        style = MaterialTheme.typography.titleMedium,
                        color = Cores.Apoio,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                    )
                }
            }
            items(lista, key = { it.id }) { r -> CartaoRegistro(r, mostrarGrupo = filtro == null) }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun Filtro(texto: String, ativo: Boolean, onClick: () -> Unit) {
    Tecla(
        onClick = onClick,
        modifier = Modifier.semantics { selected = ativo },
        cor = if (ativo) Cores.Tinta else Cores.Superficie,
        corConteudo = if (ativo) Color.White else Cores.Tinta,
        borda = if (ativo) null else Cores.Linha,
        alturaMinima = 52.dp,
        forma = RoundedCornerShape(16.dp),
    ) { Text(texto, style = MaterialTheme.typography.titleSmall, maxLines = 1) }
}

@Composable
private fun CartaoRegistro(r: RegistroVenda, mostrarGrupo: Boolean) {
    val riscado = if (r.estornada) TextDecoration.LineThrough else null
    Column(
        Modifier
            .fillMaxWidth()
            .background(Cores.Superficie, RoundedCornerShape(22.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                Formatos.dataHora(r.dataHora),
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = NUMEROS_TABULARES),
                color = Cores.Apoio,
                modifier = Modifier.weight(1f),
            )
            if (r.estornada) Selo(stringResource(R.string.estornada), Cores.Erro, Cores.ErroFundo)
        }
        if (mostrarGrupo) {
            Text(r.rotuloGrupo(), style = MaterialTheme.typography.bodyMedium, color = Cores.AzulTexto, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(r.itens, style = MaterialTheme.typography.titleMedium, color = Cores.Tinta, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Valor(stringResource(R.string.total), r.totalCentavos, Cores.Tinta, riscado, Modifier.weight(1f))
            Valor(stringResource(R.string.recebido), r.recebidoCentavos, Cores.AzulTexto, riscado, Modifier.weight(1f))
            Valor(stringResource(R.string.troco), r.trocoCentavos, Cores.VerdeTexto, riscado, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Valor(rotulo: String, centavos: Long, cor: Color, riscado: TextDecoration?, modifier: Modifier) {
    Column(
        modifier
            .background(Cores.Fundo, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .widthIn(min = 80.dp),
    ) {
        Text(rotulo, style = MaterialTheme.typography.bodyMedium, color = Cores.Apoio)
        Text(
            Dinheiro.formatar(centavos),
            style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = NUMEROS_TABULARES, textDecoration = riscado),
            color = if (riscado != null) Cores.Apoio else cor,
            maxLines = 1,
        )
    }
}
