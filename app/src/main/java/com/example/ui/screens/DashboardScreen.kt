package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CrisisAlert
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.TatStatus
import com.example.data.entity.UserRole
import com.example.ui.components.CxKpiCard
import com.example.ui.components.HblBrandHeader
import com.example.ui.components.SlaDonutChart
import com.example.ui.components.TaskPipelineProgressBar
import com.example.ui.components.TimeMotionActivityBreakdown
import com.example.ui.components.UnitSlaBenchmarkChart
import com.example.ui.theme.HblLime
import com.example.ui.theme.HblOnLime
import com.example.ui.theme.HblPrimary
import com.example.ui.theme.HblPrimaryDark
import com.example.ui.theme.HblTertiaryGold
import com.example.ui.theme.StatusAtRisk
import com.example.ui.theme.StatusBreachedTat
import com.example.ui.theme.StatusWithinTat
import com.example.ui.viewmodel.CxViewModel
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: CxViewModel,
    onNavigateToTasks: (TatStatus?) -> Unit,
    onNavigateToExecutiveSummary: () -> Unit = {},
    onNavigateToExcelExport: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val analytics by viewModel.analytics.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("dashboard_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            HblBrandHeader()
        }

        // User Identity & Relevant Unit Portion Banner
        item {
            val isUnitHead = currentUser?.role == UserRole.UNIT_HEAD.name ||
                    currentUser?.role == UserRole.ADMIN.name ||
                    currentUser?.fullName?.equals("Sabeen Shafique", ignoreCase = true) == true

            Card(
                modifier = Modifier.fillMaxWidth().testTag("user_profile_portion_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (isUnitHead) HblLime else HblPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentUser?.fullName?.split(" ")?.mapNotNull { it.firstOrNull()?.toString() }?.take(2)?.joinToString("") ?: "CX",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = if (isUnitHead) HblOnLime else Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentUser?.fullName ?: "Sabeen Shafique",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = HblPrimary)
                            )
                            if (isUnitHead) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = HblLime.copy(alpha = 0.35f)
                                ) {
                                    Text(
                                        text = "UNIT HEAD",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = HblOnLime,
                                            fontSize = 9.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = if (isUnitHead)
                                "Customer Experience Unit Head • Full Department Analytics"
                            else
                                "${currentUser?.role ?: "Team Member"} • ${currentUser?.designation ?: "Assigned Unit"} Portion",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }

        // Executive Summary & Excel Download Action Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("executive_and_export_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = HblPrimaryDark
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = HblLime,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Assessment,
                                        contentDescription = null,
                                        tint = HblOnLime,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Executive Summary & Reports",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "Download full dashboard analysis in Excel / CSV format",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onNavigateToExecutiveSummary,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = HblLime,
                                contentColor = HblOnLime
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("btn_dashboard_executive_summary")
                        ) {
                            Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Executive Brief", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = { viewModel.exportExcelReport(context) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.18f),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.2f).testTag("btn_dashboard_download_excel")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp), tint = HblLime)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Download Excel", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = onNavigateToExcelExport,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_dashboard_view_tables")
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Critical Attention Alert if Breaches or At-Risk tasks exist
        if (analytics.breachedTatCount > 0 || analytics.atRiskCount > 0) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToTasks(if (analytics.breachedTatCount > 0) TatStatus.BREACHED_TAT else TatStatus.AT_RISK) }
                        .testTag("critical_alert_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = if (analytics.breachedTatCount > 0) Color(0xFFFEF2F2) else Color(0xFFFFFBEB)),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (analytics.breachedTatCount > 0) Color(0xFFFCA5A5) else Color(0xFFFDE68A)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (analytics.breachedTatCount > 0) StatusBreachedTat else StatusAtRisk),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (analytics.breachedTatCount > 0) Icons.Default.CrisisAlert else Icons.Default.HourglassTop,
                                contentDescription = "Alert",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (analytics.breachedTatCount > 0)
                                    "TAT Breach Alert: ${analytics.breachedTatCount} Overdue Task(s)"
                                else
                                    "TAT Warning: ${analytics.atRiskCount} Task(s) Approaching SLA Deadline",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (analytics.breachedTatCount > 0) StatusBreachedTat else StatusAtRisk
                                )
                            )
                            Text(
                                text = "Tap to review escalated customer cases and branch bottlenecks",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        }

        // Primary KPI 2x2 Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CxKpiCard(
                    title = "SLA Compliance",
                    value = String.format(Locale.US, "%.1f%%", analytics.overallSlaPercent),
                    subtitle = "${analytics.withinTatCount} of ${analytics.totalTasks} within TAT",
                    icon = Icons.Default.CheckCircle,
                    accentColor = if (analytics.overallSlaPercent >= 95.0) StatusWithinTat else StatusBreachedTat,
                    trendPositive = analytics.overallSlaPercent >= 95.0,
                    modifier = Modifier.weight(1f)
                )
                CxKpiCard(
                    title = "TAT Breach Rate",
                    value = String.format(Locale.US, "%.1f%%", analytics.breachRatePercent),
                    subtitle = "${analytics.breachedTatCount} breaches total",
                    icon = Icons.Default.ErrorOutline,
                    accentColor = if (analytics.breachedTatCount > 0) StatusBreachedTat else StatusWithinTat,
                    trendPositive = analytics.breachedTatCount == 0,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CxKpiCard(
                    title = "Time-Motion Logged",
                    value = "${String.format(Locale.US, "%.1f", analytics.totalTimeMotionMinutes / 60.0)}h",
                    subtitle = "Avg AHT: ${String.format(Locale.US, "%.0f", analytics.avgHandlingTimeMinutes)} min/task",
                    icon = Icons.Default.Timelapse,
                    accentColor = HblPrimary,
                    modifier = Modifier.weight(1f)
                )
                CxKpiCard(
                    title = "Avg Turnaround",
                    value = "${String.format(Locale.US, "%.1f", analytics.avgTurnaroundHours)}h",
                    subtitle = "${analytics.completedTasks} completed tasks",
                    icon = Icons.Default.Speed,
                    accentColor = HblTertiaryGold,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // SLA Donut Chart
        item {
            SlaDonutChart(
                withinTatCount = analytics.withinTatCount,
                atRiskCount = analytics.atRiskCount,
                breachedCount = analytics.breachedTatCount,
                slaPercent = analytics.overallSlaPercent
            )
        }

        // Task Pipeline Bar
        item {
            TaskPipelineProgressBar(statusCounts = analytics.statusCounts)
        }

        // Unit SLA Benchmark
        item {
            UnitSlaBenchmarkChart(unitSummaries = analytics.unitSummaries)
        }

        // Time Motion Activity Allocation
        item {
            TimeMotionActivityBreakdown(
                activityDist = analytics.activityDistribution,
                totalMinutes = analytics.totalTimeMotionMinutes
            )
        }

        // Top Performers Leaderboard
        item {
            TopPerformersCard(memberSummaries = analytics.memberSummaries)
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun TopPerformersCard(
    memberSummaries: List<com.example.data.model.MemberPerformanceSummary>,
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
                    text = "CX Team Member Performance Leaderboard",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Leaderboard",
                    tint = HblTertiaryGold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val sortedMembers = memberSummaries.sortedByDescending { it.productivityScore }.take(4)

            sortedMembers.forEachIndexed { index, m ->
                val medalColor = when (index) {
                    0 -> Color(0xFFFFD700)
                    1 -> Color(0xFFC0C0C0)
                    2 -> Color(0xFFCD7F32)
                    else -> HblPrimary
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(medalColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "#${index + 1}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = medalColor
                                )
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = m.member.fullName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${m.member.employeeId} • ${m.unitCode} • ${m.member.role}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${String.format(Locale.US, "%.0f", m.productivityScore)} pts",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = HblPrimary
                                )
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.0f%%", m.slaPercent)} SLA • ${m.completedCount} done",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (m.slaPercent >= 90.0) StatusWithinTat else StatusBreachedTat,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
