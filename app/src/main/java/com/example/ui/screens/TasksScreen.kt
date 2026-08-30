package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.CxTask
import com.example.data.entity.CxUnit
import com.example.data.entity.TaskPriority
import com.example.data.entity.TaskStatus
import com.example.data.entity.TatStatus
import com.example.data.entity.TeamMember
import com.example.data.entity.UserRole
import com.example.ui.components.PriorityBadge
import com.example.ui.components.StatusBadge
import com.example.ui.components.TatBadge
import com.example.ui.components.formatDateTime
import com.example.ui.components.formatDurationMinutes
import com.example.ui.theme.HblPrimary
import com.example.ui.theme.HblPrimaryDark
import com.example.ui.theme.HblTertiaryGold
import com.example.ui.theme.StatusAtRisk
import com.example.ui.theme.StatusBreachedTat
import com.example.ui.theme.StatusInProgress
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusWithinTat
import com.example.ui.viewmodel.CxViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    viewModel: CxViewModel,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.filteredTasks.collectAsStateWithLifecycle()
    val allUnits by viewModel.units.collectAsStateWithLifecycle()
    val allMembers by viewModel.teamMembers.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val unitFilter by viewModel.unitFilter.collectAsStateWithLifecycle()
    val statusFilter by viewModel.statusFilter.collectAsStateWithLifecycle()
    val tatFilter by viewModel.tatFilter.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val taskViewScope by viewModel.taskViewScope.collectAsStateWithLifecycle()

    var showCreateTaskDialog by remember { mutableStateOf(false) }
    var taskToUpdateStatus by remember { mutableStateOf<CxTask?>(null) }
    var taskToLogTime by remember { mutableStateOf<CxTask?>(null) }
    var taskForDetails by remember { mutableStateOf<CxTask?>(null) }

    val unitMap = remember(allUnits) { allUnits.associateBy { it.id } }
    val memberMap = remember(allMembers) { allMembers.associateBy { it.id } }

    val isUnitHead = currentUser?.role == UserRole.UNIT_HEAD.name ||
            currentUser?.role == UserRole.ADMIN.name ||
            currentUser?.fullName?.equals("Sabeen Shafique", ignoreCase = true) == true

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .testTag("tasks_screen")
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Task Visibility Scope Chips (Relevant User Scope vs Full Department)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = taskViewScope == "RELEVANT",
                    onClick = { viewModel.setTaskViewScope("RELEVANT") },
                    label = { Text(if (isUnitHead) "🎯 My Unit Tasks" else "🎯 Tasks Assigned to Me") },
                    modifier = Modifier.weight(1f).testTag("scope_relevant_chip")
                )
                FilterChip(
                    selected = taskViewScope == "ALL",
                    onClick = { viewModel.setTaskViewScope("ALL") },
                    label = { Text("🏢 All Department Tasks") },
                    modifier = Modifier.weight(1f).testTag("scope_all_chip")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_search_field"),
                placeholder = { Text("Search task ID, title, account, category...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Horizontal Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // All Filter Reset Chip
                val hasActiveFilters = unitFilter != null || statusFilter != null || tatFilter != null || searchQuery.isNotEmpty()
                if (hasActiveFilters) {
                    FilterChip(
                        selected = false,
                        onClick = { viewModel.clearFilters() },
                        label = { Text("Clear Filters (X)", color = StatusBreachedTat) },
                        leadingIcon = { Icon(Icons.Default.Close, contentDescription = "Clear", tint = StatusBreachedTat, modifier = Modifier.size(14.dp)) }
                    )
                }

                // TAT Filter Chips
                TatStatus.entries.forEach { tat ->
                    FilterChip(
                        selected = tatFilter == tat,
                        onClick = { viewModel.setTatFilter(if (tatFilter == tat) null else tat) },
                        label = { Text(tat.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = when (tat) {
                                TatStatus.WITHIN_TAT -> StatusWithinTat.copy(alpha = 0.2f)
                                TatStatus.AT_RISK -> StatusAtRisk.copy(alpha = 0.2f)
                                TatStatus.BREACHED_TAT -> StatusBreachedTat.copy(alpha = 0.2f)
                            }
                        )
                    )
                }

                // Status Filter Chips
                TaskStatus.entries.forEach { status ->
                    FilterChip(
                        selected = statusFilter == status,
                        onClick = { viewModel.setStatusFilter(if (statusFilter == status) null else status) },
                        label = { Text(status.displayName) }
                    )
                }

                // Unit Filter Chips
                allUnits.forEach { unit ->
                    FilterChip(
                        selected = unitFilter == unit.id,
                        onClick = { viewModel.setUnitFilter(if (unitFilter == unit.id) null else unit.id) },
                        label = { Text("${unit.code} - ${unit.name.take(15)}") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Task List Count Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${tasks.size} Tasks Displayed",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                Text(
                    text = "Sorted by Newest",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.AssignmentTurnedIn,
                            contentDescription = "No tasks",
                            modifier = Modifier.size(54.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No tasks found matching criteria",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try clearing search filters or assign a new task",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        val unit = unitMap[task.unitId]
                        val assignee = memberMap[task.assigneeId]

                        TaskCard(
                            task = task,
                            unit = unit,
                            assignee = assignee,
                            onCardClick = { taskForDetails = task },
                            onUpdateStatusClick = { taskToUpdateStatus = task },
                            onLogTimeClick = { taskToLogTime = task }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        // Floating Action Button to Assign Task
        FloatingActionButton(
            onClick = { showCreateTaskDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("create_task_fab"),
            containerColor = HblPrimary,
            contentColor = Color.White
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, contentDescription = "Assign Task")
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Assign Task",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }

    // Dialogs
    if (showCreateTaskDialog) {
        val defaultCreator = currentUser?.let { "${it.fullName} (${it.role})" } ?: "Sabeen Shafique (CX Unit Head)"
        CreateTaskDialog(
            units = allUnits,
            members = allMembers,
            defaultAssignedBy = defaultCreator,
            onDismiss = { showCreateTaskDialog = false },
            onConfirm = { title, desc, unitId, assigneeId, assignedBy, priority, cat, tat, acct, dueDateTime ->
                viewModel.createTask(
                    title = title,
                    description = desc,
                    unitId = unitId,
                    assigneeId = assigneeId,
                    assignedByName = assignedBy,
                    priority = priority,
                    category = cat,
                    tatHours = tat,
                    customerAccountOrTicket = acct,
                    dueDateTime = dueDateTime
                )
                showCreateTaskDialog = false
            }
        )
    }

    taskToUpdateStatus?.let { task ->
        UpdateTaskStatusDialog(
            task = task,
            onDismiss = { taskToUpdateStatus = null },
            onConfirm = { newStatus, remarks, pendingReason, breachReason ->
                viewModel.updateTaskStatus(
                    task = task,
                    newStatus = newStatus,
                    resolutionRemarks = remarks,
                    pendingReason = pendingReason,
                    breachReason = breachReason
                )
                taskToUpdateStatus = null
            }
        )
    }

    taskToLogTime?.let { task ->
        val activeTimerTaskId by viewModel.activeTimerTaskId.collectAsStateWithLifecycle()
        val timerSeconds by viewModel.timerSeconds.collectAsStateWithLifecycle()

        TimeMotionLoggerDialog(
            task = task,
            isTimerActive = activeTimerTaskId == task.id,
            timerSeconds = if (activeTimerTaskId == task.id) timerSeconds else 0,
            onStartTimer = { viewModel.startTimerForTask(task.id) },
            onStopTimer = { activity, notes ->
                viewModel.stopAndSaveTimer(activity, notes)
                taskToLogTime = null
            },
            onLogManual = { duration, activity, notes ->
                viewModel.logManualTimeMotion(task.id, duration, activity, notes)
                taskToLogTime = null
            },
            onDismiss = { taskToLogTime = null }
        )
    }

    taskForDetails?.let { task ->
        val unit = unitMap[task.unitId]
        val assignee = memberMap[task.assigneeId]
        val canDelete = viewModel.canUserDeleteTask(task)

        TaskDetailsBottomSheet(
            task = task,
            unit = unit,
            assignee = assignee,
            canDelete = canDelete,
            onDismiss = { taskForDetails = null },
            onUpdateStatus = {
                taskToUpdateStatus = task
                taskForDetails = null
            },
            onLogTime = {
                taskToLogTime = task
                taskForDetails = null
            },
            onDelete = {
                viewModel.deleteTask(task)
                taskForDetails = null
            }
        )
    }
}

@Composable
fun TaskCard(
    task: CxTask,
    unit: CxUnit?,
    assignee: TeamMember?,
    onCardClick: () -> Unit,
    onUpdateStatusClick: () -> Unit,
    onLogTimeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val now = System.currentTimeMillis()
    val tatStatus = task.computeTatStatus(now)
    val actualTurnaround = task.getActualTurnaroundHours(now)
    val breachHours = task.getBreachHours(now)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
            .testTag("task_card_${task.trackingNumber}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (tatStatus == TatStatus.BREACHED_TAT) {
            androidx.compose.foundation.BorderStroke(1.5.dp, StatusBreachedTat.copy(alpha = 0.5f))
        } else if (tatStatus == TatStatus.AT_RISK) {
            androidx.compose.foundation.BorderStroke(1.5.dp, StatusAtRisk.copy(alpha = 0.5f))
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row: Tracking No, Unit Code, Priority Badge, TAT Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = task.trackingNumber,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = HblPrimary
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = unit?.code ?: "CX",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    PriorityBadge(priority = task.priority)
                    TatBadge(tatStatus = tatStatus, compact = true)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Task Title
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Category & Account Tag
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.category,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                )
                if (!task.customerAccountOrTicket.isNullOrEmpty()) {
                    Text(
                        text = " • AC/Ref: ${task.customerAccountOrTicket}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = HblTertiaryGold,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // TAT & Timing Grid Box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TAT SLA Target: ${task.tatHours.toInt()} Hours",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = "Due: ${formatDateTime(task.dueDateTime)}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        if (task.status == TaskStatus.COMPLETED) {
                            Text(
                                text = "Resolved in ${String.format(Locale.US, "%.1f", actualTurnaround)}h",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (tatStatus == TatStatus.WITHIN_TAT) StatusWithinTat else StatusBreachedTat
                                )
                            )
                        } else if (tatStatus == TatStatus.BREACHED_TAT) {
                            Text(
                                text = "Breached +${String.format(Locale.US, "%.1f", breachHours)}h",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = StatusBreachedTat
                                )
                            )
                        } else {
                            val remainingHours = (task.dueDateTime - now) / 3600000.0
                            Text(
                                text = "${String.format(Locale.US, "%.1f", remainingHours.coerceAtLeast(0.0))}h Left",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (tatStatus == TatStatus.AT_RISK) StatusAtRisk else StatusWithinTat
                                )
                            )
                        }

                        Text(
                            text = "Motion: ${task.timeMotionMinutes}m logged",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 10.sp,
                                color = HblPrimary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Action Row: Assignee & Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Assignee Info
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(HblPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Assignee",
                            tint = HblPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = assignee?.fullName ?: "Unassigned",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "By ${task.assignedByName}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                // Workflow status + Actions
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(status = task.status, compact = true)

                    IconButton(
                        onClick = onLogTimeClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timelapse,
                            contentDescription = "Log Time Motion",
                            tint = HblPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onUpdateStatusClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Update Status",
                            tint = HblPrimaryDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// DIALOGS & BOTTOM SHEETS
// -------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskDialog(
    units: List<CxUnit>,
    members: List<TeamMember>,
    defaultAssignedBy: String = "Sabeen Shafique (CX Unit Head)",
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        description: String,
        unitId: Long,
        assigneeId: Long,
        assignedByName: String,
        priority: TaskPriority,
        category: String,
        tatHours: Double,
        customerAccountOrTicket: String?,
        dueDateTime: Long?
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedUnitId by remember { mutableStateOf(units.firstOrNull()?.id ?: 1L) }
    var selectedAssigneeId by remember { mutableStateOf(members.firstOrNull { it.unitId == selectedUnitId }?.id ?: members.firstOrNull()?.id ?: 1L) }
    var assignedByName by remember { mutableStateOf(defaultAssignedBy) }
    var priority by remember { mutableStateOf(TaskPriority.HIGH) }
    var category by remember { mutableStateOf("Customer Complaint") }
    var tatHours by remember { mutableStateOf(24.0) }
    var customerAccountOrTicket by remember { mutableStateOf("") }

    // Calendar marking state
    var calendarPreset by remember { mutableStateOf("Tomorrow") }
    var markedDueTimestamp by remember { mutableLongStateOf(System.currentTimeMillis() + (24 * 3600000L)) }
    var customDaysOffset by remember { mutableStateOf(1) }

    val categories = listOf(
        "Customer Complaint",
        "Regulatory Escalation (SBP/Banking Mohtasib)",
        "Call QA Calibration Audit",
        "Branch Service Quality Floor Audit",
        "Digital App & Raast Dispute",
        "Micro-Enterprise Loan CX Inquiry",
        "Deposit Customer Retention Request",
        "ATM & Debit Card Dispute"
    )

    val tatOptions = listOf(4.0, 8.0, 12.0, 16.0, 24.0, 48.0, 72.0)

    val dateFormat = remember { SimpleDateFormat("EEE, dd MMM yyyy 'at' hh:mm a", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = HblPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Assign CX Task with Calendar",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = HblPrimary)
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Task Title *") },
                        modifier = Modifier.fillMaxWidth().testTag("task_title_input"),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Task Scope / Description *") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )
                }

                item {
                    OutlinedTextField(
                        value = customerAccountOrTicket,
                        onValueChange = { customerAccountOrTicket = it },
                        label = { Text("Customer AC / Ticket / Ref No") },
                        placeholder = { Text("e.g. AC-0412-8890 or LN-9931") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // 📅 CALENDAR MARKING & DEADLINE SELECTION
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
                                    text = "Calendar Deadline Marking",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF166534))
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))

                            // Quick preset chips
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val presets = listOf(
                                    Triple("Today", 4.0, 4 * 3600000L),
                                    Triple("Tomorrow", 24.0, 24 * 3600000L),
                                    Triple("+2 Days", 48.0, 48 * 3600000L),
                                    Triple("+3 Days", 72.0, 72 * 3600000L),
                                    Triple("+1 Week", 168.0, 168 * 3600000L)
                                )
                                presets.forEach { (label, hours, millis) ->
                                    val isSelected = calendarPreset == label
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            calendarPreset = label
                                            tatHours = hours
                                            markedDueTimestamp = System.currentTimeMillis() + millis
                                        },
                                        label = { Text(label) },
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
                                            text = "📅 Marked Deadline:",
                                            style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                                        )
                                        Text(
                                            text = dateFormat.format(Date(markedDueTimestamp)),
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
                                            text = "${tatHours.toInt()}h TAT",
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

                // Unit Selector
                item {
                    Text("Select CX Unit *", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        units.forEach { u ->
                            FilterChip(
                                selected = selectedUnitId == u.id,
                                onClick = {
                                    selectedUnitId = u.id
                                    val unitMember = members.firstOrNull { it.unitId == u.id }
                                    if (unitMember != null) selectedAssigneeId = unitMember.id
                                },
                                label = { Text(u.code) }
                            )
                        }
                    }
                }

                // Team Member Assignee Selector
                item {
                    val filteredMembers = members.filter { it.unitId == selectedUnitId }
                    Text("Assign To Team Member *", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        (if (filteredMembers.isNotEmpty()) filteredMembers else members).forEach { m ->
                            FilterChip(
                                selected = selectedAssigneeId == m.id,
                                onClick = { selectedAssigneeId = m.id },
                                label = { Text(m.fullName) }
                            )
                        }
                    }
                }

                // TAT Hours Selector
                item {
                    Text("Define Target Turnaround Time (TAT) *", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tatOptions.forEach { hours ->
                            FilterChip(
                                selected = tatHours == hours,
                                onClick = {
                                    tatHours = hours
                                    calendarPreset = "${hours.toInt()}h"
                                    markedDueTimestamp = System.currentTimeMillis() + (hours * 3600000L).toLong()
                                },
                                label = { Text("${hours.toInt()} Hours") }
                            )
                        }
                    }
                }

                // Priority Selector
                item {
                    Text("Priority Level *", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TaskPriority.entries.forEach { p ->
                            FilterChip(
                                selected = priority == p,
                                onClick = { priority = p },
                                label = { Text(p.displayName) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(
                            title,
                            description,
                            selectedUnitId,
                            selectedAssigneeId,
                            assignedByName,
                            priority,
                            category,
                            tatHours,
                            customerAccountOrTicket.ifBlank { null },
                            markedDueTimestamp
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = HblPrimary),
                enabled = title.isNotBlank()
            ) {
                Text("Create & Assign Task")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun UpdateTaskStatusDialog(
    task: CxTask,
    onDismiss: () -> Unit,
    onConfirm: (
        newStatus: TaskStatus,
        remarks: String?,
        pendingReason: String?,
        breachReason: String?
    ) -> Unit
) {
    var selectedStatus by remember { mutableStateOf(task.status) }
    var remarks by remember { mutableStateOf(task.resolutionRemarks ?: "") }
    var pendingReason by remember { mutableStateOf(task.pendingReason ?: "") }
    var breachReason by remember { mutableStateOf(task.breachReason ?: "") }

    val isBreached = task.computeTatStatus() == TatStatus.BREACHED_TAT

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Update Task Status & Workflow",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = HblPrimary)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "${task.trackingNumber}: ${task.title}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )

                Text("Select Workflow Status", style = MaterialTheme.typography.labelMedium)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TaskStatus.entries.forEach { status ->
                        FilterChip(
                            selected = selectedStatus == status,
                            onClick = { selectedStatus = status },
                            label = { Text(status.displayName, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (selectedStatus == TaskStatus.PENDING) {
                    OutlinedTextField(
                        value = pendingReason,
                        onValueChange = { pendingReason = it },
                        label = { Text("Pending Reason / Blocked By *") },
                        placeholder = { Text("e.g. Awaiting Branch credit manager response or switch journal log") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }

                if (selectedStatus == TaskStatus.COMPLETED) {
                    OutlinedTextField(
                        value = remarks,
                        onValueChange = { remarks = it },
                        label = { Text("Resolution Remarks & Outcome *") },
                        placeholder = { Text("e.g. Reconciliation verified, amount reversed, customer notified") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }

                if (isBreached) {
                    OutlinedTextField(
                        value = breachReason,
                        onValueChange = { breachReason = it },
                        label = { Text("TAT Breach Root Cause Justification") },
                        placeholder = { Text("e.g. Branch physical audit delay or customer unreachable") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        selectedStatus,
                        remarks.ifBlank { null },
                        pendingReason.ifBlank { null },
                        breachReason.ifBlank { null }
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = HblPrimary)
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun TimeMotionLoggerDialog(
    task: CxTask,
    isTimerActive: Boolean,
    timerSeconds: Int,
    onStartTimer: () -> Unit,
    onStopTimer: (activityType: String, notes: String) -> Unit,
    onLogManual: (durationMinutes: Int, activityType: String, notes: String) -> Unit,
    onDismiss: () -> Unit
) {
    var activityType by remember { mutableStateOf("Investigation & Verification") }
    var manualMinutes by remember { mutableStateOf("30") }
    var notes by remember { mutableStateOf("") }
    var activeTab by remember { mutableStateOf(if (isTimerActive) 0 else 1) } // 0 = Live Timer, 1 = Manual Log

    val activities = listOf(
        "Investigation & Verification",
        "Customer Outreach / Call",
        "Branch Coordination",
        "QA Scoring & Calibration",
        "System Resolution & Core Entry",
        "Report & Dossier Compilation"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Time & Motion Study Tracker",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = HblPrimary)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "${task.trackingNumber}: ${task.title}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )

                // Tab Switcher (Stopwatch vs Manual)
                Row(modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        label = { Text("Live Stopwatch") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        label = { Text("Manual Entry") },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (activeTab == 0) {
                    // Stopwatch Section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val mins = timerSeconds / 60
                            val secs = timerSeconds % 60
                            Text(
                                text = String.format(Locale.US, "%02d:%02d", mins, secs),
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isTimerActive) StatusInProgress else MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (!isTimerActive) {
                                Button(
                                    onClick = onStartTimer,
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusWithinTat)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Start")
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Start Task Timer")
                                }
                            } else {
                                Button(
                                    onClick = { onStopTimer(activityType, notes) },
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusBreachedTat)
                                ) {
                                    Icon(Icons.Default.Stop, contentDescription = "Stop")
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Stop & Log Session")
                                }
                            }
                        }
                    }
                } else {
                    // Manual Entry Section
                    OutlinedTextField(
                        value = manualMinutes,
                        onValueChange = { manualMinutes = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Duration (Minutes) *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // Activity Category Picker
                Text("Select Activity Type *", style = MaterialTheme.typography.labelMedium)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    activities.take(4).forEach { act ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { activityType = act }
                        ) {
                            androidx.compose.material3.RadioButton(
                                selected = activityType == act,
                                onClick = { activityType = act }
                            )
                            Text(text = act, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Activity Motion Notes") },
                    placeholder = { Text("e.g. Reviewed 3 call logs and contacted Sukkur Branch manager") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            if (activeTab == 1) {
                Button(
                    onClick = {
                        val mins = manualMinutes.toIntOrNull() ?: 15
                        onLogManual(mins, activityType, notes)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HblPrimary)
                ) {
                    Text("Record Time Motion")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun TaskDetailsBottomSheet(
    task: CxTask,
    unit: CxUnit?,
    assignee: TeamMember?,
    canDelete: Boolean = true,
    onDismiss: () -> Unit,
    onUpdateStatus: () -> Unit,
    onLogTime: () -> Unit,
    onDelete: () -> Unit
) {
    val now = System.currentTimeMillis()
    val tatStatus = task.computeTatStatus(now)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = task.trackingNumber,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = HblPrimary)
                    )
                    Text(
                        text = unit?.name ?: "Customer Experience",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
                TatBadge(tatStatus = tatStatus)
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                item {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("TAT SLA Audit Trail", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text("• Assigned At: ${formatDateTime(task.assignedAt)}")
                            Text("• Target TAT: ${task.tatHours.toInt()} Hours")
                            Text("• Due Date: ${formatDateTime(task.dueDateTime)}")
                            if (task.completedAt != null) {
                                Text("• Completed At: ${formatDateTime(task.completedAt)}")
                                Text("• Actual Turnaround: ${String.format(Locale.US, "%.1f", task.getActualTurnaroundHours(now))} Hours")
                            }
                            Text("• Total Time-Motion Logged: ${formatDurationMinutes(task.timeMotionMinutes)}")
                            Text("• Assigned By: ${task.assignedByName}")
                            Text("• Assignee: ${assignee?.fullName ?: "Unassigned"} (${assignee?.employeeId ?: ""})")
                            if (!task.customerAccountOrTicket.isNullOrEmpty()) {
                                Text("• Customer Ref/Account: ${task.customerAccountOrTicket}")
                            }
                            if (!task.pendingReason.isNullOrEmpty()) {
                                Text("• Pending Block Reason: ${task.pendingReason}", color = StatusPending)
                            }
                            if (!task.breachReason.isNullOrEmpty()) {
                                Text("• TAT Breach Cause: ${task.breachReason}", color = StatusBreachedTat)
                            }
                            if (!task.resolutionRemarks.isNullOrEmpty()) {
                                Text("• Resolution Remarks: ${task.resolutionRemarks}", color = StatusWithinTat)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onLogTime) {
                    Text("Log Time")
                }
                Button(
                    onClick = onUpdateStatus,
                    colors = ButtonDefaults.buttonColors(containerColor = HblPrimary)
                ) {
                    Text("Update Status")
                }
            }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (canDelete) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Task", tint = StatusBreachedTat)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}
