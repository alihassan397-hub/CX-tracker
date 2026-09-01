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
