package br.com.santachiara.vendinha.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.remember
import br.com.santachiara.vendinha.ui.tema.Cores
import br.com.santachiara.vendinha.ui.telas.caixa.AbrirCaixaTela
import br.com.santachiara.vendinha.ui.telas.estoque.EstoqueTela
import br.com.santachiara.vendinha.ui.telas.fechamento.ConferirTela
import br.com.santachiara.vendinha.ui.telas.fechamento.FecharCaixaTela
import br.com.santachiara.vendinha.ui.telas.fechamento.FechamentoViewModel
import br.com.santachiara.vendinha.ui.telas.fechamento.RelatorioTela
import br.com.santachiara.vendinha.ui.telas.historico.HistoricoTela
import br.com.santachiara.vendinha.ui.telas.historico.VendaDetalheTela
import br.com.santachiara.vendinha.ui.telas.venda.PagamentoTela
import br.com.santachiara.vendinha.ui.telas.venda.ProntoTela
import br.com.santachiara.vendinha.ui.telas.venda.VendaTela
import br.com.santachiara.vendinha.ui.telas.venda.VendaViewModel
import br.com.santachiara.vendinha.ui.telas.inicio.IniciarGrupoTela
import br.com.santachiara.vendinha.ui.telas.organizador.OrganizadorTela
import br.com.santachiara.vendinha.ui.telas.organizador.RegistroVendasTela
import br.com.santachiara.vendinha.ui.telas.organizador.TurmaTela
import br.com.santachiara.vendinha.ui.telas.organizador.TurmasTela
import br.com.santachiara.vendinha.ui.telas.painel.PainelTela
import br.com.santachiara.vendinha.ui.telas.produtos.ProdutoFormTela
import br.com.santachiara.vendinha.ui.telas.produtos.ProdutosTela
import br.com.santachiara.vendinha.ui.telas.senha.CriarSenhaTela
import kotlinx.coroutines.flow.first

object Rotas {
    const val CRIAR_SENHA = "criar-senha"
    const val INICIO = "inicio"
    const val PAINEL = "painel"
    const val PRODUTOS = "produtos"
    const val PRODUTO = "produto/{id}"
    fun produto(id: Long) = "produto/$id"
    const val ABRIR_CAIXA = "abrir-caixa"
    const val VENDA = "venda"
    const val PAGAMENTO = "pagamento"
    const val PRONTO = "pronto"
    const val ESTOQUE = "estoque"
    const val HISTORICO = "historico"
    const val VENDA_DETALHE = "venda/{id}"
    fun vendaDetalhe(id: Long) = "venda/$id"
    const val FECHAR = "fechar"
    const val CONFERIR = "conferir"
    const val RELATORIO = "relatorio/{id}"
    fun relatorio(id: Long) = "relatorio/$id"
    const val ORGANIZADOR = "organizador"
    const val TURMAS = "turmas"
    const val TURMA = "turma/{id}"
    fun turma(id: Long) = "turma/$id"
    const val REGISTRO = "registro"
}

@Composable
fun AppVendinha() {
    val container = appContainer()
    val inicio by produceState<String?>(null) {
        val config = container.config.observar().first()
        val grupo = container.grupos.observarAtivo().first()
        value = when {
            config == null -> Rotas.CRIAR_SENHA
            grupo == null -> Rotas.INICIO
            else -> Rotas.PAINEL
        }
    }
    val rota = inicio
    if (rota == null) {
        Box(Modifier.fillMaxSize().background(Cores.Fundo))
    } else {
        VendinhaNavHost(rememberNavController(), rota)
    }
}

fun NavHostController.irParaRaiz(rota: String) {
    navigate(rota) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}

@Composable
fun VendinhaNavHost(nav: NavHostController, inicio: String) {
    NavHost(navController = nav, startDestination = inicio) {
        composable(Rotas.CRIAR_SENHA) {
            CriarSenhaTela(onPronto = { nav.irParaRaiz(Rotas.INICIO) })
        }
        composable(Rotas.INICIO) {
            IniciarGrupoTela(
                onIniciado = { nav.irParaRaiz(Rotas.PAINEL) },
                onOrganizador = { nav.navigate(Rotas.ORGANIZADOR) { launchSingleTop = true } },
            )
        }
        composable(Rotas.PAINEL) {
            PainelTela(
                onVender = { nav.navigate(Rotas.VENDA) },
                onAbrirCaixa = { nav.navigate(Rotas.ABRIR_CAIXA) },
                onEstoque = { nav.navigate(Rotas.ESTOQUE) },
                onHistorico = { nav.navigate(Rotas.HISTORICO) },
                onProdutos = { nav.navigate(Rotas.PRODUTOS) },
                onFecharCaixa = { nav.navigate(Rotas.FECHAR) },
                onRelatorio = { nav.navigate(Rotas.relatorio(it)) },
                onOrganizador = { nav.navigate(Rotas.ORGANIZADOR) { launchSingleTop = true } },
                onSemGrupo = { nav.irParaRaiz(Rotas.INICIO) },
            )
        }
        composable(Rotas.PRODUTOS) {
            ProdutosTela(
                onVoltar = { nav.popBackStack() },
                onNovo = { nav.navigate(Rotas.produto(0)) },
                onEditar = { nav.navigate(Rotas.produto(it)) },
            )
        }
        composable(Rotas.PRODUTO, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entrada ->
            ProdutoFormTela(entrada.arguments?.getLong("id") ?: 0L, onVoltar = { nav.popBackStack() })
        }
        composable(Rotas.ABRIR_CAIXA) {
            AbrirCaixaTela(
                onVoltar = { nav.popBackStack() },
                onAberto = { nav.navigate(Rotas.VENDA) { popUpTo(Rotas.PAINEL) } },
            )
        }
        composable(Rotas.VENDA) { entrada ->
            VendaTela(
                viewModel = vm(dono = entrada) { VendaViewModel(it) },
                onVoltar = { nav.popBackStack() },
                onCobrar = { nav.navigate(Rotas.PAGAMENTO) { launchSingleTop = true } },
            )
        }
        composable(Rotas.PAGAMENTO) { entrada ->
            val dono = remember(entrada) { nav.getBackStackEntry(Rotas.VENDA) }
            PagamentoTela(
                viewModel = vm(dono = dono) { VendaViewModel(it) },
                onVoltar = { nav.popBackStack() },
                onVendaFeita = { nav.navigate(Rotas.PRONTO) { popUpTo(Rotas.VENDA) } },
            )
        }
        composable(Rotas.PRONTO) { entrada ->
            val dono = remember(entrada) { nav.getBackStackEntry(Rotas.VENDA) }
            ProntoTela(
                viewModel = vm(dono = dono) { VendaViewModel(it) },
                onProximo = { nav.popBackStack(Rotas.VENDA, inclusive = false) },
            )
        }
        composable(Rotas.ESTOQUE) { EstoqueTela(onVoltar = { nav.popBackStack() }) }
        composable(Rotas.HISTORICO) {
            HistoricoTela(onVoltar = { nav.popBackStack() }, onAbrirVenda = { nav.navigate(Rotas.vendaDetalhe(it)) })
        }
        composable(Rotas.VENDA_DETALHE, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entrada ->
            VendaDetalheTela(entrada.arguments?.getLong("id") ?: 0L, onVoltar = { nav.popBackStack() })
        }
        val aoFechar: (Long) -> Unit = { id -> nav.navigate(Rotas.relatorio(id)) { popUpTo(Rotas.PAINEL) } }
        composable(Rotas.FECHAR) { entrada ->
            FecharCaixaTela(
                viewModel = vm(dono = entrada) { FechamentoViewModel(it) },
                onVoltar = { nav.popBackStack() },
                onContar = { nav.navigate(Rotas.CONFERIR) },
                onFechado = aoFechar,
            )
        }
        composable(Rotas.CONFERIR) { entrada ->
            val dono = remember(entrada) { nav.getBackStackEntry(Rotas.FECHAR) }
            ConferirTela(
                viewModel = vm(dono = dono) { FechamentoViewModel(it) },
                onVoltar = { nav.popBackStack() },
                onFechado = aoFechar,
            )
        }
        composable(Rotas.RELATORIO, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entrada ->
            RelatorioTela(entrada.arguments?.getLong("id") ?: 0L, onVoltar = { nav.popBackStack() })
        }
        composable(Rotas.ORGANIZADOR) {
            OrganizadorTela(
                onVoltar = { nav.popBackStack() },
                onAbrirRelatorio = { nav.navigate(Rotas.relatorio(it)) },
                onZerado = { nav.irParaRaiz(Rotas.INICIO) },
                onTurmas = { nav.navigate(Rotas.TURMAS) },
                onRegistro = { nav.navigate(Rotas.REGISTRO) },
            )
        }
        composable(Rotas.TURMAS) {
            TurmasTela(onVoltar = { nav.popBackStack() }, onAbrirTurma = { nav.navigate(Rotas.turma(it)) })
        }
        composable(Rotas.TURMA, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entrada ->
            TurmaTela(entrada.arguments?.getLong("id") ?: 0L, onVoltar = { nav.popBackStack() })
        }
        composable(Rotas.REGISTRO) { RegistroVendasTela(onVoltar = { nav.popBackStack() }) }
    }
}
