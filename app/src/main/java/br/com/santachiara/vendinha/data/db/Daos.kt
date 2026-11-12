package br.com.santachiara.vendinha.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

class EstoqueInsuficienteException(val nomeItem: String) : IllegalStateException("Sem estoque: $nomeItem")
class CaixaFechadoException : IllegalStateException("O caixa não está aberto")

@Dao
interface ConfigDao {
    @Query("SELECT * FROM config WHERE id = 1")
    fun observar(): Flow<Config?>

    @Query("SELECT * FROM config WHERE id = 1")
    suspend fun obter(): Config?

    @Upsert
    suspend fun salvar(config: Config)

    @Query("UPDATE config SET somAtivo = :ativo WHERE id = 1")
    suspend fun definirSom(ativo: Boolean)
}

@Dao
interface GrupoDao {
    @Query("SELECT * FROM grupo WHERE ativo = 1 ORDER BY id DESC LIMIT 1")
    fun observarAtivo(): Flow<Grupo?>

    @Query("SELECT * FROM grupo WHERE ativo = 1 ORDER BY id DESC LIMIT 1")
    suspend fun obterAtivo(): Grupo?

    @Insert
    suspend fun inserir(grupo: Grupo): Long

    @Query("UPDATE grupo SET nome = :nome WHERE id = :id")
    suspend fun renomear(id: Long, nome: String)
}

@Dao
interface ItemDao {
    @Query("SELECT * FROM item WHERE grupoId = :grupoId ORDER BY ativo DESC, nome COLLATE NOCASE")
    fun observarDoGrupo(grupoId: Long): Flow<List<Item>>

    @Query("SELECT * FROM item WHERE grupoId = :grupoId AND ativo = 1 ORDER BY nome COLLATE NOCASE")
    fun observarAtivos(grupoId: Long): Flow<List<Item>>

    @Query("SELECT * FROM item WHERE id = :id")
    suspend fun obter(id: Long): Item?

    @Insert
    suspend fun inserir(item: Item): Long

    @Update
    suspend fun atualizar(item: Item)

    @Query("SELECT fotoPath FROM item")
    suspend fun todasAsFotos(): List<String>
}

@Dao
interface CaixaDao {
    @Query("SELECT * FROM caixa WHERE grupoId = :grupoId ORDER BY id DESC LIMIT 1")
    fun observarDoGrupo(grupoId: Long): Flow<Caixa?>

    @Query("SELECT * FROM caixa WHERE grupoId = :grupoId ORDER BY id DESC LIMIT 1")
    suspend fun ultimoDoGrupo(grupoId: Long): Caixa?

    @Query("SELECT * FROM caixa WHERE id = :id")
    suspend fun obter(id: Long): Caixa?

    @Insert
    suspend fun inserir(caixa: Caixa): Long

    @Query("UPDATE caixa SET status = 'FECHADO', fechadoEm = :fechadoEm, contadoCentavos = :contado WHERE id = :id")
    suspend fun fechar(id: Long, fechadoEm: Long, contado: Long?)

    @Query("UPDATE caixa SET status = 'ABERTO', fechadoEm = NULL WHERE id = :id")
    suspend fun reabrir(id: Long)
}

@Dao
abstract class VendaDao {

    @Transaction
    @Query("SELECT * FROM venda WHERE caixaId = :caixaId ORDER BY dataHora DESC, id DESC")
    abstract fun observarDoCaixa(caixaId: Long): Flow<List<VendaComItens>>

    @Transaction
    @Query("SELECT * FROM venda WHERE caixaId = :caixaId ORDER BY dataHora DESC, id DESC")
    abstract suspend fun listarDoCaixa(caixaId: Long): List<VendaComItens>

    @Transaction
    @Query("SELECT * FROM venda WHERE id = :id")
    abstract fun observar(id: Long): Flow<VendaComItens?>

    @Transaction
    @Query("SELECT * FROM venda WHERE id = :id")
    abstract suspend fun obter(id: Long): VendaComItens?

    @Query(
        """SELECT iv.itemId AS itemId, SUM(iv.quantidade) AS quantidade
           FROM item_venda iv JOIN venda v ON v.id = iv.vendaId
           WHERE v.caixaId = :caixaId AND v.estornada = 0
           GROUP BY iv.itemId"""
    )
    abstract fun observarQtdVendida(caixaId: Long): Flow<List<QtdVendida>>

    @Insert protected abstract suspend fun inserirVenda(venda: Venda): Long
    @Insert protected abstract suspend fun inserirItens(itens: List<ItemVenda>)

    @Query("SELECT * FROM caixa WHERE id = :id")
    protected abstract suspend fun caixa(id: Long): Caixa?

    @Query("SELECT * FROM item WHERE id = :id")
    protected abstract suspend fun item(id: Long): Item?

    @Query(
        """UPDATE item SET qtdDisponivel = qtdDisponivel - :quantidade
           WHERE id = :itemId AND controlaEstoque = 1 AND qtdDisponivel >= :quantidade"""
    )
    protected abstract suspend fun baixarEstoque(itemId: Long, quantidade: Int): Int

    @Query("UPDATE item SET qtdDisponivel = qtdDisponivel + :quantidade WHERE id = :itemId AND controlaEstoque = 1")
    protected abstract suspend fun devolverEstoque(itemId: Long, quantidade: Int): Int

    @Query("UPDATE venda SET estornada = 1, estornadaEm = :em WHERE id = :vendaId AND estornada = 0")
    protected abstract suspend fun marcarEstornada(vendaId: Long, em: Long): Int

    @Query("SELECT * FROM item_venda WHERE vendaId = :vendaId")
    protected abstract suspend fun itensDaVenda(vendaId: Long): List<ItemVenda>

    @Query("SELECT * FROM grupo WHERE id = :id")
    protected abstract suspend fun grupo(id: Long): Grupo?

    @Insert protected abstract suspend fun inserirRegistro(r: RegistroVenda)

    @Query("UPDATE registro_venda SET estornada = 1, estornadaEm = :em WHERE vendaId = :vendaId")
    protected abstract suspend fun estornarRegistro(vendaId: Long, em: Long)

    @Transaction
    open suspend fun confirmarVenda(
        caixaId: Long,
        linhas: List<LinhaPedido>,
        recebidoCentavos: Long,
        agora: Long,
    ): Long {
        val linhasValidas = linhas.filter { it.quantidade > 0 }
        require(linhasValidas.isNotEmpty()) { "Pedido vazio" }
        val total = linhasValidas.sumOf { it.precoUnitCentavos * it.quantidade }
        require(recebidoCentavos >= total) { "Recebido menor que o total" }
        val caixaAtual = caixa(caixaId)
        if (caixaAtual?.status != StatusCaixa.ABERTO) throw CaixaFechadoException()

        val vendaId = inserirVenda(
            Venda(
                caixaId = caixaId,
                dataHora = agora,
                totalCentavos = total,
                recebidoCentavos = recebidoCentavos,
                trocoCentavos = recebidoCentavos - total,
                estornada = false,
                estornadaEm = null,
            )
        )
        for (l in linhasValidas) {
            val atual = item(l.itemId)
            if (atual != null && atual.controlaEstoque && baixarEstoque(l.itemId, l.quantidade) == 0) {
                throw EstoqueInsuficienteException(l.nome)
            }
        }
        inserirItens(
            linhasValidas.map {
                ItemVenda(
                    vendaId = vendaId,
                    itemId = it.itemId,
                    nomeItem = it.nome,
                    quantidade = it.quantidade,
                    precoUnitCentavos = it.precoUnitCentavos,
                )
            }
        )
        val g = caixaAtual?.grupoId?.let { grupo(it) }
        inserirRegistro(
            RegistroVenda(
                vendaId = vendaId,
                dataHora = agora,
                turma = g?.turma.orEmpty(),
                numeroGrupo = g?.numeroGrupo ?: 0,
                nomeGrupo = g?.nome.orEmpty(),
                itens = linhasValidas.joinToString(", ") { "${it.quantidade}× ${it.nome}" },
                totalCentavos = total,
                recebidoCentavos = recebidoCentavos,
                trocoCentavos = recebidoCentavos - total,
                estornada = false,
                estornadaEm = null,
            )
        )
        return vendaId
    }

    @Transaction
    open suspend fun estornar(vendaId: Long, agora: Long): Boolean {
        if (marcarEstornada(vendaId, agora) == 0) return false
        for (iv in itensDaVenda(vendaId)) devolverEstoque(iv.itemId, iv.quantidade)
        estornarRegistro(vendaId, agora)
        return true
    }
}

@Dao
abstract class TurmaDao {
    @Transaction
    @Query("SELECT * FROM turma ORDER BY nome COLLATE NOCASE")
    abstract fun observarComGrupos(): Flow<List<TurmaComGrupos>>

    @Transaction
    @Query("SELECT * FROM turma WHERE id = :id")
    abstract fun observar(id: Long): Flow<TurmaComGrupos?>

    @Query("SELECT * FROM turma ORDER BY nome COLLATE NOCASE")
    abstract suspend fun listar(): List<Turma>

    @Insert abstract suspend fun inserir(t: Turma): Long

    @Query("UPDATE turma SET nome = :nome WHERE id = :id")
    abstract suspend fun renomear(id: Long, nome: String)

    @Query("DELETE FROM grupo_catalogo WHERE turmaId = :turmaId")
    protected abstract suspend fun apagarGruposDaTurma(turmaId: Long)

    @Query("DELETE FROM turma WHERE id = :id")
    protected abstract suspend fun apagarSoTurma(id: Long)

    @Transaction
    open suspend fun apagar(id: Long) {
        apagarGruposDaTurma(id)
        apagarSoTurma(id)
    }

    @Query("SELECT COALESCE(MAX(numero), 0) FROM grupo_catalogo WHERE turmaId = :turmaId")
    protected abstract suspend fun maiorNumero(turmaId: Long): Int

    @Insert protected abstract suspend fun inserirGrupoCatalogo(g: GrupoCatalogo): Long

    @Transaction
    open suspend fun adicionarGrupo(turmaId: Long, produto: String): Long =
        inserirGrupoCatalogo(GrupoCatalogo(turmaId = turmaId, numero = maiorNumero(turmaId) + 1, produto = produto))

    @Query("UPDATE grupo_catalogo SET produto = :produto WHERE id = :id")
    abstract suspend fun renomearGrupo(id: Long, produto: String)

    @Query("DELETE FROM grupo_catalogo WHERE id = :id")
    abstract suspend fun apagarGrupo(id: Long)
}

@Dao
interface RegistroDao {
    @Query("SELECT * FROM registro_venda ORDER BY dataHora DESC, id DESC")
    fun observarTodos(): Flow<List<RegistroVenda>>

    @Query("SELECT * FROM registro_venda ORDER BY dataHora DESC, id DESC")
    suspend fun listarTodos(): List<RegistroVenda>

    @Query("DELETE FROM registro_venda")
    suspend fun apagarTodos()
}

@Dao
interface FechamentoDao {
    @Query("SELECT * FROM fechamento ORDER BY data DESC, id DESC")
    fun observarTodos(): Flow<List<Fechamento>>

    @Query("SELECT * FROM fechamento ORDER BY data DESC, id DESC")
    suspend fun listarTodos(): List<Fechamento>

    @Query("SELECT * FROM fechamento WHERE id = :id")
    fun observar(id: Long): Flow<Fechamento?>

    @Query("SELECT * FROM fechamento WHERE caixaId = :caixaId ORDER BY id DESC LIMIT 1")
    suspend fun obterPorCaixa(caixaId: Long): Fechamento?

    @Insert
    suspend fun inserir(f: Fechamento): Long

    @Update
    suspend fun atualizar(f: Fechamento)

    @Query("UPDATE fechamento SET imagemPath = :path WHERE id = :id")
    suspend fun definirImagem(id: Long, path: String)

    @Query("SELECT imagemPath FROM fechamento WHERE imagemPath IS NOT NULL")
    suspend fun todasAsImagens(): List<String>

    @Query("DELETE FROM fechamento")
    suspend fun apagarTodos()
}

@Dao
abstract class ManutencaoDao {
    @Query("DELETE FROM item_venda") protected abstract suspend fun apagarItensVenda()
    @Query("DELETE FROM venda") protected abstract suspend fun apagarVendas()
    @Query("DELETE FROM caixa") protected abstract suspend fun apagarCaixas()
    @Query("DELETE FROM item") protected abstract suspend fun apagarItens()
    @Query("DELETE FROM grupo") protected abstract suspend fun apagarGrupos()

    @Transaction
    open suspend fun zerarTablet() {
        apagarItensVenda()
        apagarVendas()
        apagarCaixas()
        apagarItens()
        apagarGrupos()
    }
}
