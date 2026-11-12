package br.com.santachiara.vendinha.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "grupo")
data class Grupo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val turma: String,
    val numeroGrupo: Int,
    val nome: String,
    val corIndex: Int,
    val iniciadoEm: Long,
    val ativo: Boolean,
)

@Entity(
    tableName = "item",
    foreignKeys = [ForeignKey(Grupo::class, ["id"], ["grupoId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("grupoId")],
)
data class Item(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val grupoId: Long,
    val nome: String,
    val fotoPath: String,
    val precoCentavos: Long,
    val controlaEstoque: Boolean,
    val qtdInicial: Int?,
    val qtdDisponivel: Int?,
    val ativo: Boolean,
)

enum class StatusCaixa { ABERTO, FECHADO }

@Entity(
    tableName = "caixa",
    foreignKeys = [ForeignKey(Grupo::class, ["id"], ["grupoId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("grupoId")],
)
data class Caixa(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val grupoId: Long,
    val abertoEm: Long,
    val fechadoEm: Long?,
    val trocoInicialCentavos: Long,
    val contadoCentavos: Long?,
    val status: StatusCaixa,
)

@Entity(
    tableName = "venda",
    foreignKeys = [ForeignKey(Caixa::class, ["id"], ["caixaId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("caixaId")],
)
data class Venda(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val caixaId: Long,
    val dataHora: Long,
    val totalCentavos: Long,
    val recebidoCentavos: Long,
    val trocoCentavos: Long,
    val estornada: Boolean,
    val estornadaEm: Long?,
)

@Entity(
    tableName = "item_venda",
    foreignKeys = [ForeignKey(Venda::class, ["id"], ["vendaId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("vendaId"), Index("itemId")],
)
data class ItemVenda(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vendaId: Long,
    val itemId: Long,
    val nomeItem: String,
    val quantidade: Int,
    val precoUnitCentavos: Long,
)

@Entity(tableName = "fechamento")
data class Fechamento(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val turma: String,
    val numeroGrupo: Int,
    val nomeGrupo: String,
    val data: Long,
    val totalCentavos: Long,
    val numVendas: Int,
    val trocoInicialCentavos: Long,
    val contadoCentavos: Long?,
    val resumoJson: String,
    val imagemPath: String?,
    val caixaId: Long?,
)

@Entity(tableName = "config")
data class Config(
    @PrimaryKey val id: Int = 1,
    val senhaHash: String,
    val senhaSalt: String,
    val somAtivo: Boolean,
)

@Entity(tableName = "turma", indices = [Index(value = ["nome"], unique = true)])
data class Turma(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
)

@Entity(
    tableName = "grupo_catalogo",
    foreignKeys = [ForeignKey(Turma::class, ["id"], ["turmaId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("turmaId")],
)
data class GrupoCatalogo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val turmaId: Long,
    val numero: Int,
    val produto: String,
)

data class TurmaComGrupos(
    @Embedded val turma: Turma,
    @Relation(parentColumn = "id", entityColumn = "turmaId") val grupos: List<GrupoCatalogo>,
) {
    val gruposOrdenados: List<GrupoCatalogo> get() = grupos.sortedBy { it.numero }
}

@Entity(tableName = "registro_venda", indices = [Index("vendaId")])
data class RegistroVenda(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vendaId: Long,
    val dataHora: Long,
    val turma: String,
    val numeroGrupo: Int,
    val nomeGrupo: String,
    val itens: String,
    val totalCentavos: Long,
    val recebidoCentavos: Long,
    val trocoCentavos: Long,
    val estornada: Boolean,
    val estornadaEm: Long?,
)

data class VendaComItens(
    @Embedded val venda: Venda,
    @Relation(parentColumn = "id", entityColumn = "vendaId") val itens: List<ItemVenda>,
)

data class QtdVendida(val itemId: Long, val quantidade: Int)

data class LinhaPedido(
    val itemId: Long,
    val nome: String,
    val precoUnitCentavos: Long,
    val quantidade: Int,
)
