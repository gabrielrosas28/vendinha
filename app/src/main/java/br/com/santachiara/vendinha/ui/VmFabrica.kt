package br.com.santachiara.vendinha.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.santachiara.vendinha.AppContainer
import br.com.santachiara.vendinha.VendinhaApp

@Composable
fun appContainer(): AppContainer = (LocalContext.current.applicationContext as VendinhaApp).container

@Composable
inline fun <reified VM : ViewModel> vm(
    dono: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current),
    chave: String? = null,
    noinline criar: (AppContainer) -> VM,
): VM {
    val container = appContainer()
    return viewModel(viewModelStoreOwner = dono, key = chave) { criar(container) }
}
