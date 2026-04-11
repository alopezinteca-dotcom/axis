package vtsen.hashnode.dev.newemptycomposeapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Tema Material 3 mínimo y CI-safe:
 * - No depende de Accompanist (SystemUiController)
 * - No depende de BuildExt ni utilidades externas
 * - Compila en Codemagic sin librerías adicionales
 *
 * Si más adelante quieres colorear la status bar/navigation bar,
 * lo hacemos con APIs oficiales o añadiendo dependencias explícitas.
 */
@Composable
fun AxisTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme()
    } else {
        lightColorScheme()
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        shapes = Shapes(),
        content = content
    )
}
