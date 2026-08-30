package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CorporateFare
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.entity.UserRole
import com.example.ui.components.SideMainMenuDrawer
import com.example.ui.components.TatAlertCenterSheet
import com.example.ui.theme.HblLime
import com.example.ui.theme.HblOnLime
import com.example.ui.theme.HblPrimary
import com.example.ui.theme.HblPrimaryDark
import com.example.ui.theme.HblSecondary
import com.example.ui.theme.HblTertiaryGold
import com.example.ui.viewmodel.CxViewModel
import com.example.util.TatNotificationManager
import kotlinx.coroutines.launch

sealed class ScreenTab(val title: String, val icon: ImageVector, val tag: String) {
    object Dashboard : ScreenTab("Dashboard", Icons.Default.Dashboard, "tab_dashboard")
    object Tasks : ScreenTab("Tasks & TAT", Icons.Default.Assignment, "tab_tasks")
    object DailyTasks : ScreenTab("Daily Tasks", Icons.Default.Speed, "tab_daily_tasks")
    object AiAdvisor : ScreenTab("AI Intelligence", Icons.Default.AutoAwesome, "tab_ai_advisor")
    object UnitsAndTeam : ScreenTab("Member Mgmt", Icons.Default.Groups, "tab_units")
    object ExecutiveSummary : ScreenTab("Briefing", Icons.Default.Assessment, "tab_executive")
    object ExcelExport : ScreenTab("Excel", Icons.Default.TableChart, "tab_excel")
    object AuthPortal : ScreenTab("Sign In / Profile", Icons.Default.AccountCircle, "tab_auth")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    viewModel: CxViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val units by viewModel.units.collectAsStateWithLifecycle()
    val teamMembers by viewModel.teamMembers.collectAsStateWithLifecycle()

    val primaryBottomTabs = listOf(
        ScreenTab.Dashboard,
        ScreenTab.Tasks,
        ScreenTab.DailyTasks,
        ScreenTab.AiAdvisor,
        ScreenTab.UnitsAndTeam
    )

    var isUserLoggedIn by rememberSaveable { mutableStateOf(false) }
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var showMenu by remember { mutableStateOf(false) }
    var showAlertCenter by remember { mutableStateOf(false) }

    // Dialog state controllers accessible from side menu
    var showCreateTaskModal by remember { mutableStateOf(false) }
    var showAddUnitModal by remember { mutableStateOf(false) }
    var showAddMemberModal by remember { mutableStateOf(false) }
    var addMemberPreselectedUnitId by remember { mutableStateOf<Long?>(null) }

    val totalAlertsCount by viewModel.totalAlertsCount.collectAsState()
    val breachedTasks by viewModel.activeBreachedTasks.collectAsState()

    // Android 13+ Notification Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.checkAndTriggerTatNotifications()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!TatNotificationManager.hasNotificationPermission(context)) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // When not logged in, show Auth / Login Screen first
    if (!isUserLoggedIn) {
        AuthScreen(
            viewModel = viewModel,
            onAuthSuccess = {
                isUserLoggedIn = true
                selectedTabIndex = 0
            },
            modifier = modifier
        )
        return
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            SideMainMenuDrawer(
                viewModel = viewModel,
                selectedTabIndex = selectedTabIndex,
                onTabSelected = { newIndex ->
                    selectedTabIndex = newIndex
                },
                onCloseDrawer = {
                    coroutineScope.launch { drawerState.close() }
                },
                onOpenAlertCenter = {
                    showAlertCenter = true
                },
                onOpenCreateTask = {
                    showCreateTaskModal = true
                },
                onOpenAddUnit = {
                    showAddUnitModal = true
                },
                onOpenAddMember = { preselectedUnitId ->
                    addMemberPreselectedUnitId = preselectedUnitId
                    showAddMemberModal = true
                },
                onLogout = {
                    viewModel.logout()
                    isUserLoggedIn = false
                }
            )
        }
    ) {
        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val isExpandedScreen = maxWidth >= 840.dp

            Scaffold(
                topBar = {
                    TopAppBar(
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                    }
                                },
                                modifier = Modifier.testTag("btn_side_menu_toggle")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Side Main Menu",
                                    tint = Color.White
                                )
                            }
                        },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Unique CX Emblem
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.18f))
                                        .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_cx_experience_logo),
                                        contentDescription = "CX Tracker Emblem",
                                        tint = Color.Unspecified,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "CX Tracker",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(HblLime)
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = "PRO",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = HblOnLime,
                                                    fontSize = 8.5.sp
                                                )
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Customer Experience & SLA Monitor",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.White.copy(alpha = 0.85f),
                                            fontSize = 9.5.sp
                                        )
                                    )
                                }
                            }
                        },
                        actions = {
                            // User Role Avatar Chip in Top Bar (Click to switch or view profile)
                            currentUser?.let { user ->
                                Surface(
                                    color = Color.White.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier
                                        .padding(end = 4.dp)
                                        .clickable { selectedTabIndex = 7 } // Go to Auth/Profile
                                        .testTag("topbar_user_profile_chip")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (user.isSuperAdmin) HblLime else Color(0xFF38BDF8)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (user.isSuperAdmin) "👑" else user.fullName.firstOrNull()?.toString() ?: "U",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (user.isSuperAdmin) "Ali Hassan" else user.fullName.substringBefore(" "),
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // TAT Alert Center Bell Icon with Badge
                            IconButton(
                                onClick = { showAlertCenter = true },
                                modifier = Modifier.testTag("btn_alert_center")
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (totalAlertsCount > 0) {
                                            Badge(
                                                containerColor = if (breachedTasks.isNotEmpty()) Color(0xFFDC2626) else Color(0xFFD97706),
                                                contentColor = Color.White
                                            ) {
                                                Text(
                                                    text = if (totalAlertsCount > 99) "99+" else "$totalAlertsCount",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (totalAlertsCount > 0) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                        contentDescription = "TAT SLA Alerts",
                                        tint = if (totalAlertsCount > 0 && breachedTasks.isNotEmpty()) Color(0xFFFF8A80) else if (totalAlertsCount > 0) HblLime else Color.White
                                    )
                                }
                            }

                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Options",
                                    tint = Color.White
                                )
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("👤 User Login / Switch Role") },
                                    leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null, tint = HblPrimary) },
                                    onClick = {
                                        selectedTabIndex = 7
                                        showMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("⚡ Gemini AI CX Advisor") },
                                    leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = HblPrimary) },
                                    onClick = {
                                        selectedTabIndex = 3
                                        showMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("⚠️ Test Near-Breach Alert") },
                                    leadingIcon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD97706)) },
                                    onClick = {
                                        viewModel.sendTestNotification(isBreach = false)
                                        showMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("🚨 Test Breached SLA Alert") },
                                    leadingIcon = { Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFDC2626)) },
                                    onClick = {
                                        viewModel.sendTestNotification(isBreach = true)
                                        showMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("🔄 Check Real-time SLA Status") },
                                    leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                                    onClick = {
                                        viewModel.checkAndTriggerTatNotifications()
                                        showMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("🧹 Clear All Tasks / Test Data") },
                                    leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color(0xFFDC2626)) },
                                    onClick = {
                                        viewModel.clearAllTasksAndTestData()
                                        showMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("🚪 Sign Out") },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = Color(0xFFEF4444)) },
                                    onClick = {
                                        viewModel.logout()
                                        isUserLoggedIn = false
                                        showMenu = false
                                    }
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = HblPrimaryDark,
                            titleContentColor = Color.White
                        )
                    )
                },
                bottomBar = {
                    if (!isExpandedScreen) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = HblPrimary
                        ) {
                            primaryBottomTabs.forEachIndexed { index, tab ->
                                NavigationBarItem(
                                    selected = selectedTabIndex == index,
                                    onClick = { selectedTabIndex = index },
                                    icon = { Icon(tab.icon, contentDescription = tab.title) },
                                    label = {
                                        Text(
                                            text = tab.title,
                                            maxLines = 1,
                                            fontSize = 9.5.sp,
                                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = HblPrimary,
                                        selectedTextColor = HblPrimary,
                                        indicatorColor = HblPrimary.copy(alpha = 0.15f)
                                    ),
                                    modifier = Modifier.testTag(tab.tag)
                                )
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isExpandedScreen) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = HblPrimary
                        ) {
                            primaryBottomTabs.forEachIndexed { index, tab ->
                                NavigationRailItem(
                                    selected = selectedTabIndex == index,
                                    onClick = { selectedTabIndex = index },
                                    icon = { Icon(tab.icon, contentDescription = tab.title) },
                                    label = { Text(tab.title, fontSize = 10.sp) },
                                    modifier = Modifier.testTag(tab.tag)
                                )
                            }
                        }
                    }

                    // Screen Content
                    Box(modifier = Modifier.weight(1f)) {
                        when (selectedTabIndex) {
                            0 -> DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToTasks = { tatFilter ->
                                    viewModel.clearFilters()
                                    if (tatFilter != null) {
                                        viewModel.setTatFilter(tatFilter)
                                    }
                                    selectedTabIndex = 1
                                }
                            )
                            1 -> TasksScreen(viewModel = viewModel)
                            2 -> DailyTasksScreen(viewModel = viewModel)
                            3 -> AiAdvisorScreen(viewModel = viewModel)
                            4 -> UnitsAndTeamScreen(viewModel = viewModel)
                            5 -> ExecutiveSummaryScreen(viewModel = viewModel)
                            6 -> ExcelExportScreen(viewModel = viewModel)
                            7 -> AuthScreen(
                                viewModel = viewModel,
                                onAuthSuccess = { selectedTabIndex = 0 }
                            )
                        }
                    }
                }
            }

            // TAT Alert Center Bottom Sheet
            if (showAlertCenter) {
                TatAlertCenterSheet(
                    viewModel = viewModel,
                    onDismiss = { showAlertCenter = false },
                    onNavigateToTask = { task ->
                        viewModel.selectTask(task)
                        selectedTabIndex = 1 // Switch to Tasks tab
                    }
                )
            }

            // Create Task Modal triggered from Side Menu
            if (showCreateTaskModal) {
                CreateTaskDialog(
                    units = units,
                    members = teamMembers,
                    onDismiss = { showCreateTaskModal = false },
                    onConfirm = { title, description, unitId, assigneeId, assignedByName, priority, category, tatHours, customerAccountOrTicket, dueDateTime ->
                        viewModel.createTask(
                            title = title,
                            description = description,
                            unitId = unitId,
                            assigneeId = assigneeId,
                            assignedByName = assignedByName,
                            priority = priority,
                            category = category,
                            tatHours = tatHours,
                            customerAccountOrTicket = customerAccountOrTicket,
                            dueDateTime = dueDateTime
                        )
                        showCreateTaskModal = false
                        selectedTabIndex = 1 // Navigate to Tasks
                    }
                )
            }

            // Add Unit Modal triggered from Side Menu
            if (showAddUnitModal) {
                AddUnitDialog(
                    onDismiss = { showAddUnitModal = false },
                    onConfirm = { name, code, description, unitHeadName, headEmail, colorHex, defaultTatHours, targetSlaPercent ->
                        viewModel.createUnit(
                            name = name,
                            code = code,
                            description = description,
                            unitHeadName = unitHeadName,
                            headEmail = headEmail,
                            colorHex = colorHex,
                            defaultTatHours = defaultTatHours,
                            targetSlaPercent = targetSlaPercent
                        )
                        showAddUnitModal = false
                    }
                )
            }

            // Add Member Modal triggered from Side Menu
            if (showAddMemberModal) {
                AddMemberDialog(
                    units = units,
                    initialUnitId = addMemberPreselectedUnitId,
                    onDismiss = {
                        showAddMemberModal = false
                        addMemberPreselectedUnitId = null
                    },
                    onConfirm = { unitId, fullName, employeeId, email, phone, role, designation, avatarColorHex ->
                        viewModel.createTeamMember(
                            unitId = unitId,
                            fullName = fullName,
                            employeeId = employeeId,
                            email = email,
                            phone = phone,
                            role = role,
                            designation = designation,
                            avatarColorHex = avatarColorHex
                        )
                        showAddMemberModal = false
                        addMemberPreselectedUnitId = null
                    }
                )
            }
        }
    }
}
