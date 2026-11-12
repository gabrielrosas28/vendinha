package br.com.santachiara.vendinha.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatos {
    private val ptBR = Locale.forLanguageTag("pt-BR")
    fun hora(millis: Long): String = SimpleDateFormat("HH:mm", ptBR).format(Date(millis))
    fun data(millis: Long): String = SimpleDateFormat("dd/MM/yyyy", ptBR).format(Date(millis))
    fun dataHora(millis: Long): String = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", ptBR).format(Date(millis))
    fun arquivo(millis: Long): String = SimpleDateFormat("yyyy-MM-dd_HH'h'mm", ptBR).format(Date(millis))
}
