package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.CxTask
import com.example.data.entity.CxUnit
import com.example.data.entity.TaskStatus
import com.example.data.entity.TatStatus
import com.example.data.entity.TeamMember
import com.example.data.entity.TimeMotionLog
import com.example.data.model.MemberPerformanceSummary
import com.example.data.model.UnitPerformanceSummary
import com.example.ui.components.HblBrandHeader
import com.example.ui.components.TatBadge
import com.example.ui.components.formatDateTime
import com.example.ui.theme.HblPrimary
import com.example.ui.theme.HblPrimaryDark
import com.example.ui.theme.HblSecondary
import com.example.ui.theme.HblTertiaryGold
import com.example.ui.theme.StatusBreachedTat
import com.example.ui.theme.StatusWithinTat
import com.example.ui.viewmodel.CxViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class DateFilterPreset(val label: String) {
    ALL_TIME("All Time"),
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    LAST_7_DAYS("Last 7 Days"),
    LAST_30_DAYS("Last 30 Days"),
    LAST_90_DAYS("Last 90 Days"),
    CUSTOM("Custom Range 📅")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExcelExportScreen(
    viewModel: CxViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allTasks by viewModel.tasks.collectAsStateWithLifecycle()
    val units by viewModel.units.collectAsStateWithLifecycle()
    val members by viewModel.teamMembers.collectAsStateWithLifecycle()
    val allLogs by viewModel.timeMotionLogs.collectAsStateWithLifecycle()

    val unitMap = remember(units) { units.associateBy { it.id } }
    val memberMap = remember(members) { members.associateBy { it.id } }

    // Date & Dimension Filter States
    var selectedPreset by remember { mutableStateOf(DateFilterPreset.LAST_30_DAYS) }
    val now = remember { System.currentTimeMillis() }

    val calendar = remember { Calendar.getInstance() }
    val simpleDateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val displayDateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    // Custom date bounds (default to past 30 days)
    var customStartDate by remember {
        mutableStateOf(
            Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -30)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        )
    }

    var customEndDate by remember {
        mutableStateOf(
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis
        )
    }

    var selectedUnitId by remember { mutableStateOf<Long?>(null) }
    var selectedStatus by remember { mutableStateOf<TaskStatus?>(null) }
    var selectedTatStatus by remember { mutableStateOf<TatStatus?>(null) }
    var showFilterPanel by remember { mutableStateOf(true) }

    // Compute effective date bounds
    val dateBounds by remember(selectedPreset, customStartDate, customEndDate) {
        derivedStateOf {
            val cal = Calendar.getInstance()
            when (selectedPreset) {
                DateFilterPreset.ALL_TIME -> Pair(null, null)
                DateFilterPreset.TODAY -> {
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    val start = cal.timeInMillis
                    cal.set(Calendar.HOUR_OF_DAY, 23)
                    cal.set(Calendar.MINUTE, 59)
                    cal.set(Calendar.SECOND, 59)
                    cal.set(Calendar.MILLISECOND, 999)
                    val end = cal.timeInMillis
                    Pair(start, end)
                }
                DateFilterPreset.YESTERDAY -> {
                    cal.add(Calendar.DAY_OF_YEAR, -1)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    val start = cal.timeInMillis
                    cal.set(Calendar.HOUR_OF_DAY, 23)
                    cal.set(Calendar.MINUTE, 59)
                    cal.set(Calendar.SECOND, 59)
                    cal.set(Calendar.MILLISECOND, 999)
                    val end = cal.timeInMillis
                    Pair(start, end)
                }
                DateFilterPreset.LAST_7_DAYS -> {
                    cal.add(Calendar.DAY_OF_YEAR, -7)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    val start = cal.timeInMillis
                    Pair(start, System.currentTimeMillis())
                }
                DateFilterPreset.LAST_30_DAYS -> {
                    cal.add(Calendar.DAY_OF_YEAR, -30)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    val start = cal.timeInMillis
                    Pair(start, System.currentTimeMillis())
                }
                DateFilterPreset.LAST_90_DAYS -> {
                    cal.add(Calendar.DAY_OF_YEAR, -90)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    val start = cal.timeInMillis
                    Pair(start, System.currentTimeMillis())
                }
                DateFilterPreset.CUSTOM -> Pair(customStartDate, customEndDate)
            }
        }
    }

    // Filter tasks based on selected Date Range and dimensions
    val filteredTasks by remember(allTasks, dateBounds, selectedUnitId, selectedStatus, selectedTatStatus) {
        derivedStateOf {
            val (startMs, endMs) = dateBounds
            allTasks.filter { task ->
                // Date filter matches assignedAt, createdAt or completedAt
                val inDateRange = when {
                    startMs == null && endMs == null -> true
                    startMs != null && endMs != null -> {
                        (task.assignedAt in startMs..endMs) ||
                                (task.completedAt != null && task.completedAt in startMs..endMs) ||
                                (task.createdAt in startMs..endMs)
                    }
                    startMs != null -> task.assignedAt >= startMs || (task.completedAt ?: 0L) >= startMs
                    endMs != null -> task.assignedAt <= endMs
                    else -> true
                }

                val matchesUnit = selectedUnitId == null || task.unitId == selectedUnitId
                val matchesStatus = selectedStatus == null || task.status == selectedStatus
                val matchesTat = selectedTatStatus == null || task.computeTatStatus(System.currentTimeMillis()) == selectedTatStatus

                inDateRange && matchesUnit && matchesStatus && matchesTat
            }
        }
    }

    // Filter time motion logs
    val filteredLogs by remember(allLogs, dateBounds, filteredTasks) {
        derivedStateOf {
            val (startMs, endMs) = dateBounds
            val taskIds = filteredTasks.map { it.id }.toSet()
            allLogs.filter { log ->
                val inDateRange = when {
                    startMs == null && endMs == null -> true
                    startMs != null && endMs != null -> log.loggedAt in startMs..endMs
                    startMs != null -> log.loggedAt >= startMs
                    endMs != null -> log.loggedAt <= endMs
                    else -> true
                }
                inDateRange && (taskIds.isEmpty() || taskIds.contains(log.taskId))
            }
        }
    }

    // Recomputed Performance Analytics for Filtered Dataset
    val filteredAnalytics by remember(filteredTasks, units, members, filteredLogs) {
        derivedStateOf {
            viewModel.computeAnalytics(filteredTasks, units, members, filteredLogs)
        }
    }

    val periodLabelString = remember(selectedPreset, dateBounds) {
        val (start, end) = dateBounds
        if (start != null && end != null) {
            "${displayDateFormat.format(Date(start))} to ${displayDateFormat.format(Date(end))} (${selectedPreset.label})"
        } else {
            selectedPreset.label
        }
    }

    val dateFilterSummaryString = remember(selectedPreset, dateBounds) {
        val (start, end) = dateBounds
        if (start != null && end != null) {
            "${simpleDateFormat.format(Date(start))} ~ ${simpleDateFormat.format(Date(end))}"
        } else {
            "All Historical Records"
        }
    }

    val unitFilterLabel = remember(selectedUnitId, units) {
        if (selectedUnitId == null) "All Units" else units.find { it.id == selectedUnitId }?.name ?: "Selected Unit"
    }

    val sanitizedFilePrefix = remember(dateFilterSummaryString) {
        val cleanDate = dateFilterSummaryString.replace(" ", "").replace("~", "_to_").replace(":", "-")
        "HBL_CX_Performance_Report_$cleanDate"
    }

    var selectedTableTab by remember { mutableStateOf(0) } // 0 = Master Tasks, 1 = Unit Performance, 2 = Team Scorecards, 3 = Time Logs
    val scrollState = rememberScrollState()

    // Helper Date Picker Trigger
    fun showDatePicker(isStartDate: Boolean) {
        val initTime = if (isStartDate) customStartDate else customEndDate
        val cal = Calendar.getInstance().apply { timeInMillis = initTime }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val pickedCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    if (isStartDate) {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                        customStartDate = timeInMillis
                    } else {
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                        customEndDate = timeInMillis
                    }
                }
                selectedPreset = DateFilterPreset.CUSTOM
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("excel_export_screen")
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        HblBrandHeader(
            title = "HBL Microfinance Bank",
            subtitle = "Excel & CSV Report Generator • Date-Filtered Performance Export"
        )

        Spacer(modifier = Modifier.height(10.dp))

        // -------------------------------------------------------------
        // DATE & PERFORMANCE FILTER CONTROLS CARD
        // -------------------------------------------------------------
        Card(
            modifier = Modifier.fillMaxWidth().testTag("card_date_filter_controls"),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = "Date Filter",
                            tint = HblPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Performance Report Date Filter",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = HblPrimary)
                        )
                    }

                    IconButton(
                        onClick = { showFilterPanel = !showFilterPanel },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Toggle Filters",
                            tint = HblPrimary
                        )
                    }
                }

                // Date Presets FlowRow
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DateFilterPreset.values().forEach { preset ->
                        FilterChip(
                            selected = selectedPreset == preset,
                            onClick = { selectedPreset = preset },
                            label = { Text(preset.label, fontSize = 11.sp, fontWeight = if (selectedPreset == preset) FontWeight.Bold else FontWeight.Normal) },
                            leadingIcon = if (selectedPreset == preset) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HblPrimary.copy(alpha = 0.15f),
                                selectedLabelColor = HblPrimaryDark
                            )
                        )
                    }
                }

                // Custom Date Range Selector (When Custom Range is active or expanded)
                AnimatedVisibility(visible = selectedPreset == DateFilterPreset.CUSTOM || showFilterPanel) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showDatePicker(isStartDate = true) }
                                    .testTag("btn_custom_start_date"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = HblPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text("From (Start Date)", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(displayDateFormat.format(Date(customStartDate)), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            OutlinedCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showDatePicker(isStartDate = false) }
                                    .testTag("btn_custom_end_date"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = HblPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text("To (End Date)", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(displayDateFormat.format(Date(customEndDate)), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Unit and Status Filters Row
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = selectedUnitId == null,
                                onClick = { selectedUnitId = null },
                                label = { Text("All Units", fontSize = 11.sp) }
                            )
                            units.forEach { unit ->
                                FilterChip(
                                    selected = selectedUnitId == unit.id,
                                    onClick = { selectedUnitId = if (selectedUnitId == unit.id) null else unit.id },
                                    label = { Text(unit.code, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }

                // Active Filter Summary Line
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🗓️ Filter: $periodLabelString • $unitFilterLabel",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = HblPrimaryDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = "${filteredTasks.size} Tasks Matched",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (filteredTasks.isNotEmpty()) StatusWithinTat else StatusBreachedTat
                        )
                    }
                }

                // -------------------------------------------------------------
                // ACTION BUTTONS: DOWNLOAD EXCEL (.CSV) & COPY TO CLIPBOARD
                // -------------------------------------------------------------
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            viewModel.exportExcelReport(
                                context = context,
                                filteredTasksList = filteredTasks,
                                filteredAnalyticsData = filteredAnalytics,
                                filteredLogsList = filteredLogs,
                                periodLabel = periodLabelString,
                                dateFilterSummary = dateFilterSummaryString,
                                unitFilterLabel = unitFilterLabel,
                                fileNamePrefix = sanitizedFilePrefix
                            )
                        },
                        modifier = Modifier.weight(1.3f).testTag("download_excel_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = HblPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Download Excel")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Download Excel (${filteredTasks.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.copyExcelTableToClipboard(
                                context = context,
                                filteredTasksList = filteredTasks,
                                filteredAnalyticsData = filteredAnalytics,
                                filteredLogsList = filteredLogs,
                                periodLabel = periodLabelString,
                                dateFilterSummary = dateFilterSummaryString,
                                unitFilterLabel = unitFilterLabel
                            )
                        },
                        modifier = Modifier.weight(1f).testTag("copy_excel_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Table", fontSize = 13.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // -------------------------------------------------------------
        // DYNAMIC METRICS FOR SELECTED DATE WINDOW
        // -------------------------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryMetricCard(
                title = "Total Tasks",
                value = "${filteredAnalytics.totalTasks}",
                subtitle = "${filteredAnalytics.completedTasks} Done",
                modifier = Modifier.weight(1f)
            )
            SummaryMetricCard(
                title = "SLA Compliance",
                value = String.format(Locale.US, "%.1f%%", filteredAnalytics.overallSlaPercent),
                subtitle = "${filteredAnalytics.withinTatCount} on-time",
                valueColor = if (filteredAnalytics.overallSlaPercent >= 90.0) StatusWithinTat else StatusBreachedTat,
                modifier = Modifier.weight(1.2f)
            )
            SummaryMetricCard(
                title = "Breach Rate",
                value = String.format(Locale.US, "%.1f%%", filteredAnalytics.breachRatePercent),
                subtitle = "${filteredAnalytics.breachedTatCount} breached",
                valueColor = if (filteredAnalytics.breachedTatCount > 0) StatusBreachedTat else StatusWithinTat,
                modifier = Modifier.weight(1.1f)
            )
            SummaryMetricCard(
                title = "Motion Time",
                value = String.format(Locale.US, "%.1fh", filteredAnalytics.totalTimeMotionMinutes / 60.0),
                subtitle = "Logged",
                valueColor = HblPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // -------------------------------------------------------------
        // TABLE SELECTION TABS
        // -------------------------------------------------------------
        ScrollableTabRow(
            selectedTabIndex = selectedTableTab,
            edgePadding = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTableTab == 0,
                onClick = { selectedTableTab = 0 },
                text = { Text("Master Tasks (${filteredTasks.size})", fontWeight = if (selectedTableTab == 0) FontWeight.Bold else FontWeight.Normal) }
            )
            Tab(
                selected = selectedTableTab == 1,
                onClick = { selectedTableTab = 1 },
                text = { Text("Unit Analysis (${filteredAnalytics.unitSummaries.size})", fontWeight = if (selectedTableTab == 1) FontWeight.Bold else FontWeight.Normal) }
            )
            Tab(
                selected = selectedTableTab == 2,
                onClick = { selectedTableTab = 2 },
                text = { Text("Team Scorecards (${filteredAnalytics.memberSummaries.size})", fontWeight = if (selectedTableTab == 2) FontWeight.Bold else FontWeight.Normal) }
            )
            Tab(
                selected = selectedTableTab == 3,
                onClick = { selectedTableTab = 3 },
                text = { Text("Time-Motion (${filteredLogs.size})", fontWeight = if (selectedTableTab == 3) FontWeight.Bold else FontWeight.Normal) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // -------------------------------------------------------------
        // SPREADSHEET TABLE PREVIEW CONTAINER
        // -------------------------------------------------------------
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            when (selectedTableTab) {
                0 -> MasterTasksTable(
                    tasks = filteredTasks,
                    unitMap = unitMap,
                    memberMap = memberMap,
                    horizontalScrollState = scrollState
                )
                1 -> UnitPerformanceTable(
                    summaries = filteredAnalytics.unitSummaries,
                    horizontalScrollState = scrollState
                )
                2 -> TeamScorecardTable(
                    summaries = filteredAnalytics.memberSummaries,
                    horizontalScrollState = scrollState
                )
                3 -> TimeMotionAuditTable(
                    logs = filteredLogs,
                    horizontalScrollState = scrollState
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
    }
}

// ---------------------------------------------------------------------
// SUMMARY METRIC MINI-CARD
// ---------------------------------------------------------------------
@Composable
private fun SummaryMetricCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
            Text(
                text = subtitle,
                fontSize = 8.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

// ---------------------------------------------------------------------
// SPREADSHEET TABLE COMPONENTS
// ---------------------------------------------------------------------

@Composable
fun MasterTasksTable(
    tasks: List<CxTask>,
    unitMap: Map<Long, CxUnit>,
    memberMap: Map<Long, TeamMember>,
    horizontalScrollState: androidx.compose.foundation.ScrollState
) {
    val now = System.currentTimeMillis()

    if (tasks.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No tasks found for the selected date range and filter criteria.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(horizontalScrollState)
    ) {
        // Sticky Header Row
        Row(
            modifier = Modifier
                .background(HblPrimaryDark)
                .padding(vertical = 10.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TableHeaderCell("Task No", 110.dp)
            TableHeaderCell("Title", 200.dp)
            TableHeaderCell("Unit", 70.dp)
            TableHeaderCell("Assignee", 140.dp)
            TableHeaderCell("Priority", 80.dp)
            TableHeaderCell("Status", 100.dp)
            TableHeaderCell("Target TAT", 85.dp)
            TableHeaderCell("Actual TAT", 85.dp)
            TableHeaderCell("TAT SLA State", 120.dp)
            TableHeaderCell("Motion Mins", 90.dp)
            TableHeaderCell("Assigned Date", 140.dp)
            TableHeaderCell("Due Date", 140.dp)
        }

        // Data Rows
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(tasks, key = { it.id }) { task ->
                val unit = unitMap[task.unitId]?.code ?: "CX"
                val assignee = memberMap[task.assigneeId]?.fullName ?: "Unassigned"
                val tatStatus = task.computeTatStatus(now)
                val actualTat = task.getActualTurnaroundHours(now)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        .background(if (task.id % 2L == 0L) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(vertical = 8.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TableCell(task.trackingNumber, 110.dp, isBold = true, color = HblPrimary)
                    TableCell(task.title, 200.dp)
                    TableCell(unit, 70.dp, isBold = true)
                    TableCell(assignee, 140.dp)
                    TableCell(task.priority.displayName, 80.dp)
                    TableCell(task.status.displayName, 100.dp)
                    TableCell("${task.tatHours.toInt()}h", 85.dp)
                    TableCell("${String.format(Locale.US, "%.1f", actualTat)}h", 85.dp)
                    Box(modifier = Modifier.width(120.dp), contentAlignment = Alignment.CenterStart) {
                        TatBadge(tatStatus = tatStatus, compact = true)
                    }
                    TableCell("${task.timeMotionMinutes}m", 90.dp, isBold = true, color = HblPrimary)
                    TableCell(formatDateTime(task.assignedAt), 140.dp)
                    TableCell(formatDateTime(task.dueDateTime), 140.dp)
                }
            }
        }
    }
}

@Composable
fun UnitPerformanceTable(
    summaries: List<UnitPerformanceSummary>,
    horizontalScrollState: androidx.compose.foundation.ScrollState
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(horizontalScrollState)
    ) {
        Row(
            modifier = Modifier
                .background(HblPrimaryDark)
                .padding(vertical = 10.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TableHeaderCell("Code", 70.dp)
            TableHeaderCell("Unit Name", 220.dp)
            TableHeaderCell("Unit Head", 140.dp)
            TableHeaderCell("Tasks in Range", 100.dp)
            TableHeaderCell("Done", 70.dp)
            TableHeaderCell("Within TAT", 90.dp)
            TableHeaderCell("Breached", 80.dp)
            TableHeaderCell("Actual SLA %", 100.dp)
            TableHeaderCell("Target SLA %", 100.dp)
            TableHeaderCell("Avg Res (h)", 90.dp)
            TableHeaderCell("Motion (h)", 90.dp)
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(summaries, key = { it.unit.id }) { u ->
                val isSlaMet = u.slaPercent >= u.targetSlaPercent

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        .background(if (u.unit.id % 2L == 0L) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(vertical = 8.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TableCell(u.unit.code, 70.dp, isBold = true, color = HblPrimary)
                    TableCell(u.unit.name, 220.dp)
                    TableCell(u.unit.unitHeadName, 140.dp)
                    TableCell("${u.totalTasks}", 100.dp, isBold = true)
                    TableCell("${u.completedCount}", 70.dp)
                    TableCell("${u.withinTatCount}", 90.dp, color = StatusWithinTat, isBold = true)
                    TableCell("${u.breachedTatCount}", 80.dp, color = if (u.breachedTatCount > 0) StatusBreachedTat else StatusWithinTat)
                    TableCell("${String.format(Locale.US, "%.1f%%", u.slaPercent)}", 100.dp, isBold = true, color = if (isSlaMet) StatusWithinTat else StatusBreachedTat)
                    TableCell("${u.targetSlaPercent.toInt()}%", 100.dp)
                    TableCell("${String.format(Locale.US, "%.1f", u.avgResolutionHours)}", 90.dp)
                    TableCell("${String.format(Locale.US, "%.1f", u.totalTimeMotionHours)}", 90.dp, color = HblPrimary, isBold = true)
                }
            }
        }
    }
}

@Composable
fun TeamScorecardTable(
    summaries: List<MemberPerformanceSummary>,
    horizontalScrollState: androidx.compose.foundation.ScrollState
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(horizontalScrollState)
    ) {
        Row(
            modifier = Modifier
                .background(HblPrimaryDark)
                .padding(vertical = 10.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TableHeaderCell("Emp ID", 90.dp)
            TableHeaderCell("Full Name", 150.dp)
            TableHeaderCell("Unit", 70.dp)
            TableHeaderCell("Role", 150.dp)
            TableHeaderCell("Assigned", 80.dp)
            TableHeaderCell("Done", 70.dp)
            TableHeaderCell("Within TAT", 90.dp)
            TableHeaderCell("Breaches", 80.dp)
            TableHeaderCell("SLA %", 85.dp)
            TableHeaderCell("Motion (h)", 90.dp)
            TableHeaderCell("Score (0-100)", 95.dp)
            TableHeaderCell("Rating Tier", 160.dp)
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(summaries, key = { it.member.id }) { m ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        .background(if (m.member.id % 2L == 0L) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(vertical = 8.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TableCell(m.member.employeeId, 90.dp, isBold = true)
                    TableCell(m.member.fullName, 150.dp)
                    TableCell(m.unitCode, 70.dp, isBold = true, color = HblPrimary)
                    TableCell(m.member.role, 150.dp)
                    TableCell("${m.assignedCount}", 80.dp, isBold = true)
                    TableCell("${m.completedCount}", 70.dp)
                    TableCell("${m.withinTatCount}", 90.dp, color = StatusWithinTat, isBold = true)
                    TableCell("${m.breachedCount}", 80.dp, color = if (m.breachedCount > 0) StatusBreachedTat else StatusWithinTat)
                    TableCell("${String.format(Locale.US, "%.0f%%", m.slaPercent)}", 85.dp, isBold = true, color = if (m.slaPercent >= 90.0) StatusWithinTat else StatusBreachedTat)
                    TableCell("${String.format(Locale.US, "%.1f", m.totalTimeMotionHours)}", 90.dp, color = HblPrimary)
                    TableCell("${String.format(Locale.US, "%.0f", m.productivityScore)}", 95.dp, isBold = true, color = HblTertiaryGold)
                    TableCell(m.tierRating, 160.dp, isBold = true)
                }
            }
        }
    }
}

@Composable
fun TimeMotionAuditTable(
    logs: List<TimeMotionLog>,
    horizontalScrollState: androidx.compose.foundation.ScrollState
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    if (logs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No time-motion logs found for the selected date range.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(horizontalScrollState)
    ) {
        Row(
            modifier = Modifier
                .background(HblPrimaryDark)
                .padding(vertical = 10.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TableHeaderCell("Log ID", 70.dp)
            TableHeaderCell("Task ID", 70.dp)
            TableHeaderCell("Activity Type", 160.dp)
            TableHeaderCell("Duration", 90.dp)
            TableHeaderCell("Logged At", 140.dp)
            TableHeaderCell("Notes / Activity Description", 260.dp)
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(logs, key = { it.id }) { log ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        .background(if (log.id % 2L == 0L) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(vertical = 8.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TableCell("#${log.id}", 70.dp)
                    TableCell("Task #${log.taskId}", 70.dp, isBold = true, color = HblPrimary)
                    TableCell(log.activityType, 160.dp, isBold = true)
                    TableCell("${log.durationMinutes} mins", 90.dp, color = HblPrimaryDark, isBold = true)
                    TableCell(dateFormat.format(Date(log.loggedAt)), 140.dp)
                    TableCell(log.notes.ifEmpty { "-" }, 260.dp)
                }
            }
        }
    }
}

@Composable
fun TableHeaderCell(text: String, width: androidx.compose.ui.unit.Dp) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color.White),
        modifier = Modifier.width(width).padding(horizontal = 4.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun TableCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    isBold: Boolean = false,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(
            fontSize = 11.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = color
        ),
        modifier = Modifier.width(width).padding(horizontal = 4.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}
