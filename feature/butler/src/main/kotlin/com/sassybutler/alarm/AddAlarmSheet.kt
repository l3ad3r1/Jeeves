package com.sassybutler.alarm

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.sassybutler.alarm.databinding.SheetAddAlarmBinding
import java.util.Calendar

/**
 * "A New Appointment" — add/edit alarm bottom sheet (design: ParlourScreen
 * modal). Pass [existing] to edit that alarm in place, with a delete link;
 * omit it to create a new one.
 */
class AddAlarmSheet(
    private val existing: Alarm? = null,
    private val onSaved: () -> Unit,
) : BottomSheetDialogFragment() {

    private var _binding: SheetAddAlarmBinding? = null
    private val binding get() = _binding!!

    private val selectedDays = (existing?.days ?: Alarm.WEEKDAYS).toMutableSet()
    private var hour24 = existing?.hour ?: Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    private var minute = existing?.minute ?: Calendar.getInstance().get(Calendar.MINUTE)

    override fun getTheme() = R.style.ParlourBottomSheet

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        _binding = SheetAddAlarmBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (existing != null) {
            binding.tvSheetTitle.text = "Amend the Appointment"
            binding.tvSheetSubtitle.text = "I shall adjust the ledger accordingly."
            binding.etLabel.setText(existing.label)
            binding.btnCreate.text = "Update the Alarm, Butler"
            binding.btnDelete.visibility = View.VISIBLE
            binding.btnDelete.setOnClickListener { delete() }
        }

        updateTimeLabel()
        binding.timeField.setOnClickListener { showTimePicker() }

        buildDayChips()
        binding.btnCreate.setOnClickListener { save() }
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
            updateTimeLabel()
        }
        picker.show(parentFragmentManager, "time_picker")
    }

    private fun updateTimeLabel() {
        val h12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        val period = if (hour24 < 12) "AM" else "PM"
        binding.tvTimeValue.text = "%d:%02d %s".format(h12, minute, period)
    }

    private fun buildDayChips() {
        Alarm.DAY_ORDER.forEach { day ->
            val chip = TextView(requireContext()).apply {
                text = Alarm.DAY_NAMES[day]!!.first().toString()
                gravity = Gravity.CENTER
                textSize = 12f
                letterSpacing = 0.04f
                applyCinzel()
                setTextColor(requireContext().getColorStateList(R.color.chip_text))
                background = requireContext().getDrawable(R.drawable.chip_bg)
                isSelected = day in selectedDays
                layoutParams = LinearLayout.LayoutParams(0, dp(36), 1f).apply {
                    marginEnd = dp(6)
                }
                setOnClickListener {
                    isSelected = !isSelected
                    if (isSelected) selectedDays.add(day) else selectedDays.remove(day)
                }
            }
            binding.dayChips.addView(chip)
        }
    }

    private fun save() {
        val ctx = requireContext()

        val alarm = Alarm(
            id = existing?.id ?: AlarmStore.nextId(ctx),
            hour = hour24,
            minute = minute,
            label = binding.etLabel.text.toString().ifBlank { "Unnamed Appointment" },
            enabled = existing?.enabled ?: true,
            days = selectedDays.toSet(),
        )

        // Cancel the existing schedule before re-arming. (PendingIntent
        // identity is the request code — the alarm id — plus filterEquals,
        // which ignores extras; hour/minute are passed only for logging.)
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

    private fun TextView.applyCinzel() {
        typeface = androidx.core.content.res.ResourcesCompat.getFont(requireContext(), R.font.cinzel)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
