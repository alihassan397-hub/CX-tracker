package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.CxTask
import com.example.data.entity.TaskPriority
import com.example.data.entity.TaskStatus
import com.example.data.entity.TatStatus
import com.example.ui.theme.HblLime
import com.example.ui.theme.HblOnLime
import com.example.ui.theme.HblPrimary
import com.example.ui.theme.HblPrimaryDark
import com.example.ui.theme.StatusAtRisk
import com.example.ui.theme.StatusAtRiskContainer
import com.example.ui.theme.StatusBreachedTat
import com.example.ui.theme.StatusBreachedTatContainer
import com.example.ui.theme.StatusWithinTat
import com.example.ui.theme.StatusWithinTatContainer
import com.example.ui.viewmodel.CxViewModel
import com.example.util.TatNotificationManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TatAlertCenterSheet(
    viewModel: CxViewModel,
    onDismiss: () -> Unit,
    onNavigateToTask: (CxTask) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val context = LocalContext.current
    val breachedTasks by viewModel.activeBreachedTasks.collectAsState()
    val nearBreachTasks by viewModel.activeNearBreachTasks.collectAsState()
    val units by viewModel.units.collectAsState()
    val hasPermission = TatNotificationManager.hasNotificationPermission(context)

    val totalAlerts = breachedTasks.size + nearBreachTasks.size

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .testTag("tat_alert_center_sheet")
        ) {
            // Header Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                HblPrimaryDark,
                                HblPrimary,
                                if (breachedTasks.isNotEmpty()) Color(0xFF991B1B) else HblPrimary
                            )
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (breachedTasks.isNotEmpty()) Color(0xFFEF4444) else HblLime
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (breachedTasks.isNotEmpty()) Icons.Default.ErrorOutline else Icons.Default.NotificationsActive,
                            contentDescription = "Alerts",
                            tint = if (breachedTasks.isNotEmpty()) Color.White else HblOnLime,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "TAT SLA Alert Center",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (breachedTasks.isNotEmpty()) Color(0xFFDC2626)
                                        else if (nearBreachTasks.isNotEmpty()) Color(0xFFD97706)
                                        else Color(0xFF059669)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (totalAlerts > 0) "$totalAlerts Active" else "All On Track",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }
                        Text(
                            text = "Real-time automated turnaround time notification monitor",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp
                            )
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }
            }

            // Notification Permission Banner (if needed)
            if (!hasPermission) {
                Surface(
                    color = Color(0xFFFEF3C7),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsOff,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "System notifications are disabled. Enable permissions in Android Settings to receive popups when SLA is breached.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF92400E),
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Test Notification Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.sendTestNotification(isBreach = false) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("test_near_breach_btn"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = StatusAtRisk
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Test Near-Breach", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = { viewModel.sendTestNotification(isBreach = true) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("test_breached_btn"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusBreachedTat,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Test Breach Alert", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Alert List Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (totalAlerts == 0) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = StatusWithinTatContainer)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(StatusWithinTat),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "All Tasks On Track!",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = StatusWithinTat
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "No customer tasks are currently in danger of SLA breach. Notifications will automatically fire when any ticket nears its deadline.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFF065F46),
                                        fontSize = 12.sp
                                    ),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Breached Tasks Section
                if (breachedTasks.isNotEmpty()) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = StatusBreachedTat,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "CRITICAL: SLA BREACHED (${breachedTasks.size})",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = StatusBreachedTat,
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }
                    }

                    items(breachedTasks, key = { "breach_${it.id}" }) { task ->
                        val unitName = units.find { it.id == task.unitId }?.name ?: "CX Unit"
                        AlertTaskCard(
                            task = task,
                            unitName = unitName,
                            isBreached = true,
                            onResolve = {
                                viewModel.updateTaskStatus(
                                    task = task,
                                    newStatus = TaskStatus.COMPLETED,
                                    resolutionRemarks = "Resolved via TAT SLA Alert Center"
                                )
                            },
                            onViewTask = {
                                onNavigateToTask(task)
                                onDismiss()
                            }
                        )
                    }
                }

                // Near Breach Tasks Section
                if (nearBreachTasks.isNotEmpty()) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 12.dp, bottom = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = StatusAtRisk,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "WARNING: APPROACHING BREACH (${nearBreachTasks.size})",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = StatusAtRisk,
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }
                    }

                    items(nearBreachTasks, key = { "near_${it.id}" }) { task ->
                        val unitName = units.find { it.id == task.unitId }?.name ?: "CX Unit"
                        AlertTaskCard(
                            task = task,
                            unitName = unitName,
                            isBreached = false,
                            onResolve = {
                                viewModel.updateTaskStatus(
                                    task = task,
                                    newStatus = TaskStatus.COMPLETED,
                                    resolutionRemarks = "Resolved via TAT Alert Center"
                                )
                            },
                            onViewTask = {
                                onNavigateToTask(task)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertTaskCard(
    task: CxTask,
    unitName: String,
    isBreached: Boolean,
    onResolve: () -> Unit,
    onViewTask: () -> Unit
) {
    val now = System.currentTimeMillis()
    val breachHours = task.getBreachHours(now)
    val remainingMinutes = task.getRemainingMinutes(now)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewTask() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isBreached) StatusBreachedTatContainer.copy(alpha = 0.7f) else StatusAtRiskContainer.copy(alpha = 0.7f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isBreached) StatusBreachedTat.copy(alpha = 0.5f) else StatusAtRisk.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isBreached) StatusBreachedTat else StatusAtRisk)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isBreached) "BREACHED +${String.format("%.1fh", breachHours)}" else "DUE IN ${remainingMinutes}m",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 10.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = task.trackingNumber,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                Spacer(modifier = Modifier.weight(1f))

                // Priority
                Text(
                    text = task.priority.displayName.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = when (task.priority) {
                            TaskPriority.CRITICAL -> StatusBreachedTat
                            TaskPriority.HIGH -> StatusAtRisk
                            else -> MaterialTheme.colorScheme.primary
                        },
                        fontSize = 10.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(
                    text = "$unitName • SLA: ${task.tatHours.toInt()}h",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                )
                if (!task.customerAccountOrTicket.isNullOrBlank()) {
                    Text(
                        text = " • Ref: ${task.customerAccountOrTicket}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onViewTask,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(32.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
                ) {
                    Text("View Task", fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onResolve,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(32.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HblPrimary
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Mark Resolved", fontSize = 11.sp, color = Color.White)
                }
            }
        }
    }
}
