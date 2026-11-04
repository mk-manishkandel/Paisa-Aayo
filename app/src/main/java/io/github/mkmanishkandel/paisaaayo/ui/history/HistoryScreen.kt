package io.github.mkmanishkandel.paisaaayo.ui.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mkmanishkandel.paisaaayo.R
import io.github.mkmanishkandel.paisaaayo.container
import io.github.mkmanishkandel.paisaaayo.data.PaymentEntity
import io.github.mkmanishkandel.paisaaayo.parser.Direction
import io.github.mkmanishkandel.paisaaayo.ui.Format
import io.github.mkmanishkandel.paisaaayo.ui.components.EmptyState
import io.github.mkmanishkandel.paisaaayo.ui.components.PaisaCard
import io.github.mkmanishkandel.paisaaayo.ui.components.PaymentRow
import io.github.mkmanishkandel.paisaaayo.ui.theme.LocalNepali
import io.github.mkmanishkandel.paisaaayo.ui.theme.LocalPaisaColors
import io.github.mkmanishkandel.paisaaayo.ui.theme.Spacing
import io.github.mkmanishkandel.paisaaayo.ui.theme.tabular

private enum class Range(val label: Int, val daysAgo: Int?) {
    TODAY(R.string.filter_today, 0),
    WEEK(R.string.filter_7d, 6),
    MONTH(R.string.filter_30d, 29),
    ALL(R.string.filter_all, null),
}

private enum class TypeFilter(val label: Int) {
    ALL(R.string.filter_all_types),
    RECEIVED(R.string.filter_received),
    PAID(R.string.filter_paid),
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val nepali = LocalNepali.current
    val paisa = LocalPaisaColors.current
    var range by rememberSaveable { mutableStateOf(Range.WEEK) }
    var type by rememberSaveable { mutableStateOf(TypeFilter.ALL) }

    val from = remember(range) { range.daysAgo?.let { Format.startOfDay(daysAgo = it) } ?: 0L }
    val all by remember(from) { context.container.database.payments().since(from) }.collectAsStateWithLifecycle(emptyList())
    val payments = remember(all, type) {
        when (type) {
            TypeFilter.ALL -> all
            TypeFilter.RECEIVED -> all.filter { it.direction == Direction.CREDIT.name }
            TypeFilter.PAID -> all.filter { it.direction == Direction.DEBIT.name }
        }
    }
    val totalIn = remember(all) { all.filter { it.direction == Direction.CREDIT.name && !it.isTest }.sumOf { it.amountPaisa } }
    val totalOut = remember(all) { all.filter { it.direction == Direction.DEBIT.name && !it.isTest }.sumOf { it.amountPaisa } }
    val byDay: Map<Long, List<PaymentEntity>> = remember(payments) { payments.groupBy { Format.startOfDay(it.timestamp) } }
    val today = remember { Format.startOfDay() }
    val yesterday = remember { Format.startOfDay(daysAgo = 1) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title), modifier = Modifier.semantics { heading() }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Range.entries.forEach { r -> Chip(stringResource(r.label), range == r) { range = r } }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    TypeFilter.entries.forEach { t -> Chip(stringResource(t.label), type == t) { type = t } }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    Total(stringResource(R.string.history_total_in), Format.amount(totalIn, nepali), paisa.credit, Modifier.weight(1f))
                    Total(stringResource(R.string.history_total_out), Format.amount(totalOut, nepali), paisa.debit, Modifier.weight(1f))
                }
            }
            if (payments.isEmpty()) {
                item {
                    PaisaCard {
                        EmptyState(Icons.AutoMirrored.Rounded.ReceiptLong, stringResource(R.string.history_title), stringResource(R.string.history_empty))
                    }
                }
            }
            byDay.forEach { (day, list) ->
                stickyHeader(key = "h$day") {
                    val dayIn = list.filter { it.direction == Direction.CREDIT.name && !it.isTest }.sumOf { it.amountPaisa }
                    Row(
                        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(vertical = Spacing.sm),
                    ) {
                        Text(
                            when (day) {
                                today -> stringResource(R.string.today)
                                yesterday -> stringResource(R.string.yesterday)
                                else -> Format.day(day, nepali)
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f).semantics { heading() },
                        )
                        Text("+ " + Format.amount(dayIn, nepali), style = MaterialTheme.typography.labelLarge.tabular, color = paisa.credit)
                    }
                }
                item(key = "d$day") {
                    PaisaCard {
                        Column {
                            list.forEach { PaymentRow(it) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelLarge) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        ),
    )
}

@Composable
private fun Total(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    PaisaCard(modifier) {
        Column(Modifier.padding(Spacing.lg)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge.tabular, fontWeight = FontWeight.SemiBold, color = color)
        }
    }
}
