package com.sassybutler.alarm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.sassybutler.alarm.ui.ButlerTheme
import java.util.Calendar

class AddAlarmSheet(
    private val existing: Alarm? = null,
    private val onSaved: () -> Unit,
) : BottomSheetDialogFragment() {

    private var hour24 = existing?.hour ?: Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    private var minute = existing?.minute ?: Calendar.getInstance().get(Calendar.MINUTE)
    
    // We update this state to trigger recomposition when the picker returns
    private val timeState = mutableStateOf(Pair(hour24, minute))

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                ButlerTheme {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        AddAlarmContent()
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun AddAlarmContent() {
        var label by remember { mutableStateOf(existing?.label ?: "") }
        var selectedDays by remember { mutableStateOf((existing?.days ?: Alarm.WEEKDAYS).toSet()) }
        val (h, m) = timeState.value

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = if (existing != null) "Amend the Appointment" else "A New Appointment",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = if (existing != null) "I shall adjust the ledger accordingly." else "When shall I disturb you?",
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
                h == 0 -> 12
                h > 12 -> h - 12
                else -> h
            }
            val period = if (h < 12) "AM" else "PM"
            val timeText = "%d:%02d %s".format(h12, m, period)

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Time", style = MaterialTheme.typography.titleMedium)
                Card(
                    onClick = { showTimePicker() },
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
                            .size(40.dp)
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
                onClick = { save(label, selectedDays, h, m) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (existing != null) "Update the Alarm, Butler" else "Make it so, Butler")
            }

            if (existing != null) {
                TextButton(
                    onClick = { delete() },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remove from Ledger")
                }
            }
        }
    }

    private fun showTimePicker() {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(hour24)
            .setMinute(minute)
            .setTitleText("Select time")
            .build()
        picker.addOnPositiveButtonClickListener {
            hour24 = picker.hour
            minute = picker.minute
            timeState.value = Pair(hour24, minute)
        }
        picker.show(parentFragmentManager, "time_picker")
    }

    private fun save(label: String, days: Set<Int>, h: Int, m: Int) {
        val ctx = requireContext()
        val alarm = Alarm(
            id = existing?.id ?: AlarmStore.nextId(ctx),
            hour = h,
            minute = m,
            label = label.ifBlank { "Unnamed Appointment" },
            enabled = existing?.enabled ?: true,
            days = days,
        )

        existing?.let { AlarmScheduler(ctx).cancel(it.id, it.hour, it.minute) }

        AlarmStore.upsert(ctx, alarm)
        if (alarm.enabled) AlarmScheduler(ctx).schedule(alarm)

        onSaved()
        dismiss()
    }

    private fun delete() {
        val ctx = requireContext()
        existing?.let {
            AlarmScheduler(ctx).cancel(it.id, it.hour, it.minute)
            AlarmStore.delete(ctx, it.id)
        }
        onSaved()
        dismiss()
    }
}
