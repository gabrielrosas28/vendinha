package br.com.santachiara.vendinha.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.santachiara.vendinha.data.db.Caixa
import br.com.santachiara.vendinha.data.db.Config
import br.com.santachiara.vendinha.data.db.EstoqueInsuficienteException
import br.com.santachiara.vendinha.data.db.Grupo
import br.com.santachiara.vendinha.data.db.Item
import br.com.santachiara.vendinha.data.db.LinhaPedido
import br.com.santachiara.vendinha.data.db.StatusCaixa
import br.com.santachiara.vendinha.data.db.VendinhaDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config as RoboConfig

@RunWith(RobolectricTestRunner::class)
@RoboConfig(sdk = [35], application = Application::class)
class DaoTest {

    private lateinit var db: VendinhaDatabase
    private var grupoId = 0L
    private var caixaId = 0L
    private var brigadeiro = 0L
    private var bolo = 0L

    @Before
    fun preparar() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), VendinhaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        grupoId = db.grupoDao().inserir(Grupo(turma = "2º A", numeroGrupo = 2, nome = "Brigadeiros", corIndex = 1, iniciadoEm = 1L, ativo = true))
        brigadeiro = db.itemDao().inserir(Item(grupoId = grupoId, nome = "Brigadeiro", fotoPath = "/x/b.jpg", precoCentavos = 250, controlaEstoque = true, qtdInicial = 5, qtdDisponivel = 5, ativo = true))
        bolo = db.itemDao().inserir(Item(grupoId = grupoId, nome = "Fatia de bolo", fotoPath = "/x/c.jpg", precoCentavos = 500, controlaEstoque = false, qtdInicial = null, qtdDisponivel = null, ativo = true))
        caixaId = db.caixaDao().inserir(Caixa(grupoId = grupoId, abertoEm = 1L, fechadoEm = null, trocoInicialCentavos = 2_000, contadoCentavos = null, status = StatusCaixa.ABERTO))
    }

    @After
    fun fechar() = db.close()

    private fun pedido(qtdBrigadeiro: Int, qtdBolo: Int) = listOf(
        LinhaPedido(brigadeiro, "Brigadeiro", 250, qtdBrigadeiro),
        LinhaPedido(bolo, "Fatia de bolo", 500, qtdBolo),
    )

    @Test
    fun confirmarVendaGravaTudoEBaixaEstoque() = runBlocking {
        val vendaId = db.vendaDao().confirmarVenda(caixaId, pedido(2, 3), recebidoCentavos = 2_500, agora = 10L)

        val venda = db.vendaDao().obter(vendaId)!!
        assertEquals(2_000L, venda.venda.totalCentavos)
        assertEquals(500L, venda.venda.trocoCentavos)
        assertEquals(2, venda.itens.size)
        assertEquals(3, db.itemDao().obter(brigadeiro)!!.qtdDisponivel)
        assertNull(db.itemDao().obter(bolo)!!.qtdDisponivel)
        val vendidos = db.vendaDao().observarQtdVendida(caixaId).first().associate { it.itemId to it.quantidade }
        assertEquals(mapOf(brigadeiro to 2, bolo to 3), vendidos)
    }

    @Test
    fun vendaSemEstoqueDesfazTudo() = runBlocking {
        try {
            db.vendaDao().confirmarVenda(caixaId, pedido(6, 1), recebidoCentavos = 5_000, agora = 10L)
            fail("deveria faltar estoque")
        } catch (e: EstoqueInsuficienteException) {
            assertEquals("Brigadeiro", e.nomeItem)
        }
        assertTrue(db.vendaDao().listarDoCaixa(caixaId).isEmpty())
        assertEquals(5, db.itemDao().obter(brigadeiro)!!.qtdDisponivel)
    }

    @Test
    fun vendaComRecebidoMenorQueTotalNaoGrava() = runBlocking {
        try {
            db.vendaDao().confirmarVenda(caixaId, pedido(1, 0), recebidoCentavos = 200, agora = 10L)
            fail("deveria recusar")
        } catch (_: IllegalArgumentException) {
        }
        assertTrue(db.vendaDao().listarDoCaixa(caixaId).isEmpty())
    }

    @Test
    fun estornarDevolveEstoqueSoComControleENuncaApaga() = runBlocking {
        val vendaId = db.vendaDao().confirmarVenda(caixaId, pedido(2, 1), recebidoCentavos = 1_000, agora = 10L)
        assertEquals(3, db.itemDao().obter(brigadeiro)!!.qtdDisponivel)

        assertTrue(db.vendaDao().estornar(vendaId, agora = 20L))
        val venda = db.vendaDao().obter(vendaId)
        assertNotNull(venda)
        assertTrue(venda!!.venda.estornada)
        assertEquals(20L, venda.venda.estornadaEm)
        assertEquals(5, db.itemDao().obter(brigadeiro)!!.qtdDisponivel)
        assertNull(db.itemDao().obter(bolo)!!.qtdDisponivel)
        assertTrue(db.vendaDao().observarQtdVendida(caixaId).first().isEmpty())

        assertFalse(db.vendaDao().estornar(vendaId, agora = 30L))
        assertEquals(5, db.itemDao().obter(brigadeiro)!!.qtdDisponivel)
    }

    @Test
    fun zerarTabletMantemFechamentoEConfig() = runBlocking {
        val repo = CaixaRepositorio(db) { 100L }
        db.configDao().salvar(Config(senhaHash = "h", senhaSalt = "s", somAtivo = true))
        db.vendaDao().confirmarVenda(caixaId, pedido(1, 1), recebidoCentavos = 1_000, agora = 10L)
        val fechamentoId = repo.fechar(caixaId, contadoCentavos = 2_750)

        db.manutencaoDao().zerarTablet()

        assertNull(db.grupoDao().obterAtivo())
        assertNull(db.itemDao().obter(brigadeiro))
        assertNull(db.caixaDao().obter(caixaId))
        assertTrue(db.vendaDao().listarDoCaixa(caixaId).isEmpty())
        val fechamentos = db.fechamentoDao().listarTodos()
        assertEquals(1, fechamentos.size)
        val f = fechamentos.single()
        assertEquals(fechamentoId, f.id)
        assertEquals(750L, f.totalCentavos)
        assertEquals("Brigadeiros", f.nomeGrupo)
        val dados = RelatorioJson.ler(f)
        assertEquals(2_750L, dados.esperadoCentavos)
        assertEquals(0L, dados.diferencaCentavos)
        assertNotNull(db.configDao().obter())
    }

    @Test
    fun fecharDeNovoDepoisDeReabrirAtualizaOMesmoRelatorio() = runBlocking {
        val repo = CaixaRepositorio(db) { 100L }
        val id1 = repo.fechar(caixaId, null)
        repo.reabrir(caixaId)
        assertEquals(StatusCaixa.ABERTO, db.caixaDao().obter(caixaId)!!.status)
        db.vendaDao().confirmarVenda(caixaId, pedido(1, 0), recebidoCentavos = 250, agora = 10L)
        val id2 = repo.fechar(caixaId, null)
        assertEquals(id1, id2)
        assertEquals(1, db.fechamentoDao().listarTodos().size)
        assertEquals(250L, db.fechamentoDao().listarTodos().single().totalCentavos)
    }
}
