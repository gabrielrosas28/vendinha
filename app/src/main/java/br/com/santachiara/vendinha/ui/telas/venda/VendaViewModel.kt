package br.com.santachiara.vendinha.ui.telas.venda

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.santachiara.vendinha.AppContainer
import br.com.santachiara.vendinha.R
import br.com.santachiara.vendinha.data.db.Caixa
import br.com.santachiara.vendinha.data.db.CaixaFechadoException
import br.com.santachiara.vendinha.data.db.EstoqueInsuficienteException
import br.com.santachiara.vendinha.data.db.Item
import br.com.santachiara.vendinha.data.db.LinhaPedido
import br.com.santachiara.vendinha.domain.ContaCaixa
import br.com.santachiara.vendinha.domain.Denominacao
import br.com.santachiara.vendinha.domain.Dinheiro
import br.com.santachiara.vendinha.domain.RegrasEstoque
import br.com.santachiara.vendinha.ui.telas.caixa.ContadorDinheiro
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LinhaPedidoUi(val item: Item, val quantidade: Int) {
    val subtotal: Long get() = item.precoCentavos * quantidade
}

data class Aviso(@StringRes val texto: Int, val args: List<Any> = emptyList(), val id: Long = System.nanoTime())

data class VendaFeita(val totalCentavos: Long, val recebidoCentavos: Long) {
    val trocoCentavos: Long get() = recebidoCentavos - totalCentavos
}

class VendaViewModel(private val c: AppContainer) : ViewModel() {

    val itens: StateFlow<List<Item>?> = c.produtos.observarAtivos()
        .map<List<Item>, List<Item>?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val caixa: StateFlow<Caixa?> = c.caixa.observarCaixaAtual()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val somAtivo: StateFlow<Boolean> = c.config.observar()
        .map { it?.somAtivo ?: true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val pedido = MutableStateFlow<Map<Long, Int>>(emptyMap())

    val linhas: StateFlow<List<LinhaPedidoUi>> = combine(itens, pedido) { lista, p ->
        val porId = lista.orEmpty().associateBy { it.id }
        p.mapNotNull { (id, q) -> porId[id]?.let { LinhaPedidoUi(it, q) } }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val total: StateFlow<Long> = linhas.map { l -> l.sumOf { it.subtotal } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    val quantidades: StateFlow<Map<Long, Int>> = pedido

    val recebido = ContadorDinheiro()

    private val _aviso = MutableStateFlow<Aviso?>(null)
    val aviso: StateFlow<Aviso?> = _aviso
    private var limparAviso: Job? = null

    var ultimaVenda: VendaFeita? = null
        private set

    private val _confirmando = MutableStateFlow(false)
    val confirmando: StateFlow<Boolean> = _confirmando

    fun tocarItem(item: Item) {
        val noPedido = pedido.value[item.id] ?: 0
        if (!RegrasEstoque.podeAdicionarUm(item.controlaEstoque, item.qtdDisponivel, noPedido)) {
            val restam = item.qtdDisponivel ?: 0
            avisar(if (restam <= 0) Aviso(R.string.venda_aviso_esgotou, listOf(item.nome)) else Aviso(R.string.venda_aviso_so_tem, listOf(restam, item.nome)))
            return
        }
        pedido.update { it + (item.id to noPedido + 1) }
    }

    fun mais(item: Item) = tocarItem(item)

    fun menos(item: Item) = pedido.update { m ->
        val q = (m[item.id] ?: 0) - 1
        if (q <= 0) m - item.id else m + (item.id to q)
    }

    fun limparPedido() {
        pedido.value = emptyMap()
        recebido.limpar()
    }

    fun receber(d: Denominacao) = recebido.somar(d)
    fun tirar(d: Denominacao) = recebido.tirar(d)
    fun apagarRecebido() = recebido.limpar()

    fun valorCertinho() {
        recebido.definir(Dinheiro.decomporTroco(total.value).pecas.associate { it.denominacao to it.quantidade })
    }

    fun confirmar(pronto: () -> Unit) {
        val caixaAtual = caixa.value ?: return
        val totalAtual = total.value
        val recebidoAtual = recebido.total
        if (!ContaCaixa.podeConfirmar(totalAtual, recebidoAtual) || _confirmando.value) return
        val linhasVenda = linhas.value.map { LinhaPedido(it.item.id, it.item.nome, it.item.precoCentavos, it.quantidade) }
        _confirmando.value = true
        viewModelScope.launch {
            try {
                c.caixa.confirmarVenda(caixaAtual.id, linhasVenda, recebidoAtual)
                ultimaVenda = VendaFeita(totalAtual, recebidoAtual)
                if (somAtivo.value) runCatching { c.som.tocar() }
                pedido.value = emptyMap()
                recebido.limpar()
                pronto()
            } catch (e: EstoqueInsuficienteException) {
                avisar(Aviso(R.string.venda_aviso_estoque_mudou, listOf(e.nomeItem)))
            } catch (_: CaixaFechadoException) {
                avisar(Aviso(R.string.venda_aviso_caixa_fechado))
            } finally {
                _confirmando.value = false
            }
        }
    }

    private fun avisar(a: Aviso) {
        _aviso.value = a
        limparAviso?.cancel()
        limparAviso = viewModelScope.launch {
            delay(3_000)
            if (_aviso.value?.id == a.id) _aviso.value = null
        }
    }
}
