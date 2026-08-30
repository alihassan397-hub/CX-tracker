package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CorporateFare
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.entity.UserRole
import com.example.data.model.AuthUiState
import com.example.ui.theme.HblLime
import com.example.ui.theme.HblOnLime
import com.example.ui.theme.HblPrimary
import com.example.ui.theme.HblPrimaryDark
import com.example.ui.theme.HblSecondary
import com.example.ui.theme.HblTertiaryGold
import com.example.ui.viewmodel.CxViewModel
import com.example.util.OnboardingEmailTrigger
import com.example.util.UserOnboardingEmailHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: CxViewModel,
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val units by viewModel.units.collectAsStateWithLifecycle()
    val authState by viewModel.authUiState.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUserAccounts.collectAsStateWithLifecycle()
    val teamMembers by viewModel.teamMembers.collectAsStateWithLifecycle()
    val latestEmailTrigger by viewModel.latestEmailTrigger.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Sign In, 1: Sign Up, 2: Forgot Password

    // Sign In Fields
    var signInEmail by remember { mutableStateOf("") }
    var signInPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var memberSelectorExpanded by remember { mutableStateOf(false) }

    // Sign Up Fields
    var signUpName by remember { mutableStateOf("") }
    var signUpEmail by remember { mutableStateOf("") }
    var signUpPassword by remember { mutableStateOf("") }
    var signUpRole by remember { mutableStateOf(UserRole.TEAM_MEMBER.name) }
    var signUpUnitId by remember { mutableLongStateOf(1L) }
    var signUpEmpId by remember { mutableStateOf("") }
    var signUpDesignation by remember { mutableStateOf("") }
    var signUpPhone by remember { mutableStateOf("") }
    var unitDropdownExpanded by remember { mutableStateOf(false) }

    // Forgot Password Fields
    var forgotEmail by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        HblPrimaryDark,
                        HblPrimary,
                        Color(0xFF0F172A)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // If user is already logged in, show quick return and logout bar
            currentUser?.let { user ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, HblLime, RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Active Session: ${user.fullName}",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "Role: ${user.role} • ${user.email}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = HblLime,
                                    fontSize = 11.sp
                                )
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = onAuthSuccess,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = HblLime,
                                    contentColor = HblOnLime
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Dashboard", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.logout()
                                    Toast.makeText(context, "Signed out successfully", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFCA5A5)),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = Brush.linearGradient(listOf(Color(0xFFEF4444), Color(0xFFDC2626)))
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Sign Out", fontSize = 11.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // Bank CX Header Banner
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.10f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(HblLime),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_cx_experience_logo),
                            contentDescription = "CX Logo",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Customer Experience Division",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "SLA, TAT & Daily Performance Tracking Platform",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.85f)
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Auth Card Container
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Navigation Tab Row
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = HblPrimary,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = HblPrimary,
                                height = 3.dp
                            )
                        }
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Sign In", fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Sign Up", fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("Forgot Pass", fontWeight = FontWeight.Bold) }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // TAB 0: SIGN IN
                    if (selectedTab == 0) {
                        Column {
                            Text(
                                text = "Welcome Back",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Sign in using your Email Address or Registered Full Name",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Quick Profile Selector for Team Roster
                            val availableProfiles = (teamMembers.map { it.fullName to it.email } +
                                    allUsers.map { it.fullName to it.email }).distinctBy { it.first }

                            if (availableProfiles.isNotEmpty()) {
                                ExposedDropdownMenuBox(
                                    expanded = memberSelectorExpanded,
                                    onExpandedChange = { memberSelectorExpanded = !memberSelectorExpanded }
                                ) {
                                    OutlinedTextField(
                                        value = if (signInEmail.isNotBlank()) signInEmail else "Select your profile (Optional)",
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Select Registered Member Profile") },
                                        leadingIcon = {
                                            Icon(Icons.Default.Person, contentDescription = null, tint = HblPrimary)
                                        },
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = memberSelectorExpanded)
                                        },
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    ExposedDropdownMenu(
                                        expanded = memberSelectorExpanded,
                                        onDismissRequest = { memberSelectorExpanded = false }
                                    ) {
                                        availableProfiles.forEach { (name, email) ->
                                            DropdownMenuItem(
                                                text = {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = if (name.equals("Sabeen Shafique", ignoreCase = true)) "🏢 $name (Unit Head)" else "👤 $name",
                                                            fontWeight = if (name.equals("Sabeen Shafique", ignoreCase = true)) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (name.equals("Sabeen Shafique", ignoreCase = true)) HblPrimary else Color.Unspecified
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    signInEmail = email.ifBlank { name }
                                                    memberSelectorExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            OutlinedTextField(
                                value = signInEmail,
                                onValueChange = { signInEmail = it },
                                label = { Text("Email Address / Full Name") },
                                leadingIcon = {
                                    Icon(Icons.Default.Email, contentDescription = null, tint = HblPrimary)
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("signin_email_input"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = signInPassword,
                                onValueChange = { signInPassword = it },
                                label = { Text("Password") },
                                leadingIcon = {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = HblPrimary)
                                },
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                        )
                                    }
                                },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth().testTag("signin_password_input"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { selectedTab = 2 }) {
                                    Text("Forgot Password?", color = HblPrimary, fontSize = 13.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    viewModel.login(
                                        emailInput = signInEmail,
                                        passwordInput = signInPassword,
                                        onSuccess = onAuthSuccess,
                                        onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("signin_submit_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = HblPrimary,
                                    contentColor = Color.White
                                )
                            ) {
                                if (authState is AuthUiState.Loading) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Sign In to Portal", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }

                    // TAB 1: SIGN UP
                    if (selectedTab == 1) {
                        Column {
                            Text(
                                text = "Create Staff Account",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Join CX division with your assigned role & unit",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = signUpName,
                                onValueChange = { signUpName = it },
                                label = { Text("Full Name *") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = HblPrimary) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = signUpEmail,
                                onValueChange = { signUpEmail = it },
                                label = { Text("Email Address (Username) *") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = HblPrimary) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = signUpPassword,
                                onValueChange = { signUpPassword = it },
                                label = { Text("Password *") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = HblPrimary) },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Unit Selection Dropdown
                            if (units.isNotEmpty()) {
                                ExposedDropdownMenuBox(
                                    expanded = unitDropdownExpanded,
                                    onExpandedChange = { unitDropdownExpanded = !unitDropdownExpanded }
                                ) {
                                    val selectedUnitName = units.find { it.id == signUpUnitId }?.name ?: units.first().name
                                    OutlinedTextField(
                                        value = selectedUnitName,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Assigned CX Unit *") },
                                        leadingIcon = { Icon(Icons.Default.CorporateFare, contentDescription = null, tint = HblPrimary) },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitDropdownExpanded) },
                                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    ExposedDropdownMenu(
                                        expanded = unitDropdownExpanded,
                                        onDismissRequest = { unitDropdownExpanded = false }
                                    ) {
                                        units.forEach { unit ->
                                            DropdownMenuItem(
                                                text = { Text("${unit.code} - ${unit.name}") },
                                                onClick = {
                                                    signUpUnitId = unit.id
                                                    unitDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = signUpEmpId,
                                    onValueChange = { signUpEmpId = it },
                                    label = { Text("Employee ID") },
                                    placeholder = { Text("CX-105") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                OutlinedTextField(
                                    value = signUpPhone,
                                    onValueChange = { signUpPhone = it },
                                    label = { Text("Phone") },
                                    placeholder = { Text("+92...") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = signUpDesignation,
                                onValueChange = { signUpDesignation = it },
                                label = { Text("Designation") },
                                placeholder = { Text("CX Specialist / Intern") },
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.Work, contentDescription = null, tint = HblPrimary) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    viewModel.signUp(
                                        fullName = signUpName,
                                        email = signUpEmail,
                                        password = signUpPassword,
                                        role = UserRole.TEAM_MEMBER.name,
                                        unitId = signUpUnitId,
                                        employeeId = signUpEmpId,
                                        designation = signUpDesignation,
                                        phone = signUpPhone,
                                        onSuccess = onAuthSuccess,
                                        onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = HblPrimary,
                                    contentColor = Color.White
                                )
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Register & Enter Portal", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }
                        }
                    }

                    // TAB 2: FORGOT PASSWORD
                    if (selectedTab == 2) {
                        Column {
                            Text(
                                text = "Reset Account Password",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Enter your registered email address and we'll send you a secure password reset link.",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = forgotEmail,
                                onValueChange = { forgotEmail = it },
                                label = { Text("Registered Email Address") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = HblPrimary) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    viewModel.forgotPassword(
                                        email = forgotEmail,
                                        onSuccess = {
                                            Toast.makeText(context, "Reset link sent! Check your email inbox.", Toast.LENGTH_LONG).show()
                                            selectedTab = 0
                                            signInEmail = forgotEmail
                                        },
                                        onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = HblPrimary,
                                    contentColor = Color.White
                                )
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Send Reset Link", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Onboarding Email Trigger Confirmation Dialog
        latestEmailTrigger?.let { trigger ->
            OnboardingEmailTriggerDialog(
                trigger = trigger,
                onDismiss = {
                    viewModel.dismissEmailTrigger()
                    onAuthSuccess()
                },
                onLaunchEmail = {
                    try {
                        val emailIntent = UserOnboardingEmailHelper.createEmailIntent(trigger)
                        context.startActivity(emailIntent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "No email app found. Notification logged to system.", Toast.LENGTH_SHORT).show()
                    }
                },
                onCopyPayload = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("CX Onboarding Email Trigger", trigger.emailBody)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Email dispatch content copied to clipboard!", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
fun OnboardingEmailTriggerDialog(
    trigger: OnboardingEmailTrigger,
    onDismiss: () -> Unit,
    onLaunchEmail: () -> Unit,
    onCopyPayload: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = HblLime.copy(alpha = 0.2f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Send, contentDescription = null, tint = HblPrimary, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Email Trigger Dispatched!",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = HblPrimary)
                    )
                    Text(
                        text = "New User Onboarding Notification",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Welcome ${trigger.newUserFullName}!",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF166534))
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "You joined ${trigger.unitName} as ${trigger.newUserRole}.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF15803D))
                        )
                    }
                }

                Text(
                    text = "Notification Dispatched To:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )

                // Recipient: Unit Head Sabeen Shafique
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CorporateFare, contentDescription = null, tint = HblPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = "🏢 CX Unit Head: ${trigger.unitHeadName}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                            Text(text = trigger.recipientUnitHeadEmail, style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Subject Preview
                Text(
                    text = "Email Subject:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = trigger.subject,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                        modifier = Modifier.padding(8.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onCopyPayload,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy Text", fontSize = 12.sp)
                    }
                    Button(
                        onClick = onLaunchEmail,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = HblPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Open Email", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = HblPrimary)
            ) {
                Text("Proceed to CX Portal")
            }
        }
    )
}
