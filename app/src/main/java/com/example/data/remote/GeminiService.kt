package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.data.entity.DailyTaskEntry
import com.example.data.entity.UserAccount
import com.example.data.model.CxDepartmentAnalytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"
    private const val MODEL = "gemini-2.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun getApiKey(): String {
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }
    }

    /**
     * Generate an AI-powered comprehensive CX Performance Review & Commentary for a user
     */
    suspend fun analyzeUserDailyPerformance(
        user: UserAccount,
        unitName: String,
        dailyTasks: List<DailyTaskEntry>,
        analytics: CxDepartmentAnalytics
    ): String = withContext(Dispatchers.IO) {
        val totalVolume = dailyTasks.sumOf { it.tasksCount }
        val totalHours = dailyTasks.sumOf { it.hoursSpent }
        val withinTatCount = dailyTasks.count { it.tatStatus == com.example.data.entity.TatStatus.WITHIN_TAT }
        val slaRate = if (dailyTasks.isNotEmpty()) (withinTatCount.toDouble() / dailyTasks.size) * 100.0 else 100.0
        val avgQuality = if (dailyTasks.isNotEmpty()) dailyTasks.sumOf { it.qualityScorePercent } / dailyTasks.size else 95.0
        val fcrRate = if (dailyTasks.isNotEmpty()) (dailyTasks.count { it.fcrResolved }.toDouble() / dailyTasks.size) * 100.0 else 100.0

        val prompt = """
            You are the Chief Customer Experience (CX) AI Performance Evaluator for a major commercial bank.
            Perform a rigorous, structured, and constructive performance analysis for the following CX team member:
            
            Team Member: ${user.fullName}
            Role: ${user.role} (${user.designation})
            Assigned Unit: $unitName
            Daily Logged Work Items: ${dailyTasks.size} tasks logged ($totalVolume customer interactions processed)
            Total Operational Hours: ${"%.1f".format(totalHours)} hrs
            SLA TAT Adherence Rate: ${"%.1f".format(slaRate)}%
            Quality & Accuracy Average: ${"%.1f".format(avgQuality)}%
            First Contact Resolution (FCR) Rate: ${"%.1f".format(fcrRate)}%
            
            Task Breakdown:
            ${dailyTasks.joinToString("\n") { "- ${it.title} | Cat: ${it.category} | TAT: ${it.tatStatus.name} | Quality: ${it.qualityScorePercent}% | Hours: ${it.hoursSpent}h" }}
            
            Provide a crisp, actionable appraisal with:
            1. **Executive Performance Rating & Tier** (e.g. Platinum Elite / Gold Performer)
            2. **Key Productivity & SLA Strengths**
            3. **Identified Bottlenecks or Risk Factors**
            4. **Actionable Coaching Recommendations for Next Shift**
            Keep it executive, concise, and motivating.
        """.trimIndent()

        callGeminiApi(prompt) ?: generateHeuristicPerformanceReport(user, unitName, dailyTasks, slaRate, avgQuality, totalVolume, totalHours)
    }

    /**
     * Interactive AI CX Assistant Q&A
     */
    suspend fun askAiAdvisor(
        question: String,
        currentUser: UserAccount?,
        activeUnitName: String?,
        analytics: CxDepartmentAnalytics
    ): String = withContext(Dispatchers.IO) {
        val prompt = """
            You are the CX Intelligence Advisor for our Customer Experience & Operations department.
            User Profile: ${currentUser?.fullName ?: "Staff"} (${currentUser?.role ?: "Officer"}), Unit: ${activeUnitName ?: "All CX Units"}
            Overall Department SLA: ${"%.1f".format(analytics.overallSlaPercent)}%, Total Tasks: ${analytics.totalTasks}, Breaches: ${analytics.breachedTatCount}, Active Alerts: ${analytics.atRiskCount}
            
            User Inquiry:
            "$question"
            
            Provide a clear, authoritative, and practical banking CX recommendation.
        """.trimIndent()

        callGeminiApi(prompt) ?: generateHeuristicAdvisorResponse(question, currentUser, analytics)
    }

    /**
     * AI Task Smart Predictor (suggests SLA TAT hours, priority, category)
     */
    suspend fun suggestTaskSlaAndCategory(taskTitle: String, taskDescription: String): Map<String, String> = withContext(Dispatchers.IO) {
        val prompt = """
            Analyze this banking CX task and return only a JSON object with:
            {"category": "...", "priority": "...", "recommendedTatHours": ...}
            
            Task Title: $taskTitle
            Task Description: $taskDescription
        """.trimIndent()

        val raw = callGeminiApi(prompt)
        if (raw != null) {
            try {
                val cleaned = raw.substringAfter("{").substringBeforeLast("}")
                val json = JSONObject("{$cleaned}")
                return@withContext mapOf(
                    "category" to json.optString("category", "Customer Resolution"),
                    "priority" to json.optString("priority", "HIGH"),
                    "tatHours" to json.optString("recommendedTatHours", "24")
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse JSON from AI response: ${e.message}")
            }
        }

        // Heuristic fallback
        val lower = "$taskTitle $taskDescription".lowercase()
        when {
            lower.contains("dispute") || lower.contains("fraud") || lower.contains("ombudsman") -> mapOf("category" to "Regulatory Dispute", "priority" to "CRITICAL", "tatHours" to "12")
            lower.contains("app") || lower.contains("raast") || lower.contains("login") -> mapOf("category" to "Digital CX Exception", "priority" to "HIGH", "tatHours" to "8")
            lower.contains("card") || lower.contains("atm") || lower.contains("block") -> mapOf("category" to "Contact Center Urgent", "priority" to "HIGH", "tatHours" to "4")
            lower.contains("audit") || lower.contains("branch") -> mapOf("category" to "Branch Experience", "priority" to "MEDIUM", "tatHours" to "48")
            else -> mapOf("category" to "Customer Service Request", "priority" to "MEDIUM", "tatHours" to "24")
        }
    }

    private fun callGeminiApi(prompt: String): String? {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.d(TAG, "No valid Gemini API key found in BuildConfig, using smart heuristic engine.")
            return null
        }

        return try {
            val url = "$BASE_URL/$MODEL:generateContent?key=$apiKey"
            val jsonBody = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().put("text", prompt))
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)
            }

            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Gemini API call failed with HTTP ${response.code}: ${response.message}")
                    return null
                }
                val bodyStr = response.body?.string() ?: return null
                val rootJson = JSONObject(bodyStr)
                val candidates = rootJson.optJSONArray("candidates") ?: return null
                if (candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.getJSONObject("content")
                    val parts = content.getJSONArray("parts")
                    if (parts.length() > 0) {
                        return parts.getJSONObject(0).getString("text")
                    }
                }
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing Gemini API call: ${e.message}")
            null
        }
    }

    private fun generateHeuristicPerformanceReport(
        user: UserAccount,
        unitName: String,
        dailyTasks: List<DailyTaskEntry>,
        slaRate: Double,
        avgQuality: Double,
        totalVolume: Int,
        totalHours: Double
    ): String {
        val tier = when {
            slaRate >= 95.0 && avgQuality >= 95.0 -> "Platinum CX Elite ⭐⭐⭐"
            slaRate >= 90.0 -> "Gold Performer ⭐⭐"
            slaRate >= 80.0 -> "Silver Standard ⭐"
            else -> "Needs Focused Coaching ⚠️"
        }

        return """
            ### 🌟 CX Executive Performance Review
            **Officer:** ${user.fullName} | **Designation:** ${user.designation}
            **Unit:** $unitName | **Evaluated Shift:** Today
            
            #### 📊 Performance Rating: **$tier**
            - **SLA Adherence (Within TAT):** ${"%.1f".format(slaRate)}% (Target: 95.0%)
            - **Quality & Accuracy Score:** ${"%.1f".format(avgQuality)}%
            - **Volume Handled:** $totalVolume cases across ${dailyTasks.size} daily logs
            - **Total Active Time:** ${"%.1f".format(totalHours)} hours
            
            #### 🚀 Key Strengths:
            - Outstanding responsiveness on critical customer touchpoints.
            - Consistently high resolution quality with minimal escalations.
            - Timely documentation and swift time-motion logging.
            
            #### 💡 Optimization & Growth Guidance:
            - Continue prioritizing cases with less than 25% TAT remaining to maintain 100% breach-free status.
            - Leverage automated root-cause tagging for recurring digital transactions.
        """.trimIndent()
    }

    private fun generateHeuristicAdvisorResponse(
        question: String,
        currentUser: UserAccount?,
        analytics: CxDepartmentAnalytics
    ): String {
        val q = question.lowercase()
        return when {
            q.contains("sla") || q.contains("breach") -> """
                ### ⏱️ SLA Optimization Strategy
                Current Department SLA is at **${"%.1f".format(analytics.overallSlaPercent)}%** with **${analytics.breachedTatCount} breaches** recorded.
                
                **Immediate Action Plan:**
                1. **Prioritize At-Risk Queue:** Review the ${analytics.atRiskCount} tasks in the At-Risk bucket immediately.
                2. **Unit Calibration:** Contact Center and CMU should cross-share high-load peak hour tickets.
                3. **Pending Reason Audits:** Ensure any ticket on 'Pending Info' is followed up within 2 hours.
            """.trimIndent()

            q.contains("cmu") || q.contains("complaint") -> """
                ### 📑 Complaints Management Unit (CMU) Best Practices
                - Enforce 24-hour turnaround for general grievances and 12-hour priority for regulatory/ombudsman disputes.
                - Utilize the Time & Motion tracker to identify verification bottlenecks.
                - Ensure every closed ticket has comprehensive resolution remarks and customer confirmation.
            """.trimIndent()

            q.contains("daily task") || q.contains("performance") -> """
                ### 🎯 Maximizing Your Daily Performance Score
                - Log your daily tasks promptly using the **Daily Tasks** tab.
                - Target a **First Contact Resolution (FCR)** of >85% and **Quality Accuracy** of >95%.
                - Keep all active tasks within TAT to achieve the **Platinum CX Elite** badge!
            """.trimIndent()

            else -> """
                ### 💡 CX Intelligence Recommendation
                Based on current department telemetry (**${analytics.totalTasks} active tasks**, **${"%.1f".format(analytics.overallSlaPercent)}% SLA**):
                - Ensure all daily assignments are acknowledged within 15 minutes of distribution.
                - Keep notes updated so Unit Heads and Admins have real-time visibility.
                - Use the AI Performance Evaluator at end of shift for personalized self-calibration.
            """.trimIndent()
        }
    }
}
