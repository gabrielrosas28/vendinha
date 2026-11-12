package br.com.santachiara.vendinha.ui.telas.fechamento

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.santachiara.vendinha.AppContainer
import br.com.santachiara.vendinha.R
import br.com.santachiara.vendinha.data.Compartilhar
import br.com.santachiara.vendinha.data.DadosRelatorio
import br.com.santachiara.vendinha.data.RelatorioJson
import br.com.santachiara.vendinha.domain.ContaCaixa
import br.com.santachiara.vendinha.domain.Dinheiro
import br.com.santachiara.vendinha.domain.ResultadoConferencia
import br.com.santachiara.vendinha.domain.ResumoCaixa
import br.com.santachiara.vendinha.ui.Formatos
import br.com.santachiara.vendinha.ui.componentes.FaixaAviso
import br.com.santachiara.vendinha.ui.componentes.TeclaSecundaria
import br.com.santachiara.vendinha.ui.componentes.TeclaTexto
import br.com.santachiara.vendinha.ui.componentes.Tela
import br.com.santachiara.vendinha.ui.componentes.TipoAviso
import br.com.santachiara.vendinha.ui.tema.Cores
import br.com.santachiara.vendinha.ui.tema.NUMEROS_TABULARES
import br.com.santachiara.vendinha.ui.vm
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RelatorioViewModel(private val id: Long, private val c: AppContainer) : ViewModel() {
    val dados: StateFlow<DadosRelatorio?> = c.caixa.observarFechamento(id)
        .map { f -> f?.let(RelatorioJson::ler) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    suspend fun salvarImagem(context: android.content.Context, bitmap: Bitmap): java.io.File {
        val f = Compartilhar.salvarRelatorioPng(context, id, bitmap)
        c.caixa.definirImagemFechamento(id, f.absolutePath)
        return f
    }
}

@Composable
fun RelatorioTela(fechamentoId: Long, onVoltar: () -> Unit) {
    val viewModel = vm(chave = "relatorio-$fechamentoId") { RelatorioViewModel(fechamentoId, it) }
    val dados by viewModel.dados.collectAsStateWithLifecycle()
    val camada = rememberGraphicsLayer()
    val escopo = rememberCoroutineScope()
    val context = LocalContext.current
    var falhou by remember { mutableStateOf(false) }
    var gerando by remember { mutableStateOf(false) }
    val tituloCompartilhar = stringResource(R.string.relatorio_compartilhar)

    Tela(titulo = stringResource(R.string.relatorio_titulo), onVoltar = onVoltar) {
        val d = dados ?: return@Tela
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .widthIn(max = 640.dp)
                    .drawWithContent {
                        camada.record { this@drawWithContent.drawContent() }
                        drawLayer(camada)
                    }
            ) {
                CartaoRelatorio(d)
            }
            if (falhou) FaixaAviso(stringResource(R.string.relatorio_sem_app), TipoAviso.ERRO)
            TeclaTexto(
                tituloCompartilhar,
                onClick = {
                    gerando = true
                    escopo.launch {
                        val bmp = sobreBranco(camada.toImageBitmap().asAndroidBitmap())
                        val arquivo = viewModel.salvarImagem(context, bmp)
                        falhou = !Compartilhar.arquivo(context, arquivo, "image/png", tituloCompartilhar)
                        gerando = false
                    }
                },
                icone = R.drawable.ic_compartilhar,
                cor = Cores.Azul,
                corConteudo = Color.White,
                habilitada = !gerando,
                alturaMinima = 80.dp,
                modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
            )
            TeclaSecundaria(
                stringResource(R.string.relatorio_voltar),
                onClick = onVoltar,
                modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth().padding(bottom = 16.dp),
            )
        }
    }
}

@Composable
private fun CartaoRelatorio(d: DadosRelatorio) {
    val f = d.fechamento
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(28.dp))
            .border(2.dp, Cores.Linha, RoundedCornerShape(28.dp))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Image(
            painterResource(R.drawable.logo_feirinha),
            contentDescription = stringResource(R.string.logo_feirinha),
            modifier = Modifier.fillMaxWidth(),
        )
        Column {
            Text(stringResource(R.string.relatorio_cabecalho), style = MaterialTheme.typography.bodyLarge, color = Cores.Apoio)
            Text(f.nomeGrupo, style = MaterialTheme.typography.headlineLarge, color = Cores.Tinta)
            Text(
                if (f.numeroGrupo > 0) stringResource(R.string.turma_grupo, f.turma, f.numeroGrupo) else f.turma,
                style = MaterialTheme.typography.titleMedium,
                color = Cores.Apoio,
            )
            Text(Formatos.dataHora(f.data), style = MaterialTheme.typography.bodyLarge, color = Cores.Apoio)
        }
        Column(
            Modifier
                .fillMaxWidth()
                .background(Cores.Visor, RoundedCornerShape(20.dp))
                .padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            Text(stringResource(R.string.total_ganho), style = MaterialTheme.typography.titleMedium, color = Cores.VisorRotulo)
            Text(
                Dinheiro.formatar(f.totalCentavos),
                style = MaterialTheme.typography.displayMedium.copy(fontFeatureSettings = NUMEROS_TABULARES),
                color = Cores.VisorNumero,
            )
            Text(pluralStringResource(R.plurals.vendas, f.numVendas, f.numVendas), style = MaterialTheme.typography.bodyLarge, color = Cores.VisorRotulo)
        }
        d.campeao?.let { c ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).background(Cores.Laranja, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_estrela), null, tint = Cores.Tinta, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(R.string.relatorio_campeao, c.nome, c.quantidade),
                    style = MaterialTheme.typography.titleMedium,
                    color = Cores.Tinta,
                )
            }
        }
        if (d.itens.isNotEmpty()) {
            TabelaItens(ResumoCaixa(f.totalCentavos, f.numVendas, d.numeroEstornadas, d.itens, d.campeao))
        }
        Column(Modifier.fillMaxWidth().background(Cores.Fundo, RoundedCornerShape(20.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            LinhaConta(stringResource(R.string.abrir_troco_inicial), Dinheiro.formatar(f.trocoInicialCentavos))
            LinhaConta(stringResource(R.string.conferir_esperado), Dinheiro.formatar(d.esperadoCentavos))
            val contado = f.contadoCentavos
            if (contado == null) {
                LinhaConta(stringResource(R.string.conferir_contado), stringResource(R.string.relatorio_nao_contado))
            } else {
                LinhaConta(stringResource(R.string.conferir_contado), Dinheiro.formatar(contado))
                val dif = d.diferencaCentavos ?: 0
                val (texto, cor) = when (ContaCaixa.resultado(dif)) {
                    ResultadoConferencia.BATEU -> stringResource(R.string.conferir_bateu) to Cores.VerdeTexto
                    ResultadoConferencia.SOBROU -> stringResource(R.string.conferir_sobrou, Dinheiro.formatar(dif)) to Cores.AzulTexto
                    ResultadoConferencia.FALTOU -> stringResource(R.string.conferir_faltou, Dinheiro.formatar(-dif)) to Cores.Erro
                }
                Text(texto, style = MaterialTheme.typography.titleMedium, color = cor)
            }
            if (d.numeroEstornadas > 0) {
                Text(pluralStringResource(R.plurals.estornadas, d.numeroEstornadas, d.numeroEstornadas), style = MaterialTheme.typography.bodyMedium, color = Cores.Apoio)
            }
        }
        Text(
            stringResource(R.string.relatorio_rodape),
            style = MaterialTheme.typography.bodySmall,
            color = Cores.Apoio,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun sobreBranco(origem: Bitmap): Bitmap {
    val copia = origem.copy(Bitmap.Config.ARGB_8888, false)
    val final = Bitmap.createBitmap(copia.width, copia.height, Bitmap.Config.ARGB_8888)
    android.graphics.Canvas(final).apply {
        drawColor(android.graphics.Color.WHITE)
        drawBitmap(copia, 0f, 0f, null)
    }
    copia.recycle()
    return final
}

@Composable
private fun LinhaConta(rotulo: String, valor: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(rotulo, style = MaterialTheme.typography.bodyLarge, color = Cores.Apoio, modifier = Modifier.weight(1f))
        Text(valor, style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = NUMEROS_TABULARES), color = Cores.Tinta)
    }
}
