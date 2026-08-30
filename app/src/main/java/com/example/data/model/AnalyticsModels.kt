package com.example.data.model

import com.example.data.entity.CxTask
import com.example.data.entity.CxUnit
import com.example.data.entity.TaskPriority
import com.example.data.entity.TaskStatus
import com.example.data.entity.TatStatus
import com.example.data.entity.TeamMember

data class UnitPerformanceSummary(
    val unit: CxUnit,
    val totalTasks: Int,
    val completedCount: Int,
    val inProgressCount: Int,
    val pendingCount: Int,
    val toDoCount: Int,
    val withinTatCount: Int,
    val atRiskCount: Int,
    val breachedTatCount: Int,
    val slaPercent: Double,
    val targetSlaPercent: Double,
    val avgResolutionHours: Double,
    val totalTimeMotionHours: Double,
    val activeMembersCount: Int
)

data class MemberPerformanceSummary(
    val member: TeamMember,
    val unitName: String,
    val unitCode: String,
    val assignedCount: Int,
    val completedCount: Int,
    val inProgressCount: Int,
    val pendingCount: Int,
    val toDoCount: Int,
    val withinTatCount: Int,
    val breachedCount: Int,
    val slaPercent: Double,
    val totalTimeMotionHours: Double,
    val avgHandlingMinutes: Double,
    val productivityScore: Double,
    val tierRating: String
)

data class CxDepartmentAnalytics(
    val totalTasks: Int,
    val completedTasks: Int,
    val inProgressTasks: Int,
    val pendingTasks: Int,
    val toDoTasks: Int,
    val withinTatCount: Int,
    val atRiskCount: Int,
    val breachedTatCount: Int,
    val overallSlaPercent: Double,
    val breachRatePercent: Double,
    val avgTurnaroundHours: Double,
    val totalTimeMotionMinutes: Int,
    val avgHandlingTimeMinutes: Double,
    val criticalBreachesCount: Int,
    val unitSummaries: List<UnitPerformanceSummary>,
    val memberSummaries: List<MemberPerformanceSummary>,
    val activityDistribution: Map<String, Int>,
    val breachReasonDistribution: Map<String, Int>,
    val statusCounts: Map<TaskStatus, Int>,
    val tatCounts: Map<TatStatus, Int>
)

enum class FilterUnit(val label: String) {
    ALL("All Units"),
    CMU("CMU"),
    CCO("Contact Center"),
    SQE("Service Quality"),
    QAC("QA Calibration"),
    DCX("Digital CX"),
    RED("Retention & Escalations")
}

enum class FilterStatus(val label: String) {
    ALL("All Status"),
    TO_DO("To Do"),
    IN_PROGRESS("In Progress"),
    PENDING("Pending Info"),
    COMPLETED("Completed")
}

enum class FilterTat(val label: String) {
    ALL("All TAT"),
    WITHIN_TAT("Within TAT"),
    AT_RISK("At Risk (<25%)"),
    BREACHED("Breached TAT")
}
