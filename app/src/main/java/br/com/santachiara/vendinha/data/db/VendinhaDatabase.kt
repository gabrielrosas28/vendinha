package br.com.santachiara.vendinha.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import br.com.santachiara.vendinha.domain.GruposPredefinidos

@Database(
    entities = [Grupo::class, Item::class, Caixa::class, Venda::class, ItemVenda::class, Fechamento::class, Config::class,
        Turma::class, GrupoCatalogo::class, RegistroVenda::class],
    version = 2,
    exportSchema = true,
)
abstract class VendinhaDatabase : RoomDatabase() {
    abstract fun configDao(): ConfigDao
    abstract fun grupoDao(): GrupoDao
    abstract fun itemDao(): ItemDao
    abstract fun caixaDao(): CaixaDao
    abstract fun vendaDao(): VendaDao
    abstract fun fechamentoDao(): FechamentoDao
    abstract fun manutencaoDao(): ManutencaoDao
    abstract fun turmaDao(): TurmaDao
    abstract fun registroDao(): RegistroDao

    companion object {
        const val NOME = "vendinha.db"

        fun criar(context: Context): VendinhaDatabase =
            Room.databaseBuilder(context.applicationContext, VendinhaDatabase::class.java, NOME)
                .addMigrations(*Migracoes.TODAS)
                .addCallback(Semente)
                .build()
    }
}

object Semente : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        semearTurmas(db)
    }

    fun semearTurmas(db: SupportSQLiteDatabase) {
        for (nome in GruposPredefinidos.TURMAS) {
            val turmaId = db.insert("turma", SQLiteDatabase.CONFLICT_IGNORE, ContentValues().apply { put("nome", nome) })
            if (turmaId <= 0) continue
            for (g in GruposPredefinidos.daTurma(nome)) {
                db.insert("grupo_catalogo", SQLiteDatabase.CONFLICT_ABORT, ContentValues().apply {
                    put("turmaId", turmaId)
                    put("numero", g.numero)
                    put("produto", g.produto)
                })
            }
        }
    }
}

object Migracoes {

    val DE_1_PARA_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `turma` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `nome` TEXT NOT NULL)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_turma_nome` ON `turma` (`nome`)")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `grupo_catalogo` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `turmaId` INTEGER NOT NULL, " +
                    "`numero` INTEGER NOT NULL, `produto` TEXT NOT NULL, " +
                    "FOREIGN KEY(`turmaId`) REFERENCES `turma`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_grupo_catalogo_turmaId` ON `grupo_catalogo` (`turmaId`)")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `registro_venda` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `vendaId` INTEGER NOT NULL, " +
                    "`dataHora` INTEGER NOT NULL, `turma` TEXT NOT NULL, `numeroGrupo` INTEGER NOT NULL, `nomeGrupo` TEXT NOT NULL, " +
                    "`itens` TEXT NOT NULL, `totalCentavos` INTEGER NOT NULL, `recebidoCentavos` INTEGER NOT NULL, " +
                    "`trocoCentavos` INTEGER NOT NULL, `estornada` INTEGER NOT NULL, `estornadaEm` INTEGER)"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_registro_venda_vendaId` ON `registro_venda` (`vendaId`)")

            Semente.semearTurmas(db)

            db.execSQL(
                """INSERT INTO registro_venda (vendaId, dataHora, turma, numeroGrupo, nomeGrupo, itens,
                       totalCentavos, recebidoCentavos, trocoCentavos, estornada, estornadaEm)
                   SELECT v.id, v.dataHora, g.turma, g.numeroGrupo, g.nome,
                       COALESCE((SELECT GROUP_CONCAT(iv.quantidade || '× ' || iv.nomeItem, ', ')
                                 FROM item_venda iv WHERE iv.vendaId = v.id), ''),
                       v.totalCentavos, v.recebidoCentavos, v.trocoCentavos, v.estornada, v.estornadaEm
                   FROM venda v JOIN caixa c ON c.id = v.caixaId JOIN grupo g ON g.id = c.grupoId"""
            )
        }
    }

    val TODAS: Array<Migration> = arrayOf(DE_1_PARA_2)
}
