package br.com.santachiara.vendinha

import android.content.Context
import br.com.santachiara.vendinha.data.CaixaRepositorio
import br.com.santachiara.vendinha.data.ConfigRepositorio
import br.com.santachiara.vendinha.data.FotoArmazenamento
import br.com.santachiara.vendinha.data.GrupoRepositorio
import br.com.santachiara.vendinha.data.ProdutoRepositorio
import br.com.santachiara.vendinha.data.SomCaixa
import br.com.santachiara.vendinha.data.TurmaRepositorio
import br.com.santachiara.vendinha.data.db.VendinhaDatabase

class AppContainer(context: Context) {
    private val app = context.applicationContext

    val db: VendinhaDatabase by lazy { VendinhaDatabase.criar(app) }
    val config by lazy { ConfigRepositorio(db.configDao()) }
    val grupos by lazy { GrupoRepositorio(db) }
    val turmas by lazy { TurmaRepositorio(db) }
    val fotos by lazy { FotoArmazenamento(app) }
    val produtos by lazy { ProdutoRepositorio(db, fotos) }
    val caixa by lazy { CaixaRepositorio(db) }
    val som by lazy { SomCaixa(app) }

    suspend fun zerarTablet() {
        db.manutencaoDao().zerarTablet()
        fotos.apagarTodas()
    }
}
