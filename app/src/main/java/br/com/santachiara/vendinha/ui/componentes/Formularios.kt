package br.com.santachiara.vendinha.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import br.com.santachiara.vendinha.R
import br.com.santachiara.vendinha.ui.tema.Cores

@Composable
fun CampoTexto(
    valor: String,
    onMudar: (String) -> Unit,
    rotulo: String,
    modifier: Modifier = Modifier,
    exemplo: String? = null,
    erro: String? = null,
    ajuda: String? = null,
    teclado: KeyboardOptions = KeyboardOptions.Default,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onMudar,
        modifier = modifier.fillMaxWidth(),
        label = { Text(rotulo, style = MaterialTheme.typography.bodyLarge) },
        placeholder = exemplo?.let { { Text(it, style = MaterialTheme.typography.titleMedium, color = Cores.Apoio.copy(alpha = 0.6f)) } },
        supportingText = (erro ?: ajuda)?.let { { Text(it, style = MaterialTheme.typography.bodyMedium) } },
        isError = erro != null,
        singleLine = true,
        textStyle = MaterialTheme.typography.titleMedium.copy(color = Cores.Tinta),
        keyboardOptions = teclado,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Cores.Azul,
            unfocusedBorderColor = Cores.Linha,
            focusedLabelColor = Cores.AzulTexto,
            unfocusedLabelColor = Cores.Apoio,
            focusedContainerColor = Cores.Superficie,
            unfocusedContainerColor = Cores.Superficie,
            errorContainerColor = Cores.Superficie,
            cursorColor = Cores.Azul,
            focusedSupportingTextColor = Cores.Apoio,
            unfocusedSupportingTextColor = Cores.Apoio,
        ),
    )
}

@Composable
fun JanelaDialogo(onFechar: () -> Unit, conteudo: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onFechar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(16.dp)
                .imePadding()
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .background(Cores.Fundo, RoundedCornerShape(28.dp))
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            content = conteudo,
        )
    }
}

@Composable
fun DialogoSairDoApp(onFechar: () -> Unit) {
    val activity = LocalContext.current.acharActivity()
    DialogoConfirmacao(
        titulo = stringResource(R.string.sair_titulo),
        texto = stringResource(R.string.sair_texto),
        textoConfirmar = stringResource(R.string.sair_confirmar),
        onConfirmar = { onFechar(); activity?.finish() },
        onCancelar = onFechar,
    )
}
