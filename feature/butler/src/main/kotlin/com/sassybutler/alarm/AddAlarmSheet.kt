package com.sassybutler.alarm

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAlarmSheet(
    existing: Alarm? = null,
    onSaved: () -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf(existing?.label ?: "") }
    var selectedDays by remember { mutableStateOf((existing?.days ?: Alarm.WEEKDAYS).toSet()) }

    var hour24 by remember { mutableStateOf(existing?.hour ?: Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) }
    var minute by remember { mutableStateOf(existing?.minute ?: Calendar.getInstance().get(Calendar.MINUTE)) }

    var showTimePicker by remember { mutableStateOf(false) }

    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Text(
                text = if (existing != null) "Amend Appointment" else "A New Appointment",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = if (existing != null) "Adjust your alarm details below." else "When shall Jeeves wake you?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Label") },
                placeholder = { Text("Unnamed Appointment") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            )

            val h12 = when {
                hour24 == 0 -> 12
                hour24 > 12 -> hour24 - 12
                else -> hour24
            }
            val period = if (hour24 < 12) "AM" else "PM"
            val timeText = "%d:%02d %s".format(h12, minute, period)

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Time", style = MaterialTheme.typography.titleMedium)
                Card(
                    onClick = { showTimePicker = true },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        text = timeText,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            Text("Days", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Alarm.DAY_ORDER.forEach { day ->
                    val isSelected = day in selectedDays
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable {
                                selectedDays = if (isSelected) selectedDays - day else selectedDays + day
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = Alarm.DAY_NAMES[day]!!.first().toString(),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            Button(
                onClick = { 
                    save(context, existing, label, selectedDays, hour24, minute, onSaved, onDismiss) 
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (existing != null) "Update Alarm" else "Set Alarm")
            }

            if (existing != null) {
                TextButton(
                    onClick = { 
                        delete(context, existing, onSaved, onDismiss) 
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Alarm")
                }
            }
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = hour24,
            initialMinute = minute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    hour24 = timePickerState.hour
                    minute = timePickerState.minute
                    showTimePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }
}

private fun save(
    context: Context, 
    existing: Alarm?, 
    label: String, 
    days: Set<Int>, 
    h: Int, 
    m: Int, 
    onSaved: () -> Unit,
    onDismiss: () -> Unit
) {
    val alarm = Alarm(
        id = existing?.id ?: AlarmStore.nextId(context),
        hour = h,
        minute = m,
        label = label.ifBlank { "Unnamed Appointment" },
        enabled = existing?.enabled ?: true,
        days = days,
    )

    existing?.let { AlarmScheduler(context).cancel(it.id, it.hour, it.minute) }

    AlarmStore.upsert(context, alarm)
    if (alarm.enabled) AlarmScheduler(context).schedule(alarm)

    onSaved()
    onDismiss()
}

private fun delete(
    context: Context, 
    existing: Alarm, 
    onSaved: () -> Unit,
    onDismiss: () -> Unit
) {
    AlarmScheduler(context).cancel(existing.id, existing.hour, existing.minute)
    AlarmStore.delete(context, existing.id)
    onSaved()
    onDismiss()
}
