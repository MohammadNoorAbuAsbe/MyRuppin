package com.MohammadNoorAbuAsbe.myruppin.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Utility class for scaling UI elements based on screen size
 */
class UiScaler {
    companion object {
        @Composable
        fun getScaleFactor(): Float {
            val configuration = LocalConfiguration.current
            val screenHeight = configuration.screenHeightDp.dp
            return (screenHeight / 960.dp)
        }

        @Composable
        fun scaleTextSize(size: Int): TextUnit {
            return (size * getScaleFactor()).sp
        }

        @Composable
        fun scaleDp(size: Int): Dp {
            return (size * getScaleFactor()).dp
        }
    }
}