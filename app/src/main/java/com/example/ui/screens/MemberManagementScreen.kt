package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CorporateFare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.example.data.entity.TeamMember
import com.example.data.model.MemberPerformanceSummary
import com.example.data.model.UnitPerformanceSummary
import com.example.ui.theme.HblLime
import com.example.ui.theme.HblOnLime
import com.example.ui.theme.HblPrimary
import com.example.ui.theme.HblPrimaryDark
import com.example.ui.theme.HblTertiaryGold
import com.example.ui.theme.StatusBreachedTat
import com.example.ui.theme.StatusWithinTat
import com.example.ui.viewmodel.CxViewModel
import java.util.Locale

/**
 * Dedicated Member Management Screen for the Unit Head (Sabeen Shafique).
 * Allows the Unit Head to add, edit, transfer/re-map, or remove team members and interns,
 * mapping them to the appropriate organizational units.
 */
@Composable
fun MemberManagementScreen(
    viewModel: CxViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val units by viewModel.units.collectAsStateWithLifecycle()
    val teamMembers by viewModel.teamMembers.collectAsStateWithLifecycle()
    val analytics by viewModel.analytics.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allTasks by viewModel.tasks.collectAsStateWithLifecycle()

    val isUnitHead = currentUser?.isUnitHead ?: false

    // Tabs: 0 = Personnel Directory & Mapping, 1 = Organizational Units
    var selectedScreenTab by remember { mutableStateOf(0) }

    // Search and filter state
    var searchQuery by remember { mutableStateOf("") }
    var personnelTypeFilter by remember { mutableStateOf("ALL") } // "ALL", "STAFF", "INTERN"
    var selectedUnitIdFilter by remember { mutableStateOf<Long?>(null) }

    // Dialog controllers
    var showAddDialog by remember { mutableStateOf(false) }
    var addDialogPresetIntern by remember { mutableStateOf(false) }
    var targetAddUnitId by remember { mutableStateOf<Long?>(null) }

    var memberToEdit by remember { mutableStateOf<TeamMember?>(null) }
    var memberToTransfer by remember { mutableStateOf<TeamMember?>(null) }
    var memberToRemove by remember { mutableStateOf<TeamMember?>(null) }

    var showAddUnitDialog by remember { mutableStateOf(false) }

    // Calculate metrics
    val totalCount = teamMembers.size
    val internMembers = remember(teamMembers, units) {
        teamMembers.filter { isInternMember(it, units) }
    }
    val staffMembers = remember(teamMembers, units) {
        teamMembers.filter { !isInternMember(it, units) }
    }
    val internCount = internMembers.size
    val staffCount = staffMembers.size
    val activeUnitsCount = units.size

    // Filtered personnel list
    val filteredMembers = remember(teamMembers, units, searchQuery, personnelTypeFilter, selectedUnitIdFilter) {
        teamMembers.filter { member ->
            val isIntern = isInternMember(member, units)
            val matchesType = when (personnelTypeFilter) {
                "STAFF" -> !isIntern
                "INTERN" -> isIntern
                else -> true
            }

            val matchesUnit = selectedUnitIdFilter == null || member.unitId == selectedUnitIdFilter

            val mappedUnit = units.find { it.id == member.unitId }
            val unitName = mappedUnit?.name ?: ""
            val unitCode = mappedUnit?.code ?: ""

            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                val q = searchQuery.trim().lowercase(Locale.ROOT)
                member.fullName.lowercase(Locale.ROOT).contains(q) ||
                        member.employeeId.lowercase(Locale.ROOT).contains(q) ||
                        member.designation.lowercase(Locale.ROOT).contains(q) ||
                        member.email.lowercase(Locale.ROOT).contains(q) ||
                        unitName.lowercase(Locale.ROOT).contains(q) ||
                        unitCode.lowercase(Locale.ROOT).contains(q)
            }

            matchesType && matchesUnit && matchesSearch
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("member_management_screen")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Unit Head Authority Banner
            item {
                UnitHeadAuthorityCard(
                    currentUser = currentUser,
                    isUnitHead = isUnitHead
                )
            }

            // 2. Executive Staffing & Allocation KPI Metrics
            item {
                ExecutiveStaffingKpiRow(
                    totalCount = totalCount,
                    staffCount = staffCount,
                    internCount = internCount,
                    unitsCount = activeUnitsCount
                )
            }

            // 3. Quick Action Launchpad: Add Staff Member vs Add Intern
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            addDialogPresetIntern = false
                            targetAddUnitId = selectedUnitIdFilter
                            showAddDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HblPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_add_team_member")
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Add Team Member",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                    }

                    Button(
                        onClick = {
                            addDialogPresetIntern = true
                            // If an "INT" unit exists, preselect it, else null
                            val internUnit = units.find { it.code.equals("INT", ignoreCase = true) || it.name.contains("Intern", ignoreCase = true) }
                            targetAddUnitId = internUnit?.id ?: selectedUnitIdFilter
                            showAddDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HblLime,
                            contentColor = HblOnLime
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_add_intern")
                    ) {
                        Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Add Intern / Trainee",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                    }
                }
            }

            // 4. View Switcher Tabs: Personnel & Unit Mapping vs Organizational Units Overview
            item {
                TabRow(
                    selectedTabIndex = selectedScreenTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = HblPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)),
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedScreenTab]),
                            color = HblPrimary,
                            height = 3.dp
                        )
                    }
                ) {
                    Tab(
                        selected = selectedScreenTab == 0,
                        onClick = { selectedScreenTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Personnel & Mappings (${filteredMembers.size})",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    )
                    Tab(
                        selected = selectedScreenTab == 1,
                        onClick = { selectedScreenTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CorporateFare, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "CX Units Matrix (${units.size})",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    )
                }
            }

            if (selectedScreenTab == 0) {
                // Search Bar
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("member_search_input"),
                        placeholder = { Text("Search by name, ID, role, or unit...", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = HblPrimary, modifier = Modifier.size(20.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                // Personnel Type Filter Chips
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = personnelTypeFilter == "ALL",
                            onClick = { personnelTypeFilter = "ALL" },
                            label = { Text("All Personnel ($totalCount)", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HblPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                        FilterChip(
                            selected = personnelTypeFilter == "STAFF",
                            onClick = { personnelTypeFilter = "STAFF" },
                            label = { Text("Officers ($staffCount)", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(14.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HblPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                        FilterChip(
                            selected = personnelTypeFilter == "INTERN",
                            onClick = { personnelTypeFilter = "INTERN" },
                            label = { Text("Interns ($internCount)", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(14.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HblLime,
                                selectedLabelColor = HblOnLime
                            )
                        )
                    }
                }

                // Horizontal Organizational Unit Filter Chips
                item {
                    Column {
                        Text(
                            text = "Filter by Organizational Unit:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = selectedUnitIdFilter == null,
                                onClick = { selectedUnitIdFilter = null },
                                label = { Text("All Units") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = HblPrimaryDark,
                                    selectedLabelColor = Color.White
                                )
                            )
                            units.forEach { u ->
                                val isSelected = selectedUnitIdFilter == u.id
                                val isInternUnit = u.code.equals("INT", ignoreCase = true) || u.name.contains("Intern", ignoreCase = true)
                                val unitMembersCount = teamMembers.count { it.unitId == u.id }

                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedUnitIdFilter = if (isSelected) null else u.id
                                    },
                                    label = {
                                        Text("${u.code} ($unitMembersCount)")
                                    },
                                    leadingIcon = if (isInternUnit) {
                                        { Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = HblPrimary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }

                // Personnel List Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Mapped Staff & Interns (${filteredMembers.size})",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = HblPrimary
                            )
                        )
                        if (selectedUnitIdFilter != null) {
                            val activeUnit = units.find { it.id == selectedUnitIdFilter }
                            Text(
                                text = "Showing ${activeUnit?.name ?: ""}",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
                }

                // Empty state if search or filter gives 0
                if (filteredMembers.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("No personnel match the current criteria", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Try changing your search query or unit filter.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        searchQuery = ""
                                        personnelTypeFilter = "ALL"
                                        selectedUnitIdFilter = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = HblPrimary)
                                ) {
                                    Text("Reset Filters")
                                }
                            }
                        }
                    }
                } else {
                    // Member Cards
                    items(filteredMembers, key = { it.id }) { member ->
                        val mappedUnit = units.find { it.id == member.unitId }
                        val isIntern = isInternMember(member, units)
                        val memberSummary = analytics.memberSummaries.find { it.member.id == member.id }
                        val activeTasksCount = allTasks.count { it.assigneeId == member.id && it.status != TaskStatus.COMPLETED }

                        UnitHeadMemberCard(
                            member = member,
                            unit = mappedUnit,
                            isIntern = isIntern,
                            summary = memberSummary,
                            activeTasksCount = activeTasksCount,
                            onTransferUnit = { memberToTransfer = member },
                            onEdit = { memberToEdit = member },
                            onDelete = { memberToRemove = member }
                        )
                    }
                }
            } else {
                // Tab 1: Organizational Units & Mapping Overview
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Organizational Units Matrix",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = HblPrimary
                                )
                            )
                            Text(
                                text = "Configure mandates, default TATs, and see assigned rosters",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }

                        Button(
                            onClick = { showAddUnitDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = HblPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("btn_add_cx_unit")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Unit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                items(analytics.unitSummaries, key = { it.unit.id }) { uSummary ->
                    val unitMembers = teamMembers.filter { it.unitId == uSummary.unit.id }
                    OrganizationalUnitCard(
                        unitSummary = uSummary,
                        members = unitMembers,
                        onAddMemberToUnit = {
                            targetAddUnitId = uSummary.unit.id
                            addDialogPresetIntern = uSummary.unit.code.equals("INT", ignoreCase = true)
                            showAddDialog = true
                        },
                        onDeleteUnit = {
                            viewModel.deleteUnit(uSummary.unit)
                            Toast.makeText(context, "Deleted unit ${uSummary.unit.name}", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = {
                if (selectedScreenTab == 0) {
                    addDialogPresetIntern = false
                    targetAddUnitId = selectedUnitIdFilter
                    showAddDialog = true
                } else {
                    showAddUnitDialog = true
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("member_mgmt_fab"),
            containerColor = HblPrimary,
            contentColor = Color.White
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (selectedScreenTab == 0) "Add Personnel" else "Add CX Unit",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }

    // -------------------------------------------------------------
    // DIALOGS
    // -------------------------------------------------------------

    // 1. Add Member / Intern Dialog
    if (showAddDialog) {
        AddPersonnelDialog(
            units = units,
            initialUnitId = targetAddUnitId,
            initialIsIntern = addDialogPresetIntern,
            onDismiss = { showAddDialog = false },
            onConfirm = { unitId, fullName, employeeId, email, phone, role, designation, avatarColor ->
                viewModel.createTeamMember(
                    unitId = unitId,
                    fullName = fullName,
                    employeeId = employeeId,
                    email = email,
                    phone = phone,
                    role = role,
                    designation = designation,
                    avatarColorHex = avatarColor
                )
                showAddDialog = false
                Toast.makeText(context, "Added $fullName to organizational unit!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 2. Edit Member / Intern Dialog
    memberToEdit?.let { member ->
        EditPersonnelDialog(
            member = member,
            units = units,
            onDismiss = { memberToEdit = null },
            onConfirm = { fullName, email, unitId, employeeId, designation, phone, role ->
                viewModel.updateMember(
                    id = member.id,
                    name = fullName,
                    email = email,
                    unitId = unitId,
                    employeeId = employeeId,
                    designation = designation,
                    phone = phone,
                    role = role
                )
                memberToEdit = null
                Toast.makeText(context, "Saved changes for $fullName!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 3. Quick Unit Transfer Dialog (Re-map Member to another Unit)
    memberToTransfer?.let { member ->
        val currentUnit = units.find { it.id == member.unitId }
        TransferUnitDialog(
            member = member,
            currentUnit = currentUnit,
            availableUnits = units,
            onDismiss = { memberToTransfer = null },
            onConfirmTransfer = { newUnitId ->
                viewModel.reassignMemberUnit(member.id, newUnitId)
                val destUnit = units.find { it.id == newUnitId }
                memberToTransfer = null
                Toast.makeText(
                    context,
                    "Transferred ${member.fullName} to ${destUnit?.name ?: "Unit"}!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    // 4. Remove Confirmation Dialog
    memberToRemove?.let { member ->
        val mappedUnit = units.find { it.id == member.unitId }
        val activeTasksCount = allTasks.count { it.assigneeId == member.id && it.status != TaskStatus.COMPLETED }
        val isIntern = isInternMember(member, units)

        RemovePersonnelDialog(
            member = member,
            unit = mappedUnit,
            isIntern = isIntern,
            activeTasksCount = activeTasksCount,
            onDismiss = { memberToRemove = null },
            onConfirmDelete = {
                viewModel.deleteTeamMember(member)
                memberToRemove = null
                Toast.makeText(context, "Removed ${member.fullName} from team roster", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 5. Add Unit Dialog
    if (showAddUnitDialog) {
        AddUnitDialog(
            onDismiss = { showAddUnitDialog = false },
            onConfirm = { name, code, desc, head, email, color, tat, sla ->
                viewModel.createUnit(name, code, desc, head, email, color, tat, sla)
                showAddUnitDialog = false
                Toast.makeText(context, "Created CX Unit $name ($code)", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

// -------------------------------------------------------------
// HELPER: Intern Check
// -------------------------------------------------------------
fun isInternMember(member: TeamMember, units: List<CxUnit>): Boolean {
    val unit = units.find { it.id == member.unitId }
    return member.role.contains("Intern", ignoreCase = true) ||
            member.designation.contains("Intern", ignoreCase = true) ||
            member.fullName.contains("Intern", ignoreCase = true) ||
            member.employeeId.startsWith("INT", ignoreCase = true) ||
            unit?.code.equals("INT", ignoreCase = true) ||
            unit?.name?.contains("Intern", ignoreCase = true) == true
}

// -------------------------------------------------------------
// COMPOSABLES: Cards & Headers
// -------------------------------------------------------------

@Composable
fun UnitHeadAuthorityCard(
    currentUser: com.example.data.entity.UserAccount?,
    isUnitHead: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("unit_head_authority_banner"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnitHead) HblPrimaryDark else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isUnitHead) HblLime else HblPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isUnitHead) Icons.Default.Security else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (isUnitHead) HblOnLime else HblPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Customer Experience Unit Head",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isUnitHead) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isUnitHead) HblLime.copy(alpha = 0.9f) else Color.Gray.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = if (isUnitHead) "UNIT HEAD" else "RESTRICTED",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isUnitHead) HblOnLime else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 9.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }

                        Text(
                            text = if (isUnitHead)
                                "Sabeen Shafique • Full Personnel & Unit Allocation Privileges"
                            else
                                "Active User: ${currentUser?.fullName ?: "Guest"} (Read-Only Mode)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = if (isUnitHead) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            if (!isUnitHead) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    // SECURITY FIX: the self-service "Switch to Unit Head" button was
                    // removed. It let any signed-in Team Member grant themselves full
                    // admin rights with a single tap and no authentication. Unit Head
                    // access now requires signing in with an actual Unit Head account.
                    text = "To add, edit, transfer or remove team members & interns, please sign in with a Unit Head account.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }
    }
}

@Composable
fun ExecutiveStaffingKpiRow(
    totalCount: Int,
    staffCount: Int,
    internCount: Int,
    unitsCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        KpiMetricCard(
            label = "Total Staff",
            value = "$totalCount",
            icon = Icons.Default.Groups,
            color = HblPrimary,
            modifier = Modifier.weight(1f)
        )
        KpiMetricCard(
            label = "Officers",
            value = "$staffCount",
            icon = Icons.Default.Badge,
            color = Color(0xFF0284C7),
            modifier = Modifier.weight(1f)
        )
        KpiMetricCard(
            label = "Interns",
            value = "$internCount",
            icon = Icons.Default.School,
            color = Color(0xFF7C3AED),
            modifier = Modifier.weight(1f)
        )
        KpiMetricCard(
            label = "Units",
            value = "$unitsCount",
            icon = Icons.Default.CorporateFare,
            color = Color(0xFF059669),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun KpiMetricCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = color)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
fun UnitHeadMemberCard(
    member: TeamMember,
    unit: CxUnit?,
    isIntern: Boolean,
    summary: MemberPerformanceSummary?,
    activeTasksCount: Int,
    onTransferUnit: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("member_card_${member.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Row: Avatar, Name, Intern/Officer Tag, Performance Tier
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (isIntern) HblLime.copy(alpha = 0.35f)
                                else HblPrimary.copy(alpha = 0.18f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = member.fullName.split(" ")
                                .mapNotNull { it.firstOrNull()?.toString() }
                                .take(2)
                                .joinToString(""),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isIntern) HblOnLime else HblPrimary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = member.fullName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isIntern) HblLime.copy(alpha = 0.3f) else HblPrimary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = if (isIntern) "INTERN" else "STAFF",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isIntern) HblOnLime else HblPrimary,
                                        fontSize = 9.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }

                        Text(
                            text = "${member.employeeId} • ${member.designation}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                if (summary != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = HblTertiaryGold.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = summary.tierRating,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = HblTertiaryGold,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ORGANIZATIONAL UNIT MAPPING PILL
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = HblPrimary.copy(alpha = 0.08f),
                border = androidx.compose.foundation.BorderStroke(1.dp, HblPrimary.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CorporateFare,
                            contentDescription = null,
                            tint = HblPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Mapped Unit:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = HblPrimary
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (unit != null) "[${unit.code}] ${unit.name}" else "Unassigned Unit",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (unit != null) {
                        Text(
                            text = "SLA Target: ${unit.targetSlaPercent.toInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Contact & Active Workload Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = member.email,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (activeTasksCount > 0) Color(0xFFFEF3C7) else Color(0xFFECFDF5)
                ) {
                    Text(
                        text = if (activeTasksCount > 0) "$activeTasksCount Active Tasks" else "Idle / Available",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (activeTasksCount > 0) Color(0xFF92400E) else Color(0xFF065F46),
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            // UNIT HEAD ACTION CONTROLS: Transfer Unit, Edit Profile, Remove
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quick Transfer Unit Button
                OutlinedButton(
                    onClick = onTransferUnit,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("btn_transfer_unit_${member.id}")
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = "Transfer Unit", modifier = Modifier.size(14.dp), tint = HblPrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Re-map Unit", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HblPrimary)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Edit Profile Button
                    Button(
                        onClick = onEdit,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isIntern) HblLime else HblPrimary,
                            contentColor = if (isIntern) HblOnLime else Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("btn_edit_member_${member.id}")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isIntern) "Edit Intern" else "Edit Details",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Delete Button
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("btn_delete_member_${member.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove Member",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OrganizationalUnitCard(
    unitSummary: UnitPerformanceSummary,
    members: List<TeamMember>,
    onAddMemberToUnit: () -> Unit,
    onDeleteUnit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val u = unitSummary.unit
    val isInternUnit = u.code.equals("INT", ignoreCase = true) || u.name.contains("Intern", ignoreCase = true)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isInternUnit) HblLime.copy(alpha = 0.35f) else HblPrimary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = u.code,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isInternUnit) HblOnLime else HblPrimary
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = u.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${members.size} Personnel Mapped • TAT: ${u.defaultTatHours.toInt()}h • SLA: ${u.targetSlaPercent.toInt()}%",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }

                IconButton(onClick = onDeleteUnit, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Unit", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = u.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Mapped Staff Avatars Preview
            if (members.isNotEmpty()) {
                Text(
                    text = "Assigned Team Members & Interns:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = HblPrimary)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    members.forEach { m ->
                        val isInt = m.role.contains("Intern", ignoreCase = true) || m.designation.contains("Intern", ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isInt) HblLime.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isInt) Icons.Default.School else Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = if (isInt) HblOnLime else HblPrimary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = m.fullName,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onAddMemberToUnit,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = HblPrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Map Member to ${u.code}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HblPrimary)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// MODALS & DIALOGS
// -------------------------------------------------------------

/**
 * Dialog to Add a new Team Member or Intern with explicit Unit Mapping
 */
@Composable
fun AddPersonnelDialog(
    units: List<CxUnit>,
    initialUnitId: Long? = null,
    initialIsIntern: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (
        unitId: Long,
        fullName: String,
        employeeId: String,
        email: String,
        phone: String,
        role: String,
        designation: String,
        avatarColor: String
    ) -> Unit
) {
    var isIntern by remember { mutableStateOf(initialIsIntern) }
    var selectedUnitId by remember {
        mutableStateOf(
            initialUnitId ?: if (initialIsIntern) {
                units.find { it.code.equals("INT", ignoreCase = true) || it.name.contains("Intern", ignoreCase = true) }?.id ?: (units.firstOrNull()?.id ?: 1L)
            } else {
                units.firstOrNull()?.id ?: 1L
            }
        )
    }

    var fullName by remember { mutableStateOf("") }
    var employeeId by remember {
        mutableStateOf(if (initialIsIntern) "INT-${(400..499).random()}" else "CX-${(100..999).random()}")
    }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(if (initialIsIntern) "Intern" else "Team Member") }
    var designation by remember {
        mutableStateOf(if (initialIsIntern) "Customer Experience Intern" else "Customer Experience Specialist")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isIntern) Icons.Default.School else Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = HblPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isIntern) "Add CX Intern / Trainee" else "Add CX Team Member",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = HblPrimary)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Personnel Type Segmented Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            isIntern = false
                            role = "Team Member"
                            designation = "Customer Experience Specialist"
                            if (employeeId.startsWith("INT")) employeeId = "CX-${(100..999).random()}"
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (!isIntern) HblPrimary.copy(alpha = 0.15f) else Color.Transparent
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Regular Staff", fontSize = 12.sp, fontWeight = if (!isIntern) FontWeight.Bold else FontWeight.Normal)
                    }

                    OutlinedButton(
                        onClick = {
                            isIntern = true
                            role = "Intern"
                            designation = "Customer Experience Intern"
                            if (employeeId.startsWith("CX")) employeeId = "INT-${(400..499).random()}"
                            val internUnit = units.find { it.code.equals("INT", ignoreCase = true) || it.name.contains("Intern", ignoreCase = true) }
                            if (internUnit != null) selectedUnitId = internUnit.id
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isIntern) HblLime.copy(alpha = 0.3f) else Color.Transparent
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Intern / Trainee", fontSize = 12.sp, fontWeight = if (isIntern) FontWeight.Bold else FontWeight.Normal)
                    }
                }

                // Organizational Unit Mapping Selector
                Column {
                    Text(
                        text = "Map to Organizational Unit *",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = HblPrimary)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        units.forEach { u ->
                            FilterChip(
                                selected = selectedUnitId == u.id,
                                onClick = { selectedUnitId = u.id },
                                label = { Text("${u.code} - ${u.name}") }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = fullName,
                    onValueChange = {
                        fullName = it
                        if (email.isBlank() || email.endsWith("@example.com")) {
                            val cleanName = it.trim().lowercase().replace(" ", ".")
                            if (cleanName.isNotEmpty()) {
                                email = "$cleanName@example.com"
                            }
                        }
                    },
                    label = { Text(if (isIntern) "Intern Full Name *" else "Full Name *") },
                    placeholder = { Text(if (isIntern) "e.g. Ayesha Khan" else "e.g. Tariq Mehmood") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_member_fullname_field"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = employeeId,
                    onValueChange = { employeeId = it },
                    label = { Text(if (isIntern) "Intern Roll / ID *" else "Employee ID *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = designation,
                    onValueChange = { designation = it },
                    label = { Text("Designation / Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Official Email *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Contact Phone") },
                    placeholder = { Text("e.g. +92 300 1234567") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fullName.isNotBlank() && employeeId.isNotBlank()) {
                        val finalEmail = if (email.isNotBlank()) email.trim() else "${fullName.trim().lowercase().replace(" ", ".")}@example.com"
                        onConfirm(
                            selectedUnitId,
                            fullName.trim(),
                            employeeId.trim(),
                            finalEmail,
                            phone.trim(),
                            role,
                            designation.trim(),
                            if (isIntern) "#7C3AED" else "#008269"
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = HblPrimary),
                enabled = fullName.isNotBlank() && employeeId.isNotBlank(),
                modifier = Modifier.testTag("add_member_confirm_btn")
            ) {
                Text(if (isIntern) "Add Intern" else "Add Member")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Dialog to Edit Member or Intern Details, including modifying organizational unit mapping
 */
@Composable
fun EditPersonnelDialog(
    member: TeamMember,
    units: List<CxUnit>,
    onDismiss: () -> Unit,
    onConfirm: (
        fullName: String,
        email: String,
        unitId: Long,
        employeeId: String,
        designation: String,
        phone: String,
        role: String
    ) -> Unit
) {
    val isIntern = isInternMember(member, units)

    var fullName by remember { mutableStateOf(member.fullName) }
    var email by remember { mutableStateOf(member.email) }
    var employeeId by remember { mutableStateOf(member.employeeId) }
    var phone by remember { mutableStateOf(member.phone) }
    var designation by remember { mutableStateOf(member.designation) }
    var role by remember { mutableStateOf(member.role) }
    var selectedUnitId by remember { mutableStateOf(member.unitId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = HblPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isIntern) "Edit Intern Profile & Mapping" else "Edit Team Member Profile",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = HblPrimary)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text(if (isIntern) "Intern Full Name *" else "Full Name *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_member_name_field"),
                    singleLine = true
                )

                // Organizational Unit Mapping Selector
                Column {
                    Text("Mapped Organizational Unit *", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = HblPrimary))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        units.forEach { u ->
                            FilterChip(
                                selected = selectedUnitId == u.id,
                                onClick = { selectedUnitId = u.id },
                                label = { Text("${u.code} - ${u.name}") }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = designation,
                    onValueChange = { designation = it },
                    label = { Text("Designation") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = employeeId,
                    onValueChange = { employeeId = it },
                    label = { Text("Employee / Intern ID") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Contact Phone") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fullName.isNotBlank()) {
                        onConfirm(fullName.trim(), email.trim(), selectedUnitId, employeeId.trim(), designation.trim(), phone.trim(), role)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = HblPrimary),
                enabled = fullName.isNotBlank(),
                modifier = Modifier.testTag("edit_member_confirm_btn")
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Dedicated Fast Dialog for Re-mapping / Transferring a Member or Intern to another Unit
 */
@Composable
fun TransferUnitDialog(
    member: TeamMember,
    currentUnit: CxUnit?,
    availableUnits: List<CxUnit>,
    onDismiss: () -> Unit,
    onConfirmTransfer: (newUnitId: Long) -> Unit
) {
    var selectedTargetUnitId by remember { mutableStateOf(currentUnit?.id ?: availableUnits.firstOrNull()?.id ?: 1L) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = null,
                    tint = HblPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Transfer to Another Unit",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = HblPrimary)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = member.fullName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${member.designation} (${member.employeeId})",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Currently mapped to: ${currentUnit?.let { "[${it.code}] ${it.name}" } ?: "None"}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = HblPrimary)
                        )
                    }
                }

                Text(
                    text = "Select Destination Organizational Unit:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    availableUnits.forEach { targetUnit ->
                        val isSelected = selectedTargetUnitId == targetUnit.id
                        val isCurrent = currentUnit?.id == targetUnit.id

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedTargetUnitId = targetUnit.id },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) HblPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) HblPrimary else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedTargetUnitId = targetUnit.id },
                                    colors = RadioButtonDefaults.colors(selectedColor = HblPrimary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "[${targetUnit.code}] ${targetUnit.name}",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                        if (isCurrent) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color.Gray.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = "CURRENT",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "Target SLA: ${targetUnit.targetSlaPercent.toInt()}% • Standard TAT: ${targetUnit.defaultTatHours.toInt()}h",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmTransfer(selectedTargetUnitId) },
                colors = ButtonDefaults.buttonColors(containerColor = HblPrimary),
                enabled = selectedTargetUnitId != currentUnit?.id,
                modifier = Modifier.testTag("btn_confirm_transfer_unit")
            ) {
                Text("Confirm Transfer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Safety confirmation dialog before removing a member or intern
 */
@Composable
fun RemovePersonnelDialog(
    member: TeamMember,
    unit: CxUnit?,
    isIntern: Boolean,
    activeTasksCount: Int,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isIntern) "Remove Intern from Program" else "Remove Team Member",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Are you sure you want to remove ${member.fullName} (${member.designation}) from ${unit?.name ?: "their unit"}?"
                )

                if (activeTasksCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEF2F2),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Warning: This person currently has $activeTasksCount active/pending task(s). Deleting will move tasks back to the unassigned queue.",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF991B1B), fontSize = 11.sp)
                            )
                        }
                    }
                } else {
                    Text(
                        text = "This member has no active tasks. This action will delete their profile from the active roster.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                modifier = Modifier.testTag("confirm_delete_member_btn")
            ) {
                Text("Confirm Removal", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
