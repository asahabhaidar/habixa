package com.example

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Habit
import com.example.ui.HabitStats
import com.example.ui.HabitViewModel
import com.example.ui.HabitUiState
import com.example.ui.theme.MyApplicationTheme
import com.example.utils.DateUtils

class MainActivity : ComponentActivity() {
    private val viewModel: HabitViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("habixa_scaffold"),
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    HabixaAppScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabixaAppScreen(
    viewModel: HabitViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var habitToDelete by remember { mutableStateOf<Habit?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Elegant App Top Header
            AppHeader(
                onHelpClick = { showHelpDialog = true },
                onResetHistoryClick = { viewModel.resetAllHistoricalProgress() }
            )

            // Weekly Calendar Strip Row (Horizontal Selection list of 7 days)
            CalendarStripRow(
                selectedDate = uiState.selectedDate,
                onDateSelected = { viewModel.selectDate(it) }
            )

            // Dynamic Main List utilizing LazyColumn
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Today Summary Progress Card
                item {
                    ProgressSummaryCard(
                        uiState = uiState,
                        onResetSelectedDateCompletions = { showResetDialog = true }
                    )
                }

                // Header for Habit section
                item {
                    Text(
                        text = if (uiState.isToday) "Today's Routine" else "Routine for ${uiState.selectedDate}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                if (uiState.habits.isEmpty()) {
                    // Empty Routine state
                    item {
                        EmptyHabitState(onAddClick = { showAddDialog = true })
                    }
                } else {
                    // Habit card items
                    items(uiState.habits) { habit ->
                        val stats = uiState.habitStats[habit.id] ?: HabitStats(0, 0, 0, false, emptySet())
                        HabitCard(
                            habit = habit,
                            stats = stats,
                            onToggleCompletion = { viewModel.toggleHabitCompletion(habit.id) },
                            onDelete = { habitToDelete = habit }
                        )
                    }
                }

                // Dynamic Onboarding Tip widget for first time consistency tracking
                item {
                    OnboardingTipSection()
                }
            }
        }

        // Action FAB to add habit
        LargeFloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_habit_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Create Clean Habit",
                modifier = Modifier.size(28.dp)
            )
        }

        // Dialog - Create Habit Sheet
        if (showAddDialog) {
            AddHabitDialog(
                onDismiss = { showAddDialog = false },
                onSave = { title, desc, icon, color ->
                    viewModel.addHabit(title, desc, icon, color)
                    showAddDialog = false
                }
            )
        }

        // Dialog - Reset Current Date Progress Confirmation
        if (showResetDialog) {
            ResetConfirmDialog(
                onDismiss = { showResetDialog = false },
                onConfirm = {
                    viewModel.resetSelectedDateTasks()
                    showResetDialog = false
                }
            )
        }

        // Dialog - Help & Quick Beginner Guide Popup
        if (showHelpDialog) {
            HelpInstructionsDialog(onDismiss = { showHelpDialog = false })
        }

        // Dialog - Habit Deletion confirmation
        habitToDelete?.let { habit ->
            AlertDialog(
                onDismissRequest = { habitToDelete = null },
                title = { Text("Delete Habit?") },
                text = { Text("Are you sure you want to delete '${habit.title}'? This will remove all calculated streaks and completions for this habit.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteHabit(habit)
                            habitToDelete = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { habitToDelete = null }) {
                        Text("Cancel")
                    }
                },
                modifier = Modifier.testTag("delete_confirm_dialog")
            )
        }
    }
}

@Composable
fun AppHeader(
    onHelpClick: () -> Unit,
    onResetHistoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSecondarySettings by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Habixa",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "Build better daily routines ✨",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onHelpClick,
                modifier = Modifier.testTag("help_icon_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                    contentDescription = "How to use Habixa app",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
            }

            Box {
                IconButton(
                    onClick = { showSecondarySettings = true },
                    modifier = Modifier.testTag("settings_menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options and Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = showSecondarySettings,
                    onDismissRequest = { showSecondarySettings = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Clear Completion History") },
                        onClick = {
                            onResetHistoryClick()
                            showSecondarySettings = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CalendarStripRow(
    selectedDate: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val daysList = remember { DateUtils.getLast7Days() }
    val todayDateStr = remember { DateUtils.getTodayDateString() }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Track Consistency",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 8.dp)
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("calendar_lazy_row"),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(daysList) { (dateStr, displayLabel) ->
                val isSelected = dateStr == selectedDate
                val isToday = dateStr == todayDateStr
                val parts = displayLabel.split("\n")
                val dayName = parts.getOrNull(0) ?: ""
                val dayNum = parts.getOrNull(1) ?: ""

                val borderModifier = if (isToday && !isSelected) {
                    Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    )
                } else Modifier

                Column(
                    modifier = Modifier
                        .width(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                        .then(borderModifier)
                        .clickable { onDateSelected(dateStr) }
                        .padding(vertical = 10.dp)
                        .testTag("calendar_day_$dateStr"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dayNum,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                    if (isToday) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProgressSummaryCard(
    uiState: HabitUiState,
    onResetSelectedDateCompletions: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag("progress_summary_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(18.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Column: Circular Progress Meter
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(96.dp)
                    .testTag("circular_progress_box")
            ) {
                CircularProgressIndicator(
                    progress = { if (uiState.totalCount > 0) uiState.completionPercentage / 100f else 0f },
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 8.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${uiState.completionPercentage}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "${uiState.completedCount}/${uiState.totalCount}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Right Column: Summary metrics and Actions
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (uiState.isToday) "Today's Routine Progress" else "Consistency Progress",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "Max Streak",
                        tint = Color(0xFFFF5722),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Global Best Streak: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "${uiState.bestActiveStreak} days",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Total Completions",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Total Completes: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "${uiState.totalCompletions} times",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                if (uiState.completedCount > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = onResetSelectedDateCompletions,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .testTag("reset_daily_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Reset This Date",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HabitCard(
    habit: Habit,
    stats: HabitStats,
    onToggleCompletion: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColor = parseHexColor(habit.colorHex, MaterialTheme.colorScheme.primary)
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("habit_card_${habit.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (stats.isCompletedOnSelectedDate) themeColor.copy(alpha = 0.08f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Colored category bullet or icon
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(themeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getHabitIcon(habit.iconName),
                    contentDescription = null,
                    tint = themeColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Middle info description
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = habit.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Compact streak flame
                    if (stats.currentStreak > 0) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFFEBE6), RoundedCornerShape(6.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "🔥 ${stats.currentStreak}d",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFF5722),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (habit.description.isNotEmpty()) {
                    Text(
                        text = habit.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = "Build better consistency daily",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                // Micro record analytics under card
                if (stats.totalCompletions > 0) {
                    Text(
                        text = "Completed ${stats.totalCompletions} times overall | Max streak: ${stats.maxStreak}d",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Right actions: Complete checkbox & delete options
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("delete_habit_button_${habit.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Habit record",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (stats.isCompletedOnSelectedDate) themeColor
                            else Color.Transparent
                        )
                        .border(
                            2.dp,
                            if (stats.isCompletedOnSelectedDate) Color.Transparent else themeColor.copy(
                                alpha = 0.5f
                            ),
                            CircleShape
                        )
                        .clickable { onToggleCompletion() }
                        .testTag("toggle_completion_button_${habit.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (stats.isCompletedOnSelectedDate) Icons.Default.Check else Icons.Default.Add,
                        contentDescription = "Toggle completion",
                        tint = if (stats.isCompletedOnSelectedDate) Color.White else themeColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyHabitState(
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("empty_habit_state_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Spa,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Welcome to your new Routine!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "No habits are added yet. Click below to add a starting routine or press the button to build customized trackers.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("empty_add_button")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Create First Habit")
            }
        }
    }
}

@Composable
fun OnboardingTipSection(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F6FF)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = Color(0xFF7E57C2),
                modifier = Modifier
                    .size(20.dp)
                    .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Habixa Tips & Advice:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4A148C)
                )
                Text(
                    text = "consistency beats intensity! Select any previous day in the scroll strip to mark completed tasks you missed, keeping your streaks alive! Tap on complete status when finished.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4A148C).copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun AddHabitDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, desc: String, icon: String, color: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#9C27B0") } // Purple
    var selectedIconName by remember { mutableStateOf("Star") }

    val colorsMap = remember {
        listOf(
            "#9C27B0" to "Purple",
            "#2196F3" to "Blue",
            "#4CAF50" to "Green",
            "#E91E63" to "Pink",
            "#FF9800" to "Orange"
        )
    }

    val iconsMap = remember {
        listOf("Star", "Water", "Book", "Fitness", "Meditation", "Coffee", "Food", "Study")
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .testTag("add_habit_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Build New Habit 📈",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("What is the habit name?") },
                    placeholder = { Text("e.g. Read 15 pages, Workout") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("habit_title_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Short motivation or goal detail") },
                    placeholder = { Text("e.g. 15 minutes before sleeping") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("habit_desc_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Color picker block
                Column {
                    Text(
                        text = "Pick Brand Accent Color",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        colorsMap.forEach { (hex, name) ->
                            val isSelected = hex == selectedColor
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(AndroidColor.parseColor(hex)))
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = hex }
                                    .testTag("color_picker_$name"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Icon picker block
                Column {
                    Text(
                        text = "Choose Habit Icon representation",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(iconsMap) { name ->
                            val isSelected = name == selectedIconName
                            val parsedColor = parseHexColor(selectedColor, MaterialTheme.colorScheme.primary)
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) parsedColor.copy(alpha = 0.2f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    )
                                    .clickable { selectedIconName = name }
                                    .testTag("icon_picker_$name"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getHabitIcon(name),
                                    contentDescription = name,
                                    tint = if (isSelected) parsedColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Submit Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("cancel_habit_button")
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.trim().isNotEmpty()) {
                                onSave(title, desc, selectedIconName, selectedColor)
                            }
                        },
                        enabled = title.trim().isNotEmpty(),
                        modifier = Modifier.testTag("create_habit_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = parseHexColor(selectedColor, MaterialTheme.colorScheme.primary))
                    ) {
                        Text("Build Routines")
                    }
                }
            }
        }
    }
}

@Composable
fun ResetConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reset Routine Progress?") },
        text = { Text("This will clear all checked/completed icons for this Selected Date. Current streaks might decrease. Are you sure you want to proceed?") },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag("reset_confirm_button"),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Reset Work")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("reset_cancel_button")
            ) {
                Text("Cancel")
            }
        },
        modifier = modifier.testTag("reset_confirm_dialog")
    )
}

@Composable
fun HelpInstructionsDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier.testTag("help_instructions_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Beginner Quick Start Guide 💡",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Welcome to Habixa, the simple habit routine companion! Here's how to build your schedule consistency in three easy steps:\n\n" +
                            "1. **Tapping Complete:** Tap the round checklist button on any task card to mark it as done! Doing so automatically builds your dynamic visual fire count (streaks! 🔥).\n\n" +
                            "2. **Backfill Dates:** Tapped a calendar day in the top header to examine previous dates or tick days you missed, helping you stay consistent.\n\n" +
                            "3. **Actions:** Delete habits by pressing the trashcan icon, or completely clear daily completions for a clean slate with the Reset action directly in the dashboard.\n\n" +
                            "Building better habits has never been so beginner-friendly!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().testTag("close_help_button")
                ) {
                    Text("Got It, Thanks!")
                }
            }
        }
    }
}

// Utility mapper to parse hex color safely
fun parseHexColor(hex: String, fallback: Color): Color {
    return try {
        Color(AndroidColor.parseColor(hex))
    } catch (e: Exception) {
        fallback
    }
}

// Utility helper to map string keys to high visual quality icons
fun getHabitIcon(iconName: String): ImageVector {
    return when (iconName) {
        "Water" -> Icons.Default.WaterDrop
        "Book" -> Icons.Default.MenuBook
        "Fitness" -> Icons.Default.DirectionsRun
        "Meditation" -> Icons.Default.Spa
        "Coffee" -> Icons.Default.Coffee
        "Food" -> Icons.Default.Restaurant
        "Study" -> Icons.Default.School
        "Star" -> Icons.Default.Star
        else -> Icons.Default.CheckCircle
    }
}
