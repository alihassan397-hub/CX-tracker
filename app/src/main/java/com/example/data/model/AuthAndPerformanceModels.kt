package com.example.data.model

import com.example.data.entity.DailyTaskEntry
import com.example.data.entity.UserAccount

/**
 * UI State for Authentication
 */
sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val user: UserAccount, val message: String) : AuthUiState()
    data class Error(val errorMessage: String) : AuthUiState()
}

/**
 * Automated Real-Time Daily Performance Scorecard for a User
 */
data class UserPerformanceScorecard(
    val userId: Long,
    val userName: String,
    val userRole: String,
    val unitName: String,
    val evaluatedDate: String,
    val totalLoggedTasks: Int,
    val totalResolvedItems: Int,
    val totalHoursWorked: Double,
    val completedCount: Int,
    val inProgressCount: Int,
    val pendingCount: Int,
    val withinTatCount: Int,
    val atRiskCount: Int,
    val breachedCount: Int,
    val slaAdherencePercent: Double,
    val qualityScorePercent: Double,
    val fcrPercent: Double, // First contact resolution
    val timeEfficiencyScore: Double, // 0 - 100
    val overallPerformanceScore: Double, // 0 - 100
    val performanceTier: String, // "Platinum CX Elite", "Gold Leader", "Silver Standard", "Needs Coaching"
    val tierColorHex: String,
    val keyIndicatorsList: List<PerformanceIndicatorItem>,
    val automatedFeedbackSummary: String
)

data class PerformanceIndicatorItem(
    val title: String,
    val valueStr: String,
    val targetStr: String,
    val statusColorHex: String,
    val iconName: String,
    val isMet: Boolean
)

/**
 * AI Performance Analysis State
 */
sealed class AiAnalysisState {
    object Idle : AiAnalysisState()
    object Loading : AiAnalysisState()
    data class Success(val analysisText: String, val timestamp: Long = System.currentTimeMillis()) : AiAnalysisState()
    data class Error(val error: String) : AiAnalysisState()
}

/**
 * AI Chat Message for CX Advisor
 */
data class AiChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "User" or "Gemini AI"
    val isFromAi: Boolean,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)
