package br.com.santachiara.vendinha.ui.telas.caixa

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.santachiara.vendinha.R
import br.com.santachiara.vendinha.data.CaixaRepositorio
import br.com.santachiara.vendinha.domain.Denominacao
import br.com.santachiara.vendinha.domain.Dinheiro
import br.com.santachiara.vendinha.ui.componentes.BandejaPecas
import br.com.santachiara.vendinha.ui.componentes.LinhaVisor
import br.com.santachiara.vendinha.ui.componentes.TeclaSecundaria
import br.com.santachiara.vendinha.ui.componentes.TeclaTexto
import br.com.santachiara.vendinha.ui.componentes.TecladoDinheiro
import br.com.santachiara.vendinha.ui.componentes.Tela
import br.com.santachiara.vendinha.ui.componentes.Visor
import br.com.santachiara.vendinha.ui.tema.Cores
import br.com.santachiara.vendinha.ui.vm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ContadorDinheiro {
    private val _contagem = MutableStateFlow<Map<Denominacao, Int>>(emptyMap())
    val contagem: StateFlow<Map<Denominacao, Int>> = _contagem

    val total: Long get() = Dinheiro.somar(_contagem.value)

    fun somar(d: Denominacao) = _contagem.update { it + (d to ((it[d] ?: 0) + 1)) }

    fun tirar(d: Denominacao) = _contagem.update { m ->
        val q = (m[d] ?: 0) - 1
        if (q <= 0) m - d else m + (d to q)
    }

    fun definir(m: Map<Denominacao, Int>) { _contagem.value = m }
    fun limpar() { _contagem.value = emptyMap() }
}

class AbrirCaixaViewModel(private val caixa: CaixaRepositorio) : ViewModel() {
    val contador = ContadorDinheiro()
    private var abrindo = false

    fun abrir(troco: Long, pronto: () -> Unit) {
        if (abrindo) return
        abrindo = true
        viewModelScope.launch {
            caixa.abrir(troco)
            pronto()
        }
    }
}

@Composable
fun AbrirCaixaTela(onVoltar: () -> Unit, onAberto: () -> Unit) {
    val viewModel = vm { AbrirCaixaViewModel(it.caixa) }
    val contagem by viewModel.contador.contagem.collectAsStateWithLifecycle()
    val total = Dinheiro.somar(contagem)

    Tela(titulo = stringResource(R.string.abrir_titulo), onVoltar = onVoltar) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.abrir_pergunta), style = MaterialTheme.typography.titleLarge, color = Cores.Tinta)
            Visor { LinhaVisor(stringResource(R.string.abrir_troco_inicial), total, grande = true) }
            BandejaPecas(
                Dinheiro.emPecas(contagem),
                textoVazio = stringResource(R.string.abrir_bandeja_vazia),
                tamanho = 84.dp,
                onTirar = viewModel.contador::tirar,
            )
            TecladoDinheiro(onToque = viewModel.contador::somar)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TeclaSecundaria(
                    stringResource(R.string.apagar),
                    onClick = viewModel.contador::limpar,
                    corTexto = Cores.Erro,
                    habilitada = total > 0,
                    modifier = Modifier.weight(1f),
                )
                TeclaTexto(
                    stringResource(R.string.abrir_comecar_com, Dinheiro.formatar(total)),
                    onClick = { viewModel.abrir(total, onAberto) },
                    cor = Cores.Verde,
                    corConteudo = Color.White,
                    habilitada = total > 0,
                    modifier = Modifier.weight(2f),
                )
            }
            TeclaSecundaria(
                stringResource(R.string.abrir_sem_troco),
                onClick = { viewModel.abrir(0, onAberto) },
                modifier = Modifier.fillMaxWidth(),
                alturaMinima = 72.dp,
            )
        }
    }
}
