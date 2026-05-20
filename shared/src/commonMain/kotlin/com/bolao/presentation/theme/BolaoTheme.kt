package com.bolao.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Paleta de cores da marca ───────────────────────────────────────────────────

/** Verde principal — tom vibrante do gramado */
val BolaoGreen      = Color(0xFF00C853)
val BolaoGreenDark  = Color(0xFF003912)
val BolaoGreenLight = Color(0xFF69FF9C)

/** Dourado para pontos e conquistas */
val BolaoGold       = Color(0xFFFFD740)
val BolaoGoldDark   = Color(0xFF4A3800)
val BolaoGoldLight  = Color(0xFFFFE57F)

/** Vermelho para partidas ao vivo */
val LiveRed         = Color(0xFFEF4444)
val LiveRedMuted    = Color(0xFF3D1212)

/** Âmbar para intervalo */
val HalfTimeAmber   = Color(0xFFF59E0B)

/** Fundo e superfícies escuros */
val DeepNavy        = Color(0xFF07091A)
val DarkCard        = Color(0xFF111827)
val DarkCardVariant = Color(0xFF1A2235)
val DarkOutline     = Color(0xFF253044)

val TextPrimary     = Color(0xFFE8EDF5)
val TextSecondary   = Color(0xFF8FA3BE)

// ── Esquema de cores Material 3 ────────────────────────────────────────────────

private val BolaoDarkColorScheme = darkColorScheme(
    primary              = BolaoGreen,
    onPrimary            = Color(0xFF003910),
    primaryContainer     = BolaoGreenDark,
    onPrimaryContainer   = BolaoGreenLight,

    secondary            = BolaoGold,
    onSecondary          = Color(0xFF3D2F00),
    secondaryContainer   = BolaoGoldDark,
    onSecondaryContainer = BolaoGoldLight,

    tertiary             = Color(0xFF82B1FF),
    onTertiary           = Color(0xFF002171),
    tertiaryContainer    = Color(0xFF002D6E),
    onTertiaryContainer  = Color(0xFFD5E3FF),

    background           = DeepNavy,
    onBackground         = TextPrimary,

    surface              = DarkCard,
    onSurface            = TextPrimary,
    surfaceVariant       = DarkCardVariant,
    onSurfaceVariant     = TextSecondary,

    outline              = DarkOutline,
    outlineVariant       = DarkOutline.copy(alpha = 0.5f),

    error                = Color(0xFFCF6679),
    onError              = Color(0xFF690020),
    errorContainer       = Color(0xFF93000A),
    onErrorContainer     = Color(0xFFFFDAD6),
)

// ── Tipografia ─────────────────────────────────────────────────────────────────

/**
 * Tipografia do Bolão com pesos personalizados.
 * Utiliza a fonte do sistema para máxima compatibilidade multiplataforma.
 * Para adicionar Google Fonts (Inter, Outfit, etc.), use o recurso de fontes
 * do Compose Multiplatform Resources.
 */
val BolaoTypography = Typography(
    headlineLarge  = TextStyle(fontWeight = FontWeight.Black,     fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall  = TextStyle(fontWeight = FontWeight.Bold,      fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge     = TextStyle(fontWeight = FontWeight.Bold,      fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium    = TextStyle(fontWeight = FontWeight.SemiBold,  fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    titleSmall     = TextStyle(fontWeight = FontWeight.SemiBold,  fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    bodyLarge      = TextStyle(fontWeight = FontWeight.Normal,    fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium     = TextStyle(fontWeight = FontWeight.Normal,    fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall      = TextStyle(fontWeight = FontWeight.Normal,    fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge     = TextStyle(fontWeight = FontWeight.SemiBold,  fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium    = TextStyle(fontWeight = FontWeight.Medium,    fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelSmall     = TextStyle(fontWeight = FontWeight.Medium,    fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
)

// ── Tema principal ─────────────────────────────────────────────────────────────

/**
 * Tema Material 3 do Bolão.
 *
 * Sempre usa dark mode — adequado para o contexto de assistir jogos
 * em ambientes com pouca luz (ex: estadio, bar).
 * Light mode pode ser adicionado futuramente via `isSystemInDarkTheme()`.
 */
@Composable
fun BolaoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BolaoDarkColorScheme,
        typography  = BolaoTypography,
        content     = content,
    )
}
