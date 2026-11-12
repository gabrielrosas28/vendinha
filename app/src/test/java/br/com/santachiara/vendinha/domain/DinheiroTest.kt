package br.com.santachiara.vendinha.domain

import br.com.santachiara.vendinha.domain.Denominacao.M005
import br.com.santachiara.vendinha.domain.Denominacao.M010
import br.com.santachiara.vendinha.domain.Denominacao.M025
import br.com.santachiara.vendinha.domain.Denominacao.M050
import br.com.santachiara.vendinha.domain.Denominacao.M1
import br.com.santachiara.vendinha.domain.Denominacao.R100
import br.com.santachiara.vendinha.domain.Denominacao.R2
import br.com.santachiara.vendinha.domain.Denominacao.R20
import br.com.santachiara.vendinha.domain.Denominacao.R5
import br.com.santachiara.vendinha.domain.Denominacao.R50
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DecomposicaoTrocoTest {

    private fun pecas(centavos: Long) = Dinheiro.decomporTroco(centavos).pecas

    @Test fun `troco zero nao tem pecas`() {
        val d = Dinheiro.decomporTroco(0)
        assertTrue(d.pecas.isEmpty())
        assertEquals(0L, d.restoCentavos)
    }

    @Test fun `cinco centavos`() {
        assertEquals(listOf(Pecas(M005, 1)), pecas(5))
    }

    @Test fun `sete e cinquenta`() {
        assertEquals(listOf(Pecas(R5, 1), Pecas(R2, 1), Pecas(M050, 1)), pecas(750))
    }

    @Test fun `oito reais vira 5 + 2 + 1`() {
        assertEquals(listOf(Pecas(R5, 1), Pecas(R2, 1), Pecas(M1, 1)), pecas(800))
    }

    @Test fun `cento e noventa e nove e noventa e cinco`() {
        assertEquals(
            listOf(
                Pecas(R100, 1), Pecas(R50, 1), Pecas(R20, 2), Pecas(R5, 1), Pecas(R2, 2),
                Pecas(M050, 1), Pecas(M025, 1), Pecas(M010, 2),
            ),
            pecas(19_995),
        )
        val d = Dinheiro.decomporTroco(19_995)
        assertEquals(19_995L, d.pecas.sumOf { it.totalCentavos })
        assertEquals(0L, d.restoCentavos)
    }

    @Test fun `quatro reais usa duas notas de 2`() {
        assertEquals(listOf(Pecas(R2, 2)), pecas(400))
    }

    @Test fun `valor fora do multiplo de 5 informa o resto`() {
        val d = Dinheiro.decomporTroco(103)
        assertEquals(listOf(Pecas(M1, 1)), d.pecas)
        assertEquals(3L, d.restoCentavos)
    }

    @Test fun `guloso e otimo para todos os valores ate 50 reais`() {
        val dens = Denominacao.entries.map { it.centavos.toInt() }
        val limite = 5_000
        val melhor = IntArray(limite + 1) { Int.MAX_VALUE }
        melhor[0] = 0
        for (v in 5..limite step 5) for (d in dens) if (d <= v && melhor[v - d] != Int.MAX_VALUE) {
            melhor[v] = minOf(melhor[v], melhor[v - d] + 1)
        }
        for (v in 0..limite step 5) {
            assertEquals("valor $v", melhor[v], Dinheiro.decomporTroco(v.toLong()).totalPecas)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `troco negativo e erro`() {
        Dinheiro.decomporTroco(-5)
    }
}

class FormatacaoTest {

    @Test fun zero() = assertEquals("R$ 0,00", Dinheiro.formatar(0))
    @Test fun centavos() = assertEquals("R$ 0,05", Dinheiro.formatar(5))
    @Test fun reais() = assertEquals("R$ 7,50", Dinheiro.formatar(750))
    @Test fun milhar() = assertEquals("R$ 1.234,56", Dinheiro.formatar(123_456))
    @Test fun milhao() = assertEquals("R$ 1.000.000,00", Dinheiro.formatar(100_000_000))
    @Test fun negativo() = assertEquals("-R$ 5,00", Dinheiro.formatar(-500))
    @Test fun semSimbolo() = assertEquals("199,95", Dinheiro.formatarSemSimbolo(19_995))

    @Test fun `ler valores digitados`() {
        assertEquals(200L, Dinheiro.lerReais("2"))
        assertEquals(250L, Dinheiro.lerReais("2,5"))
        assertEquals(250L, Dinheiro.lerReais("2,50"))
        assertEquals(250L, Dinheiro.lerReais("2.50"))
        assertEquals(123_456L, Dinheiro.lerReais("1.234,56"))
        assertEquals(300L, Dinheiro.lerReais("R$ 3,00"))
        assertEquals(5L, Dinheiro.lerReais(",05"))
        assertNull(Dinheiro.lerReais(""))
        assertNull(Dinheiro.lerReais("abc"))
        assertNull(Dinheiro.lerReais("2,555"))
        assertNull(Dinheiro.lerReais("1,2,3"))
    }

    @Test fun `somar contagem de pecas`() {
        assertEquals(1_275L, Dinheiro.somar(mapOf(R5 to 2, R2 to 1, M050 to 1, M025 to 1)))
    }
}
