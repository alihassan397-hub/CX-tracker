package com.example.ui.viewmodel

// --- Base Models ---
data class AssignedTask(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val assignedTo: String = "",
    val assignedUnit: String = "",
    val priority: String = "Medium",
    val status: String = "Pending",
    val slaDays: Int = 3,
    val createdAt: Long = System.currentTimeMillis()
)

data class DailyEntry(
    val id: String = "",
    val entryDate: String = "",
    val category: String = "",
    val totalReceived: Int = 0,
    val totalResolved: Int = 0,
    val totalPending: Int = 0,
    val loggedBy: String = ""
)

// --- Auth UI State ---
sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Authenticated(val userId: String, val email: String) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

// --- AI Models ---
data class AiChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String = "",
    val message: String = "",
    val isFromAi: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

sealed class AiAnalysisState {
    object Idle : AiAnalysisState()
    object Loading : AiAnalysisState()
    data class Success(val analysisText: String) : AiAnalysisState()
    data class Error(val error: String) : AiAnalysisState()
}

// --- Performance Scorecard Models ---
data class PerformanceIndicatorItem(
    val title: String = "",
    val valueStr: String = "",
    val targetStr: String = "",
    val statusColorHex: String = "#000000"
)

data class UserPerformanceScorecard(
    val userName: String = "",
    val userRole: String = "",
    val unitName: String = "",
    val performanceTier: String = "",
    val tierColorHex: String = "#000000",
    val overallPerformanceScore: Double = 0.0,
    val totalHoursWorked: Double = 0.0,
    val keyIndicatorsList: List<PerformanceIndicatorItem> = emptyList()
)
