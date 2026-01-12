package dev.mcd.chess.ui.settings

import androidx.compose.foundation.layout.height
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import dev.mcd.chess.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun ColorSchemeChip(
    colorScheme: AppColorScheme,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    ElevatedFilterChip(
        modifier = Modifier.height(48.dp),
        //label = { Text(text = colorScheme.name) },
        label = {
            Text(
                text = stringResource(
                    when (colorScheme) {
                        AppColorScheme.Blue -> R.string.color_scheme_blue
                        AppColorScheme.Brown -> R.string.color_scheme_brown
                        AppColorScheme.Green -> R.string.color_scheme_green
                        AppColorScheme.Gray -> R.string.color_scheme_gray
                    },
                ),
            )
        },
        selected = isSelected,
        onClick = onClick,
    )
}
