package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.entity.CxUnit
import com.example.data.entity.UserAccount
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class OnboardingEmailTrigger(
    val recipientUnitHeadEmail: String = "sabeen.shafique@example.com",
    val unitHeadName: String = "Sabeen Shafique",
    val unitName: String,
    val unitCode: String,
    val newUserFullName: String,
    val newUserEmail: String,
    val newUserRole: String,
    val newUserDesignation: String,
    val newUserEmployeeId: String,
    val newUserPhone: String,
    val joinedTimestamp: String,
    val subject: String,
    val emailBody: String
)

object UserOnboardingEmailHelper {

    const val UNIT_HEAD_EMAIL = "sabeen.shafique@example.com"

    fun buildOnboardingTrigger(
        newUser: UserAccount,
        unit: CxUnit?
    ): OnboardingEmailTrigger {
        val unitName = unit?.name ?: "Customer Experience Division"
        val unitCode = unit?.code ?: "CX"
        val unitHeadName = "Sabeen Shafique (CX Unit Head)"
        val unitHeadEmail = UNIT_HEAD_EMAIL

        val timeStr = SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.US).format(Date(newUser.createdAt))
        val roleDisplay = when (newUser.role) {
            "UNIT_HEAD" -> "🏢 Customer Experience Unit Head"
            "TEAM_MEMBER" -> "👤 Team Member"
            else -> "👤 Team Member"
        }

        val subject = "[CX Tracker Alert] New Member Joined: ${newUser.fullName} joined $unitName as $roleDisplay"

        val body = buildString {
            appendLine("══════════════════════════════════════════════════════════════")
            appendLine("  CUSTOMER EXPERIENCE DIVISION - NEW TEAM ONBOARDING ALERT")
            appendLine("══════════════════════════════════════════════════════════════")
            appendLine()
            appendLine("Attention: Sabeen Shafique (Customer Experience Unit Head)")
            appendLine()
            appendLine("This is an automated notification to confirm that a new team member has registered on the CX Tracker Platform:")
            appendLine()
            appendLine("👤 Full Name:      ${newUser.fullName}")
            appendLine("📧 Email (Login):   ${newUser.email}")
            appendLine("🏷️ System Role:     $roleDisplay")
            appendLine("🏢 Assigned Unit:   $unitName ($unitCode)")
            appendLine("💼 Designation:     ${newUser.designation}")
            appendLine("🆔 Employee ID:     ${newUser.employeeId}")
            appendLine("📞 Contact Phone:   ${if (newUser.phone.isNotBlank()) newUser.phone else "Not specified"}")
            appendLine("📅 Registration:    $timeStr")
            appendLine()
            appendLine("Status: ACTIVE. The user can now record daily tasks, view unit SLA targets, track time-motion activities, and receive work assignments.")
            appendLine()
            appendLine("Regards,")
            appendLine("CX Tracker Automated Notification Dispatcher")
            appendLine("Customer Experience Division")
        }

        return OnboardingEmailTrigger(
            recipientUnitHeadEmail = unitHeadEmail,
            unitHeadName = unitHeadName,
            unitName = unitName,
            unitCode = unitCode,
            newUserFullName = newUser.fullName,
            newUserEmail = newUser.email,
            newUserRole = roleDisplay,
            newUserDesignation = newUser.designation,
            newUserEmployeeId = newUser.employeeId,
            newUserPhone = newUser.phone,
            joinedTimestamp = timeStr,
            subject = subject,
            emailBody = body
        )
    }

    /**
     * Creates an Intent to launch the user's email client (Gmail, Outlook, etc.)
     * with recipient Unit Head pre-filled.
     */
    fun createEmailIntent(trigger: OnboardingEmailTrigger): Intent {
        val recipients = arrayOf(trigger.recipientUnitHeadEmail)

        val mailtoUri = Uri.parse("mailto:" + recipients.joinToString(","))
        return Intent(Intent.ACTION_SENDTO).apply {
            data = mailtoUri
            putExtra(Intent.EXTRA_EMAIL, recipients)
            putExtra(Intent.EXTRA_SUBJECT, trigger.subject)
            putExtra(Intent.EXTRA_TEXT, trigger.emailBody)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
}
