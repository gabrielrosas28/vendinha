package br.com.santachiara.vendinha.domain

data class ValorVenda(val totalCentavos: Long, val estornada: Boolean)

enum class ResultadoConferencia { BATEU, SOBROU, FALTOU }

object ContaCaixa {

    fun totalGanho(vendas: List<ValorVenda>): Long =
        vendas.sumOf { it.totalCentavos } - vendas.filter { it.estornada }.sumOf { it.totalCentavos }

    fun numeroVendasValidas(vendas: List<ValorVenda>): Int = vendas.count { !it.estornada }

    fun dinheiroEsperado(trocoInicialCentavos: Long, totalGanhoCentavos: Long): Long =
        trocoInicialCentavos + totalGanhoCentavos

    fun diferenca(contadoCentavos: Long, esperadoCentavos: Long): Long =
        contadoCentavos - esperadoCentavos

    fun resultado(diferencaCentavos: Long): ResultadoConferencia = when {
        diferencaCentavos > 0 -> ResultadoConferencia.SOBROU
        diferencaCentavos < 0 -> ResultadoConferencia.FALTOU
        else -> ResultadoConferencia.BATEU
    }

    fun troco(totalCentavos: Long, recebidoCentavos: Long): Long = recebidoCentavos - totalCentavos

    fun podeConfirmar(totalCentavos: Long, recebidoCentavos: Long): Boolean =
        totalCentavos > 0 && recebidoCentavos >= totalCentavos
}
