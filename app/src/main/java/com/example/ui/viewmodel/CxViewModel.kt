package com.example.ui.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.entity.CxTask
import com.example.data.entity.CxUnit
import com.example.data.entity.DailyTaskEntry
import com.example.data.entity.TaskPriority
import com.example.data.entity.TaskStatus
import com.example.data.entity.TatStatus
import com.example.data.entity.TeamMember
import com.example.data.entity.TimeMotionLog
import com.example.data.entity.UserAccount
import com.example.data.entity.UserRole
import com.example.data.model.AiAnalysisState
import com.example.data.model.AiChatMessage
import com.example.data.model.AuthUiState
import com.example.data.model.CxDepartmentAnalytics
import com.example.data.model.MemberPerformanceSummary
import com.example.data.model.PerformanceIndicatorItem
import com.example.data.model.UnitPerformanceSummary
import com.example.data.model.UserPerformanceScorecard
import com.example.data.remote.GeminiService
import com.example.data.repository.CxRepository
import com.example.util.ExcelExporter
import com.example.util.OnboardingEmailTrigger
import com.example.util.TatNotificationManager
import com.example.util.UserOnboardingEmailHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.CoroutineContext

class CxViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CxRepository

    // ==========================================
    // CRASH FIX: every Firestore write used to run inside a bare
    // viewModelScope.launch(...) with no try/catch. The moment Firestore
    // rejected a write (e.g. "PERMISSION_DENIED" because a user's role
    // wasn't exactly UNIT_HEAD/ADMIN yet, or a network hiccup), that
    // exception had nowhere to go and crashed the whole app instantly —
    // this is why "add task" / "assign to Unit Head" were crashing while
    // sign-up (which already had its own try/catch) kept working fine.
    //
    // safeLaunch() below is a drop-in replacement for viewModelScope.launch
    // that catches any exception, turns it into a readable message, and
    // surfaces it through _errorMessage — so the UI can show a Snackbar
    // instead of the app dying.
    // ==========================================
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun clearError() {
        _errorMessage.value = null
    }

    private fun friendlyErrorMessage(t: Throwable): String {
        val fe = t as? FirebaseFirestoreException
        return when {
            fe != null && fe.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "Permission denied by Firestore. This usually means your account's Firestore profile document isn't set up correctly yet (its document ID must exactly match your Firebase Authentication UID), or — for actions that only a Unit Head can do — your role field isn't set to UNIT_HEAD. Ask your admin to check Firestore > users > (your account)."
            fe != null && fe.code == FirebaseFirestoreException.Code.UNAVAILABLE ->
                "Couldn't reach the server. Please check your internet connection and try again."
            else -> "Something went wrong: ${t.message ?: t::class.simpleName}"
        }
    }

    private fun safeLaunch(
        context: CoroutineContext = Dispatchers.IO,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        val handler = CoroutineExceptionHandler { _, throwable ->
            _errorMessage.value = friendlyErrorMessage(throwable)
        }
        return viewModelScope.launch(context + handler, block = block)
    }

    // User Authentication & Session
    val allUserAccounts: StateFlow<List<UserAccount>>
    private val _currentUser = MutableStateFlow<UserAccount?>(null)
    val currentUser = _currentUser.asStateFlow()

    private val _authUiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val authUiState = _authUiState.asStateFlow()

    // Triggered Email Notification when new user registers
    private val _latestEmailTrigger = MutableStateFlow<OnboardingEmailTrigger?>(null)
    val latestEmailTrigger = _latestEmailTrigger.asStateFlow()

    fun dismissEmailTrigger() {
        _latestEmailTrigger.value = null
    }

    // Raw Flows from Database
    val units: StateFlow<List<CxUnit>>
    val teamMembers: StateFlow<List<TeamMember>>
    val tasks: StateFlow<List<CxTask>>
    val timeMotionLogs: StateFlow<List<TimeMotionLog>>
    val allDailyTasks: StateFlow<List<DailyTaskEntry>>

    // Member Filter for Daily Tasks (Used by Sabeen Shafique to view specific member activities or all)
    private val _dailyTasksMemberFilter = MutableStateFlow<String?>("ALL")
    val dailyTasksMemberFilter = _dailyTasksMemberFilter.asStateFlow()

    fun setDailyTasksMemberFilter(memberName: String?) {
        _dailyTasksMemberFilter.value = memberName
    }

    // Task View Scope: "RELEVANT" (shows tasks assigned to current member or their unit) or "ALL"
    private val _taskViewScope = MutableStateFlow<String>("RELEVANT")
    val taskViewScope = _taskViewScope.asStateFlow()

    fun setTaskViewScope(scope: String) {
        _taskViewScope.value = scope
    }

    // Daily Tasks for Currently Logged-in User
    val userDailyTasks: StateFlow<List<DailyTaskEntry>>

    // Automated Performance Scorecard for Currently Logged-in User
    val userPerformanceScorecard: StateFlow<UserPerformanceScorecard>

    // AI Performance Analysis & Chat State
    private val _aiAnalysisState = MutableStateFlow<AiAnalysisState>(AiAnalysisState.Idle)
    val aiAnalysisState = _aiAnalysisState.asStateFlow()

    private val _aiChatMessages = MutableStateFlow<List<AiChatMessage>>(
        listOf(
            AiChatMessage(
                sender = "Gemini AI Advisor",
                isFromAi = true,
                message = "Hello! I am your CX Intelligence & Performance Advisor. Ask me anything regarding SLA turnaround, CMU complaints, QA calibration, or your daily performance indicators."
            )
        )
    )
    val aiChatMessages = _aiChatMessages.asStateFlow()

    // Filters and UI State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _unitFilter = MutableStateFlow<Long?>(null)
    val unitFilter = _unitFilter.asStateFlow()

    private val _assigneeFilter = MutableStateFlow<Long?>(null)
    val assigneeFilter = _assigneeFilter.asStateFlow()

    private val _statusFilter = MutableStateFlow<TaskStatus?>(null)
    val statusFilter = _statusFilter.asStateFlow()

    private val _tatFilter = MutableStateFlow<TatStatus?>(null)
    val tatFilter = _tatFilter.asStateFlow()

    private val _priorityFilter = MutableStateFlow<TaskPriority?>(null)
    val priorityFilter = _priorityFilter.asStateFlow()

    private val _selectedTask = MutableStateFlow<CxTask?>(null)
    val selectedTask = _selectedTask.asStateFlow()

    // Active Time-Motion Timer State
    private val _activeTimerTaskId = MutableStateFlow<Long?>(null)
    val activeTimerTaskId = _activeTimerTaskId.asStateFlow()

    private val _timerSeconds = MutableStateFlow(0)
    val timerSeconds = _timerSeconds.asStateFlow()

    private var timerJob: Job? = null

    init {
        repository = CxRepository(FirebaseFirestore.getInstance(), FirebaseAuth.getInstance())

        allUserAccounts = repository.allUserAccounts
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        units = repository.allUnits
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        teamMembers = repository.allTeamMembers
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        tasks = repository.allTasks
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        timeMotionLogs = repository.allTimeMotionLogs
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allDailyTasks = repository.allDailyTasks
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // User Daily Tasks stream with strict multi-user data isolation:
        // Regular members ONLY see their own activities.
        // Sabeen Shafique (Customer Experience Unit Head) sees department summary or selected member's tasks.
        userDailyTasks = combine(allDailyTasks, _currentUser, _dailyTasksMemberFilter) { dailyList, user, filterMember ->
            if (user == null) {
                emptyList()
            } else if (user.isUnitHead) {
                if (filterMember != null && filterMember != "ALL") {
                    dailyList.filter { it.userName.equals(filterMember, ignoreCase = true) }
                } else {
                    dailyList
                }
            } else {
                // Strict isolation: only show tasks logged by this user
                dailyList.filter {
                    it.userId == user.id || it.userName.equals(user.fullName, ignoreCase = true)
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // User Automated Performance Indicators Stream
        userPerformanceScorecard = combine(
            userDailyTasks,
            _currentUser,
            units
        ) { dailyList, user, allUnits ->
            computeUserPerformanceScorecard(dailyList, user, allUnits)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            getDefaultScorecard()
        )

        // Ensure the CX Unit list & team roster exist on first run (login accounts
        // are no longer auto-seeded — real people sign up for themselves via
        // Firebase Auth, which is what actually fixes cross-device login/sync).
        safeLaunch(Dispatchers.IO) {
            repository.seedInitialDataIfNeeded()
        }

        // Initialize Notification Channels
        TatNotificationManager.createNotificationChannels(application)

        // Launch SLA Monitor
        safeLaunch(Dispatchers.Default) {
            delay(3000)
            while (true) {
                checkAndTriggerTatNotifications()
                delay(15000)
            }
        }
    }

    // Filter Criteria Data Class
    private data class FilterParams(
        val query: String,
        val unitId: Long?,
        val assigneeId: Long?,
        val status: TaskStatus?,
        val tat: TatStatus?,
        val priority: TaskPriority?
    )

    private val filterParamsFlow = combine(
        combine(searchQuery, unitFilter, assigneeFilter) { q, u, a -> Triple(q, u, a) },
        combine(statusFilter, tatFilter, priorityFilter) { s, t, p -> Triple(s, t, p) }
    ) { (query, unitId, assigneeId), (status, tat, priority) ->
        FilterParams(query, unitId, assigneeId, status, tat, priority)
    }

    // Filtered Tasks with Role-based Task Visibility
    val filteredTasks: StateFlow<List<CxTask>> = combine(
        tasks,
        filterParamsFlow,
        _currentUser,
        teamMembers,
        _taskViewScope
    ) { allTasks, params, user, members, viewScope ->
        val now = System.currentTimeMillis()
        val currentMember = members.find {
            it.email.equals(user?.email, ignoreCase = true) || it.fullName.equals(user?.fullName, ignoreCase = true)
        }

        allTasks.filter { task ->
            // Task relevance: if viewing "RELEVANT" and user is a team member, show only relevant tasks
            val matchesRelevance = if (user == null || user.isUnitHead || viewScope == "ALL") {
                true
            } else {
                val matchesAssignee = currentMember != null && task.assigneeId == currentMember.id
                val matchesUnit = user.unitId != null && task.unitId == user.unitId
                matchesAssignee || matchesUnit
            }

            val matchesQuery = params.query.isBlank() ||
                    task.title.contains(params.query, ignoreCase = true) ||
                    task.trackingNumber.contains(params.query, ignoreCase = true) ||
                    task.description.contains(params.query, ignoreCase = true) ||
                    (task.customerAccountOrTicket?.contains(params.query, ignoreCase = true) == true) ||
                    task.category.contains(params.query, ignoreCase = true)

            val matchesUnit = params.unitId == null || task.unitId == params.unitId
            val matchesAssignee = params.assigneeId == null || task.assigneeId == params.assigneeId
            val matchesStatus = params.status == null || task.status == params.status
            val matchesTat = params.tat == null || task.computeTatStatus(now) == params.tat
            val matchesPriority = params.priority == null || task.priority == params.priority

            matchesRelevance && matchesQuery && matchesUnit && matchesAssignee && matchesStatus && matchesTat && matchesPriority
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Computed Department Analytics
    val analytics: StateFlow<CxDepartmentAnalytics> = combine(
        tasks,
        units,
        teamMembers,
        timeMotionLogs
    ) { allTasks, allUnits, allMembers, allLogs ->
        computeAnalytics(allTasks, allUnits, allMembers, allLogs)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        CxDepartmentAnalytics(
            totalTasks = 0,
            completedTasks = 0,
            inProgressTasks = 0,
            pendingTasks = 0,
            toDoTasks = 0,
            withinTatCount = 0,
            atRiskCount = 0,
            breachedTatCount = 0,
            overallSlaPercent = 0.0,
            breachRatePercent = 0.0,
            avgTurnaroundHours = 0.0,
            totalTimeMotionMinutes = 0,
            avgHandlingTimeMinutes = 0.0,
            criticalBreachesCount = 0,
            unitSummaries = emptyList(),
            memberSummaries = emptyList(),
            activityDistribution = emptyMap(),
            breachReasonDistribution = emptyMap(),
            statusCounts = emptyMap(),
            tatCounts = emptyMap()
        )
    )

    val activeBreachedTasks: StateFlow<List<CxTask>> = tasks.map { allTasks ->
        val now = System.currentTimeMillis()
        allTasks.filter { it.status != TaskStatus.COMPLETED && it.computeTatStatus(now) == TatStatus.BREACHED_TAT }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeNearBreachTasks: StateFlow<List<CxTask>> = tasks.map { allTasks ->
        val now = System.currentTimeMillis()
        allTasks.filter { it.status != TaskStatus.COMPLETED && it.computeTatStatus(now) == TatStatus.AT_RISK }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalAlertsCount: StateFlow<Int> = combine(
        activeBreachedTasks,
        activeNearBreachTasks
    ) { breached, nearBreach ->
        breached.size + nearBreach.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // ==========================================
    // AUTHENTICATION & ACCESS CONTROL
    // ==========================================

    fun login(
        emailInput: String,
        passwordInput: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val email = emailInput.trim()
        val password = passwordInput.trim()

        if (email.isBlank() || password.isBlank()) {
            val error = "Please enter your Email / Name and Password."
            _authUiState.value = AuthUiState.Error(error)
            onError(error)
            return
        }

        safeLaunch(Dispatchers.IO) {
            _authUiState.value = AuthUiState.Loading

            val authResult = repository.firebaseSignIn(email, password)
            authResult.fold(
                onSuccess = {
                    val user = repository.getUserByEmail(email)
                    if (user != null) {
                        _currentUser.value = user
                        _authUiState.value = AuthUiState.Success(user, "Welcome, ${user.fullName}!")
                        repository.seedInitialDataIfNeeded()
                        launch(Dispatchers.Main) { onSuccess() }
                    } else {
                        val err = "Signed in, but no CX Tracker profile was found for this account. Please contact your Unit Head."
                        _authUiState.value = AuthUiState.Error(err)
                        launch(Dispatchers.Main) { onError(err) }
                    }
                },
                onFailure = { e ->
                    val err = e.message ?: "Incorrect email or password. Please verify your credentials."
                    _authUiState.value = AuthUiState.Error(err)
                    launch(Dispatchers.Main) { onError(err) }
                }
            )
        }
    }

    fun signUp(
        fullName: String,
        email: String,
        password: String,
        role: String,
        unitId: Long?,
        employeeId: String,
        designation: String,
        phone: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (fullName.isBlank() || email.isBlank() || password.isBlank()) {
            val err = "Please fill in all required fields (Name, Email, Password)."
            _authUiState.value = AuthUiState.Error(err)
            onError(err)
            return
        }

        safeLaunch(Dispatchers.IO) {
            _authUiState.value = AuthUiState.Loading

            // Create the real, secure Firebase Auth account first. Firebase handles
            // password hashing/storage — this app never sees or stores it again.
            // Firebase Auth itself rejects a duplicate email automatically (its
            // error message is surfaced below), so no separate pre-check is needed
            // — a manual Firestore read here would fail anyway, since the person
            // isn't authenticated until this call succeeds.
            val authResult = repository.firebaseSignUp(email.trim(), password.trim())
            val uid = authResult.getOrNull()
            if (uid == null) {
                val err = authResult.exceptionOrNull()?.message ?: "Could not create account. Please try again."
                _authUiState.value = AuthUiState.Error(err)
                launch(Dispatchers.Main) { onError(err) }
                return@safeLaunch
            }

            // SECURITY FIX: self-service sign-up must NEVER be able to create a
            // UNIT_HEAD (admin) account. Previously, anyone typing the name
            // "Sabeen Shafique" (or passing role=UNIT_HEAD from the client) was
            // instantly granted full admin rights. Unit Head accounts must only
            // be created by seeding/provisioning, or via a separate admin-only
            // promotion flow that itself requires an authenticated Unit Head.
            val finalRole = UserRole.TEAM_MEMBER.name
            val avatarColor = "#0284C7"

            val newUser = UserAccount(
                authUid = uid,
                fullName = fullName.trim(),
                email = email.trim().lowercase(),
                role = finalRole,
                unitId = unitId ?: 1L,
                employeeId = employeeId.trim().ifEmpty { "CX-${(100..999).random()}" },
                designation = designation.trim().ifEmpty { "CX Specialist" },
                phone = phone.trim(),
                avatarColorHex = avatarColor
            )

            val newId = repository.insertUserAccount(newUser)
            val createdUser = newUser.copy(id = newId)
            _currentUser.value = createdUser

            // Everything below this point is a secondary convenience (roster
            // entry, confirmation dialog, notification) — the account itself is
            // already created and the person is already signed in. None of this
            // should ever be able to crash the sign-up flow if it hiccups.
            try {
                val allMembers = repository.allTeamMembers.firstOrNull() ?: emptyList()
                val existingMember = allMembers.find {
                    it.email.equals(createdUser.email, ignoreCase = true) ||
                    it.fullName.equals(createdUser.fullName, ignoreCase = true)
                }
                if (existingMember == null) {
                    val targetUnitId = createdUser.unitId ?: 1L
                    val teamMemberEntry = TeamMember(
                        unitId = targetUnitId,
                        fullName = createdUser.fullName,
                        employeeId = createdUser.employeeId,
                        email = createdUser.email,
                        phone = createdUser.phone,
                        role = "Team Member",
                        designation = createdUser.designation,
                        avatarColorHex = avatarColor
                    )
                    repository.insertTeamMember(teamMemberEntry)
                }
            } catch (_: Exception) { /* roster entry can be added later by a Unit Head */ }

            try {
                // The Firestore "users" document just written above also triggers a
                // Cloud Function (functions/index.js: onUserCreated) that automatically
                // emails the Unit Head, once functions are deployed — no one needs to
                // tap "send" on their own phone. We still show a local confirmation
                // dialog with the same details as a visible receipt / manual backup.
                val unit = if (createdUser.unitId != null) repository.getUnitById(createdUser.unitId) else null
                val emailTrigger = UserOnboardingEmailHelper.buildOnboardingTrigger(createdUser, unit)
                _latestEmailTrigger.value = emailTrigger

                TatNotificationManager.sendUserOnboardedNotification(
                    context = getApplication(),
                    userName = createdUser.fullName,
                    userEmail = createdUser.email,
                    userRole = emailTrigger.newUserRole,
                    unitName = emailTrigger.unitName,
                    unitHeadName = emailTrigger.unitHeadName
                )
            } catch (_: Exception) { /* confirmation dialog / notification are best-effort */ }

            try {
                repository.seedInitialDataIfNeeded()
            } catch (_: Exception) { /* retried again next successful sign-in */ }

            _authUiState.value = AuthUiState.Success(createdUser, "Account created successfully for ${createdUser.fullName}!")
            launch(Dispatchers.Main) { onSuccess() }
        }
    }

    fun forgotPassword(
        email: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (email.isBlank()) {
            val err = "Please enter your registered email."
            _authUiState.value = AuthUiState.Error(err)
            onError(err)
            return
        }

        safeLaunch(Dispatchers.IO) {
            _authUiState.value = AuthUiState.Loading

            // SECURITY FIX: this used to accept a brand-new password straight from
            // whoever filled in the form — meaning anyone who merely knew a
            // colleague's email address could silently take over their account.
            // Firebase now emails a secure reset link to the account's real inbox;
            // only whoever controls that inbox can actually change the password.
            //
            // No pre-check against Firestore here — that read requires being signed
            // in, which is never true on the Forgot Password screen. Firebase's
            // sendPasswordResetEmail already handles an unregistered email safely
            // on its own.
            val result = repository.firebaseSendPasswordReset(email.trim())
            result.fold(
                onSuccess = {
                    _authUiState.value = AuthUiState.Success(UserAccount(), "Password reset link sent to $email. Please check your inbox.")
                    launch(Dispatchers.Main) { onSuccess() }
                },
                onFailure = { e ->
                    val err = e.message ?: "Could not send reset email. Please try again."
                    _authUiState.value = AuthUiState.Error(err)
                    launch(Dispatchers.Main) { onError(err) }
                }
            )
        }
    }

    // SECURITY FIX: switchUser(user) removed — it set the active session to
    // any UserAccount object with no authentication check at all. It had no
    // legitimate callers in the app; removed as a precaution so it can never
    // be wired up as a bypass later.

    // SECURITY FIX: switchToUnitHead() removed. It previously allowed ANY signed-in
    // user (including Team Members) to instantly grant themselves full Unit Head
    // admin access with a single tap and zero authentication. Elevating privileges
    // must always go through a proper authenticated sign-in (signIn) as that account,
    // never a client-side role swap.

    fun logout() {
        repository.firebaseSignOut()
        _currentUser.value = null
        _authUiState.value = AuthUiState.Idle
    }

    // Role-based Access Rights & Permissions
    fun canUserDeleteTask(task: CxTask): Boolean {
        val user = _currentUser.value ?: return false
        return user.isUnitHead
    }

    fun canUserEditTask(task: CxTask): Boolean {
        val user = _currentUser.value ?: return false
        return true // Team members can update status and time motion
    }

    fun canUserAssignTask(): Boolean {
        val user = _currentUser.value ?: return false
        return user.isUnitHead
    }

    fun canUserDeleteUnit(unit: CxUnit): Boolean {
        val user = _currentUser.value ?: return false
        return user.isUnitHead
    }

    fun canUserDeleteMember(member: TeamMember): Boolean {
        val user = _currentUser.value ?: return false
        return user.isUnitHead
    }

    fun canUserManageMembers(): Boolean {
        val user = _currentUser.value ?: return false
        return user.isUnitHead
    }

    // ==========================================
    // MEMBER MANAGEMENT (Add Member, Edit Interns)
    // ==========================================

    fun addMember(
        fullName: String,
        unitId: Long,
        role: String = "Team Member",
        designation: String = "CX Specialist",
        email: String = "",
        phone: String = "",
        employeeId: String = ""
    ) {
        safeLaunch(Dispatchers.IO) {
            val empId = employeeId.trim().ifEmpty { "CX-${(100..999).random()}" }
            val memberEmail = email.trim().ifEmpty {
                "${fullName.trim().lowercase().replace(" ", ".")}@example.com"
            }
            val member = TeamMember(
                unitId = unitId,
                fullName = fullName.trim(),
                employeeId = empId,
                email = memberEmail,
                phone = phone.trim(),
                role = role.trim().ifEmpty { "Team Member" },
                designation = designation.trim().ifEmpty { "CX Specialist" },
                avatarColorHex = "#008269"
            )
            repository.insertTeamMember(member)
        }
    }

    fun editIntern(
        memberId: Long,
        newName: String,
        newDesignation: String = "Customer Experience Intern",
        newEmail: String = ""
    ) {
        safeLaunch(Dispatchers.IO) {
            val existing = repository.getTeamMemberById(memberId)
            if (existing != null) {
                val updated = existing.copy(
                    fullName = newName.trim(),
                    designation = newDesignation.trim(),
                    email = if (newEmail.isNotBlank()) newEmail.trim() else existing.email
                )
                repository.updateTeamMember(updated)

                val existingUser = repository.getUserByEmail(existing.email)
                if (existingUser != null) {
                    repository.updateUserAccount(
                        existingUser.copy(
                            fullName = newName.trim(),
                            email = if (newEmail.isNotBlank()) newEmail.trim() else existingUser.email,
                            designation = newDesignation.trim()
                        )
                    )
                }
            }
        }
    }

    fun updateMember(member: TeamMember) {
        safeLaunch(Dispatchers.IO) {
            repository.updateTeamMember(member)
            val existingUser = repository.getUserByEmail(member.email)
            if (existingUser != null) {
                repository.updateUserAccount(
                    existingUser.copy(
                        fullName = member.fullName,
                        unitId = member.unitId,
                        designation = member.designation
                    )
                )
            }
        }
    }

    // ==========================================
    // DAILY TASKS & AUTOMATED PERFORMANCE ENGINE
    // ==========================================

    fun addDailyTask(
        title: String,
        category: String,
        tasksCount: Int,
        hoursSpent: Double,
        status: TaskStatus,
        tatStatus: TatStatus,
        qualityScore: Double,
        fcrResolved: Boolean,
        impactMetric: String,
        notes: String,
        customDateString: String? = null,
        customTimestamp: Long? = null,
        totalDialledCalls: Int = 0,
        connectedCalls: Int = 0,
        answeredCalls: Int = 0
    ) {
        val user = _currentUser.value ?: return
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val taskDate = customDateString?.ifBlank { null } ?: dateFormat.format(Date())
        val taskTimestamp = customTimestamp ?: System.currentTimeMillis()

        safeLaunch(Dispatchers.IO) {
            val entry = DailyTaskEntry(
                userId = user.id,
                userName = user.fullName,
                unitId = user.unitId ?: 1L,
                title = title.trim(),
                category = category.trim().ifEmpty { "Customer Resolution" },
                dateString = taskDate,
                timestamp = taskTimestamp,
                hoursSpent = hoursSpent.coerceAtLeast(0.1),
                tasksCount = tasksCount.coerceAtLeast(1),
                status = status,
                tatStatus = tatStatus,
                qualityScorePercent = qualityScore.coerceIn(0.0, 100.0),
                fcrResolved = fcrResolved,
                impactMetric = impactMetric.ifEmpty { "SLA Turnaround" },
                notes = notes.trim(),
                totalDialledCalls = totalDialledCalls.coerceAtLeast(0),
                connectedCalls = connectedCalls.coerceAtLeast(0),
                answeredCalls = answeredCalls.coerceAtLeast(0)
            )
                        try {
                repository.insertDailyTask(entry)
            } catch (e: Exception) {
                val liveUid = repository.firebaseCurrentUid()
                throw Exception(
                    "DEBUG entry.userId=${entry.userId} | user.id=${user.id} | user.authUid=${user.authUid} | " +
                        "liveFirebaseUid=$liveUid | user.role=${user.role} | original=${e.message}",
                    e
                )
            }
        }
    }

    fun updateDailyTask(task: DailyTaskEntry) {
        safeLaunch(Dispatchers.IO) {
            repository.updateDailyTask(task)
        }
    }

    fun deleteDailyTask(task: DailyTaskEntry) {
        safeLaunch(Dispatchers.IO) {
            repository.deleteDailyTask(task)
        }
    }

    private fun computeUserPerformanceScorecard(
        dailyTasks: List<DailyTaskEntry>,
        user: UserAccount?,
        allUnits: List<CxUnit>
    ): UserPerformanceScorecard {
        if (user == null || dailyTasks.isEmpty()) {
            return getDefaultScorecard(user, allUnits)
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayStr = dateFormat.format(Date())

        val totalTasks = dailyTasks.size
        val totalVolume = dailyTasks.sumOf { it.tasksCount }
        val totalHours = dailyTasks.sumOf { it.hoursSpent }
        val completed = dailyTasks.count { it.status == TaskStatus.COMPLETED }
        val inProgress = dailyTasks.count { it.status == TaskStatus.IN_PROGRESS }
        val pending = dailyTasks.count { it.status == TaskStatus.PENDING }

        val withinTat = dailyTasks.count { it.tatStatus == TatStatus.WITHIN_TAT }
        val atRisk = dailyTasks.count { it.tatStatus == TatStatus.AT_RISK }
        val breached = dailyTasks.count { it.tatStatus == TatStatus.BREACHED_TAT }

        val slaRate = if (totalTasks > 0) (withinTat.toDouble() / totalTasks) * 100.0 else 100.0
        val avgQuality = if (totalTasks > 0) dailyTasks.sumOf { it.qualityScorePercent } / totalTasks else 95.0
        val fcrRate = if (totalTasks > 0) (dailyTasks.count { it.fcrResolved }.toDouble() / totalTasks) * 100.0 else 100.0

        // Time efficiency calculation (target standard ~ 1.0 hr per major task batch)
        val efficiencyFactor = if (totalHours > 0) ((totalVolume / totalHours) / 3.0).coerceIn(0.5, 1.5) else 1.0
        val timeEfficiencyScore = (efficiencyFactor * 85.0).coerceIn(0.0, 100.0)

        // Overall Weighted Performance Score (0-100)
        // 40% SLA Adherence + 30% Quality & Accuracy + 15% FCR + 15% Time Efficiency
        val overallScore = ((slaRate * 0.40) + (avgQuality * 0.30) + (fcrRate * 0.15) + (timeEfficiencyScore * 0.15)).coerceIn(0.0, 100.0)

        val (tier, colorHex) = when {
            overallScore >= 92.0 && slaRate >= 95.0 -> "Platinum CX Elite ⭐⭐⭐" to "#008269"
            overallScore >= 82.0 && slaRate >= 90.0 -> "Gold Star Leader ⭐⭐" to "#0284C7"
            overallScore >= 70.0 -> "Silver Standard ⭐" to "#D97706"
            else -> "Needs Focus & Coaching ⚠️" to "#DC2626"
        }

        val unitName = allUnits.find { it.id == user.unitId }?.name ?: "Customer Experience Division"

        val indicators = listOf(
            PerformanceIndicatorItem(
                title = "SLA TAT Adherence",
                valueStr = "${"%.1f".format(slaRate)}%",
                targetStr = "Target: 95.0%",
                statusColorHex = if (slaRate >= 95.0) "#008269" else if (slaRate >= 90.0) "#D97706" else "#DC2626",
                iconName = "timer",
                isMet = slaRate >= 95.0
            ),
            PerformanceIndicatorItem(
                title = "Resolution Output",
                valueStr = "$totalVolume items",
                targetStr = "Across $totalTasks tasks",
                statusColorHex = "#0284C7",
                iconName = "check_circle",
                isMet = totalVolume >= 5
            ),
            PerformanceIndicatorItem(
                title = "Quality & Accuracy",
                valueStr = "${"%.1f".format(avgQuality)}%",
                targetStr = "Target: 95.0%",
                statusColorHex = if (avgQuality >= 95.0) "#008269" else "#D97706",
                iconName = "assessment",
                isMet = avgQuality >= 95.0
            ),
            PerformanceIndicatorItem(
                title = "First Contact Resolution",
                valueStr = "${"%.1f".format(fcrRate)}%",
                targetStr = "Target: 85.0%",
                statusColorHex = if (fcrRate >= 85.0) "#008269" else "#D97706",
                iconName = "groups",
                isMet = fcrRate >= 85.0
            )
        )

        val automatedFeedback = when {
            overallScore >= 90.0 -> "Outstanding performance! You are exceeding SLA and Quality benchmarks. Keep up the high responsiveness."
            overallScore >= 80.0 -> "Strong operational delivery. Focus on closing pending follow-ups faster to reach Platinum tier."
            else -> "Immediate attention required on ${breached} breached items. Seek guidance from your Unit Head on escalation triage."
        }

        return UserPerformanceScorecard(
            userId = user.id,
            userName = user.fullName,
            userRole = user.role,
            unitName = unitName,
            evaluatedDate = todayStr,
            totalLoggedTasks = totalTasks,
            totalResolvedItems = totalVolume,
            totalHoursWorked = totalHours,
            completedCount = completed,
            inProgressCount = inProgress,
            pendingCount = pending,
            withinTatCount = withinTat,
            atRiskCount = atRisk,
            breachedCount = breached,
            slaAdherencePercent = slaRate,
            qualityScorePercent = avgQuality,
            fcrPercent = fcrRate,
            timeEfficiencyScore = timeEfficiencyScore,
            overallPerformanceScore = overallScore,
            performanceTier = tier,
            tierColorHex = colorHex,
            keyIndicatorsList = indicators,
            automatedFeedbackSummary = automatedFeedback
        )
    }

    private fun getDefaultScorecard(user: UserAccount? = null, allUnits: List<CxUnit> = emptyList()): UserPerformanceScorecard {
        val uName = user?.fullName ?: "Staff Member"
        val uUnit = allUnits.find { it.id == user?.unitId }?.name ?: "Customer Experience"
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        return UserPerformanceScorecard(
            userId = user?.id ?: 0L,
            userName = uName,
            userRole = user?.role ?: "Officer",
            unitName = uUnit,
            evaluatedDate = todayStr,
            totalLoggedTasks = 0,
            totalResolvedItems = 0,
            totalHoursWorked = 0.0,
            completedCount = 0,
            inProgressCount = 0,
            pendingCount = 0,
            withinTatCount = 0,
            atRiskCount = 0,
            breachedCount = 0,
            slaAdherencePercent = 100.0,
            qualityScorePercent = 95.0,
            fcrPercent = 100.0,
            timeEfficiencyScore = 90.0,
            overallPerformanceScore = 95.0,
            performanceTier = "Platinum CX Elite ⭐⭐⭐",
            tierColorHex = "#008269",
            keyIndicatorsList = listOf(
                PerformanceIndicatorItem("SLA Adherence", "100.0%", "Target: 95.0%", "#008269", "timer", true),
                PerformanceIndicatorItem("Quality Score", "95.0%", "Target: 95.0%", "#008269", "assessment", true),
                PerformanceIndicatorItem("FCR Rate", "100.0%", "Target: 85.0%", "#008269", "groups", true)
            ),
            automatedFeedbackSummary = "Ready to log daily tasks. Add your completed and ongoing cases to track automatic KPIs."
        )
    }

    // ==========================================
    // AI FEATURES (GEMINI INTEGRATION)
    // ==========================================

    fun runAiPerformanceAnalysis() {
        val user = _currentUser.value ?: return
        val currentTasks = userDailyTasks.value
        val currentAnalytics = analytics.value
        val currentUnit = units.value.find { it.id == user.unitId }?.name ?: "CX Division"

        safeLaunch {
            _aiAnalysisState.value = AiAnalysisState.Loading
            try {
                val report = GeminiService.analyzeUserDailyPerformance(
                    user = user,
                    unitName = currentUnit,
                    dailyTasks = currentTasks,
                    analytics = currentAnalytics
                )
                _aiAnalysisState.value = AiAnalysisState.Success(report)
            } catch (e: Exception) {
                _aiAnalysisState.value = AiAnalysisState.Error("AI Performance analysis failed: ${e.message}")
            }
        }
    }

    fun sendAiAdvisorPrompt(question: String) {
        if (question.isBlank()) return
        val user = _currentUser.value
        val unitName = units.value.find { it.id == user?.unitId }?.name
        val currentAnalytics = analytics.value

        val userMessage = AiChatMessage(
            sender = user?.fullName ?: "You",
            isFromAi = false,
            message = question
        )
        _aiChatMessages.value = _aiChatMessages.value + userMessage

        safeLaunch {
            val response = GeminiService.askAiAdvisor(
                question = question,
                currentUser = user,
                activeUnitName = unitName,
                analytics = currentAnalytics
            )

            val aiMessage = AiChatMessage(
                sender = "Gemini AI Advisor",
                isFromAi = true,
                message = response
            )
            _aiChatMessages.value = _aiChatMessages.value + aiMessage
        }
    }

    fun suggestTaskMetadata(
        title: String,
        description: String,
        onResult: (category: String, priority: TaskPriority, tatHours: Double) -> Unit
    ) {
        safeLaunch {
            val res = GeminiService.suggestTaskSlaAndCategory(title, description)
            val category = res["category"] ?: "Customer Resolution"
            val priority = when (res["priority"]?.uppercase()) {
                "CRITICAL" -> TaskPriority.CRITICAL
                "HIGH" -> TaskPriority.HIGH
                "LOW" -> TaskPriority.LOW
                else -> TaskPriority.MEDIUM
            }
            val tat = res["tatHours"]?.toDoubleOrNull() ?: 24.0
            launch(Dispatchers.Main) {
                onResult(category, priority, tat)
            }
        }
    }

    // ==========================================
    // SLA NOTIFICATIONS & ALERTS
    // ==========================================

    fun checkAndTriggerTatNotifications() {
        val currentTasks = tasks.value
        val currentUnits = units.value
        if (currentTasks.isEmpty()) return

        val now = System.currentTimeMillis()
        val activeTasks = currentTasks.filter { it.status != TaskStatus.COMPLETED }

        for (task in activeTasks) {
            val tatStatus = task.computeTatStatus(now)
            val unitName = currentUnits.find { it.id == task.unitId }?.name ?: "CX Operations"

            when (tatStatus) {
                TatStatus.BREACHED_TAT -> {
                    TatNotificationManager.sendTatBreachedNotification(
                        context = getApplication(),
                        task = task,
                        unitName = unitName
                    )
                }
                TatStatus.AT_RISK -> {
                    val remainingMinutes = task.getRemainingMinutes(now)
                    TatNotificationManager.sendTatNearBreachNotification(
                        context = getApplication(),
                        task = task,
                        unitName = unitName,
                        remainingMinutes = remainingMinutes
                    )
                }
                TatStatus.WITHIN_TAT -> {}
            }
        }
    }

    fun sendTestNotification(isBreach: Boolean) {
        TatNotificationManager.sendTestNotification(getApplication(), isBreach)
    }

    // ==========================================
    // DEPARTMENT ANALYTICS ENGINE
    // ==========================================

    fun computeAnalytics(
        allTasks: List<CxTask>,
        allUnits: List<CxUnit>,
        allMembers: List<TeamMember>,
        allLogs: List<TimeMotionLog>
    ): CxDepartmentAnalytics {
        val now = System.currentTimeMillis()
        val total = allTasks.size
        val completed = allTasks.filter { it.status == TaskStatus.COMPLETED }
        val inProgress = allTasks.filter { it.status == TaskStatus.IN_PROGRESS }
        val pending = allTasks.filter { it.status == TaskStatus.PENDING }
        val toDo = allTasks.filter { it.status == TaskStatus.TO_DO }

        val withinTatTasks = allTasks.filter { it.computeTatStatus(now) == TatStatus.WITHIN_TAT }
        val atRiskTasks = allTasks.filter { it.computeTatStatus(now) == TatStatus.AT_RISK }
        val breachedTasks = allTasks.filter { it.computeTatStatus(now) == TatStatus.BREACHED_TAT }

        val slaPercent = if (total > 0) {
            (withinTatTasks.size.toDouble() / total.toDouble()) * 100.0
        } else {
            100.0
        }

        val breachPercent = if (total > 0) {
            (breachedTasks.size.toDouble() / total.toDouble()) * 100.0
        } else {
            0.0
        }

        val completedTurnaroundSum = completed.sumOf { it.getActualTurnaroundHours(now) }
        val avgTurnaround = if (completed.isNotEmpty()) completedTurnaroundSum / completed.size else 0.0

        val totalMotionMins = allTasks.sumOf { it.timeMotionMinutes }
        val avgHandlingMins = if (total > 0) totalMotionMins.toDouble() / total.toDouble() else 0.0

        val criticalBreaches = breachedTasks.count { it.priority == TaskPriority.CRITICAL }

        // Unit Summaries
        val unitSummaries = allUnits.map { unit ->
            val uTasks = allTasks.filter { it.unitId == unit.id }
            val uCompleted = uTasks.count { it.status == TaskStatus.COMPLETED }
            val uInProgress = uTasks.count { it.status == TaskStatus.IN_PROGRESS }
            val uPending = uTasks.count { it.status == TaskStatus.PENDING }
            val uToDo = uTasks.count { it.status == TaskStatus.TO_DO }
            val uWithinTat = uTasks.count { it.computeTatStatus(now) == TatStatus.WITHIN_TAT }
            val uAtRisk = uTasks.count { it.computeTatStatus(now) == TatStatus.AT_RISK }
            val uBreached = uTasks.count { it.computeTatStatus(now) == TatStatus.BREACHED_TAT }
            val uSla = if (uTasks.isNotEmpty()) (uWithinTat.toDouble() / uTasks.size) * 100.0 else 100.0
            val uCompletedTasks = uTasks.filter { it.status == TaskStatus.COMPLETED }
            val uAvgRes = if (uCompletedTasks.isNotEmpty()) uCompletedTasks.sumOf { it.getActualTurnaroundHours(now) } / uCompletedTasks.size else 0.0
            val uMotionHours = uTasks.sumOf { it.timeMotionMinutes } / 60.0
            val uMembers = allMembers.count { it.unitId == unit.id && it.isActive }

            UnitPerformanceSummary(
                unit = unit,
                totalTasks = uTasks.size,
                completedCount = uCompleted,
                inProgressCount = uInProgress,
                pendingCount = uPending,
                toDoCount = uToDo,
                withinTatCount = uWithinTat,
                atRiskCount = uAtRisk,
                breachedTatCount = uBreached,
                slaPercent = uSla,
                targetSlaPercent = unit.targetSlaPercent,
                avgResolutionHours = uAvgRes,
                totalTimeMotionHours = uMotionHours,
                activeMembersCount = uMembers
            )
        }

        // Member Summaries
        val unitMap = allUnits.associateBy { it.id }
        val memberSummaries = allMembers.map { member ->
            val mTasks = allTasks.filter { it.assigneeId == member.id }
            val mCompleted = mTasks.count { it.status == TaskStatus.COMPLETED }
            val mInProgress = mTasks.count { it.status == TaskStatus.IN_PROGRESS }
            val mPending = mTasks.count { it.status == TaskStatus.PENDING }
            val mToDo = mTasks.count { it.status == TaskStatus.TO_DO }
            val mWithinTat = mTasks.count { it.computeTatStatus(now) == TatStatus.WITHIN_TAT }
            val mBreached = mTasks.count { it.computeTatStatus(now) == TatStatus.BREACHED_TAT }
            val mSla = if (mTasks.isNotEmpty()) (mWithinTat.toDouble() / mTasks.size) * 100.0 else 100.0
            val mMotionHours = mTasks.sumOf { it.timeMotionMinutes } / 60.0
            val mAvgHandling = if (mTasks.isNotEmpty()) mTasks.sumOf { it.timeMotionMinutes }.toDouble() / mTasks.size else 0.0

            val completionRate = if (mTasks.isNotEmpty()) (mCompleted.toDouble() / mTasks.size) * 40.0 else 20.0
            val slaScore = (mSla / 100.0) * 40.0
            val motionFactor = (mMotionHours.coerceAtMost(20.0) / 20.0) * 20.0
            val prodScore = (completionRate + slaScore + motionFactor).coerceIn(0.0, 100.0)

            val tier = when {
                prodScore >= 85.0 && mSla >= 95.0 -> "Platinum Performer"
                prodScore >= 70.0 && mSla >= 90.0 -> "Gold (High SLA)"
                prodScore >= 50.0 -> "Silver (Standard)"
                else -> "Needs Focus / Coaching"
            }

            val unit = unitMap[member.unitId]
            MemberPerformanceSummary(
                member = member,
                unitName = unit?.name ?: "Customer Experience",
                unitCode = unit?.code ?: "CX",
                assignedCount = mTasks.size,
                completedCount = mCompleted,
                inProgressCount = mInProgress,
                pendingCount = mPending,
                toDoCount = mToDo,
                withinTatCount = mWithinTat,
                breachedCount = mBreached,
                slaPercent = mSla,
                totalTimeMotionHours = mMotionHours,
                avgHandlingMinutes = mAvgHandling,
                productivityScore = prodScore,
                tierRating = tier
            )
        }

        val activityDist = allLogs.groupBy { it.activityType }
            .mapValues { (_, logs) -> logs.sumOf { it.durationMinutes } }

        val breachReasons = breachedTasks
            .mapNotNull { it.breachReason ?: it.pendingReason }
            .groupingBy { it }
            .eachCount()

        val statusMap = mapOf(
            TaskStatus.TO_DO to toDo.size,
            TaskStatus.IN_PROGRESS to inProgress.size,
            TaskStatus.PENDING to pending.size,
            TaskStatus.COMPLETED to completed.size
        )

        val tatMap = mapOf(
            TatStatus.WITHIN_TAT to withinTatTasks.size,
            TatStatus.AT_RISK to atRiskTasks.size,
            TatStatus.BREACHED_TAT to breachedTasks.size
        )

        return CxDepartmentAnalytics(
            totalTasks = total,
            completedTasks = completed.size,
            inProgressTasks = inProgress.size,
            pendingTasks = pending.size,
            toDoTasks = toDo.size,
            withinTatCount = withinTatTasks.size,
            atRiskCount = atRiskTasks.size,
            breachedTatCount = breachedTasks.size,
            overallSlaPercent = slaPercent,
            breachRatePercent = breachPercent,
            avgTurnaroundHours = avgTurnaround,
            totalTimeMotionMinutes = totalMotionMins,
            avgHandlingTimeMinutes = avgHandlingMins,
            criticalBreachesCount = criticalBreaches,
            unitSummaries = unitSummaries,
            memberSummaries = memberSummaries,
            activityDistribution = activityDist,
            breachReasonDistribution = breachReasons,
            statusCounts = statusMap,
            tatCounts = tatMap
        )
    }

    // Filter Actions
    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setUnitFilter(unitId: Long?) { _unitFilter.value = unitId }
    fun setAssigneeFilter(memberId: Long?) { _assigneeFilter.value = memberId }
    fun setStatusFilter(status: TaskStatus?) { _statusFilter.value = status }
    fun setTatFilter(tat: TatStatus?) { _tatFilter.value = tat }
    fun setPriorityFilter(priority: TaskPriority?) { _priorityFilter.value = priority }
    fun clearFilters() {
        _searchQuery.value = ""
        _unitFilter.value = null
        _assigneeFilter.value = null
        _statusFilter.value = null
        _tatFilter.value = null
        _priorityFilter.value = null
    }

    fun selectTask(task: CxTask?) { _selectedTask.value = task }

    // Task Management
    fun createTask(
        title: String,
        description: String,
        unitId: Long,
        assigneeId: Long,
        assignedByName: String,
        priority: TaskPriority,
        category: String,
        tatHours: Double,
        customerAccountOrTicket: String?,
        dueDateTime: Long? = null
    ) {
        safeLaunch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val due = dueDateTime ?: (now + (tatHours * 3600000L).toLong())
            val effectiveTatHours = if (dueDateTime != null && dueDateTime > now) {
                ((dueDateTime - now) / 3600000.0).coerceAtLeast(1.0)
            } else tatHours
            val trackingNum = "CX-${(1000..9999).random()}"

            val author = if (assignedByName.isNotBlank()) assignedByName else (_currentUser.value?.fullName ?: "Unit Head")

            val newTask = CxTask(
                trackingNumber = trackingNum,
                title = title.trim(),
                description = description.trim(),
                unitId = unitId,
                assigneeId = assigneeId,
                assignedByName = author,
                priority = priority,
                category = category.trim().ifEmpty { "Customer Complaint" },
                status = TaskStatus.TO_DO,
                createdAt = now,
                assignedAt = now,
                tatHours = effectiveTatHours,
                dueDateTime = due,
                customerAccountOrTicket = customerAccountOrTicket?.trim()?.ifEmpty { null }
            )
            repository.insertTask(newTask)
        }
    }

    fun updateTask(task: CxTask) {
        safeLaunch(Dispatchers.IO) {
            repository.updateTask(task.copy(lastUpdated = System.currentTimeMillis()))
        }
    }

    fun updateTaskStatus(
        task: CxTask,
        newStatus: TaskStatus,
        resolutionRemarks: String? = null,
        pendingReason: String? = null,
        breachReason: String? = null
    ) {
        safeLaunch(Dispatchers.IO) {
            repository.updateTaskStatus(
                task = task,
                newStatus = newStatus,
                resolutionRemarks = resolutionRemarks,
                pendingReason = pendingReason,
                breachReason = breachReason
            )
            if (newStatus == TaskStatus.COMPLETED) {
                TatNotificationManager.clearTaskAlert(getApplication(), task.id)
            }
            if (_selectedTask.value?.id == task.id) {
                _selectedTask.value = repository.getTaskById(task.id)
            }
        }
    }

    fun deleteTask(task: CxTask) {
        safeLaunch(Dispatchers.IO) {
            TatNotificationManager.clearTaskAlert(getApplication(), task.id)
            repository.deleteTask(task)
            if (_selectedTask.value?.id == task.id) {
                _selectedTask.value = null
            }
        }
    }

    // Time Motion Tracking
    fun startTimerForTask(taskId: Long) {
        if (_activeTimerTaskId.value == taskId && timerJob != null) return
        timerJob?.cancel()
        _activeTimerTaskId.value = taskId
        _timerSeconds.value = 0

        timerJob = safeLaunch {
            while (true) {
                delay(1000)
                _timerSeconds.value += 1
            }
        }
    }

    fun stopAndSaveTimer(activityType: String, notes: String) {
        val taskId = _activeTimerTaskId.value ?: return
        val seconds = _timerSeconds.value
        val minutes = ((seconds + 59) / 60).coerceAtLeast(1)

        timerJob?.cancel()
        timerJob = null
        _activeTimerTaskId.value = null
        _timerSeconds.value = 0

        safeLaunch(Dispatchers.IO) {
            val task = repository.getTaskById(taskId)
            val memberId = task?.assigneeId ?: 1L
            repository.logTimeMotion(
                taskId = taskId,
                memberId = memberId,
                durationMinutes = minutes,
                activityType = activityType,
                notes = notes
            )
            if (_selectedTask.value?.id == taskId) {
                _selectedTask.value = repository.getTaskById(taskId)
            }
        }
    }

    fun logManualTimeMotion(taskId: Long, durationMinutes: Int, activityType: String, notes: String) {
        safeLaunch(Dispatchers.IO) {
            val task = repository.getTaskById(taskId)
            val memberId = task?.assigneeId ?: 1L
            repository.logTimeMotion(
                taskId = taskId,
                memberId = memberId,
                durationMinutes = durationMinutes,
                activityType = activityType,
                notes = notes
            )
            if (_selectedTask.value?.id == taskId) {
                _selectedTask.value = repository.getTaskById(taskId)
            }
        }
    }

    // Units Management
    fun createUnit(
        name: String,
        code: String,
        description: String,
        unitHeadName: String,
        headEmail: String,
        colorHex: String,
        defaultTatHours: Double,
        targetSlaPercent: Double
    ) {
        safeLaunch(Dispatchers.IO) {
            val unit = CxUnit(
                name = name.trim(),
                code = code.trim().uppercase(),
                description = description.trim(),
                unitHeadName = unitHeadName.trim(),
                headEmail = headEmail.trim(),
                colorHex = colorHex.ifEmpty { "#008269" },
                defaultTatHours = defaultTatHours,
                targetSlaPercent = targetSlaPercent
            )
            repository.insertUnit(unit)
        }
    }

    fun updateUnit(unit: CxUnit) {
        safeLaunch(Dispatchers.IO) {
            repository.updateUnit(unit)
        }
    }

    fun deleteUnit(unit: CxUnit) {
        safeLaunch(Dispatchers.IO) {
            repository.deleteUnit(unit)
        }
    }

    // Team Members Management
    fun createTeamMember(
        unitId: Long,
        fullName: String,
        employeeId: String,
        email: String,
        phone: String,
        role: String,
        designation: String,
        avatarColorHex: String
    ) {
        safeLaunch(Dispatchers.IO) {
            val member = TeamMember(
                unitId = unitId,
                fullName = fullName.trim(),
                employeeId = employeeId.trim().uppercase(),
                email = email.trim(),
                phone = phone.trim(),
                role = role.trim(),
                designation = designation.trim(),
                avatarColorHex = avatarColorHex.ifEmpty { "#008269" }
            )
            repository.insertTeamMember(member)
        }
    }

    fun updateTeamMember(member: TeamMember) {
        safeLaunch(Dispatchers.IO) {
            repository.updateTeamMember(member)
        }
    }

    fun addMember(
        name: String,
        email: String,
        unitId: Long,
        employeeId: String = "",
        designation: String = "CX Specialist",
        phone: String = "",
        role: String = "Team Member",
        avatarColorHex: String = "#0284C7"
    ) {
        createTeamMember(
            unitId = unitId,
            fullName = name,
            employeeId = employeeId.ifEmpty { "CX-${(100..999).random()}" },
            email = email,
            phone = phone,
            role = role,
            designation = designation,
            avatarColorHex = avatarColorHex
        )
    }

    fun updateMember(
        id: Long,
        name: String,
        email: String,
        unitId: Long,
        employeeId: String,
        designation: String,
        phone: String,
        role: String = ""
    ) {
        safeLaunch(Dispatchers.IO) {
            val existing = repository.allTeamMembers.firstOrNull()?.find { it.id == id }
            if (existing != null) {
                val updated = existing.copy(
                    fullName = name.trim(),
                    email = email.trim(),
                    unitId = unitId,
                    employeeId = employeeId.trim(),
                    designation = designation.trim(),
                    phone = phone.trim(),
                    role = if (role.isNotBlank()) role.trim() else existing.role
                )
                repository.updateTeamMember(updated)
                val existingUser = repository.getUserByEmail(existing.email)
                if (existingUser != null) {
                    repository.updateUserAccount(
                        existingUser.copy(
                            fullName = name.trim(),
                            email = email.trim(),
                            unitId = unitId,
                            employeeId = employeeId.trim(),
                            designation = designation.trim(),
                            phone = phone.trim()
                        )
                    )
                }
            }
        }
    }

    fun reassignMemberUnit(memberId: Long, newUnitId: Long) {
        safeLaunch(Dispatchers.IO) {
            val existing = repository.allTeamMembers.firstOrNull()?.find { it.id == memberId }
            if (existing != null) {
                val updated = existing.copy(unitId = newUnitId)
                repository.updateTeamMember(updated)
                val existingUser = repository.getUserByEmail(existing.email)
                if (existingUser != null) {
                    repository.updateUserAccount(existingUser.copy(unitId = newUnitId))
                }
            }
        }
    }

    fun editIntern(
        id: Long,
        name: String,
        email: String,
        unitId: Long,
        employeeId: String,
        designation: String = "CX Intern / Trainee",
        phone: String = ""
    ) {
        safeLaunch(Dispatchers.IO) {
            val existing = repository.allTeamMembers.firstOrNull()?.find { it.id == id }
            if (existing != null) {
                val updated = existing.copy(
                    fullName = name.trim(),
                    email = email.trim(),
                    unitId = unitId,
                    employeeId = employeeId.trim(),
                    designation = designation.trim(),
                    phone = phone.trim(),
                    role = "Intern"
                )
                repository.updateTeamMember(updated)
                val existingUser = repository.getUserByEmail(existing.email)
                if (existingUser != null) {
                    repository.updateUserAccount(
                        existingUser.copy(
                            fullName = name.trim(),
                            email = email.trim(),
                            unitId = unitId,
                            employeeId = employeeId.trim(),
                            designation = designation.trim(),
                            phone = phone.trim()
                        )
                    )
                }
            }
        }
    }

    fun deleteTeamMember(member: TeamMember) {
        safeLaunch(Dispatchers.IO) {
            repository.deleteTeamMember(member)
            val existingUser = repository.getUserByEmail(member.email)
            if (existingUser != null && !existingUser.isUnitHead) {
                repository.deleteUserAccount(existingUser)
            }
        }
    }

    fun deleteMemberById(id: Long) {
        safeLaunch(Dispatchers.IO) {
            val existing = repository.allTeamMembers.firstOrNull()?.find { it.id == id }
            if (existing != null) {
                deleteTeamMember(existing)
            }
        }
    }

    // Excel Export & Sharing
    fun exportExcelReport(
        context: Context,
        filteredTasksList: List<CxTask>? = null,
        filteredAnalyticsData: CxDepartmentAnalytics? = null,
        filteredLogsList: List<TimeMotionLog>? = null,
        periodLabel: String = "All Time",
        dateFilterSummary: String = "All Time",
        unitFilterLabel: String = "All Units",
        fileNamePrefix: String = "HBL_CX_Performance_Report"
    ) {
        safeLaunch(Dispatchers.IO) {
            val currentTasks = filteredTasksList ?: tasks.value
            val currentUnits = units.value
            val currentMembers = teamMembers.value
            val currentLogs = filteredLogsList ?: timeMotionLogs.value
            val currentAnalytics = filteredAnalyticsData ?: computeAnalytics(
                currentTasks,
                currentUnits,
                currentMembers,
                currentLogs
            )

            val csvData = ExcelExporter.generateMasterReportCsv(
                tasks = currentTasks,
                units = currentUnits,
                members = currentMembers,
                analytics = currentAnalytics,
                timeLogs = currentLogs,
                periodLabel = periodLabel,
                dateFilterSummary = dateFilterSummary,
                unitFilterLabel = unitFilterLabel
            )

            val success = ExcelExporter.exportAndShareCsv(
                context = context,
                csvContent = csvData,
                fileNamePrefix = fileNamePrefix
            )

            launch(Dispatchers.Main) {
                if (success) {
                    Toast.makeText(
                        context,
                        "Exporting Performance Report (${currentTasks.size} tasks for $periodLabel)",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(context, "Failed to generate report file", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun copyExcelTableToClipboard(
        context: Context,
        filteredTasksList: List<CxTask>? = null,
        filteredAnalyticsData: CxDepartmentAnalytics? = null,
        filteredLogsList: List<TimeMotionLog>? = null,
        periodLabel: String = "All Time",
        dateFilterSummary: String = "All Time",
        unitFilterLabel: String = "All Units"
    ) {
        val currentTasks = filteredTasksList ?: tasks.value
        val currentUnits = units.value
        val currentMembers = teamMembers.value
        val currentLogs = filteredLogsList ?: timeMotionLogs.value
        val currentAnalytics = filteredAnalyticsData ?: computeAnalytics(
            currentTasks,
            currentUnits,
            currentMembers,
            currentLogs
        )

        val csvData = ExcelExporter.generateMasterReportCsv(
            tasks = currentTasks,
            units = currentUnits,
            members = currentMembers,
            analytics = currentAnalytics,
            timeLogs = currentLogs,
            periodLabel = periodLabel,
            dateFilterSummary = dateFilterSummary,
            unitFilterLabel = unitFilterLabel
        )

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("CX Tracker Performance Report", csvData)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(
            context,
            "Performance report copied for $periodLabel (${currentTasks.size} tasks)! Ready to paste in Excel.",
            Toast.LENGTH_LONG
        ).show()
    }

    // Reset Actions
    fun clearAllTasksAndTestData() {
        safeLaunch(Dispatchers.IO) {
            repository.deleteAllTasks()
            _selectedTask.value = null
            _activeTimerTaskId.value = null
            _timerSeconds.value = 0
            timerJob?.cancel()
            timerJob = null
            TatNotificationManager.cancelAllAlerts(getApplication())
        }
    }

    fun clearEntireDatabase() {
        safeLaunch(Dispatchers.IO) {
            repository.clearAllData()
            _selectedTask.value = null
            _activeTimerTaskId.value = null
            _timerSeconds.value = 0
            timerJob?.cancel()
            timerJob = null
            TatNotificationManager.cancelAllAlerts(getApplication())
        }
    }

    fun resetToSampleData() {
        safeLaunch(Dispatchers.IO) {
            repository.resetToSampleData()
        }
    }

    fun resetToDemoData() {
        resetToSampleData()
    }
}
