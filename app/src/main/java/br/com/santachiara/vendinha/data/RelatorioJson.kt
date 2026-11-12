package br.com.santachiara.vendinha.data

import br.com.santachiara.vendinha.data.db.Fechamento
import br.com.santachiara.vendinha.data.db.RegistroVenda
import br.com.santachiara.vendinha.domain.ContaCaixa
import br.com.santachiara.vendinha.domain.ItemVendido
import br.com.santachiara.vendinha.domain.ResumoCaixa
import org.json.JSONArray
import org.json.JSONObject

data class DadosRelatorio(
    val fechamento: Fechamento,
    val itens: List<ItemVendido>,
    val campeao: ItemVendido?,
    val esperadoCentavos: Long,
    val numeroEstornadas: Int,
    val abertoEm: Long?,
) {
    val diferencaCentavos: Long?
        get() = fechamento.contadoCentavos?.let { ContaCaixa.diferenca(it, esperadoCentavos) }
}

object RelatorioJson {

    fun gerar(resumo: ResumoCaixa, esperadoCentavos: Long, abertoEm: Long, fechadoEm: Long): String =
        JSONObject().apply {
            put("versao", 1)
            put("esperadoCentavos", esperadoCentavos)
            put("numeroEstornadas", resumo.numeroEstornadas)
            put("abertoEm", abertoEm)
            put("fechadoEm", fechadoEm)
            put("campeaoItemId", resumo.campeao?.itemId ?: JSONObject.NULL)
            put("itens", JSONArray().apply {
                resumo.itens.forEach {
                    put(JSONObject().apply {
                        put("itemId", it.itemId)
                        put("nome", it.nome)
                        put("quantidade", it.quantidade)
                        put("totalCentavos", it.totalCentavos)
                    })
                }
            })
        }.toString()

    fun ler(f: Fechamento): DadosRelatorio {
        val o = runCatching { JSONObject(f.resumoJson) }.getOrElse { JSONObject() }
        val arr = o.optJSONArray("itens") ?: JSONArray()
        val itens = (0 until arr.length()).map { i ->
            val it = arr.getJSONObject(i)
            ItemVendido(it.optLong("itemId"), it.optString("nome"), it.optInt("quantidade"), it.optLong("totalCentavos"))
        }
        val campeaoId = if (o.isNull("campeaoItemId")) null else o.optLong("campeaoItemId")
        return DadosRelatorio(
            fechamento = f,
            itens = itens,
            campeao = itens.firstOrNull { it.itemId == campeaoId } ?: itens.firstOrNull(),
            esperadoCentavos = o.optLong("esperadoCentavos", f.trocoInicialCentavos + f.totalCentavos),
            numeroEstornadas = o.optInt("numeroEstornadas"),
            abertoEm = if (o.has("abertoEm")) o.optLong("abertoEm") else null,
        )
    }

    fun backup(fechamentos: List<Fechamento>, geradoEm: Long, registro: List<RegistroVenda> = emptyList()): String = JSONObject().apply {
        put("app", "Vendinha")
        put("formato", 2)
        put("registroVendas", JSONArray().apply {
            registro.forEach { r ->
                put(JSONObject().apply {
                    put("dataHora", r.dataHora)
                    put("turma", r.turma)
                    put("numeroGrupo", r.numeroGrupo)
                    put("nomeGrupo", r.nomeGrupo)
                    put("itens", r.itens)
                    put("totalCentavos", r.totalCentavos)
                    put("recebidoCentavos", r.recebidoCentavos)
                    put("trocoCentavos", r.trocoCentavos)
                    put("estornada", r.estornada)
                    put("estornadaEm", r.estornadaEm ?: JSONObject.NULL)
                })
            }
        })
        put("geradoEm", geradoEm)
        put("fechamentos", JSONArray().apply {
            fechamentos.forEach { f ->
                put(JSONObject().apply {
                    put("id", f.id)
                    put("turma", f.turma)
                    put("numeroGrupo", f.numeroGrupo)
                    put("nomeGrupo", f.nomeGrupo)
                    put("data", f.data)
                    put("totalCentavos", f.totalCentavos)
                    put("numVendas", f.numVendas)
                    put("trocoInicialCentavos", f.trocoInicialCentavos)
                    put("contadoCentavos", f.contadoCentavos ?: JSONObject.NULL)
                    put("resumo", runCatching { JSONObject(f.resumoJson) }.getOrElse { JSONObject() })
                })
            }
        })
    }.toString(2)
}
