package com.sassybutler.alarm

import android.app.AlarmManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.sassybutler.alarm.ui.ButlerTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainAlarmSetupActivity : AppCompatActivity() {
    private lateinit var scheduler: AlarmScheduler

    private val locationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) refreshWeather() }

    private val calendarPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> refreshCalendar() }

    private var weatherState by mutableStateOf<WeatherService.Weather?>(null)
    private var alarmsState by mutableStateOf(emptyList<Alarm>())
    private var honorificState by mutableStateOf("Sir")
    private var calendarEventsState by mutableStateOf<List<CalendarSyncManager.TodayEvent>>(emptyList())
    private var calendarPermissionGrantedState by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        val isEmbedded = intent.getBooleanExtra("EXTRA_EMBEDDED", false)
        super.onCreate(savedInstanceState)
        scheduler = AlarmScheduler(this)

        if (isEmbedded) {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }

        setContent {
            var showAddAlarm by remember { mutableStateOf<Alarm?>(null) }
            var isAddingNewAlarm by remember { mutableStateOf(false) }
            var showPrefs by remember { mutableStateOf(false) }

            ButlerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainAlarmScreen(
                        alarms = alarmsState,
                        weather = weatherState,
                        calendarEvents = calendarEventsState,
                        calendarPermissionGranted = calendarPermissionGrantedState,
                        honorific = honorificState,
                        onToggle = { toggleAlarm(it) },
                        onDelete = { deleteAlarm(it) },
                        onEdit = { alarm ->
                            if (alarm != null) showAddAlarm = alarm else isAddingNewAlarm = true
                        },
                        onPrefs = {
                            showPrefs = true
                        }
                    )
                }

                if (showAddAlarm != null || isAddingNewAlarm) {
                    AddAlarmSheet(
                        existing = showAddAlarm,
                        onSaved = { refresh() },
                        onDismiss = {
                            showAddAlarm = null
                            isAddingNewAlarm = false
                        }
                    )
                }

                if (showPrefs) {
                    PreferencesSheet(
                        onDismiss = { 
                            refresh()
                            showPrefs = false 
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
        checkExactAlarmPermission()

        if (WeatherService.hasLocationPermission(this)) {
            refreshWeather()
        } else {
            locationPermission.launch(android.Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        if (CalendarSyncManager.hasReadPermission(this)) {
            refreshCalendar()
        } else {
            calendarPermission.launch(
                arrayOf(
                    android.Manifest.permission.READ_CALENDAR,
                    android.Manifest.permission.WRITE_CALENDAR
                )
            )
        }
    }

    private fun refreshCalendar() {
        calendarPermissionGrantedState = CalendarSyncManager.hasReadPermission(this)
        calendarEventsState = CalendarSyncManager.todayEvents(this)
    }

    private fun refreshWeather() {
        weatherState = WeatherService.cached(this)
        lifecycleScope.launch {
            WeatherService.refresh(this@MainAlarmSetupActivity)?.let { weatherState = it }
        }
    }

    private fun refresh() {
        honorificState = ButlerPrefs.honorific(this)
        alarmsState = AlarmStore.all(this)
    }

    private fun toggleAlarm(alarm: Alarm) {
        val updated = alarm.copy(enabled = !alarm.enabled)
        AlarmStore.upsert(this, updated)
        if (updated.enabled) {
            if (checkExactAlarmPermission()) scheduler.schedule(updated)
        } else {
            scheduler.cancel(alarm.id, alarm.hour, alarm.minute)
        }
        refresh()
    }

    private fun deleteAlarm(alarm: Alarm) {
        scheduler.cancel(alarm.id, alarm.hour, alarm.minute)
        AlarmStore.delete(this, alarm.id)
        refresh()
        Toast.makeText(this, "Appointment struck from the ledger.", Toast.LENGTH_SHORT).show()
    }



    private fun checkExactAlarmPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = getSystemService(ALARM_SERVICE) as AlarmManager
            if (!am.canScheduleExactAlarms()) {
                Toast.makeText(this, "I require the exact-alarm permission to be punctual.", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                return false
            }
        }
        return true
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAlarmScreen(
    alarms: List<Alarm>,
    weather: WeatherService.Weather?,
    calendarEvents: List<CalendarSyncManager.TodayEvent>,
    calendarPermissionGranted: Boolean,
    honorific: String,
    onToggle: (Alarm) -> Unit,
    onDelete: (Alarm) -> Unit,
    onEdit: (Alarm?) -> Unit,
    onPrefs: () -> Unit
) {
    var currentTime by remember { mutableStateOf(Date()) }
    var alarmToDelete by remember { mutableStateOf<Alarm?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Date()
            delay(1000)
        }
    }

    if (alarmToDelete != null) {
        AlertDialog(
            onDismissRequest = { alarmToDelete = null },
            title = { Text("Delete Alarm") },
            text = { Text("Are you sure you want to delete this alarm?") },
            confirmButton = {
                TextButton(onClick = {
                    alarmToDelete?.let { onDelete(it) }
                    alarmToDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { alarmToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { onEdit(null) }) {
                Text("+")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Header — label + greeting share one text block so a two-line
                // greeting stays left-aligned under the label instead of drifting
                // under the cog button (SpaceBetween previously fought this).
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "JEEVES DAYBOOK",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = greetingFor(honorific),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape)
                            .clickable(onClick = onPrefs),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Preferences",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            item {
                // Clock card — time and date only; weather and calendar are their
                // own cards below so each source of truth reads independently.
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // alignByBaseline, not box-bottom alignment: with two different
                        // text sizes, Alignment.Bottom aligns descent boxes and the
                        // smaller seconds visibly sag below the minutes' baseline.
                        Text(
                            text = SimpleDateFormat("HH:mm", Locale.UK).format(currentTime),
                            style = MaterialTheme.typography.displayLarge,
                            modifier = Modifier.alignByBaseline()
                        )
                        Text(
                            text = SimpleDateFormat(":ss", Locale.UK).format(currentTime),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.alignByBaseline().padding(start = 4.dp)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = SimpleDateFormat("EEE", Locale.UK).format(currentTime).uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = SimpleDateFormat("d MMM", Locale.UK).format(currentTime),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }

            item {
                WeatherCard(weather = weather)
            }

            item {
                CalendarCard(events = calendarEvents, permissionGranted = calendarPermissionGranted)
            }

            item {
                // Alarms card — header (count + preview action) and list share one
                // card boundary instead of floating loose against the screen edge.
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${alarms.size} ALARM${if (alarms.size == 1) "" else "S"} SCHEDULED",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (alarms.isEmpty()) {
                            Text(
                                text = "No appointments yet. Tap + to add one.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                alarms.forEach { alarm ->
                                    AlarmItem(
                                        alarm = alarm,
                                        onToggle = onToggle,
                                        onDeleteRequest = { alarmToDelete = alarm },
                                        onClick = { onEdit(alarm) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeatherCard(weather: WeatherService.Weather?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Cloud,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            if (weather != null) {
                Text(
                    text = "${weather.tempC}° · ${weather.label}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                Text(
                    text = "Weather unavailable — check location permission",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CalendarCard(events: List<CalendarSyncManager.TodayEvent>, permissionGranted: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Today",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            when {
                !permissionGranted -> Text(
                    text = "Calendar access needed to show today's events.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                events.isEmpty() -> Text(
                    text = "Nothing on the calendar today.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                else -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    events.take(3).forEach { event ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (event.allDay) "All day" else
                                    SimpleDateFormat("HH:mm", Locale.UK).format(Date(event.startMillis)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.width(52.dp)
                            )
                            Text(
                                text = event.title,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (events.size > 3) {
                        Text(
                            text = "+ ${events.size - 3} more",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmItem(
    alarm: Alarm,
    onToggle: (Alarm) -> Unit,
    onDeleteRequest: () -> Unit,
    onClick: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                onDeleteRequest()
                return@rememberSwipeToDismissBoxState false
            }
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color by androidx.compose.animation.animateColorAsState(
                if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) MaterialTheme.colorScheme.errorContainer
                else Color.Transparent
            )
            Box(
                modifier = Modifier.fillMaxSize().background(color).padding(16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                    Text("Delete", color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        },
        enableDismissFromStartToEnd = false
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { onClick() },
            colors = CardDefaults.cardColors(
                containerColor = if (alarm.enabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val h12 = when {
                        alarm.hour == 0 -> 12
                        alarm.hour > 12 -> alarm.hour - 12
                        else -> alarm.hour
                    }
                    val amPm = if (alarm.hour < 12) "AM" else "PM"
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$h12:${alarm.minute.toString().padStart(2, '0')}",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = " $amPm",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    Text(text = alarm.label, style = MaterialTheme.typography.bodyMedium)
                    Text(text = alarm.daysLabel(), style = MaterialTheme.typography.labelSmall)
                }
                Switch(
                    checked = alarm.enabled,
                    onCheckedChange = { onToggle(alarm) }
                )
            }
        }
    }
}

fun greetingFor(hon: String): String {
    val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        h < 5  -> "Good evening, $hon."
        h < 12 -> "Good morning, $hon."
        h < 17 -> "Good afternoon, $hon."
        h < 21 -> "Good evening, $hon."
        else   -> "Rather late, isn't it, $hon?"
    }
}
