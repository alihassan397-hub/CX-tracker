package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.CorporateFare
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.entity.CxTask
import com.example.data.entity.CxUnit
import com.example.data.entity.TaskStatus
import com.example.data.entity.TatStatus
import com.example.data.entity.TeamMember
import com.example.data.entity.UserRole
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
import java.util.Locale

@Composable
fun SideMainMenuDrawer(
    viewModel: CxViewModel,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    onCloseDrawer: () -> Unit,
    onOpenAlertCenter: () -> Unit,
    onOpenCreateTask: () -> Unit,
    onOpenAddUnit: () -> Unit,
    onOpenAddMember: (Long?) -> Unit,
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val units by viewModel.units.collectAsStateWithLifecycle()
    val teamMembers by viewModel.teamMembers.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val dailyTasks by viewModel.userDailyTasks.collectAsStateWithLifecycle()
    val analytics by viewModel.analytics.collectAsStateWithLifecycle()
    val unitFilter by viewModel.unitFilter.collectAsStateWithLifecycle()
    val assigneeFilter by viewModel.assigneeFilter.collectAsStateWithLifecycle()
    val totalAlertsCount by viewModel.totalAlertsCount.collectAsStateWithLifecycle()

    var showClearDataConfirmDialog by remember { mutableStateOf(false) }
    val expandedUnits = remember { mutableStateMapOf<Long, Boolean>() }

    ModalDrawerSheet(
        modifier = modifier
            .width(340.dp)
            .fillMaxHeight()
            .testTag("side_main_menu_drawer"),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
        ) {
            // -------------------------------------------------------------
            // 1. BRAND & USER PROFILE HEADER
            // -------------------------------------------------------------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(HblPrimaryDark, HblPrimary)
                        )
                    )
                    .padding(18.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.18f))
                                .border(1.2.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_cx_experience_logo),
                                contentDescription = "CX Tracker Logo",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "CX Tracker",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = HblLime,
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "PRO",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = HblOnLime,
                                            fontSize = 8.5.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Customer Experience Operations",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 10.5.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Logged in User Card with Role & Access Rights
                    currentUser?.let { user ->
                        Surface(
                            color = Color.Black.copy(alpha = 0.28f),
                            shape = RoundedCornerShape(12.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = Brush.linearGradient(listOf(HblLime.copy(alpha = 0.4f), Color.White.copy(alpha = 0.2f)))
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onTabSelected(7)
                                    onCloseDrawer()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(if (user.isSuperAdmin) HblLime else HblSecondary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (user.isSuperAdmin) "👑" else user.fullName.firstOrNull()?.toString() ?: "U",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.Black
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = user.fullName,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = when (user.role) {
                                            UserRole.ADMIN.name -> "👑 Super Admin (Full Control)"
                                            UserRole.UNIT_HEAD.name -> "🏢 Unit Head (Assign & Delete Rights)"
                                            else -> "👤 Officer (Daily Work Logger)"
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = HblLime,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }

                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = "Switch",
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Real-Time Stats Ribbon
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.20f))
                            .padding(vertical = 6.dp, horizontal = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MiniStatItem("Units", "${units.size}")
                        MiniStatItem("Staff", "${teamMembers.size}")
                        MiniStatItem("Open", "${analytics.inProgressTasks + analytics.pendingTasks + analytics.toDoTasks}")
                        MiniStatItem(
                            "SLA %",
                            "${String.format(Locale.US, "%.0f%%", analytics.overallSlaPercent)}",
                            if (analytics.overallSlaPercent >= 90.0) HblLime else Color(0xFFFF8A80)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // -------------------------------------------------------------
            // 2. CORE APP MODULES NAVIGATION
            // -------------------------------------------------------------
            DrawerSectionHeader("CORE PLATFORM MODULES")

            DrawerNavItem(
                title = "Dashboard & Real-time KPI",
                subtitle = "Department SLA breakdown & metrics",
                icon = Icons.Default.Dashboard,
                isSelected = selectedTabIndex == 0,
                onClick = {
                    onTabSelected(0)
                    onCloseDrawer()
                }
            )

            DrawerNavItem(
                title = "Tasks & TAT SLA Monitor",
                subtitle = "All task assignments & time-motion",
                icon = Icons.Default.Assignment,
                isSelected = selectedTabIndex == 1,
                badgeCount = analytics.inProgressTasks + analytics.pendingTasks + analytics.toDoTasks,
                onClick = {
                    onTabSelected(1)
                    onCloseDrawer()
                }
            )

            DrawerNavItem(
                title = "Daily Tasks & Auto-KPIs",
                subtitle = "Add daily activities & live scorecard",
                icon = Icons.Default.Speed,
                isSelected = selectedTabIndex == 2,
                badgeText = "${dailyTasks.size} Today",
                onClick = {
                    onTabSelected(2)
                    onCloseDrawer()
                }
            )

            DrawerNavItem(
                title = "Gemini AI Intelligence",
                subtitle = "Automated appraisal & CX coaching",
                icon = Icons.Default.AutoAwesome,
                isSelected = selectedTabIndex == 3,
                badgeText = "AI 2.5",
                badgeColor = HblLime,
                onClick = {
                    onTabSelected(3)
                    onCloseDrawer()
                }
            )

            DrawerNavItem(
                title = "Member & Intern Management",
                subtitle = "Unit Head controls & unit mapping",
                icon = Icons.Default.Groups,
                isSelected = selectedTabIndex == 4,
                badgeText = "Unit Head",
                badgeColor = HblLime,
                onClick = {
                    onTabSelected(4)
                    onCloseDrawer()
                }
            )

            DrawerNavItem(
                title = "Executive SLA Brief",
                subtitle = "Root cause & time-motion audit",
                icon = Icons.Default.Assessment,
                isSelected = selectedTabIndex == 5,
                onClick = {
                    onTabSelected(5)
                    onCloseDrawer()
                }
            )

            DrawerNavItem(
                title = "Excel & CSV Export",
                subtitle = "Generate master reports & share",
                icon = Icons.Default.TableChart,
                isSelected = selectedTabIndex == 6,
                onClick = {
                    onTabSelected(6)
                    onCloseDrawer()
                }
            )

            DrawerNavItem(
                title = "User Login / Switch Role",
                subtitle = "Email username, password & roles",
                icon = Icons.Default.AccountCircle,
                isSelected = selectedTabIndex == 7,
                onClick = {
                    onTabSelected(7)
                    onCloseDrawer()
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // -------------------------------------------------------------
            // 3. UNITS & RELEVANT TEAM MEMBERS SECTION
            // -------------------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "UNITS & TEAM MEMBERS (${units.size})",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                )

                if (viewModel.canUserAssignTask()) {
                    TextButton(
                        onClick = {
                            onOpenAddUnit()
                            onCloseDrawer()
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Unit", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Unit", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }

            units.forEach { unit ->
                val unitMembers = teamMembers.filter { it.unitId == unit.id }
                val unitTasks = tasks.filter { it.unitId == unit.id }
                val isExpanded = expandedUnits[unit.id] ?: false
                val isSelectedUnit = unitFilter == unit.id

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                        .border(
                            width = if (isSelectedUnit) 1.5.dp else 1.dp,
                            color = if (isSelectedUnit) HblPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelectedUnit) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isSelectedUnit) {
                                        viewModel.setUnitFilter(null)
                                        viewModel.setAssigneeFilter(null)
                                    } else {
                                        viewModel.setUnitFilter(unit.id)
                                        viewModel.setAssigneeFilter(null)
                                        onTabSelected(1)
                                        onCloseDrawer()
                                    }
                                }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        Color(android.graphics.Color.parseColor(unit.colorHex.ifEmpty { "#008269" })).copy(alpha = 0.18f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = unit.code.take(3),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(android.graphics.Color.parseColor(unit.colorHex.ifEmpty { "#008269" }))
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = unit.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Head: ${unit.unitHeadName} • ${unitTasks.size} tasks",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                )
                            }

                            IconButton(
                                onClick = { expandedUnits[unit.id] = !isExpanded },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                if (unitMembers.isEmpty()) {
                                    Text(
                                        text = "No staff in this unit yet.",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = Color.Gray)
                                    )
                                } else {
                                    unitMembers.forEach { member ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    viewModel.setUnitFilter(unit.id)
                                                    viewModel.setAssigneeFilter(member.id)
                                                    onTabSelected(1)
                                                    onCloseDrawer()
                                                }
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = HblPrimary)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(member.fullName, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
                                            Spacer(modifier = Modifier.weight(1f))
                                            Text(member.role, style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray, fontSize = 9.sp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // -------------------------------------------------------------
            // 4. QUICK ACTIONS
            // -------------------------------------------------------------
            DrawerSectionHeader("QUICK ACTIONS")

            if (viewModel.canUserAssignTask()) {
                DrawerActionItem(
                    title = "Assign New Task with TAT",
                    icon = Icons.Default.Add,
                    iconColor = HblPrimary,
                    onClick = {
                        onOpenCreateTask()
                        onCloseDrawer()
                    }
                )
            }

            DrawerActionItem(
                title = "Log Daily Activity / KPIs",
                icon = Icons.Default.Speed,
                iconColor = HblSecondary,
                onClick = {
                    onTabSelected(2)
                    onCloseDrawer()
                }
            )

            DrawerActionItem(
                title = "TAT SLA Alert Center",
                icon = Icons.Default.NotificationsActive,
                iconColor = if (totalAlertsCount > 0) StatusBreachedTat else HblPrimary,
                badgeText = if (totalAlertsCount > 0) "$totalAlertsCount Active" else null,
                onClick = {
                    onOpenAlertCenter()
                    onCloseDrawer()
                }
            )

            DrawerActionItem(
                title = "Sign Out / Switch User",
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                iconColor = Color(0xFFEF4444),
                onClick = {
                    onLogout()
                    onCloseDrawer()
                }
            )

            if (currentUser?.isSuperAdmin == true) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                DrawerSectionHeader("ADMIN CONTROLS (ALI HASSAN)")

                DrawerActionItem(
                    title = "Clear All Tasks / Test Data",
                    icon = Icons.Default.DeleteSweep,
                    iconColor = Color(0xFFDC2626),
                    onClick = {
                        showClearDataConfirmDialog = true
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Confirmation Dialog for Clearing Data
    if (showClearDataConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = "Warning", tint = Color(0xFFDC2626))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear All Test Tasks?")
                }
            },
            text = {
                Text("This will remove all task records and time motion logs. Units and user accounts will be preserved.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllTasksAndTestData()
                        showClearDataConfirmDialog = false
                        onCloseDrawer()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Clear Tasks")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DrawerSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
        ),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
    )
}

@Composable
fun DrawerNavItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    badgeCount: Int? = null,
    badgeText: String? = null,
    badgeColor: Color? = null,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else Color.Transparent,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 2.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) HblPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) HblPrimary else MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            if (badgeText != null) {
                Surface(
                    color = badgeColor ?: HblPrimary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (badgeColor != null) Color.Black else HblPrimary,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            } else if (badgeCount != null && badgeCount > 0) {
                Surface(
                    color = HblPrimary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "$badgeCount",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = HblPrimary,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DrawerActionItem(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    badgeText: String? = null,
    onClick: () -> Unit
) {
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 2.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.weight(1f)
            )

            if (badgeText != null) {
                Surface(
                    color = iconColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = iconColor,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MiniStatItem(label: String, value: String, valueColor: Color = Color.White) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = valueColor,
                fontSize = 13.sp
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 9.5.sp
            )
        )
    }
}
