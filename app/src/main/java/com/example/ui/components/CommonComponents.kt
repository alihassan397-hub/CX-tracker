package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.data.entity.TaskPriority
import com.example.data.entity.TaskStatus
import com.example.data.entity.TatStatus
import com.example.ui.theme.HblLime
import com.example.ui.theme.HblOnLime
import com.example.ui.theme.HblPrimary
import com.example.ui.theme.HblPrimaryDark
import com.example.ui.theme.HblPrimaryLight
import com.example.ui.theme.StatusAtRisk
import com.example.ui.theme.StatusAtRiskContainer
import com.example.ui.theme.StatusBreachedTat
import com.example.ui.theme.StatusBreachedTatContainer
import com.example.ui.theme.StatusInProgress
import com.example.ui.theme.StatusInProgressContainer
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusPendingContainer
import com.example.ui.theme.StatusToDo
import com.example.ui.theme.StatusToDoContainer
import com.example.ui.theme.StatusWithinTat
import com.example.ui.theme.StatusWithinTatContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HblBrandHeader(
    modifier: Modifier = Modifier,
    title: String = "CX Tracker",
    subtitle: String = "Customer Experience Department • Task & SLA Performance Hub"
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("hbl_brand_header"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            HblPrimaryDark,
                            HblPrimary,
                            HblPrimaryLight
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Unique Customer Experience (CX) Emblem Box
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.18f))
                        .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_cx_experience_logo),
                        contentDescription = "CX Tracker Logo",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 0.3.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(HblLime)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "CX HUB",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = HblOnLime,
                                    fontSize = 9.sp
                                )
                            )
                        }
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 11.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun TatBadge(
    tatStatus: TatStatus,
    modifier: Modifier = Modifier,
    showIcon: Boolean = true,
    compact: Boolean = false
) {
    val (bgColor, textColor, icon) = when (tatStatus) {
        TatStatus.WITHIN_TAT -> Triple(StatusWithinTatContainer, StatusWithinTat, Icons.Default.CheckCircle)
        TatStatus.AT_RISK -> Triple(StatusAtRiskContainer, StatusAtRisk, Icons.Default.Warning)
        TatStatus.BREACHED_TAT -> Triple(StatusBreachedTatContainer, StatusBreachedTat, Icons.Default.ErrorOutline)
    }

    Surface(
        modifier = modifier.testTag("tat_badge_${tatStatus.name}"),
        shape = RoundedCornerShape(20.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, textColor.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 6.dp else 10.dp,
                vertical = if (compact) 2.dp else 4.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (showIcon) {
                Icon(
                    imageVector = icon,
                    contentDescription = tatStatus.displayName,
                    tint = textColor,
                    modifier = Modifier.size(if (compact) 12.dp else 14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = if (compact) {
                    when (tatStatus) {
                        TatStatus.WITHIN_TAT -> "Within TAT"
                        TatStatus.AT_RISK -> "At Risk"
                        TatStatus.BREACHED_TAT -> "Breached"
                    }
                } else tatStatus.displayName,
                color = textColor,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = if (compact) 10.sp else 11.sp
                )
            )
        }
    }
}

@Composable
fun StatusBadge(
    status: TaskStatus,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val (bgColor, textColor, icon) = when (status) {
        TaskStatus.TO_DO -> Triple(StatusToDoContainer, StatusToDo, Icons.Default.AccessTime)
        TaskStatus.IN_PROGRESS -> Triple(StatusInProgressContainer, StatusInProgress, Icons.Default.PendingActions)
        TaskStatus.PENDING -> Triple(StatusPendingContainer, StatusPending, Icons.Default.Warning)
        TaskStatus.COMPLETED -> Triple(StatusWithinTatContainer, StatusWithinTat, Icons.Default.CheckCircle)
    }

    Surface(
        modifier = modifier.testTag("status_badge_${status.name}"),
        shape = RoundedCornerShape(6.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 6.dp else 8.dp,
                vertical = if (compact) 2.dp else 3.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = status.displayName,
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = status.displayName,
                color = textColor,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = if (compact) 10.sp else 11.sp
                )
            )
        }
    }
}

@Composable
fun PriorityBadge(
    priority: TaskPriority,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (priority) {
        TaskPriority.CRITICAL -> Pair(Color(0xFFFEE2E2), Color(0xFFDC2626))
        TaskPriority.HIGH -> Pair(Color(0xFFFFEDD5), Color(0xFFEA580C))
        TaskPriority.MEDIUM -> Pair(Color(0xFFDBEAFE), Color(0xFF2563EB))
        TaskPriority.LOW -> Pair(Color(0xFFF3F4F6), Color(0xFF4B5563))
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = bgColor
    ) {
        Text(
            text = priority.displayName.uppercase(),
            color = textColor,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp
            ),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun CxKpiCard(
    title: String,
    value: String,
    subtitle: String? = null,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    trendPositive: Boolean? = null
) {
    Card(
        modifier = modifier.testTag("kpi_card_${title.replace(" ", "_").lowercase()}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = when (trendPositive) {
                            true -> StatusWithinTat
                            false -> StatusBreachedTat
                            null -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = if (trendPositive != null) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 11.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun formatDurationMinutes(minutes: Int): String {
    val hours = minutes / 60
    val remMins = minutes % 60
    return if (hours > 0) {
        "${hours}h ${remMins}m"
    } else {
        "${remMins}m"
    }
}
