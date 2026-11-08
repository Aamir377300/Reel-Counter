package com.aamir.reelcounter.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aamir.reelcounter.ui.viewmodel.DayAggregate
import com.aamir.reelcounter.ui.viewmodel.HistoryViewModel
import com.aamir.reelcounter.util.DateHelpers
import com.aamir.reelcounter.util.ScrollMetrics

@Composable
fun HistoryScreen(
    vm: HistoryViewModel = viewModel(factory = HistoryViewModel.factory())
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val barColor = MaterialTheme.colorScheme.primary

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(
                text = "History",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.daysRange == 7,
                    onClick = { vm.setRange(7) },
                    label = { Text("7 days") }
                )
                FilterChip(
                    selected = state.daysRange == 30,
                    onClick = { vm.setRange(30) },
                    label = { Text("30 days") }
                )
            }
            Spacer(Modifier.height(16.dp))
            BarChart(
                days = state.days.reversed(), // oldest → newest for chart
                barColor = barColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp)
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.days, key = { it.date }) { day ->
                DayRow(day, context)
            }
        }
    }
}

@Composable
private fun BarChart(
    days: List<DayAggregate>,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    val max = (days.maxOfOrNull { it.reelCount } ?: 0).coerceAtLeast(1)
    Canvas(modifier = modifier) {
        if (days.isEmpty()) return@Canvas
        val gap = 4.dp.toPx()
        val barWidth = ((size.width - gap * (days.size - 1)) / days.size).coerceAtLeast(2f)
        days.forEachIndexed { index, day ->
            val h = (day.reelCount.toFloat() / max) * size.height
            val x = index * (barWidth + gap)
            val y = size.height - h
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, h.coerceAtLeast(2f)),
                cornerRadius = CornerRadius(6f, 6f)
            )
        }
    }
}

@Composable
private fun DayRow(day: DayAggregate, context: android.content.Context) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(DateHelpers.formatDisplay(day.date), fontWeight = FontWeight.SemiBold)
            Text(
                text = ScrollMetrics.formatMetres(
                    ScrollMetrics.pixelsToMetres(day.scrollPixels, context)
                ) + " · " + ScrollMetrics.formatDuration(day.timeSpentMs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        Text(
            text = "${day.reelCount} reels",
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
