package br.com.santachiara.vendinha.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContaCaixaTest {

    private val vendas = listOf(
        ValorVenda(500, estornada = false),
        ValorVenda(300, estornada = true),
        ValorVenda(1_250, estornada = false),
    )

    @Test fun `total ganho desconta estornos`() {
        assertEquals(1_750L, ContaCaixa.totalGanho(vendas))
        assertEquals(2, ContaCaixa.numeroVendasValidas(vendas))
    }

    @Test fun `total ganho sem vendas e zero`() {
        assertEquals(0L, ContaCaixa.totalGanho(emptyList()))
    }

    @Test fun `todas estornadas da zero`() {
        assertEquals(0L, ContaCaixa.totalGanho(listOf(ValorVenda(500, true), ValorVenda(200, true))))
    }

    @Test fun `dinheiro esperado com troco inicial`() {
        assertEquals(3_750L, ContaCaixa.dinheiroEsperado(2_000, 1_750))
    }

    @Test fun `dinheiro esperado sem troco inicial`() {
        assertEquals(1_750L, ContaCaixa.dinheiroEsperado(0, 1_750))
    }

    @Test fun `diferenca positiva sobrou`() {
        val d = ContaCaixa.diferenca(contadoCentavos = 4_000, esperadoCentavos = 3_750)
        assertEquals(250L, d)
        assertEquals(ResultadoConferencia.SOBROU, ContaCaixa.resultado(d))
    }

    @Test fun `diferenca negativa faltou`() {
        val d = ContaCaixa.diferenca(3_500, 3_750)
        assertEquals(-250L, d)
        assertEquals(ResultadoConferencia.FALTOU, ContaCaixa.resultado(d))
    }

    @Test fun `diferenca zero bateu certinho`() {
        val d = ContaCaixa.diferenca(3_750, 3_750)
        assertEquals(0L, d)
        assertEquals(ResultadoConferencia.BATEU, ContaCaixa.resultado(d))
    }

    @Test fun `so confirma com recebido maior ou igual ao total`() {
        assertTrue(ContaCaixa.podeConfirmar(750, 750))
        assertTrue(ContaCaixa.podeConfirmar(750, 1_000))
        assertFalse(ContaCaixa.podeConfirmar(750, 700))
        assertFalse(ContaCaixa.podeConfirmar(0, 500))
        assertEquals(250L, ContaCaixa.troco(750, 1_000))
        assertEquals(-50L, ContaCaixa.troco(750, 700))
    }
}

class RegrasEstoqueTest {

    @Test fun `com controle bloqueia acima do disponivel`() {
        assertTrue(RegrasEstoque.podeVender(true, 3, 3))
        assertFalse(RegrasEstoque.podeVender(true, 3, 4))
        assertTrue(RegrasEstoque.podeAdicionarUm(true, 3, 2))
        assertFalse(RegrasEstoque.podeAdicionarUm(true, 3, 3))
        assertFalse(RegrasEstoque.podeAdicionarUm(true, 0, 0))
    }

    @Test fun `sem limite nunca bloqueia`() {
        assertTrue(RegrasEstoque.podeVender(false, null, 1_000))
        assertTrue(RegrasEstoque.podeAdicionarUm(false, null, 9_999))
        assertTrue(RegrasEstoque.podeVender(false, 0, 50))
        assertEquals(SituacaoEstoque.SEM_LIMITE, RegrasEstoque.situacao(false, null))
        assertEquals(SituacaoEstoque.SEM_LIMITE, RegrasEstoque.situacao(false, 0))
    }

    @Test fun `situacao acabando e esgotado`() {
        assertEquals(SituacaoEstoque.NORMAL, RegrasEstoque.situacao(true, 4))
        assertEquals(SituacaoEstoque.ACABANDO, RegrasEstoque.situacao(true, 3))
        assertEquals(SituacaoEstoque.ACABANDO, RegrasEstoque.situacao(true, 1))
        assertEquals(SituacaoEstoque.ESGOTADO, RegrasEstoque.situacao(true, 0))
    }

    @Test fun `venda baixa estoque so com controle`() {
        assertEquals(2, RegrasEstoque.aposVenda(true, 5, 3))
        assertNull(RegrasEstoque.aposVenda(false, null, 3))
    }

    @Test fun `estorno devolve estoque so quando ha controle`() {
        assertEquals(5, RegrasEstoque.aposEstorno(true, 2, 3))
        assertNull(RegrasEstoque.aposEstorno(false, null, 3))
    }

    @Test fun `reposicao acompanha a quantidade inicial`() {
        assertEquals(15, RegrasEstoque.inicialAposAjuste(10, 2, 7))
        assertEquals(8, RegrasEstoque.inicialAposAjuste(null, null, 8))
        assertEquals(4, RegrasEstoque.inicialAposAjuste(10, 9, 3))
    }
}

class ResumoTest {

    @Test fun `resumo ignora estornos e escolhe campeao`() {
        val vendas = listOf(ValorVenda(700, false), ValorVenda(200, true), ValorVenda(300, false))
        val linhas = listOf(
            LinhaVendida(1, "Brigadeiro", 2, 200, false),
            LinhaVendida(2, "Pipoca", 1, 300, false),
            LinhaVendida(1, "Brigadeiro", 1, 200, true),
            LinhaVendida(2, "Pipoca", 1, 300, false),
        )
        val r = Resumo.calcular(vendas, linhas)
        assertEquals(1_000L, r.totalGanhoCentavos)
        assertEquals(2, r.numeroVendas)
        assertEquals(1, r.numeroEstornadas)
        assertEquals(listOf(ItemVendido(1, "Brigadeiro", 2, 400), ItemVendido(2, "Pipoca", 2, 600)).sortedByDescending { it.totalCentavos }, r.itens)
        assertEquals("Pipoca", r.campeao?.nome)
    }

    @Test fun `sem vendas nao ha campeao`() {
        assertNull(Resumo.calcular(emptyList(), emptyList()).campeao)
    }
}

class SenhaTest {

    @Test fun `formato de 4 a 8 digitos`() {
        assertTrue(Senha.formatoValido("1234"))
        assertTrue(Senha.formatoValido("12345678"))
        assertFalse(Senha.formatoValido("123"))
        assertFalse(Senha.formatoValido("123456789"))
        assertFalse(Senha.formatoValido("12a4"))
    }

    @Test fun `hash com salt confere so a senha certa`() {
        val salt = Senha.gerarSalt()
        val hash = Senha.hash("2468", salt)
        assertTrue(Senha.confere("2468", salt, hash))
        assertFalse(Senha.confere("2469", salt, hash))
        assertFalse(hash == Senha.hash("2468", Senha.gerarSalt()))
    }
}

class GruposPredefinidosTest {

    @Test fun `tabela da feira`() {
        assertEquals(5, GruposPredefinidos.daTurma("2º A").size)
        assertEquals(3, GruposPredefinidos.daTurma("2º E").size)
        assertEquals("Brigadeiro de potinho", GruposPredefinidos.daTurma("2º D")[1].produto)
        assertEquals(23, GruposPredefinidos.TODOS.size)
    }

    @Test fun `cor fixa por numero do grupo`() {
        assertEquals(0, corDoNumero(1))
        assertEquals(4, corDoNumero(5))
        assertEquals("Brigadeiros · 2º A · Grupo 2", nomeCompletoGrupo("Brigadeiros", "2º A", 2))
    }
}
