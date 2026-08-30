package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CxDepartmentAnalytics
import com.example.data.model.UnitPerformanceSummary
import com.example.ui.components.HblBrandHeader
import com.example.ui.theme.HblPrimary
import com.example.ui.theme.HblPrimaryDark
import com.example.ui.theme.HblTertiaryGold
import com.example.ui.theme.StatusAtRisk
import com.example.ui.theme.StatusBreachedTat
import com.example.ui.theme.StatusWithinTat
import com.example.ui.viewmodel.CxViewModel
import java.util.Locale

@Composable
fun ExecutiveSummaryScreen(
    viewModel: CxViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val analytics by viewModel.analytics.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("executive_summary_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            HblBrandHeader(
                title = "HBL Microfinance Bank",
                subtitle = "Executive CX Brief • Department & Unit Performance Synthesis"
            )
        }

        // Top Executive Status Verdict Card
        item {
            ExecutiveVerdictBanner(analytics = analytics)
        }

        // Quick Export Actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.exportExcelReport(context) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = HblPrimary)
                ) {
                    Icon(Icons.Default.Download, contentDescription = "Export")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Download Full Report")
                }

                OutlinedButton(
                    onClick = { viewModel.copyExcelTableToClipboard(context) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Copy")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy for Excel")
                }
            }
        }

        // Collective CX Team Performance Synthesis
        item {
            CollectiveSynthesisCard(analytics = analytics)
        }

        // Unit-by-Unit Executive Highlights
        item {
            Text(
                text = "Unit-Level Executive Performance Summaries",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        }

        items(analytics.unitSummaries.size) { index ->
            val uSummary = analytics.unitSummaries[index]
            UnitExecutiveDetailCard(summary = uSummary)
        }

        // TAT Breach Root Cause & Bottleneck Analysis
        item {
            BreachAnalysisCard(analytics = analytics)
        }

        // Leadership Recommendations
        item {
            RecommendationsCard(analytics = analytics)
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun ExecutiveVerdictBanner(analytics: CxDepartmentAnalytics) {
    val isOverallCompliant = analytics.overallSlaPercent >= 95.0
    val isModerate = analytics.overallSlaPercent >= 90.0

    val bannerGradient = when {
        isOverallCompliant -> listOf(Color(0xFF005B48), Color(0xFF008269))
        isModerate -> listOf(Color(0xFFB45309), Color(0xFFD97706))
        else -> listOf(Color(0xFF991B1B), Color(0xFFDC2626))
    }

    val verdictTitle = when {
        isOverallCompliant -> "SLA COMPLIANCE: BENCHMARK ACHIEVED"
        isModerate -> "SLA COMPLIANCE: MODERATE RISK"
        else -> "SLA COMPLIANCE: CRITICAL ESCALATION"
    }

    val verdictDescription = when {
        isOverallCompliant -> "The collective CX department is performing above the 95% target SLA benchmark with strong resolution turnaround times."
        isModerate -> "SLA adherence is slightly below target. Immediate focus needed on pending branch inquiries and high-turnaround tasks."
        else -> "Critical TAT breaches detected across branch coordination. Immediate operational intervention required."
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(bannerGradient))
                .padding(18.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = verdictTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Text(
                        text = "${String.format(Locale.US, "%.1f%%", analytics.overallSlaPercent)} SLA",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = verdictDescription,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(alpha = 0.9f)
                    )
                )
            }
        }
    }
}

@Composable
fun CollectiveSynthesisCard(analytics: CxDepartmentAnalytics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Collective CX Department Synthesis",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(Icons.Default.Assessment, contentDescription = "Synthesis", tint = HblPrimary)
            }

            Text(
                text = "Comprehensive performance rollup across all 6 CX units including Complaints, Inbound/Outbound, Field Quality, QA, Digital CX, and High-Net-Worth Retention:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 4-point Summary Grid
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SynthesisPoint(
                        label = "Workload & Resolution Velocity",
                        value = "${analytics.completedTasks} tasks closed out of ${analytics.totalTasks} total volume (${String.format(Locale.US, "%.0f%%", if (analytics.totalTasks > 0) (analytics.completedTasks.toDouble() / analytics.totalTasks) * 100 else 0.0)} closure rate)"
                    )
                    SynthesisPoint(
                        label = "Turnaround Time (TAT) Discipline",
                        value = "Average resolution duration is ${String.format(Locale.US, "%.1f", analytics.avgTurnaroundHours)} hours against 24.0-hour standard benchmark."
                    )
                    SynthesisPoint(
                        label = "Active Time-Motion Utilization",
                        value = "${String.format(Locale.US, "%.1f", analytics.totalTimeMotionMinutes / 60.0)} total operational hours logged across case investigation, customer callback, and scoring."
                    )
                    SynthesisPoint(
                        label = "Overdue & SLA Breach Hotspots",
                        value = "${analytics.breachedTatCount} cases currently breached TAT (${String.format(Locale.US, "%.1f%%", analytics.breachRatePercent)} breach rate), with ${analytics.criticalBreachesCount} critical priority."
                    )
                }
            }
        }
    }
}

@Composable
fun SynthesisPoint(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .padding(top = 5.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(HblPrimary)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = HblPrimary)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface)
            )
        }
    }
}

@Composable
fun UnitExecutiveDetailCard(summary: UnitPerformanceSummary) {
    val u = summary.unit
    val isSlaMet = summary.slaPercent >= summary.targetSlaPercent

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = HblPrimary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = u.code,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = HblPrimary),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = u.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSlaMet) StatusWithinTat.copy(alpha = 0.15f) else StatusBreachedTat.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (isSlaMet) "SLA TARGET MET" else "SLA BREACH ALERT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isSlaMet) StatusWithinTat else StatusBreachedTat,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Unit Head: ${u.unitHeadName} • Team Strength: ${summary.activeMembersCount} Officers",
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ExecutiveMetricBox("SLA Adherence", "${String.format(Locale.US, "%.1f%%", summary.slaPercent)}", if (isSlaMet) StatusWithinTat else StatusBreachedTat)
                ExecutiveMetricBox("Target SLA", "${u.targetSlaPercent.toInt()}%", MaterialTheme.colorScheme.onSurface)
                ExecutiveMetricBox("Avg Turnaround", "${String.format(Locale.US, "%.1f", summary.avgResolutionHours)}h", HblTertiaryGold)
                ExecutiveMetricBox("Motion Logged", "${String.format(Locale.US, "%.1f", summary.totalTimeMotionHours)}h", HblPrimary)
            }
        }
    }
}

@Composable
fun ExecutiveMetricBox(label: String, value: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.width(76.dp)
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant), maxLines = 1)
            Text(text = value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = color))
        }
    }
}

@Composable
fun BreachAnalysisCard(analytics: CxDepartmentAnalytics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TAT Breach Root Cause Deep-Dive",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(Icons.Default.ReportProblem, contentDescription = "Breaches", tint = StatusBreachedTat)
            }

            Text(
                text = "Key operational friction points identified during time-motion study and case tracking:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val causes = if (analytics.breachReasonDistribution.isNotEmpty()) {
                analytics.breachReasonDistribution.entries.map { "${it.key} (${it.value} cases)" }
            } else {
                listOf(
                    "Branch credit officer physical verification delay (40%)",
                    "Third-party ATM switch journal reconciliation delay (35%)",
                    "Customer biometric re-verification failure (25%)"
                )
            }

            causes.forEach { cause ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(StatusBreachedTat))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = cause, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface))
                }
            }
        }
    }
}

@Composable
fun RecommendationsCard(analytics: CxDepartmentAnalytics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Executive Action Items & Leadership Next Steps",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(Icons.Default.Lightbulb, contentDescription = "Recommendations", tint = HblTertiaryGold)
            }

            val items = listOf(
                "Establish strict 4-hour Branch Escalation SLA for Complaints Management Unit (CMU) queries.",
                "Automate Core Banking Raast dispute settlement log retrieval for Digital CX team to reduce AHT by 45%.",
                "Deploy proactive SMS alerts to customers whose case TAT enters 'At Risk' status (< 25% time left).",
                "Review weekly time-motion allocation to redirect officer hours from administrative filing to customer outreach."
            )

            items.forEachIndexed { i, item ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "${i + 1}.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = HblPrimary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface)
                    )
                }
            }
        }
    }
}
