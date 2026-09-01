package com.example.cxtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    private val repository = CxRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainAppScreen(repository)
                }
            }
        }
    }
}

@Composable
fun MainAppScreen(repository: CxRepository) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Assigned Tasks", "Daily Entries")

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("CX Operations Tracker", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))

        TabRow(selectedTabIndex = selectedTab) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTab) {
            0 -> AssignedTasksScreen(repository)
            1 -> DailyEntriesScreen(repository)
        }
    }
}

@Composable
fun AssignedTasksScreen(repository: CxRepository) {
    var tasks by remember { mutableStateOf<List<AssignedTask>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        repository.getAssignedTasks().collect { tasks = it }
    }

    LazyColumn {
        items(tasks) { task ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = task.title, style = MaterialTheme.typography.titleMedium)
                    Text(text = "Assigned To: ${task.assignedTo} | Unit: ${task.assignedUnit}")
                    Text(text = "Status: ${task.status} | Priority: ${task.priority}")
                }
            }
        }
    }
}

@Composable
fun DailyEntriesScreen(repository: CxRepository) {
    var entries by remember { mutableStateOf<List<DailyEntry>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        repository.getDailyEntries().collect { entries = it }
    }

    LazyColumn {
        items(entries) { entry ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Date: ${entry.entryDate} (${entry.category})", style = MaterialTheme.typography.titleMedium)
                    Text(text = "Received: ${entry.totalReceived} | Resolved: ${entry.totalResolved} | Pending: ${entry.totalPending}")
                    Text(text = "Logged By: ${entry.loggedBy}")
                }
            }
        }
    }
}
