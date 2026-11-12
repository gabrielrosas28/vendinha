package br.com.santachiara.vendinha.domain

enum class TipoDinheiro { CEDULA, MOEDA }

enum class Denominacao(val centavos: Long, val tipo: TipoDinheiro) {
    R200(20_000, TipoDinheiro.CEDULA),
    R100(10_000, TipoDinheiro.CEDULA),
    R50(5_000, TipoDinheiro.CEDULA),
    R20(2_000, TipoDinheiro.CEDULA),
    R10(1_000, TipoDinheiro.CEDULA),
    R5(500, TipoDinheiro.CEDULA),
    R2(200, TipoDinheiro.CEDULA),
    M1(100, TipoDinheiro.MOEDA),
    M050(50, TipoDinheiro.MOEDA),
    M025(25, TipoDinheiro.MOEDA),
    M010(10, TipoDinheiro.MOEDA),
    M005(5, TipoDinheiro.MOEDA);

    companion object {
        val CEDULAS = entries.filter { it.tipo == TipoDinheiro.CEDULA }
        val MOEDAS = entries.filter { it.tipo == TipoDinheiro.MOEDA }
    }
}

data class Pecas(val denominacao: Denominacao, val quantidade: Int) {
    val totalCentavos: Long get() = denominacao.centavos * quantidade
}

data class Decomposicao(val pecas: List<Pecas>, val restoCentavos: Long) {
    val totalPecas: Int get() = pecas.sumOf { it.quantidade }
}

object Dinheiro {

    const val MENOR_MOEDA_CENTAVOS = 5L

    const val MAXIMO_CENTAVOS = 9_999_995L

    fun formatar(centavos: Long): String {
        val sinal = if (centavos < 0) "-" else ""
        return "${sinal}R$ ${formatarSemSimbolo(centavos)}"
    }

    fun formatarSemSimbolo(centavos: Long): String {
        val abs = if (centavos == Long.MIN_VALUE) Long.MAX_VALUE else kotlin.math.abs(centavos)
        val reais = abs / 100
        val cent = abs % 100
        val reaisTxt = reais.toString().reversed().chunked(3).joinToString(".").reversed()
        return "$reaisTxt,${cent.toString().padStart(2, '0')}"
    }

    fun lerReais(texto: String): Long? {
        var t = texto.trim().replace("R$", "").replace(" ", "").replace(" ", "")
        if (t.isEmpty()) return null
        if (!t.contains(',') && t.count { it == '.' } == 1 && t.substringAfter('.').length in 1..2) {
            t = t.replace('.', ',')
        }
        t = t.replace(".", "")
        val partes = t.split(',')
        if (partes.size > 2) return null
        val inteiro = partes[0].ifEmpty { "0" }
        val decimal = partes.getOrNull(1) ?: ""
        if (!inteiro.all { it.isDigit() } || !decimal.all { it.isDigit() }) return null
        if (decimal.length > 2 || inteiro.length > 7) return null
        val centavos = inteiro.toLong() * 100 + decimal.padEnd(2, '0').ifEmpty { "00" }.toLong()
        return if (centavos > MAXIMO_CENTAVOS) null else centavos
    }

    fun decomporTroco(centavos: Long): Decomposicao {
        require(centavos >= 0) { "Troco não pode ser negativo" }
        var resto = centavos
        val pecas = mutableListOf<Pecas>()
        for (d in Denominacao.entries) {
            val qtd = resto / d.centavos
            if (qtd > 0) {
                pecas += Pecas(d, qtd.toInt())
                resto -= qtd * d.centavos
            }
        }
        return Decomposicao(pecas, resto)
    }

    fun somar(contagem: Map<Denominacao, Int>): Long =
        contagem.entries.sumOf { (d, q) -> d.centavos * q }

    fun emPecas(contagem: Map<Denominacao, Int>): List<Pecas> =
        Denominacao.entries.mapNotNull { d -> contagem[d]?.takeIf { it > 0 }?.let { Pecas(d, it) } }

    fun ehMultiploDaMenorMoeda(centavos: Long): Boolean = centavos % MENOR_MOEDA_CENTAVOS == 0L
}
