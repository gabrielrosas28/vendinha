package br.com.santachiara.vendinha.domain

data class ValorRegistro(
    val totalCentavos: Long,
    val recebidoCentavos: Long,
    val trocoCentavos: Long,
    val estornada: Boolean,
)

data class TotaisRegistro(
    val numeroVendas: Int,
    val numeroEstornadas: Int,
    val vendidoCentavos: Long,
    val recebidoCentavos: Long,
    val trocoCentavos: Long,
)

object Registro {
    fun totais(vendas: List<ValorRegistro>): TotaisRegistro {
        val validas = vendas.filterNot { it.estornada }
        return TotaisRegistro(
            numeroVendas = validas.size,
            numeroEstornadas = vendas.size - validas.size,
            vendidoCentavos = validas.sumOf { it.totalCentavos },
            recebidoCentavos = validas.sumOf { it.recebidoCentavos },
            trocoCentavos = validas.sumOf { it.trocoCentavos },
        )
    }

    fun nomeTurmaValido(nome: String): Boolean = nome.trim().length in 1..20

    fun nomeGrupoValido(nome: String): Boolean = nome.trim().length in 1..40
}
