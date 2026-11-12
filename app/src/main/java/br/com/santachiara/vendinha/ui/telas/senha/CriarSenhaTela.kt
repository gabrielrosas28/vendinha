package br.com.santachiara.vendinha.ui.telas.senha

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.santachiara.vendinha.R
import br.com.santachiara.vendinha.data.ConfigRepositorio
import br.com.santachiara.vendinha.domain.Senha
import br.com.santachiara.vendinha.ui.componentes.PontosSenha
import br.com.santachiara.vendinha.ui.componentes.TeclaTexto
import br.com.santachiara.vendinha.ui.componentes.TecladoNumerico
import br.com.santachiara.vendinha.ui.componentes.Tela
import br.com.santachiara.vendinha.ui.tema.Cores
import br.com.santachiara.vendinha.ui.vm
import kotlinx.coroutines.launch

class CriarSenhaViewModel(private val config: ConfigRepositorio) : ViewModel() {
    fun salvar(senha: String, pronto: () -> Unit) {
        viewModelScope.launch {
            config.criarSenha(senha)
            pronto()
        }
    }
}

@Composable
fun CriarSenhaTela(onPronto: () -> Unit) {
    val viewModel = vm { CriarSenhaViewModel(it.config) }
    var primeira by rememberSaveable { mutableStateOf<String?>(null) }
    var digitada by rememberSaveable { mutableStateOf("") }
    var erroDiferente by rememberSaveable { mutableStateOf(false) }
    var erroTick by rememberSaveable { mutableStateOf(0) }
    var salvando by rememberSaveable { mutableStateOf(false) }
    val confirmando = primeira != null

    Tela(titulo = null, onVoltar = null) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painterResource(R.drawable.logo_feirinha),
                contentDescription = stringResource(R.string.logo_feirinha),
                modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
            )
            Spacer(Modifier.height(24.dp))
            Text(
                stringResource(if (confirmando) R.string.senha_confirmar_titulo else R.string.senha_criar_titulo),
                style = MaterialTheme.typography.headlineLarge,
                color = Cores.Tinta,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(if (confirmando) R.string.senha_confirmar_texto else R.string.senha_criar_texto),
                style = MaterialTheme.typography.bodyLarge,
                color = Cores.Apoio,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            PontosSenha(digitada.length, Senha.MAX_DIGITOS, erroTick)
            Spacer(Modifier.height(8.dp))
            Text(
                if (erroDiferente) stringResource(R.string.senha_diferente) else " ",
                style = MaterialTheme.typography.bodyLarge,
                color = Cores.Erro,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            TecladoNumerico(
                onDigito = { d -> if (digitada.length < Senha.MAX_DIGITOS) { digitada += d; erroDiferente = false } },
                onApagar = { digitada = digitada.dropLast(1) },
            )
            Spacer(Modifier.height(20.dp))
            TeclaTexto(
                texto = stringResource(if (confirmando) R.string.senha_salvar else R.string.continuar),
                onClick = {
                    val p = primeira
                    if (p == null) {
                        primeira = digitada; digitada = ""
                    } else if (p == digitada) {
                        salvando = true
                        viewModel.salvar(digitada, onPronto)
                    } else {
                        primeira = null; digitada = ""; erroDiferente = true; erroTick++
                    }
                },
                modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
                cor = Cores.Azul,
                habilitada = Senha.formatoValido(digitada) && !salvando,
                alturaMinima = 72.dp,
            )
        }
    }
}
