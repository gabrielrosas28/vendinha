package br.com.santachiara.vendinha.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RegistroTest {

    @Test fun `totais ignoram estornadas e fecham a conta`() {
        val t = Registro.totais(
            listOf(
                ValorRegistro(totalCentavos = 950, recebidoCentavos = 1_500, trocoCentavos = 550, estornada = false),
                ValorRegistro(totalCentavos = 600, recebidoCentavos = 600, trocoCentavos = 0, estornada = true),
                ValorRegistro(totalCentavos = 400, recebidoCentavos = 2_000, trocoCentavos = 1_600, estornada = false),
            )
        )
        assertEquals(2, t.numeroVendas)
        assertEquals(1, t.numeroEstornadas)
        assertEquals(1_350L, t.vendidoCentavos)
        assertEquals(3_500L, t.recebidoCentavos)
        assertEquals(2_150L, t.trocoCentavos)
        assertEquals(t.vendidoCentavos, t.recebidoCentavos - t.trocoCentavos)
    }

    @Test fun `registro vazio da zero`() {
        val t = Registro.totais(emptyList())
        assertEquals(0, t.numeroVendas)
        assertEquals(0L, t.recebidoCentavos)
    }

    @Test fun `nomes de turma e grupo`() {
        assertTrue(Registro.nomeTurmaValido("5º C"))
        assertFalse(Registro.nomeTurmaValido("   "))
        assertFalse(Registro.nomeTurmaValido("x".repeat(21)))
        assertTrue(Registro.nomeGrupoValido("Brigadeiro de potinho"))
        assertFalse(Registro.nomeGrupoValido(""))
    }
}
