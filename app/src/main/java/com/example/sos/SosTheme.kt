package com.example.sos

/**
 * SOS App — Centralized Design System
 *
 * Dark emergency-first UI.
 * All screens import tokens & composables from here.
 * Never hardcode colors or spacing in individual screens.
 */

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ═══════════════════════════════════════════════════════════════════════════
//  COLOR TOKENS
// ═══════════════════════════════════════════════════════════════════════════

// Background levels
val SosBg           = Color(0xFF000000)   // Pure dark canvas
val SosSurface      = Color(0xFF111111)   // Card / container surface
val SosSurface2     = Color(0xFF1A1A1A)   // Elevated surface
val SosBorder       = Color(0xFF252525)   // Subtle border

// Semantic accents
val SosRed          = Color(0xFFFF3B3B)   // Critical / emergency
val SosAmber        = Color(0xFFFFBF00)   // Warning / primary accent
val SosGreen        = Color(0xFF32D74B)   // Success / active
val SosCyan         = Color(0xFF0AE7FF)   // Navigation / location
val SosPurple       = Color(0xFFBF5AF2)   // Tools / security
val SosBlue         = Color(0xFF2196F3)   // Info / weather

// Text hierarchy
val SosTextPrimary   = Color(0xFFFFFFFF)
val SosTextSecondary = Color(0xFFAAAAAA)
val SosTextDisabled  = Color(0xFF555555)

// Legacy aliases kept so old code still compiles
val PipBlack = Color(0xFF000000)
val PipAmber = Color(0xFFB64200)
val PipRed = Color(0xFFFF0000)
val PipGreen = Color(0xFF00FF00)

// ═══════════════════════════════════════════════════════════════════════════
//  SPACING / SHAPE TOKENS
// ═══════════════════════════════════════════════════════════════════════════
val SosRadiusSm  = 6.dp
val SosRadiusMd  = 10.dp
val SosRadiusLg  = 16.dp
val SosSpaceSm   = 8.dp
val SosSpaceMd   = 16.dp
val SosSpaceLg   = 24.dp

// ═══════════════════════════════════════════════════════════════════════════
//  TYPOGRAPHY TOKENS
// ═══════════════════════════════════════════════════════════════════════════
val SosFontDisplay  = 28.sp
val SosFontTitle    = 20.sp
val SosFontBody     = 14.sp
val SosFontCaption  = 11.sp

// ═══════════════════════════════════════════════════════════════════════════
//  GRADIENTS
// ═══════════════════════════════════════════════════════════════════════════
val SosRedGradient = Brush.verticalGradient(listOf(SosRed.copy(0.18f), Color.Transparent))
val SosAmberGradient = Brush.verticalGradient(listOf(SosAmber.copy(0.12f), Color.Transparent))

// ═══════════════════════════════════════════════════════════════════════════
//  REUSABLE COMPOSABLES
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Standard top app bar used by every detail screen.
 * Shows back button + screen title + optional subtitle.
 */
@Composable
fun SosTopBar(
    title: String,
    subtitle: String = "",
    accentColor: Color = SosAmber,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SosSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = SosSpaceMd, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(SosRadiusSm))
                    .background(accentColor.copy(alpha = 0.15f))
                    .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(SosRadiusSm))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Geri",
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    color = accentColor,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = SosFontTitle,
                    letterSpacing = 1.sp
                )
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        color = accentColor.copy(alpha = 0.55f),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Normal,
                        fontSize = SosFontCaption,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
        Divider(color = accentColor.copy(alpha = 0.25f), thickness = 1.dp)
    }
}

/**
 * Primary action button — full width, large touch target.
 */
@Composable
fun SosPrimaryButton(
    text: String,
    accentColor: Color = SosAmber,
    isActive: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(SosRadiusMd))
            .background(
                if (isActive) accentColor.copy(alpha = 0.18f)
                else accentColor.copy(alpha = 0.08f)
            )
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                color = if (isActive) accentColor else accentColor.copy(alpha = 0.4f),
                shape = RoundedCornerShape(SosRadiusMd)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isActive) accentColor else accentColor.copy(alpha = 0.8f),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            letterSpacing = 1.sp
        )
    }
}

/**
 * Danger button for critical actions like SOS activation.
 */
@Composable
fun SosDangerButton(
    text: String,
    isActive: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(SosRadiusMd))
            .background(
                if (isActive) SosRed.copy(alpha = 0.22f) else SosRed.copy(alpha = 0.08f)
            )
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = if (isActive) SosRed else SosRed.copy(alpha = 0.5f),
                shape = RoundedCornerShape(SosRadiusMd)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = SosRed,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            letterSpacing = 1.sp
        )
    }
}

/**
 * Info card with label + value display.
 */
@Composable
fun SosInfoCard(
    label: String,
    value: String,
    accentColor: Color = SosAmber,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(SosRadiusSm))
            .background(SosSurface2)
            .border(1.dp, SosBorder, RoundedCornerShape(SosRadiusSm))
            .padding(SosSpaceMd)
    ) {
        Text(
            text = label,
            color = accentColor.copy(alpha = 0.6f),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Normal,
            fontSize = SosFontCaption,
            letterSpacing = 0.8.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = accentColor,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.ExtraBold,
            fontSize = SosFontBody,
            letterSpacing = 0.3.sp
        )
    }
}

/**
 * Status badge — small inline status pill.
 */
@Composable
fun SosStatusBadge(text: String, accentColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(accentColor.copy(alpha = 0.18f))
            .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            color = accentColor,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = SosFontCaption
        )
    }
}

/**
 * Section label with divider.
 */
@Composable
fun SosSectionLabel(text: String, color: Color = SosAmber) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = color.copy(alpha = 0.7f),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = SosFontCaption,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(end = 8.dp)
        )
        Divider(
            modifier = Modifier.weight(1f),
            color = color.copy(alpha = 0.15f),
            thickness = 1.dp
        )
    }
}

/**
 * Icon button — small square with icon.
 */
@Composable
fun SosIconButton(
    icon: ImageVector,
    contentDescription: String,
    accentColor: Color = SosAmber,
    size: Dp = 44.dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(SosRadiusSm))
            .background(accentColor.copy(alpha = 0.12f))
            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(SosRadiusSm))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = accentColor,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}

/**
 * Standard screen scaffold — sets background and system bars.
 */
@Composable
fun SosScreenScaffold(
    title: String,
    subtitle: String = "",
    accentColor: Color = SosAmber,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SosBg)
            .imePadding()
    ) {
        SosTopBar(
            title = title,
            subtitle = subtitle,
            accentColor = accentColor,
            onBack = onBack
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = SosSpaceMd),
            content = content
        )
    }
}
