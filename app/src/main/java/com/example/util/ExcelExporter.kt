package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.entity.CxTask
import com.example.data.entity.CxUnit
import com.example.data.entity.TeamMember
import com.example.data.entity.TimeMotionLog
import com.example.data.model.CxDepartmentAnalytics
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExcelExporter {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val fileDateSuffix = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault())

    fun generateMasterReportCsv(
        tasks: List<CxTask>,
        units: List<CxUnit>,
        members: List<TeamMember>,
        analytics: CxDepartmentAnalytics,
        timeLogs: List<TimeMotionLog>,
        periodLabel: String = "All Time",
        dateFilterSummary: String = "All Time",
        unitFilterLabel: String = "All Units"
    ): String {
        val unitMap = units.associateBy { it.id }
        val memberMap = members.associateBy { it.id }
        val now = System.currentTimeMillis()

        val sb = StringBuilder()

        // 1. Executive Summary Header
        sb.append("=== CX TRACKER - CUSTOMER EXPERIENCE PERFORMANCE MASTER REPORT ===\n")
        sb.append("Generated At,${dateFormat.format(Date(now))}\n")
        sb.append("Reporting Date Filter,${escapeCsv(dateFilterSummary)}\n")
        sb.append("Selected Period,${escapeCsv(periodLabel)}\n")
        sb.append("Unit Scope,${escapeCsv(unitFilterLabel)}\n")
        sb.append("Total Filtered Tasks,${analytics.totalTasks}\n")
        sb.append("Overall SLA Adherence,${String.format(Locale.US, "%.1f%%", analytics.overallSlaPercent)}\n")
        sb.append("TAT Breach Rate,${String.format(Locale.US, "%.1f%%", analytics.breachRatePercent)}\n")
        sb.append("Completed Tasks,${analytics.completedTasks}\n")
        sb.append("In-Progress Tasks,${analytics.inProgressTasks}\n")
        sb.append("Pending Branch/Ops Info,${analytics.pendingTasks}\n")
        sb.append("Total Time-Motion Logged (Hours),${String.format(Locale.US, "%.1f", analytics.totalTimeMotionMinutes / 60.0)}\n")
        sb.append("Average Handling Time (Mins),${String.format(Locale.US, "%.1f", analytics.avgHandlingTimeMinutes)}\n\n")

        // 2. Unit Performance Table
        sb.append("=== UNIT PERFORMANCE ANALYSIS ===\n")
        sb.append("Unit Code,Unit Name,Unit Head,Total Tasks,Completed,In Progress,Pending,Within TAT,Breached TAT,Actual SLA %,Target SLA %,Avg Turnaround (Hrs),Time-Motion (Hrs)\n")
        for (u in analytics.unitSummaries) {
            sb.append("\"${escapeCsv(u.unit.code)}\",")
            sb.append("\"${escapeCsv(u.unit.name)}\",")
            sb.append("\"${escapeCsv(u.unit.unitHeadName)}\",")
            sb.append("${u.totalTasks},")
            sb.append("${u.completedCount},")
            sb.append("${u.inProgressCount},")
            sb.append("${u.pendingCount},")
            sb.append("${u.withinTatCount},")
            sb.append("${u.breachedTatCount},")
            sb.append("${String.format(Locale.US, "%.1f%%", u.slaPercent)},")
            sb.append("${String.format(Locale.US, "%.1f%%", u.targetSlaPercent)},")
            sb.append("${String.format(Locale.US, "%.1f", u.avgResolutionHours)},")
            sb.append("${String.format(Locale.US, "%.1f", u.totalTimeMotionHours)}\n")
        }
        sb.append("\n")

        // 3. Team Member Scorecard Table
        sb.append("=== TEAM MEMBER PERFORMANCE SCORECARDS ===\n")
        sb.append("Emp ID,Full Name,Unit,Role,Assigned,Completed,In Progress,Pending,Within TAT,Breached,SLA %,Time-Motion (Hrs),Avg AHT (Mins),Productivity Score,Rating\n")
        for (m in analytics.memberSummaries) {
            sb.append("\"${escapeCsv(m.member.employeeId)}\",")
            sb.append("\"${escapeCsv(m.member.fullName)}\",")
            sb.append("\"${escapeCsv(m.unitCode)}\",")
            sb.append("\"${escapeCsv(m.member.role)}\",")
            sb.append("${m.assignedCount},")
            sb.append("${m.completedCount},")
            sb.append("${m.inProgressCount},")
            sb.append("${m.pendingCount},")
            sb.append("${m.withinTatCount},")
            sb.append("${m.breachedCount},")
            sb.append("${String.format(Locale.US, "%.1f%%", m.slaPercent)},")
            sb.append("${String.format(Locale.US, "%.1f", m.totalTimeMotionHours)},")
            sb.append("${String.format(Locale.US, "%.1f", m.avgHandlingMinutes)},")
            sb.append("${String.format(Locale.US, "%.1f", m.productivityScore)},")
            sb.append("\"${escapeCsv(m.tierRating)}\"\n")
        }
        sb.append("\n")

        // 4. Master Task Tracker Table
        sb.append("=== MASTER TASK & TAT TRACKER LOG ===\n")
        sb.append("Task ID,Tracking No,Title,Unit,Assignee,Priority,Category,Status,Assigned Date,Target Due Date,Completed Date,TAT Target (Hrs),Actual Turnaround (Hrs),TAT Status,Breach Duration (Hrs),Time-Motion (Mins),Account/Ticket,Breach/Pending Reason,Resolution Remarks\n")
        for (task in tasks) {
            val unit = unitMap[task.unitId]?.code ?: "CX"
            val assignee = memberMap[task.assigneeId]?.fullName ?: "Unassigned"
            val tatState = task.computeTatStatus(now).displayName
            val actualHrs = task.getActualTurnaroundHours(now)
            val breachHrs = task.getBreachHours(now)
            val assignedDateStr = dateFormat.format(Date(task.assignedAt))
            val dueDateStr = dateFormat.format(Date(task.dueDateTime))
            val completedDateStr = task.completedAt?.let { dateFormat.format(Date(it)) } ?: "In-Flight"
            val pendingOrBreach = task.pendingReason ?: task.breachReason ?: ""

            sb.append("${task.id},")
            sb.append("\"${escapeCsv(task.trackingNumber)}\",")
            sb.append("\"${escapeCsv(task.title)}\",")
            sb.append("\"${escapeCsv(unit)}\",")
            sb.append("\"${escapeCsv(assignee)}\",")
            sb.append("\"${escapeCsv(task.priority.displayName)}\",")
            sb.append("\"${escapeCsv(task.category)}\",")
            sb.append("\"${escapeCsv(task.status.displayName)}\",")
            sb.append("\"$assignedDateStr\",")
            sb.append("\"$dueDateStr\",")
            sb.append("\"$completedDateStr\",")
            sb.append("${task.tatHours},")
            sb.append("${String.format(Locale.US, "%.1f", actualHrs)},")
            sb.append("\"${escapeCsv(tatState)}\",")
            sb.append("${String.format(Locale.US, "%.1f", breachHrs)},")
            sb.append("${task.timeMotionMinutes},")
            sb.append("\"${escapeCsv(task.customerAccountOrTicket ?: "")}\",")
            sb.append("\"${escapeCsv(pendingOrBreach)}\",")
            sb.append("\"${escapeCsv(task.resolutionRemarks ?: "")}\"\n")
        }
        sb.append("\n")

        // 5. Time Motion Activity Breakdown
        sb.append("=== TIME-MOTION ACTIVITY LOGS ===\n")
        sb.append("Log ID,Task ID,Activity Type,Duration (Mins),Logged At,Notes\n")
        for (log in timeLogs) {
            sb.append("${log.id},")
            sb.append("${log.taskId},")
            sb.append("\"${escapeCsv(log.activityType)}\",")
            sb.append("${log.durationMinutes},")
            sb.append("\"${dateFormat.format(Date(log.loggedAt))}\",")
            sb.append("\"${escapeCsv(log.notes)}\"\n")
        }

        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        return value.replace("\"", "\"\"")
    }

    fun exportAndShareCsv(
        context: Context,
        csvContent: String,
        fileNamePrefix: String = "HBL_CX_Performance_Report"
    ): Boolean {
        return try {
            val exportDir = File(context.cacheDir, "reports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val fileName = "${fileNamePrefix}_${fileDateSuffix.format(Date())}.csv"
            val file = File(exportDir, fileName)

            FileOutputStream(file).use { out ->
                // Write UTF-8 BOM so Excel opens Urdu/special chars cleanly
                out.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                out.write(csvContent.toByteArray(Charsets.UTF_8))
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "HBL MFB Customer Experience Performance Report")
                putExtra(Intent.EXTRA_TEXT, "Attached is the latest Customer Experience Task & TAT Performance Analysis report for HBL Microfinance Bank.")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Download / Share CX Performance Report")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
