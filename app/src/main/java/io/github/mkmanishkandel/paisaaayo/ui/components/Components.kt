package io.github.mkmanishkandel.paisaaayo.ui.components

import androidx.compose.ui.res.painterResource
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.mkmanishkandel.paisaaayo.R
import io.github.mkmanishkandel.paisaaayo.data.PaymentEntity
import io.github.mkmanishkandel.paisaaayo.parser.Money
import io.github.mkmanishkandel.paisaaayo.parser.Direction
import io.github.mkmanishkandel.paisaaayo.ui.Format
import io.github.mkmanishkandel.paisaaayo.ui.theme.LocalNepali
import io.github.mkmanishkandel.paisaaayo.ui.theme.LocalPaisaColors
import io.github.mkmanishkandel.paisaaayo.ui.theme.LocalReducedMotion
import io.github.mkmanishkandel.paisaaayo.ui.theme.Spacing
import io.github.mkmanishkandel.paisaaayo.ui.theme.tabular

/** Bordered card used across the app — sharp, quiet, Swiss. */
@Composable
fun PaisaCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    container: Color = MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CardDefaults.cardColors(containerColor = container)
    val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, shape = MaterialTheme.shapes.large, colors = colors, border = border, content = content)
    } else {
        Card(modifier = modifier, shape = MaterialTheme.shapes.large, colors = colors, border = border, content = content)
    }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        trailing?.invoke()
    }
}

/** Full-width primary action — gold, 56 dp tall (well above the 48 dp touch minimum). */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(Spacing.sm))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 52.dp),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(Spacing.sm))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** Round tinted badge holding an icon. Decorative: hidden from screen readers. */
@Composable
fun IconBadge(icon: ImageVector, tint: Color, background: Color, size: Dp = 44.dp) {
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

/** A soft pulsing dot that signals "live". Static when reduced motion is on. */
@Composable
fun LiveDot(color: Color, modifier: Modifier = Modifier) {
    val reduced = LocalReducedMotion.current
    val transition = rememberInfiniteTransition(label = "live")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (reduced) 0f else 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart),
        label = "pulse",
    )
    Canvas(modifier.size(14.dp)) {
        val r = size.minDimension / 2
        if (!reduced) drawCircle(color.copy(alpha = (1f - pulse) * 0.45f), radius = r * (0.5f + pulse * 0.5f))
        drawCircle(color, radius = r * 0.45f)
    }
}

/** One payment line: direction badge, source, time, signed amount. */
@Composable
fun PaymentRow(payment: PaymentEntity, modifier: Modifier = Modifier) {
    val nepali = LocalNepali.current
    val paisa = LocalPaisaColors.current
    val credit = payment.direction == Direction.CREDIT.name
    val amount = Format.amount(payment.amountPaisa, nepali)
    val time = Format.time(payment.timestamp, nepali)
    val description = stringResource(
        if (credit) R.string.cd_credit else R.string.cd_debit,
        // Keep the paisa so screen readers say "175.50", not "175".
        Money.format(payment.amountPaisa, devanagariDigits = nepali), payment.sourceLabel, time,
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md)
            .clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(
            icon = if (credit) Icons.AutoMirrored.Rounded.CallReceived else Icons.AutoMirrored.Rounded.CallMade,
            tint = if (credit) paisa.credit else paisa.debit,
            background = if (credit) paisa.creditContainer else paisa.debitContainer,
        )
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f)) {
            Text(
                payment.sourceLabel,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${stringResource(if (credit) R.string.payment_received else R.string.payment_paid)} · $time",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(Spacing.sm))
        Text(
            text = (if (credit) "+ " else "− ") + amount,
            style = MaterialTheme.typography.titleMedium.tabular,
            fontWeight = FontWeight.SemiBold,
            color = if (credit) paisa.credit else paisa.debit,
        )
    }
}

/**
 * Brand hero: the Paisa Aayo mark (a speech bubble saying "रु") with sound rings rippling out,
 * "the phone speaks when money arrives". Rings are static under reduced motion.
 */
@Composable
fun HeroCoin(modifier: Modifier = Modifier, size: Dp = 180.dp) {
    val reduced = LocalReducedMotion.current
    val gold = MaterialTheme.colorScheme.primary
    val transition = rememberInfiniteTransition(label = "hero")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (reduced) 0.35f else 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "rings",
    )
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val c = Offset(this.size.width / 2, this.size.height / 2)
            val inner = this.size.minDimension * 0.3f
            for (i in 0..2) {
                val phase = (t + i / 3f) % 1f
                val r = inner + (this.size.minDimension / 2 - inner) * phase
                drawCircle(gold.copy(alpha = (1f - phase) * 0.45f), radius = r, center = c, style = Stroke(width = 2.dp.toPx()))
            }
        }
        Icon(
            painterResource(R.drawable.ic_brand_mark),
            contentDescription = null,
            tint = gold,
            modifier = Modifier.size(size * 0.46f),
        )
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        IconBadge(icon, MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.surfaceVariant, 56.dp)
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
