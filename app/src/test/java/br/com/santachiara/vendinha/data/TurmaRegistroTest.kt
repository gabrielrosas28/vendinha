package br.com.santachiara.vendinha.data

import android.app.Application
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.santachiara.vendinha.data.db.Caixa
import br.com.santachiara.vendinha.data.db.Grupo
import br.com.santachiara.vendinha.data.db.Item
import br.com.santachiara.vendinha.data.db.LinhaPedido
import br.com.santachiara.vendinha.data.db.Migracoes
import br.com.santachiara.vendinha.data.db.StatusCaixa
import br.com.santachiara.vendinha.data.db.VendinhaDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class TurmaRegistroTest {

    private lateinit var db: VendinhaDatabase

    @Before
    fun preparar() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), VendinhaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun fechar() = db.close()

    @Test
    fun criarRenomearEApagarTurmaEGrupos() = runBlocking {
        val repo = TurmaRepositorio(db)
        assertEquals(ResultadoTurma.OK, repo.criar("  4º   B "))
        assertEquals(ResultadoTurma.JA_EXISTE, repo.criar("4º b"))
        assertEquals(ResultadoTurma.NOME_INVALIDO, repo.criar("   "))
        val turma = repo.observarTodas().first().single()
        assertEquals("4º B", turma.turma.nome)

        assertTrue(repo.adicionarGrupo(turma.turma.id, "Pipoca"))
        assertTrue(repo.adicionarGrupo(turma.turma.id, "Cookies"))
        assertFalse(repo.adicionarGrupo(turma.turma.id, " "))
        var grupos = repo.observar(turma.turma.id).first()!!.gruposOrdenados
        assertEquals(listOf(1, 2), grupos.map { it.numero })

        repo.apagarGrupo(grupos[0].id)
        repo.adicionarGrupo(turma.turma.id, "Suco")
        repo.renomearGrupo(grupos[1].id, "Cookies de aveia")
        grupos = repo.observar(turma.turma.id).first()!!.gruposOrdenados
        assertEquals(listOf(2 to "Cookies de aveia", 3 to "Suco"), grupos.map { it.numero to it.produto })

        assertEquals(ResultadoTurma.OK, repo.renomear(turma.turma.id, "4º C"))
        repo.apagar(turma.turma.id)
        assertTrue(repo.observarTodas().first().isEmpty())
        assertNull(repo.observar(turma.turma.id).first())
    }

    @Test
    fun vendaVaiParaORegistroEstornoMarcaEZerarMantem() = runBlocking {
        val grupoId = db.grupoDao().inserir(Grupo(turma = "3º A", numeroGrupo = 1, nome = "Cookies", corIndex = 0, iniciadoEm = 1, ativo = true))
        val item = db.itemDao().inserir(Item(grupoId = grupoId, nome = "Cookie", fotoPath = "/x.jpg", precoCentavos = 300, controlaEstoque = false, qtdInicial = null, qtdDisponivel = null, ativo = true))
        val caixa = db.caixaDao().inserir(Caixa(grupoId = grupoId, abertoEm = 1, fechadoEm = null, trocoInicialCentavos = 0, contadoCentavos = null, status = StatusCaixa.ABERTO))

        val v1 = db.vendaDao().confirmarVenda(caixa, listOf(LinhaPedido(item, "Cookie", 300, 2)), recebidoCentavos = 1_000, agora = 10)
        db.vendaDao().confirmarVenda(caixa, listOf(LinhaPedido(item, "Cookie", 300, 1)), recebidoCentavos = 500, agora = 20)
        db.vendaDao().estornar(v1, agora = 30)

        db.manutencaoDao().zerarTablet()

        val registro = db.registroDao().listarTodos()
        assertEquals(2, registro.size)
        val (maisNova, maisAntiga) = registro
        assertEquals(300L, maisNova.totalCentavos)
        assertEquals(500L, maisNova.recebidoCentavos)
        assertEquals(200L, maisNova.trocoCentavos)
        assertFalse(maisNova.estornada)
        assertEquals("2× Cookie", maisAntiga.itens)
        assertEquals(400L, maisAntiga.trocoCentavos)
        assertTrue(maisAntiga.estornada)
        assertEquals(30L, maisAntiga.estornadaEm)
        assertEquals("Cookies", maisAntiga.nomeGrupo)
        assertEquals("3º A", maisAntiga.turma)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class MigracaoTest {

    @Test
    fun migra1Para2SemPerderDadosESemeiaTurmas() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val arquivo = context.getDatabasePath("migracao.db").apply { parentFile?.mkdirs(); delete() }

        val esquema = JSONObject(File("schemas/br.com.santachiara.vendinha.data.db.VendinhaDatabase/1.json").readText())
            .getJSONObject("database")
        SQLiteDatabase.openOrCreateDatabase(arquivo, null).use { v1 ->
            val entidades = esquema.getJSONArray("entities")
            for (i in 0 until entidades.length()) {
                val e = entidades.getJSONObject(i)
                val tabela = e.getString("tableName")
                v1.execSQL(e.getString("createSql").replace("\${TABLE_NAME}", tabela))
                val indices = e.optJSONArray("indices") ?: continue
                for (j in 0 until indices.length()) v1.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", tabela))
            }
            val setup = esquema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) v1.execSQL(setup.getString(i))
            v1.execSQL("INSERT INTO grupo (id, turma, numeroGrupo, nome, corIndex, iniciadoEm, ativo) VALUES (1, '2º A', 2, 'Brigadeiros', 1, 1, 1)")
            v1.execSQL("INSERT INTO caixa (id, grupoId, abertoEm, fechadoEm, trocoInicialCentavos, contadoCentavos, status) VALUES (1, 1, 1, NULL, 0, NULL, 'ABERTO')")
            v1.execSQL("INSERT INTO venda (id, caixaId, dataHora, totalCentavos, recebidoCentavos, trocoCentavos, estornada, estornadaEm) VALUES (1, 1, 5, 950, 1500, 550, 0, NULL)")
            v1.execSQL("INSERT INTO item_venda (vendaId, itemId, nomeItem, quantidade, precoUnitCentavos) VALUES (1, 7, 'Brigadeiro', 2, 250)")
            v1.execSQL("INSERT INTO item_venda (vendaId, itemId, nomeItem, quantidade, precoUnitCentavos) VALUES (1, 8, 'Fatia de bolo', 1, 450)")
            v1.version = 1
        }

        val db = Room.databaseBuilder(context, VendinhaDatabase::class.java, "migracao.db")
            .addMigrations(*Migracoes.TODAS)
            .allowMainThreadQueries()
            .build()
        try {
            val turmas = db.turmaDao().observarComGrupos().first()
            assertEquals(listOf("2º A", "2º B", "2º C", "2º D", "2º E"), turmas.map { it.turma.nome })
            assertEquals(3, turmas.last().grupos.size)
            assertEquals("Brigadeiros", turmas.first().gruposOrdenados[1].produto)

            assertEquals(950L, db.vendaDao().obter(1)!!.venda.totalCentavos)
            val r = db.registroDao().listarTodos().single()
            assertEquals(1_500L, r.recebidoCentavos)
            assertEquals(550L, r.trocoCentavos)
            assertEquals("Brigadeiros", r.nomeGrupo)
            assertTrue(r.itens.contains("2× Brigadeiro"))
            assertTrue(r.itens.contains("1× Fatia de bolo"))
        } finally {
            db.close()
        }
    }
}
