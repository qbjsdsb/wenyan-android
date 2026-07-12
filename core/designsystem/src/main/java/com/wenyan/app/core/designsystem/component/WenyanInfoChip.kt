package com.wenyan.app.core.designsystem.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class ChipVariant {
    NEUTRAL,
    PRIMARY,
    SECONDARY,
    TERTIARY,
    ERROR,
}

@Composable
fun WenyanInfoChip(
    text: String,
    modifier: Modifier = Modifier,
    variant: ChipVariant = ChipVariant.NEUTRAL,
) {
    val colorScheme = MaterialTheme.colorScheme
    val (containerColor, contentColor) = when (variant) {
        ChipVariant.NEUTRAL -> colorScheme.surfaceContainerHigh to colorScheme.onSurfaceVariant
        ChipVariant.PRIMARY -> colorScheme.primaryContainer to colorScheme.onPrimaryContainer
        ChipVariant.SECONDARY -> colorScheme.secondaryContainer to colorScheme.onSecondaryContainer
        ChipVariant.TERTIARY -> colorScheme.tertiaryContainer to colorScheme.onTertiaryContainer
        ChipVariant.ERROR -> colorScheme.errorContainer to colorScheme.onErrorContainer
    }

    Surface(
        modifier = modifier,
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.extraSmall,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}
