package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CorporateFare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.CxUnit
import com.example.data.entity.TeamMember
import com.example.data.model.MemberPerformanceSummary
import com.example.data.model.UnitPerformanceSummary
import com.example.ui.components.formatDurationMinutes
import com.example.ui.theme.HblLime
import com.example.ui.theme.HblOnLime
import com.example.ui.theme.HblPrimary
import com.example.ui.theme.HblPrimaryDark
import com.example.ui.theme.HblTertiaryGold
import com.example.ui.theme.StatusBreachedTat
import com.example.ui.theme.StatusWithinTat
import com.example.ui.viewmodel.CxViewModel
import android.widget.Toast
import java.util.Locale

@Composable
fun UnitsAndTeamScreen(
    viewModel: CxViewModel,
    modifier: Modifier = Modifier
) {
    MemberManagementScreen(viewModel = viewModel, modifier = modifier)
}

@Composable
fun UnitSummaryCard(
    unitSummary: UnitPerformanceSummary,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val u = unitSummary.unit
    val isSlaMet = unitSummary.slaPercent >= unitSummary.targetSlaPercent

    Card(
        modifier = modifier.fillMaxWidth(),
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
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = u.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = u.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Stats row
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Unit Head", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                        Text(u.unitHeadName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        Text(u.headEmail, style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("SLA Adherence", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                        Text(
                            "${String.format(Locale.US, "%.1f%%", unitSummary.slaPercent)}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isSlaMet) StatusWithinTat else StatusBreachedTat
                            )
                        )
                        Text("Target: ${u.targetSlaPercent.toInt()}% • TAT: ${u.defaultTatHours.toInt()}h", style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${unitSummary.totalTasks} tasks • ${unitSummary.completedCount} done • ${unitSummary.withinTatCount} on-TAT",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Text(
                    text = "${String.format(Locale.US, "%.1f", unitSummary.totalTimeMotionHours)}h motion logged",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = HblPrimary)
                )
            }
        }
    }
}

@Composable
fun MemberSummaryCard(
    memberSummary: MemberPerformanceSummary,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val m = memberSummary.member
    val isIntern = m.role.contains("Intern", ignoreCase = true) ||
            m.designation.contains("Intern", ignoreCase = true) ||
            memberSummary.unitCode.equals("INT", ignoreCase = true) ||
            m.fullName.contains("Intern", ignoreCase = true)

    Card(
        modifier = modifier.fillMaxWidth(),
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
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isIntern) HblLime.copy(alpha = 0.3f) else HblPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = m.fullName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString(""),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isIntern) HblOnLime else HblPrimary
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = m.fullName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )
                            if (isIntern) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = HblLime.copy(alpha = 0.25f)
                                ) {
                                    Text(
                                        text = "INTERN",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = HblOnLime,
                                            fontSize = 9.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "${m.employeeId} • ${memberSummary.unitCode} • ${m.designation}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = HblTertiaryGold.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = memberSummary.tierRating,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = HblTertiaryGold,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Performance Metrics Grid
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Assigned", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                        Text("${memberSummary.assignedCount}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Done", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                        Text("${memberSummary.completedCount}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = StatusWithinTat))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("SLA %", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                        Text("${String.format(Locale.US, "%.0f%%", memberSummary.slaPercent)}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = if (memberSummary.slaPercent >= 90.0) StatusWithinTat else StatusBreachedTat))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Breaches", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                        Text("${memberSummary.breachedCount}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = if (memberSummary.breachedCount > 0) StatusBreachedTat else StatusWithinTat))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Motion", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                        Text("${String.format(Locale.US, "%.1f", memberSummary.totalTimeMotionHours)}h", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = HblPrimary))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = m.email,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Text(
                    text = "Score: ${String.format(Locale.US, "%.0f", memberSummary.productivityScore)} / 100",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = HblPrimary)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons: Edit Name / Details & Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isIntern) {
                    Button(
                        onClick = onEdit,
                        colors = ButtonDefaults.buttonColors(containerColor = HblLime, contentColor = HblOnLime),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("edit_intern_btn_${m.id}")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Intern Name", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit Intern Name", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                } else {
                    OutlinedButton(
                        onClick = onEdit,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("edit_member_btn_${m.id}")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Member", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit Member", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// DIALOGS
// -------------------------------------------------------------

@Composable
fun AddUnitDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        code: String,
        description: String,
        unitHeadName: String,
        headEmail: String,
        colorHex: String,
        defaultTatHours: Double,
        targetSlaPercent: Double
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var unitHeadName by remember { mutableStateOf("") }
    var headEmail by remember { mutableStateOf("") }
    var tatHours by remember { mutableStateOf("24") }
    var targetSla by remember { mutableStateOf("95") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Customer Experience Unit", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = HblPrimary)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Unit Name *") }, placeholder = { Text("e.g. Digital CX & Raast") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Unit Code *") }, placeholder = { Text("e.g. DCX") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Unit Mandate / Description") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = unitHeadName, onValueChange = { unitHeadName = it }, label = { Text("Unit Head Name *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = headEmail, onValueChange = { headEmail = it }, label = { Text("Unit Head Email") }, modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = tatHours, onValueChange = { tatHours = it }, label = { Text("Default TAT (Hrs)") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = targetSla, onValueChange = { targetSla = it }, label = { Text("Target SLA (%)") }, modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && code.isNotBlank()) {
                        onConfirm(
                            name,
                            code,
                            description,
                            unitHeadName,
                            headEmail,
                            "#008269",
                            tatHours.toDoubleOrNull() ?: 24.0,
                            targetSla.toDoubleOrNull() ?: 95.0
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = HblPrimary),
                enabled = name.isNotBlank() && code.isNotBlank()
            ) {
                Text("Create Unit")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AddMemberDialog(
    units: List<CxUnit>,
    initialUnitId: Long? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        unitId: Long,
        fullName: String,
        employeeId: String,
        email: String,
        phone: String,
        role: String,
        designation: String,
        avatarColorHex: String
    ) -> Unit
) {
    var selectedUnitId by remember { mutableStateOf(initialUnitId ?: units.firstOrNull()?.id ?: 1L) }
    var fullName by remember { mutableStateOf("") }
    var employeeId by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("CX Officer") }
    var designation by remember { mutableStateOf("Customer Experience Analyst") }

    val roles = listOf("Unit Head", "CX Manager", "Senior CX Officer", "Resolution Specialist", "QA Analyst", "Branch Auditor")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add CX Team Member", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = HblPrimary)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Full Name *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = employeeId, onValueChange = { employeeId = it }, label = { Text("Employee ID * (e.g. HBL-CX-109)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = designation, onValueChange = { designation = it }, label = { Text("Designation") }, modifier = Modifier.fillMaxWidth())

                Text("Assign to Unit *", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    units.forEach { u ->
                        FilterChip(
                            selected = selectedUnitId == u.id,
                            onClick = { selectedUnitId = u.id },
                            label = { Text(u.code) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fullName.isNotBlank() && employeeId.isNotBlank()) {
                        onConfirm(selectedUnitId, fullName, employeeId, email, phone, role, designation, "#008269")
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = HblPrimary),
                enabled = fullName.isNotBlank() && employeeId.isNotBlank()
            ) {
                Text("Add Member")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun EditMemberDialog(
    member: TeamMember,
    units: List<CxUnit>,
    onDismiss: () -> Unit,
    onConfirm: (
        fullName: String,
        email: String,
        unitId: Long,
        employeeId: String,
        designation: String,
        phone: String
    ) -> Unit
) {
    val isIntern = member.role.contains("Intern", ignoreCase = true) ||
            member.designation.contains("Intern", ignoreCase = true) ||
            member.fullName.contains("Intern", ignoreCase = true) ||
            units.find { it.id == member.unitId }?.name?.contains("Intern", ignoreCase = true) == true

    var fullName by remember { mutableStateOf(member.fullName) }
    var email by remember { mutableStateOf(member.email) }
    var employeeId by remember { mutableStateOf(member.employeeId) }
    var phone by remember { mutableStateOf(member.phone) }
    var designation by remember { mutableStateOf(member.designation) }
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
                    text = if (isIntern) "Edit Intern Name & Details" else "Edit Team Member Details",
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
                    modifier = Modifier.fillMaxWidth().testTag("edit_member_name_field")
                )
                OutlinedTextField(
                    value = designation,
                    onValueChange = { designation = it },
                    label = { Text("Designation") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address *") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = employeeId,
                    onValueChange = { employeeId = it },
                    label = { Text("Employee ID") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Contact Phone") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Assign to Unit *", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    units.forEach { u ->
                        FilterChip(
                            selected = selectedUnitId == u.id,
                            onClick = { selectedUnitId = u.id },
                            label = { Text(u.code) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fullName.isNotBlank()) {
                        onConfirm(fullName, email, selectedUnitId, employeeId, designation, phone)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = HblPrimary),
                enabled = fullName.isNotBlank()
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
