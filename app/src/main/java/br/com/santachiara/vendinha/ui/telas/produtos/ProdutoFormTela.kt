package br.com.santachiara.vendinha.ui.telas.produtos

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.santachiara.vendinha.R
import br.com.santachiara.vendinha.data.FotoArmazenamento
import br.com.santachiara.vendinha.data.ProdutoRepositorio
import br.com.santachiara.vendinha.data.db.Item
import br.com.santachiara.vendinha.domain.Dinheiro
import br.com.santachiara.vendinha.domain.RegrasEstoque
import br.com.santachiara.vendinha.ui.appContainer
import br.com.santachiara.vendinha.ui.componentes.CacheDeFotos
import br.com.santachiara.vendinha.ui.componentes.CampoTexto
import br.com.santachiara.vendinha.ui.componentes.FaixaAviso
import br.com.santachiara.vendinha.ui.componentes.FotoProduto
import br.com.santachiara.vendinha.ui.componentes.TeclaSecundaria
import br.com.santachiara.vendinha.ui.componentes.TeclaTexto
import br.com.santachiara.vendinha.ui.componentes.Tela
import br.com.santachiara.vendinha.ui.componentes.TipoAviso
import br.com.santachiara.vendinha.ui.tema.Cores
import br.com.santachiara.vendinha.ui.vm
import kotlinx.coroutines.launch

class ProdutoFormViewModel(
    private val itemId: Long,
    private val produtos: ProdutoRepositorio,
    private val fotos: FotoArmazenamento,
) : ViewModel() {

    var original: Item? = null; private set
    var carregado by mutableStateOf(itemId == 0L); private set
    var fotoPath by mutableStateOf<String?>(null); private set
    var nome by mutableStateOf("")
    var preco by mutableStateOf("")
    var controla by mutableStateOf(false)
    var quantidade by mutableStateOf("")
    var processandoFoto by mutableStateOf(false); private set
    var salvando by mutableStateOf(false); private set
    var erro: Int? by mutableStateOf(null); private set

    private var fotoNaoSalva: String? = null

    init {
        if (itemId != 0L) viewModelScope.launch {
            produtos.obter(itemId)?.let { i ->
                original = i
                fotoPath = i.fotoPath
                nome = i.nome
                preco = Dinheiro.formatarSemSimbolo(i.precoCentavos)
                controla = i.controlaEstoque
                quantidade = i.qtdDisponivel?.toString() ?: ""
            }
            carregado = true
        }
    }

    val precoCentavos: Long? get() = Dinheiro.lerReais(preco)

    fun definirFoto(uri: Uri) {
        processandoFoto = true
        erro = null
        viewModelScope.launch {
            runCatching { fotos.salvar(uri) }
                .onSuccess { novo ->
                    fotoNaoSalva?.let(fotos::apagar)
                    fotoNaoSalva = novo
                    fotoPath = novo
                }
                .onFailure {
                    android.util.Log.w("Vendinha", "Falha ao preparar a foto", it)
                    erro = R.string.produto_erro_foto_falhou
                }
            processandoFoto = false
        }
    }

    fun avisar(@StringRes msg: Int) { erro = msg }

    fun salvar(pronto: () -> Unit) {
        val foto = fotoPath
        val p = precoCentavos
        val q = quantidade.toIntOrNull()
        erro = when {
            foto == null -> R.string.produto_erro_foto
            nome.isBlank() -> R.string.produto_erro_nome
            p == null || p <= 0 -> R.string.produto_erro_preco
            !Dinheiro.ehMultiploDaMenorMoeda(p) -> R.string.produto_erro_preco_moeda
            controla && (q == null || q < 0 || (original == null && q == 0)) -> R.string.produto_erro_quantidade
            else -> null
        }
        if (erro != null || foto == null || p == null) return
        val antigo = original
        val (inicial, disponivel) = when {
            !controla -> null to null
            antigo != null && antigo.controlaEstoque -> RegrasEstoque.inicialAposAjuste(antigo.qtdInicial, antigo.qtdDisponivel, q!!) to q
            else -> q to q
        }
        val item = (antigo ?: Item(grupoId = 0, nome = "", fotoPath = "", precoCentavos = 0, controlaEstoque = false, qtdInicial = null, qtdDisponivel = null, ativo = true))
            .copy(nome = nome.trim(), fotoPath = foto, precoCentavos = p, controlaEstoque = controla, qtdInicial = inicial, qtdDisponivel = disponivel)
        salvando = true
        viewModelScope.launch {
            produtos.salvar(item)
            antigo?.fotoPath?.takeIf { it != foto }?.let(CacheDeFotos::esquecer)
            fotoNaoSalva = null
            pronto()
        }
    }

    fun alternarAtivo(pronto: () -> Unit) {
        val i = original ?: return
        viewModelScope.launch {
            produtos.definirAtivo(i, !i.ativo)
            pronto()
        }
    }

    override fun onCleared() {
        fotoNaoSalva?.let(fotos::apagar)
    }
}

@Composable
fun ProdutoFormTela(itemId: Long, onVoltar: () -> Unit) {
    val container = appContainer()
    val viewModel = vm(chave = "produto-$itemId") { ProdutoFormViewModel(itemId, it.produtos, it.fotos) }
    var uriCamera by rememberSaveable { mutableStateOf<Uri?>(null) }

    val tirarFoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) uriCamera?.let(viewModel::definirFoto)
    }
    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(viewModel::definirFoto)
    }
    val abrirCamera = {
        val u = container.fotos.novoUriCamera()
        uriCamera = u
        try { tirarFoto.launch(u) } catch (_: ActivityNotFoundException) { viewModel.avisar(R.string.produto_erro_sem_camera) }
    }
    val abrirGaleria = {
        try {
            galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } catch (_: ActivityNotFoundException) { viewModel.avisar(R.string.produto_erro_sem_galeria) }
    }

    val novo = itemId == 0L
    Tela(titulo = stringResource(if (novo) R.string.produto_novo_titulo else R.string.produto_editar_titulo), onVoltar = onVoltar) {
        if (!viewModel.carregado) return@Tela
        Column(
            Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .widthIn(max = 340.dp)
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Cores.Superficie)
                    .border(3.dp, if (viewModel.fotoPath == null) Cores.Linha else Color.Transparent, RoundedCornerShape(24.dp))
                    .clickable(role = Role.Button, onClickLabel = stringResource(R.string.produto_tirar_foto)) { abrirCamera() },
                contentAlignment = Alignment.Center,
            ) {
                val foto = viewModel.fotoPath
                if (foto != null) {
                    FotoProduto(foto, stringResource(R.string.foto_de, viewModel.nome.ifBlank { stringResource(R.string.produto) }), Modifier.fillMaxSize())
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(20.dp)) {
                        Icon(painterResource(R.drawable.ic_camera), null, tint = Cores.Azul, modifier = Modifier.size(72.dp))
                        Spacer(Modifier.height(10.dp))
                        Text(stringResource(R.string.produto_tirar_foto), style = MaterialTheme.typography.headlineSmall, color = Cores.Tinta, textAlign = TextAlign.Center)
                        Text(stringResource(R.string.produto_foto_ajuda), style = MaterialTheme.typography.bodyMedium, color = Cores.Apoio, textAlign = TextAlign.Center)
                    }
                }
                if (viewModel.processandoFoto) {
                    Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.75f)), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Cores.Azul)
                    }
                }
            }
            Row(Modifier.widthIn(max = 520.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TeclaTexto(
                    stringResource(if (viewModel.fotoPath == null) R.string.produto_camera else R.string.produto_trocar_foto),
                    onClick = abrirCamera,
                    icone = R.drawable.ic_camera,
                    cor = Cores.Azul,
                    corConteudo = Color.White,
                    modifier = Modifier.weight(1f),
                )
                TeclaSecundaria(
                    stringResource(R.string.produto_galeria),
                    onClick = abrirGaleria,
                    icone = R.drawable.ic_galeria,
                    modifier = Modifier.weight(1f),
                )
            }

            CampoTexto(
                valor = viewModel.nome,
                onMudar = { viewModel.nome = it.take(40) },
                rotulo = stringResource(R.string.produto_nome),
                exemplo = stringResource(R.string.produto_nome_exemplo),
                teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
            )
            val p = viewModel.precoCentavos
            CampoTexto(
                valor = viewModel.preco,
                onMudar = { t -> viewModel.preco = t.filter { it.isDigit() || it == ',' || it == '.' }.take(9) },
                rotulo = stringResource(R.string.produto_preco),
                exemplo = stringResource(R.string.produto_preco_exemplo),
                ajuda = p?.let { stringResource(R.string.produto_preco_fica, Dinheiro.formatar(it)) },
                teclado = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            )

            val estadoChave = stringResource(if (viewModel.controla) R.string.ligado else R.string.desligado)
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Cores.Superficie, RoundedCornerShape(20.dp))
                    .clickable(role = Role.Switch) { viewModel.controla = !viewModel.controla }
                    .semantics { stateDescription = estadoChave }
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.produto_controlar), style = MaterialTheme.typography.titleLarge, color = Cores.Tinta)
                    Text(
                        stringResource(if (viewModel.controla) R.string.produto_controlar_sim else R.string.produto_controlar_nao),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Cores.Apoio,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = viewModel.controla,
                    onCheckedChange = null,
                    colors = SwitchDefaults.colors(checkedTrackColor = Cores.Verde, checkedThumbColor = Color.White),
                )
            }
            if (viewModel.controla) {
                CampoTexto(
                    valor = viewModel.quantidade,
                    onMudar = { t -> viewModel.quantidade = t.filter { it.isDigit() }.take(4) },
                    rotulo = stringResource(if (novo || viewModel.original?.controlaEstoque != true) R.string.produto_quantidade else R.string.produto_quantidade_agora),
                    exemplo = "20",
                    ajuda = viewModel.original?.takeIf { it.controlaEstoque }?.let {
                        stringResource(R.string.produto_quantidade_ajuda, it.qtdDisponivel ?: 0)
                    },
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }

            viewModel.erro?.let { FaixaAviso(stringResource(it), TipoAviso.ERRO) }

            TeclaTexto(
                stringResource(R.string.produto_salvar),
                onClick = { viewModel.salvar(onVoltar) },
                icone = R.drawable.ic_ok,
                cor = Cores.Verde,
                corConteudo = Color.White,
                alturaMinima = 76.dp,
                habilitada = !viewModel.salvando && !viewModel.processandoFoto,
                modifier = Modifier.fillMaxWidth(),
            )
            viewModel.original?.let { i ->
                TeclaSecundaria(
                    stringResource(if (i.ativo) R.string.produto_desativar else R.string.produto_ativar),
                    onClick = { viewModel.alternarAtivo(onVoltar) },
                    corTexto = if (i.ativo) Cores.Erro else Cores.VerdeTexto,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (i.ativo) {
                    Text(stringResource(R.string.produto_desativar_ajuda), style = MaterialTheme.typography.bodyMedium, color = Cores.Apoio, textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

