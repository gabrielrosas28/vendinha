package br.com.santachiara.vendinha.domain

data class LinhaVendida(
    val itemId: Long,
    val nome: String,
    val quantidade: Int,
    val precoUnitCentavos: Long,
    val estornada: Boolean,
)

data class ItemVendido(val itemId: Long, val nome: String, val quantidade: Int, val totalCentavos: Long)

data class ResumoCaixa(
    val totalGanhoCentavos: Long,
    val numeroVendas: Int,
    val numeroEstornadas: Int,
    val itens: List<ItemVendido>,
    val campeao: ItemVendido?,
)

object Resumo {

    fun calcular(vendas: List<ValorVenda>, linhas: List<LinhaVendida>): ResumoCaixa {
        val itens = linhas.filter { !it.estornada }
            .groupBy { it.itemId }
            .map { (id, ls) ->
                ItemVendido(
                    itemId = id,
                    nome = ls.last().nome,
                    quantidade = ls.sumOf { it.quantidade },
                    totalCentavos = ls.sumOf { it.precoUnitCentavos * it.quantidade },
                )
            }
            .sortedWith(compareByDescending<ItemVendido> { it.quantidade }.thenByDescending { it.totalCentavos }.thenBy { it.nome })
        return ResumoCaixa(
            totalGanhoCentavos = ContaCaixa.totalGanho(vendas),
            numeroVendas = ContaCaixa.numeroVendasValidas(vendas),
            numeroEstornadas = vendas.count { it.estornada },
            itens = itens,
            campeao = itens.firstOrNull(),
        )
    }
}
