package com.aezora.music.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.aezora.music.domain.model.AppTheme

// ─── Blue-Purple (default, like the screenshots) ──────────────────────────────
private val BluePurplePrimary = Color(0xFF6C63FF)
private val BluePurpleSecondary = Color(0xFF9C8FFF)
private val BluePurpleBackground = Color(0xFF0D0B2A)
private val BluePurpleSurface = Color(0xFF1A1740)
private val BluePurpleContainer = Color(0xFF2A2660)
private val BluePurpleOnSurface = Color(0xFFE8E4FF)
private val BluePurpleAccent = Color(0xFFFFB800)

// ─── Yellow-Green ─────────────────────────────────────────────────────────────
private val YellowGreenPrimary = Color(0xFFB8E04A)
private val YellowGreenSecondary = Color(0xFF8BC34A)
private val YellowGreenBackground = Color(0xFF0A1208)
private val YellowGreenSurface = Color(0xFF121E0F)
private val YellowGreenContainer = Color(0xFF1E3018)
private val YellowGreenOnSurface = Color(0xFFDCF5B0)
private val YellowGreenAccent = Color(0xFFFFEB3B)

// ─── Black-White ──────────────────────────────────────────────────────────────
private val BlackWhitePrimary = Color(0xFFFFFFFF)
private val BlackWhiteSecondary = Color(0xFFCCCCCC)
private val BlackWhiteBackground = Color(0xFF000000)
private val BlackWhiteSurface = Color(0xFF111111)
private val BlackWhiteContainer = Color(0xFF222222)
private val BlackWhiteOnSurface = Color(0xFFFFFFFF)
private val BlackWhiteAccent = Color(0xFF888888)

// ─── Public color tokens (used in screens) ────────────────────────────────────
object AezoraColors {
    var primary = BluePurplePrimary; private set
    var secondary = BluePurpleSecondary; private set
    var background = BluePurpleBackground; private set
    var surface = BluePurpleSurface; private set
    var container = BluePurpleContainer; private set
    var onSurface = BluePurpleOnSurface; private set
    var accent = BluePurpleAccent; private set

    fun update(theme: AppTheme) {
        when (theme) {
            AppTheme.BLUE_PURPLE -> {
                primary = BluePurplePrimary; secondary = BluePurpleSecondary
                background = BluePurpleBackground; surface = BluePurpleSurface
                container = BluePurpleContainer; onSurface = BluePurpleOnSurface; accent = BluePurpleAccent
            }
            AppTheme.YELLOW_GREEN -> {
                primary = YellowGreenPrimary; secondary = YellowGreenSecondary
                background = YellowGreenBackground; surface = YellowGreenSurface
                container = YellowGreenContainer; onSurface = YellowGreenOnSurface; accent = YellowGreenAccent
            }
            AppTheme.BLACK_WHITE -> {
                primary = BlackWhitePrimary; secondary = BlackWhiteSecondary
                background = BlackWhiteBackground; surface = BlackWhiteSurface
                container = BlackWhiteContainer; onSurface = BlackWhiteOnSurface; accent = BlackWhiteAccent
            }
        }
    }
}

@Composable
fun AezoraTheme(
    appTheme: AppTheme = AppTheme.BLUE_PURPLE,
    content: @Composable () -> Unit
) {
    AezoraColors.update(appTheme)

    val colorScheme = darkColorScheme(
        primary = AezoraColors.primary,
        secondary = AezoraColors.secondary,
        background = AezoraColors.background,
        surface = AezoraColors.surface,
        surfaceVariant = AezoraColors.container,
        onBackground = AezoraColors.onSurface,
        onSurface = AezoraColors.onSurface,
        onPrimary = Color.Black,
        tertiary = AezoraColors.accent
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
