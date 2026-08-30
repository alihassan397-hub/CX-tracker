package com.example.data.repository

import com.example.data.entity.CxTask
import com.example.data.entity.CxUnit
import com.example.data.entity.DailyTaskEntry
import com.example.data.entity.TaskPriority
import com.example.data.entity.TaskStatus
import com.example.data.entity.TatStatus
import com.example.data.entity.TeamMember
import com.example.data.entity.TimeMotionLog
import com.example.data.entity.UserAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

/**
 * CX Tracker data layer, backed by Cloud Firestore (shared, multi-device,
 * real-time) and Firebase Authentication (secure sign-in — no plaintext
 * passwords are ever stored by this app).
 *
 * IMPORTANT: this class keeps the exact same public API the old local-only
 * Room-backed repository had, so nothing in CxViewModel or the UI screens
 * needed to change shape — only the plumbing underneath changed, from a
 * single phone's SQLite file to a shared cloud database every team member's
 * and the Unit Head's device reads and writes to together.
 */
class CxRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    private val usersCol = firestore.collection("users")
    private val unitsCol = firestore.collection("units")
    private val teamMembersCol = firestore.collection("teamMembers")
    private val tasksCol = firestore.collection("tasks")
    private val timeMotionCol = firestore.collection("timeMotionLogs")
    private val dailyTasksCol = firestore.collection("dailyTasks")

    /** Generates a Long id that is effectively unique for this app's scale (small team). */
    private fun newId(): Long = System.currentTimeMillis() * 1000L + Random.nextInt(1000)

    private fun <T> collectionFlow(
        query: Query,
        mapper: (Map<String, Any?>) -> T?
    ): Flow<List<T>> = callbackFlow {
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc -> doc.data?.let(mapper) } ?: emptyList()
            trySend(list)
        }
        awaitClose { registration.remove() }
    }

    // ==================== User Accounts & Authentication ====================

    val allUserAccounts: Flow<List<UserAccount>> =
        collectionFlow(usersCol, ::mapToUserAccount)

    suspend fun getUserByEmail(email: String): UserAccount? {
        val snap = usersCol.whereEqualTo("email", email.trim().lowercase()).limit(1).get().await()
        return snap.documents.firstOrNull()?.data?.let(::mapToUserAccount)
    }

    suspend fun getUserById(id: Long): UserAccount? {
        val doc = usersCol.document(id.toString()).get().await()
        return doc.data?.let(::mapToUserAccount)
    }

    suspend fun insertUserAccount(user: UserAccount): Long {
        val id = if (user.id != 0L) user.id else newId()
        val toSave = user.copy(id = id, email = user.email.trim().lowercase())
        // NOTE: the users_by_uid/{authUid} mirror used by Firestore Security Rules
        // is written by a Cloud Function trigger (onUserProfileWritten), never
        // directly by the client — see functions/index.js. If the client wrote it
        // directly, a user could fake an "admin" mirror doc and bypass the rules
        // that trust it.
        usersCol.document(id.toString()).set(userToMap(toSave)).await()
        return id
    }

    suspend fun updateUserAccount(user: UserAccount) {
        usersCol.document(user.id.toString()).set(userToMap(user)).await()
    }

    suspend fun deleteUserAccount(user: UserAccount) {
        usersCol.document(user.id.toString()).delete().await()
    }

    suspend fun deleteUserAccountById(userId: Long) {
        usersCol.document(userId.toString()).delete().await()
    }

    // ---- Firebase Authentication (real, secure sign-in) ----

    /** Creates a Firebase Auth account. Returns the new account's UID on success. */
    suspend fun firebaseSignUp(email: String, password: String): Result<String> = try {
        val result = auth.createUserWithEmailAndPassword(email.trim().lowercase(), password).await()
        Result.success(result.user?.uid.orEmpty())
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun firebaseSignIn(email: String, password: String): Result<String> = try {
        val result = auth.signInWithEmailAndPassword(email.trim().lowercase(), password).await()
        Result.success(result.user?.uid.orEmpty())
    } catch (e: Exception) {
        Result.failure(e)
    }

    /** Sends a real password-reset email via Firebase — no one can silently overwrite another user's password anymore. */
    suspend fun firebaseSendPasswordReset(email: String): Result<Unit> = try {
        auth.sendPasswordResetEmail(email.trim().lowercase()).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun firebaseSignOut() {
        auth.signOut()
    }

    fun firebaseCurrentUid(): String? = auth.currentUser?.uid

    // ==================== Daily Tasks & Performance Logging ====================

    val allDailyTasks: Flow<List<DailyTaskEntry>> =
        collectionFlow(dailyTasksCol.orderBy("timestamp", Query.Direction.DESCENDING), ::mapToDailyTask)

    fun getDailyTasksByUser(userId: Long): Flow<List<DailyTaskEntry>> =
        collectionFlow(dailyTasksCol.whereEqualTo("userId", userId), ::mapToDailyTask)

    fun getDailyTasksByUnit(unitId: Long): Flow<List<DailyTaskEntry>> =
        collectionFlow(dailyTasksCol.whereEqualTo("unitId", unitId), ::mapToDailyTask)

    fun getDailyTasksByDate(dateString: String): Flow<List<DailyTaskEntry>> =
        collectionFlow(dailyTasksCol.whereEqualTo("dateString", dateString), ::mapToDailyTask)

    suspend fun getDailyTaskById(id: Long): DailyTaskEntry? {
        val doc = dailyTasksCol.document(id.toString()).get().await()
        return doc.data?.let(::mapToDailyTask)
    }

    suspend fun insertDailyTask(task: DailyTaskEntry): Long {
        val id = if (task.id != 0L) task.id else newId()
        dailyTasksCol.document(id.toString()).set(dailyTaskToMap(task.copy(id = id))).await()
        return id
    }

    suspend fun updateDailyTask(task: DailyTaskEntry) {
        dailyTasksCol.document(task.id.toString()).set(dailyTaskToMap(task)).await()
    }

    suspend fun deleteDailyTask(task: DailyTaskEntry) {
        dailyTasksCol.document(task.id.toString()).delete().await()
    }

    suspend fun deleteDailyTaskById(id: Long) {
        dailyTasksCol.document(id.toString()).delete().await()
    }

    suspend fun deleteDailyTasksByUnit(unitId: Long) {
        val snap = dailyTasksCol.whereEqualTo("unitId", unitId).get().await()
        snap.documents.forEach { it.reference.delete().await() }
    }

    // ==================== CX Units ====================

    val allUnits: Flow<List<CxUnit>> =
        collectionFlow(unitsCol, ::mapToUnit)

    suspend fun getUnitById(id: Long): CxUnit? {
        val doc = unitsCol.document(id.toString()).get().await()
        return doc.data?.let(::mapToUnit)
    }

    suspend fun insertUnit(unit: CxUnit): Long {
        val id = if (unit.id != 0L) unit.id else newId()
        unitsCol.document(id.toString()).set(unitToMap(unit.copy(id = id))).await()
        return id
    }

    suspend fun updateUnit(unit: CxUnit) {
        unitsCol.document(unit.id.toString()).set(unitToMap(unit)).await()
    }

    suspend fun deleteUnit(unit: CxUnit) {
        unitsCol.document(unit.id.toString()).delete().await()
    }

    // ==================== Team Members ====================

    val allTeamMembers: Flow<List<TeamMember>> =
        collectionFlow(teamMembersCol.whereEqualTo("isActive", true), ::mapToTeamMember)

    fun getTeamMembersByUnit(unitId: Long): Flow<List<TeamMember>> =
        collectionFlow(teamMembersCol.whereEqualTo("unitId", unitId).whereEqualTo("isActive", true), ::mapToTeamMember)

    suspend fun getTeamMemberById(id: Long): TeamMember? {
        val doc = teamMembersCol.document(id.toString()).get().await()
        return doc.data?.let(::mapToTeamMember)
    }

    suspend fun insertTeamMember(member: TeamMember): Long {
        val id = if (member.id != 0L) member.id else newId()
        teamMembersCol.document(id.toString()).set(teamMemberToMap(member.copy(id = id))).await()
        return id
    }

    suspend fun updateTeamMember(member: TeamMember) {
        teamMembersCol.document(member.id.toString()).set(teamMemberToMap(member)).await()
    }

    suspend fun deleteTeamMember(member: TeamMember) {
        teamMembersCol.document(member.id.toString()).delete().await()
    }

    // ==================== CX Tasks ====================

    val allTasks: Flow<List<CxTask>> =
        collectionFlow(tasksCol.orderBy("createdAt", Query.Direction.DESCENDING), ::mapToTask)

    fun getTasksByUnit(unitId: Long): Flow<List<CxTask>> =
        collectionFlow(tasksCol.whereEqualTo("unitId", unitId), ::mapToTask)

    fun getTasksByMember(memberId: Long): Flow<List<CxTask>> =
        collectionFlow(tasksCol.whereEqualTo("assigneeId", memberId), ::mapToTask)

    suspend fun getTaskById(id: Long): CxTask? {
        val doc = tasksCol.document(id.toString()).get().await()
        return doc.data?.let(::mapToTask)
    }

    suspend fun insertTask(task: CxTask): Long {
        val id = if (task.id != 0L) task.id else newId()
        tasksCol.document(id.toString()).set(taskToMap(task.copy(id = id))).await()
        return id
    }

    suspend fun updateTask(task: CxTask) {
        tasksCol.document(task.id.toString()).set(taskToMap(task)).await()
    }

    suspend fun deleteTask(task: CxTask) {
        tasksCol.document(task.id.toString()).delete().await()
    }

    suspend fun deleteTaskById(taskId: Long) {
        tasksCol.document(taskId.toString()).delete().await()
    }

    // ==================== Time Motion Logs ====================

    val allTimeMotionLogs: Flow<List<TimeMotionLog>> =
        collectionFlow(timeMotionCol.orderBy("loggedAt", Query.Direction.DESCENDING), ::mapToLog)

    fun getLogsForTask(taskId: Long): Flow<List<TimeMotionLog>> =
        collectionFlow(timeMotionCol.whereEqualTo("taskId", taskId), ::mapToLog)

    suspend fun insertTimeMotionLog(log: TimeMotionLog): Long {
        val id = if (log.id != 0L) log.id else newId()
        timeMotionCol.document(id.toString()).set(logToMap(log.copy(id = id))).await()
        return id
    }

    // ==================== Quick Task Update Actions ====================

    suspend fun updateTaskStatus(
        task: CxTask,
        newStatus: TaskStatus,
        resolutionRemarks: String? = null,
        pendingReason: String? = null,
        breachReason: String? = null
    ) {
        val now = System.currentTimeMillis()
        val updatedTask = task.copy(
            status = newStatus,
            completedAt = if (newStatus == TaskStatus.COMPLETED) (task.completedAt ?: now) else null,
            resolutionRemarks = resolutionRemarks ?: task.resolutionRemarks,
            pendingReason = if (newStatus == TaskStatus.PENDING) (pendingReason ?: task.pendingReason) else null,
            breachReason = breachReason ?: task.breachReason,
            lastUpdated = now
        )
        updateTask(updatedTask)
    }

    suspend fun logTimeMotion(taskId: Long, memberId: Long, durationMinutes: Int, activityType: String, notes: String) {
        val log = TimeMotionLog(
            taskId = taskId,
            memberId = memberId,
            activityType = activityType,
            durationMinutes = durationMinutes,
            notes = notes
        )
        insertTimeMotionLog(log)

        val task = getTaskById(taskId)
        if (task != null) {
            updateTask(
                task.copy(
                    timeMotionMinutes = task.timeMotionMinutes + durationMinutes,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun deleteAllTasks() {
        tasksCol.get().await().documents.forEach { it.reference.delete().await() }
        timeMotionCol.get().await().documents.forEach { it.reference.delete().await() }
    }

    suspend fun clearAllData() {
        deleteAllTasks()
        teamMembersCol.get().await().documents.forEach { it.reference.delete().await() }
        unitsCol.get().await().documents.forEach { it.reference.delete().await() }
    }

    // ==================== Seed / Demo Data ====================

    /** Seeds CX Units + the team roster once, on first launch, if they don't exist yet. Safe to call every app start. */
    suspend fun seedInitialDataIfNeeded() {
        try {
            val existing = unitsCol.limit(1).get().await()
            if (!existing.isEmpty) return
            seedUnitsAndRoster()
        } catch (e: Exception) {
            // Expected before anyone has signed in yet — Firestore rules require
            // authentication to read/write, so this simply retries (successfully)
            // the next time the app starts after someone has logged in. Must NOT
            // propagate, or it crashes the app on first open before login.
        }
    }

    /** Used by the "reset to sample data" debug action. */
    suspend fun resetToSampleData() {
        clearAllData()
        seedUnitsAndRoster()
    }

    private suspend fun seedUnitsAndRoster() {
        val units = listOf(
            CxUnit(1, "Service Quality", "SQ", "Branch mystery shopping, counter service standard audits, CSAT measurement & service TAT adherence", "Sabeen Shafique", "sabeen.shafique@example.com", "#008269", 24.0, 95.0),
            CxUnit(2, "CX Executives", "CXE", "Priority client experience touchpoints, high-value customer interactions & executive issue escalation", "Sabeen Shafique", "sabeen.shafique@example.com", "#0284C7", 12.0, 98.0),
            CxUnit(3, "Complaints Management Unit", "CMU", "Comprehensive grievance handling, dispute investigation, Banking Ombudsman cases & regulatory escalations", "Sabeen Shafique", "sabeen.shafique@example.com", "#D97706", 24.0, 96.0),
            CxUnit(4, "Interns", "INT", "Intern rotation desk, CX data processing, documentation indexing & operational support", "Sabeen Shafique", "sabeen.shafique@example.com", "#7C3AED", 8.0, 90.0),
            CxUnit(5, "Other Tasks", "OTH", "Cross-unit operational tasks, special assignments, ad-hoc CX projects & department initiatives", "Sabeen Shafique", "sabeen.shafique@example.com", "#059669", 24.0, 95.0)
        )
        units.forEach { insertUnit(it) }

        val members = listOf(
            TeamMember(1, 1, "Ajmal Hussain", "SQ-101", "ajmal.hussain@example.com", role = "Team Member", designation = "Senior Service Quality Lead", avatarColorHex = "#008269"),
            TeamMember(2, 1, "Hena Mursleen", "SQ-102", "hena.mursleen@example.com", role = "Team Member", designation = "Service Quality Specialist", avatarColorHex = "#0D9488"),
            TeamMember(3, 1, "Munirah", "SQ-103", "munirah@example.com", role = "Team Member", designation = "Service Quality Analyst", avatarColorHex = "#14B8A6"),
            TeamMember(4, 1, "Ali Hassan", "SQ-104", "ali.hassan@example.com", role = "Team Member", designation = "Service Quality Officer", avatarColorHex = "#0284C7"),
            TeamMember(5, 1, "Saira", "SQ-105", "saira@example.com", role = "Team Member", designation = "Service Quality Executive", avatarColorHex = "#6366F1"),
            TeamMember(6, 2, "CX Executives", "CXE-201", "cx.executives@example.com", role = "Team Member", designation = "Executive CX Desk", avatarColorHex = "#2563EB"),
            TeamMember(7, 2, "Saira", "CXE-202", "saira.cxe@example.com", role = "Team Member", designation = "Executive CX Officer", avatarColorHex = "#8B5CF6"),
            TeamMember(8, 3, "Nadia", "CMU-301", "nadia@example.com", role = "Team Member", designation = "CMU Senior Specialist", avatarColorHex = "#D97706"),
            TeamMember(9, 3, "Fahima", "CMU-302", "fahima@example.com", role = "Team Member", designation = "CMU Resolution Officer", avatarColorHex = "#EA580C"),
            TeamMember(10, 3, "Moh", "CMU-303", "moh@example.com", role = "Team Member", designation = "CMU Investigator", avatarColorHex = "#DC2626"),
            TeamMember(14, 4, "Intern 1", "INT-401", "intern1@example.com", role = "Team Member", designation = "Customer Experience Intern", avatarColorHex = "#7C3AED"),
            TeamMember(15, 4, "Intern 2", "INT-402", "intern2@example.com", role = "Team Member", designation = "Customer Experience Intern", avatarColorHex = "#9333EA"),
            TeamMember(16, 4, "Intern 3", "INT-403", "intern3@example.com", role = "Team Member", designation = "Customer Experience Intern", avatarColorHex = "#A855F7"),
            TeamMember(17, 5, "General CX Desk", "OTH-501", "general.cx@example.com", role = "Team Member", designation = "Cross-Functional Support", avatarColorHex = "#10B981")
        )
        members.forEach { insertTeamMember(it) }
    }

    // ==================== Mapping helpers (Firestore <-> Kotlin) ====================

    private fun asLong(v: Any?): Long = when (v) {
        is Long -> v
        is Int -> v.toLong()
        is Double -> v.toLong()
        is String -> v.toLongOrNull() ?: 0L
        else -> 0L
    }

    private fun asDouble(v: Any?): Double = when (v) {
        is Double -> v
        is Long -> v.toDouble()
        is Int -> v.toDouble()
        else -> 0.0
    }

    private fun asInt(v: Any?): Int = when (v) {
        is Long -> v.toInt()
        is Int -> v
        is Double -> v.toInt()
        else -> 0
    }

    private fun asBool(v: Any?): Boolean = v as? Boolean ?: true

    private fun mapToUnit(m: Map<String, Any?>): CxUnit = CxUnit(
        id = asLong(m["id"]),
        name = m["name"] as? String ?: "",
        code = m["code"] as? String ?: "",
        description = m["description"] as? String ?: "",
        unitHeadName = m["unitHeadName"] as? String ?: "",
        headEmail = m["headEmail"] as? String ?: "",
        colorHex = m["colorHex"] as? String ?: "",
        defaultTatHours = asDouble(m["defaultTatHours"]),
        targetSlaPercent = asDouble(m["targetSlaPercent"]),
        createdAt = asLong(m["createdAt"])
    )

    private fun unitToMap(u: CxUnit): Map<String, Any?> = mapOf(
        "id" to u.id, "name" to u.name, "code" to u.code, "description" to u.description,
        "unitHeadName" to u.unitHeadName, "headEmail" to u.headEmail, "colorHex" to u.colorHex,
        "defaultTatHours" to u.defaultTatHours, "targetSlaPercent" to u.targetSlaPercent, "createdAt" to u.createdAt
    )

    private fun mapToTeamMember(m: Map<String, Any?>): TeamMember = TeamMember(
        id = asLong(m["id"]),
        unitId = asLong(m["unitId"]),
        fullName = m["fullName"] as? String ?: "",
        employeeId = m["employeeId"] as? String ?: "",
        email = m["email"] as? String ?: "",
        phone = m["phone"] as? String ?: "",
        role = m["role"] as? String ?: "",
        designation = m["designation"] as? String ?: "",
        avatarColorHex = m["avatarColorHex"] as? String ?: "#008269",
        dailyCapacityHours = asDouble(m["dailyCapacityHours"]).let { if (it == 0.0) 8.0 else it },
        isActive = asBool(m["isActive"]),
        createdAt = asLong(m["createdAt"])
    )

    private fun teamMemberToMap(t: TeamMember): Map<String, Any?> = mapOf(
        "id" to t.id, "unitId" to t.unitId, "fullName" to t.fullName, "employeeId" to t.employeeId,
        "email" to t.email, "phone" to t.phone, "role" to t.role, "designation" to t.designation,
        "avatarColorHex" to t.avatarColorHex, "dailyCapacityHours" to t.dailyCapacityHours,
        "isActive" to t.isActive, "createdAt" to t.createdAt
    )

    private fun mapToTask(m: Map<String, Any?>): CxTask = CxTask(
        id = asLong(m["id"]),
        trackingNumber = m["trackingNumber"] as? String ?: "",
        title = m["title"] as? String ?: "",
        description = m["description"] as? String ?: "",
        unitId = asLong(m["unitId"]),
        assigneeId = asLong(m["assigneeId"]),
        assignedByName = m["assignedByName"] as? String ?: "CX Manager",
        priority = (m["priority"] as? String)?.let { runCatching { TaskPriority.valueOf(it) }.getOrNull() } ?: TaskPriority.MEDIUM,
        category = m["category"] as? String ?: "",
        status = (m["status"] as? String)?.let { runCatching { TaskStatus.valueOf(it) }.getOrNull() } ?: TaskStatus.TO_DO,
        createdAt = asLong(m["createdAt"]),
        assignedAt = asLong(m["assignedAt"]),
        tatHours = asDouble(m["tatHours"]),
        dueDateTime = asLong(m["dueDateTime"]),
        completedAt = m["completedAt"]?.let { asLong(it) },
        timeMotionMinutes = asInt(m["timeMotionMinutes"]),
        pendingReason = m["pendingReason"] as? String,
        resolutionRemarks = m["resolutionRemarks"] as? String,
        customerAccountOrTicket = m["customerAccountOrTicket"] as? String,
        breachReason = m["breachReason"] as? String,
        lastUpdated = asLong(m["lastUpdated"])
    )

    private fun taskToMap(t: CxTask): Map<String, Any?> = mapOf(
        "id" to t.id, "trackingNumber" to t.trackingNumber, "title" to t.title, "description" to t.description,
        "unitId" to t.unitId, "assigneeId" to t.assigneeId, "assignedByName" to t.assignedByName,
        "priority" to t.priority.name, "category" to t.category, "status" to t.status.name,
        "createdAt" to t.createdAt, "assignedAt" to t.assignedAt, "tatHours" to t.tatHours,
        "dueDateTime" to t.dueDateTime, "completedAt" to t.completedAt, "timeMotionMinutes" to t.timeMotionMinutes,
        "pendingReason" to t.pendingReason, "resolutionRemarks" to t.resolutionRemarks,
        "customerAccountOrTicket" to t.customerAccountOrTicket, "breachReason" to t.breachReason,
        "lastUpdated" to t.lastUpdated
    )

    private fun mapToLog(m: Map<String, Any?>): TimeMotionLog = TimeMotionLog(
        id = asLong(m["id"]),
        taskId = asLong(m["taskId"]),
        memberId = asLong(m["memberId"]),
        activityType = m["activityType"] as? String ?: "",
        durationMinutes = asInt(m["durationMinutes"]),
        loggedAt = asLong(m["loggedAt"]),
        notes = m["notes"] as? String ?: ""
    )

    private fun logToMap(l: TimeMotionLog): Map<String, Any?> = mapOf(
        "id" to l.id, "taskId" to l.taskId, "memberId" to l.memberId, "activityType" to l.activityType,
        "durationMinutes" to l.durationMinutes, "loggedAt" to l.loggedAt, "notes" to l.notes
    )

    private fun mapToUserAccount(m: Map<String, Any?>): UserAccount = UserAccount(
        id = asLong(m["id"]),
        authUid = m["authUid"] as? String ?: "",
        fullName = m["fullName"] as? String ?: "",
        email = m["email"] as? String ?: "",
        role = m["role"] as? String ?: "TEAM_MEMBER",
        unitId = m["unitId"]?.let { asLong(it) },
        employeeId = m["employeeId"] as? String ?: "CX-000",
        designation = m["designation"] as? String ?: "CX Specialist",
        phone = m["phone"] as? String ?: "",
        avatarColorHex = m["avatarColorHex"] as? String ?: "#008269",
        createdAt = asLong(m["createdAt"])
    )

    private fun userToMap(u: UserAccount): Map<String, Any?> = mapOf(
        "id" to u.id, "authUid" to u.authUid, "fullName" to u.fullName, "email" to u.email,
        "role" to u.role, "unitId" to u.unitId, "employeeId" to u.employeeId, "designation" to u.designation,
        "phone" to u.phone, "avatarColorHex" to u.avatarColorHex, "createdAt" to u.createdAt
    )

    private fun mapToDailyTask(m: Map<String, Any?>): DailyTaskEntry = DailyTaskEntry(
        id = asLong(m["id"]),
        userId = asLong(m["userId"]),
        userName = m["userName"] as? String ?: "",
        unitId = asLong(m["unitId"]),
        title = m["title"] as? String ?: "",
        category = m["category"] as? String ?: "",
        dateString = m["dateString"] as? String ?: "",
        timestamp = asLong(m["timestamp"]),
        hoursSpent = asDouble(m["hoursSpent"]),
        tasksCount = asInt(m["tasksCount"]),
        status = (m["status"] as? String)?.let { runCatching { TaskStatus.valueOf(it) }.getOrNull() } ?: TaskStatus.COMPLETED,
        tatStatus = (m["tatStatus"] as? String)?.let { runCatching { TatStatus.valueOf(it) }.getOrNull() } ?: TatStatus.WITHIN_TAT,
        qualityScorePercent = asDouble(m["qualityScorePercent"]),
        fcrResolved = asBool(m["fcrResolved"]),
        impactMetric = m["impactMetric"] as? String ?: "SLA Turnaround",
        notes = m["notes"] as? String ?: ""
    )

    private fun dailyTaskToMap(d: DailyTaskEntry): Map<String, Any?> = mapOf(
        "id" to d.id, "userId" to d.userId, "userName" to d.userName, "unitId" to d.unitId,
        "title" to d.title, "category" to d.category, "dateString" to d.dateString, "timestamp" to d.timestamp,
        "hoursSpent" to d.hoursSpent, "tasksCount" to d.tasksCount, "status" to d.status.name,
        "tatStatus" to d.tatStatus.name, "qualityScorePercent" to d.qualityScorePercent,
        "fcrResolved" to d.fcrResolved, "impactMetric" to d.impactMetric, "notes" to d.notes
    )
}
