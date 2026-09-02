package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CorporateFare
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.DailyTaskEntry
import com.example.data.entity.TaskStatus
import com.example.data.entity.TatStatus
import com.example.data.entity.UserRole
import com.example.data.model.AiAnalysisState
import com.example.ui.theme.HblLime
import com.example.ui.theme.HblOnLime
import com.example.ui.theme.HblPrimary
import com.example.ui.theme.HblPrimaryDark
import com.example.ui.theme.HblSecondary
import com.example.ui.theme.HblTertiaryGold
import com.example.ui.theme.StatusAtRisk
import com.example.ui.theme.StatusBreachedTat
import com.example.ui.theme.StatusWithinTat
import com.example.ui.viewmodel.CxViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyTasksScreen(
    viewModel: CxViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val units by viewModel.units.collectAsStateWithLifecycle()
    val dailyTasks by viewModel.userDailyTasks.collectAsStateWithLifecycle()
    val scorecard by viewModel.userPerformanceScorecard.collectAsStateWithLifecycle()
    val aiState by viewModel.aiAnalysisState.collectAsStateWithLifecycle()
    val teamMembers by viewModel.teamMembers.collectAsStateWithLifecycle()
    val memberFilter by viewModel.dailyTasksMemberFilter.collectAsStateWithLifecycle()

    val isUnitHead = currentUser?.role == UserRole.UNIT_HEAD.name ||
            currentUser?.role == UserRole.ADMIN.name ||
            currentUser?.fullName?.equals("Sabeen Shafique", ignoreCase = true) == true

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showAiModal by remember { mutableStateOf(false) }
    var selectedTaskToDelete by remember { mutableStateOf<DailyTaskEntry?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))

                // Role Banner: Unit Head Supervision or Personal Workspace
                if (isUnitHead) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth().testTag("unit_head_supervision_card")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.SupervisorAccount, contentDescription = null, tint = HblPrimary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "CX Unit Head Supervision",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = HblPrimary)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = HblLime.copy(alpha = 0.25f)
                                ) {
                                    Text(
                                        text = "Sabeen Shafique",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, color = HblOnLime),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Audit individual team member activities and automated scorecards, or view department-wide aggregate summary.",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = memberFilter.isNullOrBlank() || memberFilter == "ALL",
                                    onClick = { viewModel.setDailyTasksMemberFilter(null) },
                                    label = { Text("🏢 All Members (${dailyTasks.size})") }
                                )
                                teamMembers.forEach { m ->
                                    val isSelected = memberFilter?.equals(m.fullName, ignoreCase = true) == true
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.setDailyTasksMemberFilter(if (isSelected) null else m.fullName) },
                                        label = { Text(m.fullName) }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth().testTag("member_personal_workspace_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = HblPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Personal Work Log: Activities logged here are isolated to ${currentUser?.fullName ?: "your profile"} and summarized for Unit Head Sabeen Shafique.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
                }
            }

            item {
                // 1. AUTOMATED PERFORMANCE SCORECARD HERO CARD
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier.fillMaxWidth().testTag("performance_scorecard_card")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        // Header: User & Role & Automated Tier Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = scorecard.userName,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = HblPrimary
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = Color(android.graphics.Color.parseColor(scorecard.tierColorHex)).copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = scorecard.userRole,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color(android.graphics.Color.parseColor(scorecard.tierColorHex)),
                                                fontWeight = FontWeight.Bold
                                            ),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "${scorecard.unitName} • Auto-Evaluated",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                                )
                            }

                            // Circular Overall Score Dial
                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                Color(android.graphics.Color.parseColor(scorecard.tierColorHex)),
                                                HblPrimary
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${scorecard.overallPerformanceScore.toInt()}",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Black
                                        )
                                    )
                                    Text(
                                        text = "SCORE",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.White.copy(alpha = 0.85f),
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Performance Tier Rating Banner
                        Surface(
                            color = Color(android.graphics.Color.parseColor(scorecard.tierColorHex)).copy(alpha = 0.10f),
                            shape = RoundedCornerShape(12.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = Brush.linearGradient(
                                    listOf(
                                        Color(android.graphics.Color.parseColor(scorecard.tierColorHex)),
                                        Color(android.graphics.Color.parseColor(scorecard.tierColorHex)).copy(alpha = 0.3f)
                                    )
                                )
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(android.graphics.Color.parseColor(scorecard.tierColorHex)),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "AUTOMATED RATING TIER",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Gray
                                            )
                                        )
                                        Text(
                                            text = scorecard.performanceTier,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color(android.graphics.Color.parseColor(scorecard.tierColorHex))
                                            )
                                        )
                                    }
                                }

                                Text(
                                    text = "${"%.1f".format(scorecard.totalHoursWorked)}h logged",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Gray
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 4 Automated Key Performance Indicator Tiles
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            scorecard.keyIndicatorsList.take(2).forEach { ind ->
                                IndicatorCard(
                                    title = ind.title,
                                    value = ind.valueStr,
                                    target = ind.targetStr,
                                    colorHex = ind.statusColorHex,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            scorecard.keyIndicatorsList.drop(2).take(2).forEach { ind ->
                                IndicatorCard(
                                    title = ind.title,
                                    value = ind.valueStr,
                                    target = ind.targetStr,
                                    colorHex = ind.statusColorHex,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Automated AI Coach & Commentary Trigger
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    viewModel.runAiPerformanceAnalysis()
                                    showAiModal = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = HblLime,
                                    contentColor = HblOnLime
                                ),
                                modifier = Modifier.weight(1f).testTag("generate_ai_appraisal_btn")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("AI Performance Appraisal", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            // 2. DAILY TASKS LIST HEADER
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Daily Work & Task Log",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${dailyTasks.size} tasks logged today • Real-time indicator feed",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                        )
                    }

                    OutlinedButton(
                        onClick = { showAddTaskDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("add_daily_task_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Task", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 3. EMPTY STATE OR TASK CARDS
            if (dailyTasks.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = null,
                                tint = HblPrimary,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No Daily Tasks Logged Yet",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Click 'Add Task' to record your completed cases, calls, and audits. The system will automatically calculate your SLA & Quality indicators!",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { showAddTaskDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = HblPrimary)
                            ) {
                                Text("Log First Daily Task")
                            }
                        }
                    }
                }
            } else {
                items(dailyTasks, key = { it.id }) { task ->
                    DailyTaskCard(
                        task = task,
                        onDelete = { selectedTaskToDelete = task }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = { showAddTaskDialog = true },
            containerColor = HblPrimary,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("fab_add_daily_task")
        ) {
            Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Add, contentDescription = "Add Daily Task")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Log Task", fontWeight = FontWeight.Bold)
            }
        }
    }

    // ADD DAILY TASK DIALOG
    if (showAddTaskDialog) {
        val currentUnitName = units.find { it.id == currentUser?.unitId }?.name ?: ""
        AddDailyTaskDialog(
            unitName = currentUnitName,
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { title, category, count, hours, status, tat, quality, fcr, metric, notes, customDateStr, customTimestamp, dialled, connected, answered ->
                viewModel.addDailyTask(
                    title = title,
                    category = category,
                    tasksCount = count,
                    hoursSpent = hours,
                    status = status,
                    tatStatus = tat,
                    qualityScore = quality,
                    fcrResolved = fcr,
                    impactMetric = metric,
                    notes = notes,
                    customDateString = customDateStr,
                    customTimestamp = customTimestamp,
                    totalDialledCalls = dialled,
                    connectedCalls = connected,
                    answeredCalls = answered
                )
                showAddTaskDialog = false
                Toast.makeText(context, "Daily task logged for $customDateStr! Indicators updated automatically.", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // AI PERFORMANCE APPRAISAL MODAL
    if (showAiModal) {
        AlertDialog(
            onDismissRequest = { showAiModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = HblPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI Executive Performance Review", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    when (val state = aiState) {
                        is AiAnalysisState.Loading -> {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = HblPrimary)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Analyzing daily tasks & calculating SLA telemetry...", fontSize = 13.sp, color = Color.Gray)
                            }
                        }
                        is AiAnalysisState.Success -> {
                            Text(
                                text = state.analysisText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        is AiAnalysisState.Error -> {
                            Text(text = state.error, color = Color.Red)
                        }
                        AiAnalysisState.Idle -> {
                            Text("Ready to evaluate.")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAiModal = false },
                    colors = ButtonDefaults.buttonColors(containerColor = HblPrimary)
                ) {
                    Text("Done")
                }
            }
        )
    }

    // DELETE CONFIRMATION DIALOG
    selectedTaskToDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { selectedTaskToDelete = null },
            title = { Text("Delete Daily Task?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to remove '${task.title}'? This will recalibrate your daily performance indicators.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDailyTask(task)
                        selectedTaskToDelete = null
                        Toast.makeText(context, "Task removed and indicators recalibrated.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedTaskToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun IndicatorCard(
    title: String,
    value: String,
    target: String,
    colorHex: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(android.graphics.Color.parseColor(colorHex)).copy(alpha = 0.08f)
        ),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = Color(android.graphics.Color.parseColor(colorHex))
                )
            )
            Text(
                text = target,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    color = Color.Gray
                )
            )
        }
    }
}

@Composable
fun DailyTaskCard(
    task: DailyTaskEntry,
    onDelete: () -> Unit
) {
    val tatColor = when (task.tatStatus) {
        TatStatus.WITHIN_TAT -> StatusWithinTat
        TatStatus.AT_RISK -> StatusAtRisk
        TatStatus.BREACHED_TAT -> StatusBreachedTat
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = HblPrimary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = task.category,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = HblPrimary,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = tatColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = when (task.tatStatus) {
                                TatStatus.WITHIN_TAT -> "✓ Within TAT"
                                TatStatus.AT_RISK -> "⚠️ At Risk"
                                TatStatus.BREACHED_TAT -> "✕ Breached"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = tatColor,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )

            if (task.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = task.notes,
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.25f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📦 ${task.tasksCount} cases • ⏱️ ${"%.1f".format(task.hoursSpent)} hrs",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray, fontWeight = FontWeight.SemiBold)
                )

                Text(
                    text = "Quality: ${task.qualityScorePercent.toInt()}% • ${if (task.fcrResolved) "FCR ✓" else "Follow-up"}",
                    style = MaterialTheme.typography.labelSmall.copy(color = HblPrimary, fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDailyTaskDialog(
    unitName: String = "",
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        category: String,
        count: Int,
        hours: Double,
        status: TaskStatus,
        tat: TatStatus,
        quality: Double,
        fcr: Boolean,
        metric: String,
        notes: String,
        customDateString: String,
        customTimestamp: Long,
        totalDialledCalls: Int,
        connectedCalls: Int,
        answeredCalls: Int
    ) -> Unit
) {
    // The VOC unit logs calls, not "items/cases" — it gets its own
    // Total Dialled / Connected / Answered call-log form below. A VOC team
    // member can still switch to "Other Task" if what they need to log
    // isn't a call (e.g. a report, a meeting, coordination work).
    val isVocUnit = unitName.trim().equals("VOC", ignoreCase = true) ||
            unitName.contains("Voice of Customer", ignoreCase = true)
    var loggingOtherTask by remember { mutableStateOf(false) }
    val showCallLogForm = isVocUnit && !loggingOtherTask

    var title by remember { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf("Customer Complaint") }
    var countStr by remember { mutableStateOf("1") }
    var hoursStr by remember { mutableStateOf("1.0") }
    var qualityScore by remember { mutableDoubleStateOf(95.0) }
    var fcrResolved by remember { mutableStateOf(true) }
    var tatStatus by remember { mutableStateOf(TatStatus.WITHIN_TAT) }
    var status by remember { mutableStateOf(TaskStatus.COMPLETED) }
    var impactMetric by remember { mutableStateOf("SLA Turnaround") }
    var notes by remember { mutableStateOf("") }

    // VOC call-log fields
    var dialledStr by remember { mutableStateOf("") }
    var connectedStr by remember { mutableStateOf("") }
    var answeredStr by remember { mutableStateOf("") }
    var othersCountStr by remember { mutableStateOf("") }
    var othersDescription by remember { mutableStateOf("") }

    // Calendar Work Date selection
    val standardDateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val displayDateFormat = remember { SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault()) }

    var selectedPreset by remember { mutableStateOf("Today") }
    var markedTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var markedDateStr by remember { mutableStateOf(standardDateFormat.format(Date())) }

    val categories = listOf(
        "Customer Complaint",
        "Call Center Tele-Services",
        "Branch Quality Audit",
        "QA Call Calibration",
        "Digital App Dispute",
        "Regulatory Ombudsman",
        "General CX Operation"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = HblPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (showCallLogForm) "Log VOC Call Activity" else "Log Daily Task",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 📅 CALENDAR WORK DATE MARKING
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DateRange, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Calendar Work Date",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF166534))
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))

                            // Date Preset Chips
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val dayPresets = listOf(
                                    Pair("Today", 0),
                                    Pair("Yesterday", -1),
                                    Pair("-2 Days", -2),
                                    Pair("-3 Days", -3),
                                    Pair("-1 Week", -7)
                                )
                                dayPresets.forEach { (presetLabel, daysOffset) ->
                                    val isSelected = selectedPreset == presetLabel
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedPreset = presetLabel
                                            val cal = Calendar.getInstance()
                                            cal.add(Calendar.DAY_OF_YEAR, daysOffset)
                                            markedTimestamp = cal.timeInMillis
                                            markedDateStr = standardDateFormat.format(cal.time)
                                        },
                                        label = { Text(presetLabel) },
                                        leadingIcon = if (isSelected) {
                                            { Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                        } else null
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Formatted Calendar Date Banner
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "📅 Marked Task Date:",
                                            style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                                        )
                                        Text(
                                            text = displayDateFormat.format(Date(markedTimestamp)),
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF166534)
                                            )
                                        )
                                    }
                                    Surface(
                                        color = Color(0xFFDCFCE7),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = markedDateStr,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color(0xFF166534),
                                                fontWeight = FontWeight.Bold
                                            ),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (isVocUnit) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = loggingOtherTask,
                                onCheckedChange = { loggingOtherTask = it }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text("Other Task (not a call)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("Tick this if what you're logging isn't a call — e.g. a report or meeting", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }

                if (showCallLogForm) {
                    // ☎️ VOC CALL LOG FORM
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Call Volumes",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF1D4ED8))
                                )
                                OutlinedTextField(
                                    value = dialledStr,
                                    onValueChange = { dialledStr = it.filter { c -> c.isDigit() } },
                                    label = { Text("Total Dialled Calls *") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("voc_dialled_input"),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = connectedStr,
                                        onValueChange = { connectedStr = it.filter { c -> c.isDigit() } },
                                        label = { Text("Connected") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f).testTag("voc_connected_input"),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    OutlinedTextField(
                                        value = answeredStr,
                                        onValueChange = { answeredStr = it.filter { c -> c.isDigit() } },
                                        label = { Text("Answered") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f).testTag("voc_answered_input"),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                                HorizontalDivider()
                                Text(
                                    text = "Others",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF1D4ED8))
                                )
                                Text(
                                    "Anything besides the calls above — leave blank if not applicable",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = othersCountStr,
                                        onValueChange = { othersCountStr = it.filter { c -> c.isDigit() } },
                                        label = { Text("Count") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    OutlinedTextField(
                                        value = othersDescription,
                                        onValueChange = { othersDescription = it },
                                        label = { Text("What is it?") },
                                        singleLine = true,
                                        modifier = Modifier.weight(2f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = hoursStr,
                            onValueChange = { hoursStr = it },
                            label = { Text("Hours Spent") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                } else {
                    // GENERAL TASK FORM (non-VOC units, or VOC's "Other Task")
                    item {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Task / Activity Title *") },
                            placeholder = { Text("e.g. Resolved 6 ATM reversal tickets") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("daily_task_title_input"),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    item {
                        Text("Category", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        ExposedDropdownMenuBox(
                            expanded = categoryExpanded,
                            onExpandedChange = { categoryExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = category,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                                    .testTag("daily_task_category_dropdown"),
                                shape = RoundedCornerShape(10.dp)
                            )
                            DropdownMenu(
                                expanded = categoryExpanded,
                                onDismissRequest = { categoryExpanded = false }
                            ) {
                                categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat) },
                                        onClick = {
                                            category = cat
                                            categoryExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = countStr,
                                onValueChange = { countStr = it },
                                label = { Text("Items / Cases") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = hoursStr,
                                onValueChange = { hoursStr = it },
                                label = { Text("Hours Spent") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "Quality Score: ${qualityScore.toInt()}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = HblPrimary
                    )
                    Slider(
                        value = qualityScore.toFloat(),
                        onValueChange = { qualityScore = it.toDouble() },
                        valueRange = 50f..100f,
                        steps = 9
                    )
                }

                item {
                    Text("Turnaround TAT Status", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TatStatus.values().forEach { t ->
                            val isSelected = tatStatus == t
                            val col = when (t) {
                                TatStatus.WITHIN_TAT -> StatusWithinTat
                                TatStatus.AT_RISK -> StatusAtRisk
                                TatStatus.BREACHED_TAT -> StatusBreachedTat
                            }
                            Surface(
                                color = if (isSelected) col else col.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).clickable { tatStatus = t }
                            ) {
                                Text(
                                    text = when (t) {
                                        TatStatus.WITHIN_TAT -> "Within TAT"
                                        TatStatus.AT_RISK -> "At Risk"
                                        TatStatus.BREACHED_TAT -> "Breached"
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else col,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                if (!showCallLogForm) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = fcrResolved,
                                onCheckedChange = { fcrResolved = it }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text("First Contact Resolution (FCR)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("Resolved on initial customer engagement", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Resolution Remarks & Notes") },
                        placeholder = { Text("Optional notes or ticket reference numbers") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        maxLines = 3
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val h = hoursStr.toDoubleOrNull() ?: 1.0
                    if (showCallLogForm) {
                        val dialled = dialledStr.toIntOrNull() ?: 0
                        if (dialled <= 0) return@Button
                        val connected = connectedStr.toIntOrNull() ?: 0
                        val answered = answeredStr.toIntOrNull() ?: 0
                        val othersCount = othersCountStr.toIntOrNull() ?: 0
                        val callTitle = "VOC Call Log: $dialled dialled, $connected connected, $answered answered" +
                                if (othersCount > 0 && othersDescription.isNotBlank()) " + $othersCount $othersDescription" else ""
                        onConfirm(
                            callTitle,
                            "VOC Call Log",
                            dialled + othersCount,
                            h,
                            status,
                            tatStatus,
                            qualityScore,
                            fcrResolved,
                            impactMetric,
                            notes,
                            markedDateStr,
                            markedTimestamp,
                            dialled,
                            connected,
                            answered
                        )
                    } else {
                        if (title.isBlank()) return@Button
                        val c = countStr.toIntOrNull() ?: 1
                        onConfirm(
                            title,
                            category,
                            c,
                            h,
                            status,
                            tatStatus,
                            qualityScore,
                            fcrResolved,
                            impactMetric,
                            notes,
                            markedDateStr,
                            markedTimestamp,
                            0,
                            0,
                            0
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = HblPrimary),
                modifier = Modifier.testTag("submit_daily_task_dialog_btn")
            ) {
                Text("Save & Calibrate KPIs")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
