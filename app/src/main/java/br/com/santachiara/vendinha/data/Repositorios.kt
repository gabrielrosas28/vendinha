package br.com.santachiara.vendinha.data

import androidx.room.withTransaction
import br.com.santachiara.vendinha.data.db.Caixa
import br.com.santachiara.vendinha.data.db.Config
import br.com.santachiara.vendinha.data.db.ConfigDao
import br.com.santachiara.vendinha.data.db.Fechamento
import br.com.santachiara.vendinha.data.db.Grupo
import br.com.santachiara.vendinha.data.db.Item
import br.com.santachiara.vendinha.data.db.LinhaPedido
import br.com.santachiara.vendinha.data.db.QtdVendida
import br.com.santachiara.vendinha.data.db.RegistroVenda
import br.com.santachiara.vendinha.data.db.Turma
import br.com.santachiara.vendinha.data.db.TurmaComGrupos
import br.com.santachiara.vendinha.domain.Registro
import br.com.santachiara.vendinha.data.db.StatusCaixa
import br.com.santachiara.vendinha.data.db.VendaComItens
import br.com.santachiara.vendinha.data.db.VendinhaDatabase
import br.com.santachiara.vendinha.domain.ContaCaixa
import br.com.santachiara.vendinha.domain.LinhaVendida
import br.com.santachiara.vendinha.domain.Resumo
import br.com.santachiara.vendinha.domain.ResumoCaixa
import br.com.santachiara.vendinha.domain.Senha
import br.com.santachiara.vendinha.domain.ValorVenda
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import java.io.File

class ConfigRepositorio(private val dao: ConfigDao) {

    fun observar(): Flow<Config?> = dao.observar()

    suspend fun criarSenha(senha: String) {
        require(Senha.formatoValido(senha)) { "Senha precisa ter de 4 a 8 dígitos" }
        val salt = Senha.gerarSalt()
        val hash = withContext(Dispatchers.Default) { Senha.hash(senha, salt) }
        val atual = dao.obter()
        dao.salvar(Config(senhaHash = hash, senhaSalt = salt, somAtivo = atual?.somAtivo ?: true))
    }

    suspend fun conferir(senha: String): Boolean {
        val c = dao.obter() ?: return false
        return withContext(Dispatchers.Default) { Senha.confere(senha, c.senhaSalt, c.senhaHash) }
    }

    suspend fun definirSom(ativo: Boolean) = dao.definirSom(ativo)
}

class GrupoRepositorio(private val db: VendinhaDatabase) {
    private val dao = db.grupoDao()

    fun observarAtivo(): Flow<Grupo?> = dao.observarAtivo()

    suspend fun iniciar(turma: String, numeroGrupo: Int, nome: String, corIndex: Int, agora: Long = System.currentTimeMillis()): Long =
        db.withTransaction {
            dao.obterAtivo()?.id ?: dao.inserir(
                Grupo(turma = turma.trim(), numeroGrupo = numeroGrupo, nome = nome.trim(), corIndex = corIndex, iniciadoEm = agora, ativo = true)
            )
        }

    suspend fun renomear(id: Long, nome: String) = dao.renomear(id, nome.trim())
}

enum class ResultadoTurma { OK, NOME_INVALIDO, JA_EXISTE }

class TurmaRepositorio(private val db: VendinhaDatabase) {
    private val dao = db.turmaDao()

    fun observarTodas(): Flow<List<TurmaComGrupos>> = dao.observarComGrupos()
    fun observar(id: Long): Flow<TurmaComGrupos?> = dao.observar(id)

    suspend fun criar(nome: String): ResultadoTurma = salvarNome(nome) { dao.inserir(Turma(nome = it)) }

    suspend fun renomear(id: Long, nome: String): ResultadoTurma = salvarNome(nome, ignorarId = id) { dao.renomear(id, it) }

    suspend fun apagar(id: Long) = dao.apagar(id)

    suspend fun adicionarGrupo(turmaId: Long, produto: String): Boolean {
        if (!Registro.nomeGrupoValido(produto)) return false
        dao.adicionarGrupo(turmaId, produto.trim())
        return true
    }

    suspend fun renomearGrupo(id: Long, produto: String): Boolean {
        if (!Registro.nomeGrupoValido(produto)) return false
        dao.renomearGrupo(id, produto.trim())
        return true
    }

    suspend fun apagarGrupo(id: Long) = dao.apagarGrupo(id)

    private suspend fun salvarNome(nome: String, ignorarId: Long? = null, gravar: suspend (String) -> Unit): ResultadoTurma {
        val limpo = nome.trim().replace(Regex("\\s+"), " ")
        if (!Registro.nomeTurmaValido(limpo)) return ResultadoTurma.NOME_INVALIDO
        val repetida = dao.listar().any { it.id != ignorarId && it.nome.equals(limpo, ignoreCase = true) }
        if (repetida) return ResultadoTurma.JA_EXISTE
        gravar(limpo)
        return ResultadoTurma.OK
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ProdutoRepositorio(db: VendinhaDatabase, private val fotos: FotoArmazenamento) {
    private val grupoDao = db.grupoDao()
    private val itemDao = db.itemDao()

    fun observarTodos(): Flow<List<Item>> = grupoDao.observarAtivo().flatMapLatest { g ->
        if (g == null) flowOf(emptyList()) else itemDao.observarDoGrupo(g.id)
    }

    fun observarAtivos(): Flow<List<Item>> = grupoDao.observarAtivo().flatMapLatest { g ->
        if (g == null) flowOf(emptyList()) else itemDao.observarAtivos(g.id)
    }

    suspend fun obter(id: Long): Item? = itemDao.obter(id)

    suspend fun salvar(item: Item): Long {
        if (item.id == 0L) {
            val grupo = grupoDao.obterAtivo() ?: error("Nenhum grupo ativo")
            return itemDao.inserir(item.copy(grupoId = grupo.id))
        }
        val antigo = itemDao.obter(item.id)
        itemDao.atualizar(item)
        if (antigo != null && antigo.fotoPath != item.fotoPath) fotos.apagar(antigo.fotoPath)
        return item.id
    }

    suspend fun definirAtivo(item: Item, ativo: Boolean) = itemDao.atualizar(item.copy(ativo = ativo))
}

@OptIn(ExperimentalCoroutinesApi::class)
class CaixaRepositorio(
    private val db: VendinhaDatabase,
    private val relogio: () -> Long = System::currentTimeMillis,
) {
    private val grupoDao = db.grupoDao()
    private val caixaDao = db.caixaDao()
    private val vendaDao = db.vendaDao()
    private val fechamentoDao = db.fechamentoDao()

    fun observarCaixaAtual(): Flow<Caixa?> = grupoDao.observarAtivo().flatMapLatest { g ->
        if (g == null) flowOf(null) else caixaDao.observarDoGrupo(g.id)
    }

    fun observarVendasAtuais(): Flow<List<VendaComItens>> = observarCaixaAtual().flatMapLatest { c ->
        if (c == null) flowOf(emptyList()) else vendaDao.observarDoCaixa(c.id)
    }

    fun observarQtdVendida(): Flow<List<QtdVendida>> = observarCaixaAtual().flatMapLatest { c ->
        if (c == null) flowOf(emptyList()) else vendaDao.observarQtdVendida(c.id)
    }

    fun observarVenda(id: Long): Flow<VendaComItens?> = vendaDao.observar(id)

    suspend fun obterVenda(id: Long): VendaComItens? = vendaDao.obter(id)

    suspend fun abrir(trocoInicialCentavos: Long): Long = db.withTransaction {
        val grupo = grupoDao.obterAtivo() ?: error("Nenhum grupo ativo")
        val existente = caixaDao.ultimoDoGrupo(grupo.id)
        existente?.id ?: caixaDao.inserir(
            Caixa(
                grupoId = grupo.id,
                abertoEm = relogio(),
                fechadoEm = null,
                trocoInicialCentavos = trocoInicialCentavos.coerceAtLeast(0),
                contadoCentavos = null,
                status = StatusCaixa.ABERTO,
            )
        )
    }

    suspend fun confirmarVenda(caixaId: Long, linhas: List<LinhaPedido>, recebidoCentavos: Long): Long =
        vendaDao.confirmarVenda(caixaId, linhas, recebidoCentavos, relogio())

    suspend fun estornar(vendaId: Long): Boolean = vendaDao.estornar(vendaId, relogio())

    suspend fun resumo(caixaId: Long): ResumoCaixa = calcularResumo(vendaDao.listarDoCaixa(caixaId))

    suspend fun fechar(caixaId: Long, contadoCentavos: Long?): Long = db.withTransaction {
        val caixa = caixaDao.obter(caixaId) ?: error("Caixa não existe")
        val grupo = grupoDao.obterAtivo() ?: error("Nenhum grupo ativo")
        val agora = relogio()
        caixaDao.fechar(caixaId, agora, contadoCentavos)
        val resumo = calcularResumo(vendaDao.listarDoCaixa(caixaId))
        val esperado = ContaCaixa.dinheiroEsperado(caixa.trocoInicialCentavos, resumo.totalGanhoCentavos)
        val json = RelatorioJson.gerar(resumo, esperado, caixa.abertoEm, agora)
        val anterior = fechamentoDao.obterPorCaixa(caixaId)
        val novo = Fechamento(
            id = anterior?.id ?: 0,
            turma = grupo.turma,
            numeroGrupo = grupo.numeroGrupo,
            nomeGrupo = grupo.nome,
            data = agora,
            totalCentavos = resumo.totalGanhoCentavos,
            numVendas = resumo.numeroVendas,
            trocoInicialCentavos = caixa.trocoInicialCentavos,
            contadoCentavos = contadoCentavos,
            resumoJson = json,
            imagemPath = null,
            caixaId = caixaId,
        )
        if (anterior == null) fechamentoDao.inserir(novo) else {
            anterior.imagemPath?.let { File(it).delete() }
            fechamentoDao.atualizar(novo)
            anterior.id
        }
    }

    suspend fun reabrir(caixaId: Long) = caixaDao.reabrir(caixaId)

    suspend fun fechamentoDoCaixa(caixaId: Long): Long? = fechamentoDao.obterPorCaixa(caixaId)?.id

    fun observarFechamentos(): Flow<List<Fechamento>> = fechamentoDao.observarTodos()
    fun observarFechamento(id: Long): Flow<Fechamento?> = fechamentoDao.observar(id)
    suspend fun listarFechamentos(): List<Fechamento> = fechamentoDao.listarTodos()
    suspend fun definirImagemFechamento(id: Long, path: String) = fechamentoDao.definirImagem(id, path)

    suspend fun apagarHistorico() {
        val imagens = fechamentoDao.todasAsImagens()
        db.withTransaction {
            fechamentoDao.apagarTodos()
            db.registroDao().apagarTodos()
        }
        withContext(Dispatchers.IO) { imagens.forEach { File(it).delete() } }
    }

    fun observarRegistro(): Flow<List<RegistroVenda>> = db.registroDao().observarTodos()

    suspend fun listarRegistro(): List<RegistroVenda> = db.registroDao().listarTodos()

    private fun calcularResumo(vendas: List<VendaComItens>): ResumoCaixa = Resumo.calcular(
        vendas.map { ValorVenda(it.venda.totalCentavos, it.venda.estornada) },
        vendas.flatMap { v ->
            v.itens.map { LinhaVendida(it.itemId, it.nomeItem, it.quantidade, it.precoUnitCentavos, v.venda.estornada) }
        },
    )
}
