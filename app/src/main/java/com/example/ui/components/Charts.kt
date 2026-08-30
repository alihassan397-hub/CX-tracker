package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.TaskStatus
import com.example.data.entity.TatStatus
import com.example.data.model.UnitPerformanceSummary
import com.example.ui.theme.HblPrimary
import com.example.ui.theme.StatusAtRisk
import com.example.ui.theme.StatusBreachedTat
import com.example.ui.theme.StatusInProgress
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusToDo
import com.example.ui.theme.StatusWithinTat
import java.util.Locale

@Composable
fun SlaDonutChart(
    withinTatCount: Int,
    atRiskCount: Int,
    breachedCount: Int,
    slaPercent: Double,
    modifier: Modifier = Modifier
) {
    val total = (withinTatCount + atRiskCount + breachedCount).coerceAtLeast(1)
    val withinRatio = withinTatCount.toFloat() / total
    val atRiskRatio = atRiskCount.toFloat() / total
    val breachedRatio = breachedCount.toFloat() / total

    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(withinTatCount, atRiskCount, breachedCount) {
        animatedProgress.animateTo(1f, animationSpec = tween(750))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Turnaround Time (TAT) SLA Adherence",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Donut Canvas
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(130.dp)
                ) {
                    Canvas(modifier = Modifier.size(120.dp)) {
                        val strokeWidth = 18.dp.toPx()
                        val canvasSize = size.minDimension - strokeWidth
                        val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                        val arcSize = Size(canvasSize, canvasSize)

                        var startAngle = -90f

                        // Within TAT Arc
                        val sweepWithin = 360f * withinRatio * animatedProgress.value
                        if (sweepWithin > 0f) {
                            drawArc(
                                color = StatusWithinTat,
                                startAngle = startAngle,
                                sweepAngle = sweepWithin,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                            startAngle += 360f * withinRatio
                        }

                        // At Risk Arc
                        val sweepAtRisk = 360f * atRiskRatio * animatedProgress.value
                        if (sweepAtRisk > 0f) {
                            drawArc(
                                color = StatusAtRisk,
                                startAngle = startAngle,
                                sweepAngle = sweepAtRisk,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                            startAngle += 360f * atRiskRatio
                        }

                        // Breached TAT Arc
                        val sweepBreached = 360f * breachedRatio * animatedProgress.value
                        if (sweepBreached > 0f) {
                            drawArc(
                                color = StatusBreachedTat,
                                startAngle = startAngle,
                                sweepAngle = sweepBreached,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                    }

                    // Center SLA Text
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = String.format(Locale.US, "%.1f%%", slaPercent),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (slaPercent >= 90.0) StatusWithinTat else StatusBreachedTat
                            )
                        )
                        Text(
                            text = "SLA Met",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        )
                    }
                }

                // Legend
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(start = 12.dp)
                ) {
                    ChartLegendItem(
                        label = "Within TAT (On Track)",
                        count = withinTatCount,
                        percent = (withinRatio * 100).toDouble(),
                        color = StatusWithinTat
                    )
                    ChartLegendItem(
                        label = "At Risk (< 25% Time)",
                        count = atRiskCount,
                        percent = (atRiskRatio * 100).toDouble(),
                        color = StatusAtRisk
                    )
                    ChartLegendItem(
                        label = "Breached TAT (Overdue)",
                        count = breachedCount,
                        percent = (breachedRatio * 100).toDouble(),
                        color = StatusBreachedTat
                    )
                }
            }
        }
    }
}

@Composable
fun ChartLegendItem(
    label: String,
    count: Int,
    percent: Double,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$count tasks (${String.format(Locale.US, "%.0f%%", percent)})",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            )
        }
    }
}

@Composable
fun TaskPipelineProgressBar(
    statusCounts: Map<TaskStatus, Int>,
    modifier: Modifier = Modifier
) {
    val total = statusCounts.values.sum().coerceAtLeast(1)
    val toDoCount = statusCounts[TaskStatus.TO_DO] ?: 0
    val inProgressCount = statusCounts[TaskStatus.IN_PROGRESS] ?: 0
    val pendingCount = statusCounts[TaskStatus.PENDING] ?: 0
    val completedCount = statusCounts[TaskStatus.COMPLETED] ?: 0

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Task Workflow Status Pipeline",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$total Active / Total",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Multi-color Segmented Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (completedCount > 0) {
                    Box(
                        modifier = Modifier
                            .weight(completedCount.toFloat())
                            .fillMaxWidth()
                            .background(StatusWithinTat)
                    )
                }
                if (inProgressCount > 0) {
                    Box(
                        modifier = Modifier
                            .weight(inProgressCount.toFloat())
                            .fillMaxWidth()
                            .background(StatusInProgress)
                    )
                }
                if (pendingCount > 0) {
                    Box(
                        modifier = Modifier
                            .weight(pendingCount.toFloat())
                            .fillMaxWidth()
                            .background(StatusPending)
                    )
                }
                if (toDoCount > 0) {
                    Box(
                        modifier = Modifier
                            .weight(toDoCount.toFloat())
                            .fillMaxWidth()
                            .background(StatusToDo)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pipeline Grid Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PipelineStatItem("Completed", completedCount, StatusWithinTat)
                PipelineStatItem("In Progress", inProgressCount, StatusInProgress)
                PipelineStatItem("Pending", pendingCount, StatusPending)
                PipelineStatItem("To Do", toDoCount, StatusToDo)
            }
        }
    }
}

@Composable
private fun PipelineStatItem(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        )
    }
}

@Composable
fun UnitSlaBenchmarkChart(
    unitSummaries: List<UnitPerformanceSummary>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Unit SLA Performance vs Target (95%)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            unitSummaries.forEach { u ->
                val barProgress = (u.slaPercent / 100.0).toFloat().coerceIn(0f, 1f)
                val isSlaMet = u.slaPercent >= u.targetSlaPercent

                Column(modifier = Modifier.padding(vertical = 5.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${u.unit.code} - ${u.unit.name}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${String.format(Locale.US, "%.1f%%", u.slaPercent)} (Target: ${String.format(Locale.US, "%.0f%%", u.targetSlaPercent)})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isSlaMet) StatusWithinTat else StatusBreachedTat
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    LinearProgressIndicator(
                        progress = { barProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (isSlaMet) StatusWithinTat else StatusBreachedTat,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${u.totalTasks} total • ${u.withinTatCount} within TAT • ${u.breachedTatCount} breached",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 9.sp
                            )
                        )
                        Text(
                            text = "AHT: ${String.format(Locale.US, "%.1f", u.avgResolutionHours)}h",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 9.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimeMotionActivityBreakdown(
    activityDist: Map<String, Int>,
    totalMinutes: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Time & Motion Activity Allocation",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${String.format(Locale.US, "%.1f", totalMinutes / 60.0)} hrs logged",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = HblPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (activityDist.isEmpty()) {
                Text(
                    text = "No time-motion activities recorded yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val totalMins = totalMinutes.coerceAtLeast(1)
                activityDist.entries.sortedByDescending { it.value }.forEach { (activity, mins) ->
                    val ratio = mins.toFloat() / totalMins
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = activity,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${mins / 60}h ${mins % 60}m (${String.format(Locale.US, "%.0f%%", ratio * 100)})",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = HblPrimary
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        LinearProgressIndicator(
                            progress = { ratio.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = HblPrimary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }
    }
}
