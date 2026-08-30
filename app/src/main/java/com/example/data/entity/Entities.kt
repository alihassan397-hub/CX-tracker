package com.example.data.entity

/**
 * CX Department Unit in HBL Microfinance Bank
 * e.g., Complaints Management Unit (CMU), Contact Center Operations,
 * Service Quality & Branch Experience, QA & Calibration, Digital CX, Retention & Escalation.
 *
 * Stored in Firestore collection "units".
 */
data class CxUnit(
    val id: Long = 0,
    val name: String = "",
    val code: String = "",
    val description: String = "",
    val unitHeadName: String = "",
    val headEmail: String = "",
    val colorHex: String = "",
    val defaultTatHours: Double = 24.0,
    val targetSlaPercent: Double = 95.0,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Team Member in a CX Unit. Stored in Firestore collection "teamMembers".
 */
data class TeamMember(
    val id: Long = 0,
    val unitId: Long = 0,
    val fullName: String = "",
    val employeeId: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "", // "Unit Head", "Manager", "CX Specialist", "QA Analyst", "Resolution Officer", "Branch Auditor"
    val designation: String = "",
    val avatarColorHex: String = "#008269",
    val dailyCapacityHours: Double = 8.0,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Task Status Enums and Priorities
 */
enum class TaskStatus(val displayName: String) {
    TO_DO("To Do"),
    IN_PROGRESS("In Progress"),
    PENDING("Pending Info/Branch"),
    COMPLETED("Completed")
}

enum class TaskPriority(val displayName: String, val colorHex: String) {
    CRITICAL("Critical", "#DC2626"),
    HIGH("High", "#EA580C"),
    MEDIUM("Medium", "#2563EB"),
    LOW("Low", "#4B5563")
}

enum class TatStatus(val displayName: String, val colorHex: String) {
    WITHIN_TAT("Within TAT", "#059669"),
    AT_RISK("At Risk (< 25% Time)", "#D97706"),
    BREACHED_TAT("Breached TAT", "#DC2626")
}

/**
 * Task assigned to a CX Unit / Team Member with defined Turnaround Time (TAT).
 * Stored in Firestore collection "tasks". Breach/at-risk status is also
 * periodically recomputed server-side by a scheduled Cloud Function so that
 * the SLA dashboard is consistent across every device, not just whichever
 * phone happens to be open (see /functions/index.js).
 */
data class CxTask(
    val id: Long = 0,
    val trackingNumber: String = "",
    val title: String = "",
    val description: String = "",
    val unitId: Long = 0,
    val assigneeId: Long = 0,
    val assignedByName: String = "CX Manager",
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val category: String = "",
    val status: TaskStatus = TaskStatus.TO_DO,
    val createdAt: Long = System.currentTimeMillis(),
    val assignedAt: Long = System.currentTimeMillis(),
    val tatHours: Double = 24.0,
    val dueDateTime: Long = System.currentTimeMillis() + (24 * 3600000L),
    val completedAt: Long? = null,
    val timeMotionMinutes: Int = 0,
    val pendingReason: String? = null,
    val resolutionRemarks: String? = null,
    val customerAccountOrTicket: String? = null,
    val breachReason: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    /**
     * Compute current TAT Status dynamically (client-side quick estimate).
     * The scheduled Cloud Function performs the authoritative server-side
     * recomputation so all devices agree even if one phone's clock is wrong
     * or the app was closed when the deadline passed.
     */
    fun computeTatStatus(currentTime: Long = System.currentTimeMillis()): TatStatus {
        if (status == TaskStatus.COMPLETED) {
            val resolvedTime = completedAt ?: lastUpdated
            return if (resolvedTime <= dueDateTime) TatStatus.WITHIN_TAT else TatStatus.BREACHED_TAT
        }

        if (currentTime > dueDateTime) {
            return TatStatus.BREACHED_TAT
        }

        val totalDurationMs = (dueDateTime - assignedAt).coerceAtLeast(1L)
        val remainingMs = dueDateTime - currentTime
        val remainingRatio = remainingMs.toDouble() / totalDurationMs.toDouble()

        return if (remainingRatio <= 0.25) {
            TatStatus.AT_RISK
        } else {
            TatStatus.WITHIN_TAT
        }
    }

    fun getRemainingMinutes(currentTime: Long = System.currentTimeMillis()): Long {
        val remainingMs = dueDateTime - currentTime
        return (remainingMs / 60000L).coerceAtLeast(0L)
    }

    fun getActualTurnaroundHours(currentTime: Long = System.currentTimeMillis()): Double {
        val endTime = completedAt ?: currentTime
        val durationMs = (endTime - assignedAt).coerceAtLeast(0L)
        return durationMs / 3600000.0
    }

    fun getBreachHours(currentTime: Long = System.currentTimeMillis()): Double {
        val endTime = completedAt ?: currentTime
        return if (endTime > dueDateTime) {
            (endTime - dueDateTime) / 3600000.0
        } else {
            0.0
        }
    }
}

/**
 * Time and Motion Activity Log for a task. Stored in Firestore collection "timeMotionLogs".
 */
data class TimeMotionLog(
    val id: Long = 0,
    val taskId: Long = 0,
    val memberId: Long = 0,
    val activityType: String = "",
    val durationMinutes: Int = 0,
    val loggedAt: Long = System.currentTimeMillis(),
    val notes: String = ""
)

/**
 * User Roles for Access Control & Rights
 */
enum class UserRole(val displayName: String, val badgeColorHex: String) {
    ADMIN("Customer Experience Unit Head", "#008269"),
    UNIT_HEAD("Customer Experience Unit Head", "#008269"),
    TEAM_MEMBER("Team Member", "#0284C7"),
    USER("Team Member", "#0284C7")
}

/**
 * Registered User Account & role. Stored in Firestore collection "users",
 * one document per account, document ID = id.toString().
 *
 * SECURITY NOTE: this class intentionally has NO password field. Passwords
 * are managed entirely by Firebase Authentication (hashed & stored by
 * Google's infrastructure) — the app never stores or compares a plaintext
 * password itself. `authUid` links this profile to the Firebase Auth account.
 */
data class UserAccount(
    val id: Long = 0,
    val authUid: String = "",
    val fullName: String = "",
    val email: String = "",
    val role: String = UserRole.TEAM_MEMBER.name, // "ADMIN", "UNIT_HEAD", "TEAM_MEMBER", "USER"
    val unitId: Long? = null, // null for Department Head
    val employeeId: String = "CX-000",
    val designation: String = "CX Specialist",
    val phone: String = "",
    val avatarColorHex: String = "#008269",
    val createdAt: Long = System.currentTimeMillis()
) {
    val isUnitHead: Boolean
        // Authority comes ONLY from the server-verified `role` field, which is
        // itself only ever set by trusted seeding or an authenticated Unit
        // Head promoting someone — never by self-service sign-up. Firestore
        // Security Rules enforce this same check server-side (see firestore.rules),
        // so even a modified/rebuilt client app cannot bypass it.
        get() = role == UserRole.UNIT_HEAD.name || role == UserRole.ADMIN.name

    val isSuperAdmin: Boolean
        get() = isUnitHead

    val isTeamMember: Boolean
        get() = !isUnitHead

    val isRegularUser: Boolean
        get() = !isUnitHead
}

/**
 * User's Daily Logged Tasks with Automated Performance Indicators.
 * Stored in Firestore collection "dailyTasks".
 */
data class DailyTaskEntry(
    val id: Long = 0,
    val userId: Long = 0,
    val userName: String = "",
    val unitId: Long = 0,
    val title: String = "",
    val category: String = "",
    val dateString: String = "", // "YYYY-MM-DD"
    val timestamp: Long = System.currentTimeMillis(),
    val hoursSpent: Double = 1.0,
    val tasksCount: Int = 1,
    val status: TaskStatus = TaskStatus.COMPLETED,
    val tatStatus: TatStatus = TatStatus.WITHIN_TAT,
    val qualityScorePercent: Double = 95.0,
    val fcrResolved: Boolean = true,
    val impactMetric: String = "SLA Turnaround",
    val notes: String = ""
)
