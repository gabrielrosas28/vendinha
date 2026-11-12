package br.com.santachiara.vendinha.domain

enum class SituacaoEstoque { SEM_LIMITE, NORMAL, ACABANDO, ESGOTADO }

object RegrasEstoque {

    const val LIMITE_ACABANDO = 3

    fun situacao(controlaEstoque: Boolean, disponivel: Int?): SituacaoEstoque {
        if (!controlaEstoque) return SituacaoEstoque.SEM_LIMITE
        val d = disponivel ?: 0
        return when {
            d <= 0 -> SituacaoEstoque.ESGOTADO
            d <= LIMITE_ACABANDO -> SituacaoEstoque.ACABANDO
            else -> SituacaoEstoque.NORMAL
        }
    }

    fun podeVender(controlaEstoque: Boolean, disponivel: Int?, quantidade: Int): Boolean =
        !controlaEstoque || quantidade <= (disponivel ?: 0)

    fun podeAdicionarUm(controlaEstoque: Boolean, disponivel: Int?, noPedido: Int): Boolean =
        podeVender(controlaEstoque, disponivel, noPedido + 1)

    fun aposVenda(controlaEstoque: Boolean, disponivel: Int?, quantidade: Int): Int? =
        if (controlaEstoque) (disponivel ?: 0) - quantidade else disponivel

    fun aposEstorno(controlaEstoque: Boolean, disponivel: Int?, quantidade: Int): Int? =
        if (controlaEstoque) (disponivel ?: 0) + quantidade else disponivel

    fun inicialAposAjuste(inicialAtual: Int?, disponivelAtual: Int?, novoDisponivel: Int): Int {
        if (inicialAtual == null || disponivelAtual == null) return novoDisponivel
        return (inicialAtual + (novoDisponivel - disponivelAtual)).coerceAtLeast(novoDisponivel)
    }
}
