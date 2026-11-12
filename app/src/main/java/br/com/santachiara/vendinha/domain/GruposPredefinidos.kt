package br.com.santachiara.vendinha.domain

data class GrupoPredefinido(val turma: String, val numero: Int, val produto: String) {
    val corIndex: Int get() = corDoNumero(numero)
}

object GruposPredefinidos {

    val TURMAS = listOf("2º A", "2º B", "2º C", "2º D", "2º E")

    private val tabela: Map<String, List<String?>> = mapOf(
        "2º A" to listOf("Bolo de chocolate", "Brigadeiros", "Pipoca", "Geladinhos", "Brownie"),
        "2º B" to listOf("Cupcake", "A definir", "Pipoca", "Sanduíche natural", "Brigadeiros"),
        "2º C" to listOf("Pipoca gourmet", "Geladinhos", "Sanduíche natural", "Tortalete", "Brigadeiros"),
        "2º D" to listOf("Geladinhos", "Brigadeiro de potinho", "Brownie", "Pipoca", "Pastel"),
        "2º E" to listOf("Bolo", "Beijinhos", "Brigadeiros", null, null),
    )

    fun daTurma(turma: String): List<GrupoPredefinido> =
        tabela[turma].orEmpty().mapIndexedNotNull { i, produto ->
            produto?.let { GrupoPredefinido(turma, i + 1, it) }
        }

    val TODOS: List<GrupoPredefinido> get() = TURMAS.flatMap { daTurma(it) }
}

const val TOTAL_CORES_GRUPO = 5

fun corDoNumero(numeroGrupo: Int): Int = ((numeroGrupo - 1).mod(TOTAL_CORES_GRUPO))

fun nomeCompletoGrupo(nome: String, turma: String, numeroGrupo: Int): String =
    if (numeroGrupo > 0) "$nome · $turma · Grupo $numeroGrupo" else "$nome · $turma"
