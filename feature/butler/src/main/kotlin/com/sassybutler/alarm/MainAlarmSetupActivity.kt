package com.sassybutler.alarm

import android.app.AlarmManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sassybutler.alarm.databinding.ActivityMainBinding
import com.sassybutler.alarm.databinding.ItemAlarmBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * The Parlour — main screen (design: ParlourScreen.tsx).
 *
 *  • Time-of-day greeting addressed with the honorific
 *  • Live ink clock strip
 *  • Multi-alarm list: toggle, swipe-to-dismiss
 *  • Serving-bell FAB → AddAlarmSheet
 *  • Bowler hat → PreferencesSheet
 *  • "Preview Wake-Up →" fires the alarm service immediately
 */
class MainAlarmSetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var scheduler: AlarmScheduler
    private lateinit var adapter: AlarmAdapter

    private val clockHandler = Handler(Looper.getMainLooper())
    private val clockTick = object : Runnable {
        override fun run() {
            updateClock()
            clockHandler.postDelayed(this, 1000)
        }
    }

    private val locationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) refreshWeather() }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate(): reads the manifest's Splash
        // theme, shows the tuxedo icon, then hands off to postSplashScreenTheme.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        scheduler = AlarmScheduler(this)
        setContentView(binding.root)

        adapter = AlarmAdapter()
        binding.rvAlarms.layoutManager = LinearLayoutManager(this)
        binding.rvAlarms.adapter = adapter
        attachSwipeToDelete()

        binding.fabAdd.setOnClickListener {
            AddAlarmSheet(onSaved = { refresh() }).show(supportFragmentManager, "add_alarm")
        }
        binding.btnPrefs.setOnClickListener {
            PreferencesSheet { refresh() }.show(supportFragmentManager, "prefs")
        }
        binding.btnPreviewWake.setOnClickListener { previewWakeUp() }
    }

    override fun onResume() {
        super.onResume()
        refresh()
        clockHandler.post(clockTick)
        checkExactAlarmPermission()

        if (WeatherService.hasLocationPermission(this)) {
            refreshWeather()
        } else {
            locationPermission.launch(android.Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    /** Show cache instantly, then fetch fresh from Open-Meteo. */
    private fun refreshWeather() {
        WeatherService.cached(this)?.let { showWeather(it) }
        lifecycleScope.launch {
            WeatherService.refresh(this@MainAlarmSetupActivity)?.let { showWeather(it) }
        }
    }

    private fun showWeather(w: WeatherService.Weather) {
        binding.tvWeather.text = "${w.tempC}° · ${w.label}"
        binding.tvWeather.visibility = View.VISIBLE
    }

    override fun onPause() {
        clockHandler.removeCallbacks(clockTick)
        super.onPause()
    }

    // ─── UI state ────────────────────────────────────────────────────────

    private fun refresh() {
        val hon = ButlerPrefs.honorific(this)
        binding.tvGreeting.text = greetingFor(hon)
        binding.tvEmptyTitle.text = "No appointments, $hon."

        val alarms = AlarmStore.all(this)
        adapter.submit(alarms)
        binding.tvScheduledCount.text = "${alarms.size} SCHEDULED"
        binding.emptyState.visibility = if (alarms.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun updateClock() {
        val now = Date()
        binding.tvClock.text = SimpleDateFormat("HH:mm", Locale.UK).format(now)
        binding.tvClockSeconds.text = SimpleDateFormat(":ss", Locale.UK).format(now)
        binding.tvWeekday.text = SimpleDateFormat("EEE", Locale.UK).format(now).uppercase()
        binding.tvDate.text = SimpleDateFormat("d MMM", Locale.UK).format(now)
    }

    private fun greetingFor(hon: String): String {
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            h < 5  -> "Good evening, $hon. The night watches over you."
            h < 12 -> "Good morning, $hon. Shall I prepare your schedule?"
            h < 17 -> "Good afternoon, $hon. You are, I note, still here."
            h < 21 -> "Good evening, $hon. Shall we review the morrow?"
            else   -> "Rather late, isn't it, $hon?"
        }
    }

    // ─── Alarm actions ───────────────────────────────────────────────────

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

    private fun previewWakeUp() {
        val intent = Intent(this, AlarmForegroundService::class.java).apply {
            action = AlarmForegroundService.ACTION_START_ALARM
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, 999)
            putExtra(AlarmReceiver.EXTRA_ALARM_HOUR, Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
            putExtra(AlarmReceiver.EXTRA_ALARM_MINUTE, Calendar.getInstance().get(Calendar.MINUTE))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
        else startService(intent)
    }

    private fun checkExactAlarmPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = getSystemService(ALARM_SERVICE) as AlarmManager
            if (!am.canScheduleExactAlarms()) {
                Toast.makeText(this,
                    "I require the exact-alarm permission to be punctual.",
                    Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                return false
            }
        }
        return true
    }

    // ─── Swipe to delete ─────────────────────────────────────────────────

    private fun attachSwipeToDelete() {
        val helper = ItemTouchHelper(object :
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder,
                                t: RecyclerView.ViewHolder) = false

            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
                adapter.alarmAt(vh.layoutPosition)?.let { deleteAlarm(it) }
            }
        })
        helper.attachToRecyclerView(binding.rvAlarms)
    }

    // ─── Adapter ─────────────────────────────────────────────────────────

    private inner class AlarmAdapter : RecyclerView.Adapter<AlarmAdapter.Holder>() {
        private val items = mutableListOf<Alarm>()

        fun submit(list: List<Alarm>) {
            items.clear(); items.addAll(list)
            notifyDataSetChanged()
        }

        fun alarmAt(position: Int): Alarm? = items.getOrNull(position)

        inner class Holder(val b: ItemAlarmBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder(ItemAlarmBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val alarm = items[position]
            val b = holder.b

            val h12 = when {
                alarm.hour == 0 -> 12
                alarm.hour > 12 -> alarm.hour - 12
                else -> alarm.hour
            }
            b.tvTime.text = "$h12:${alarm.minute.toString().padStart(2, '0')}"
            b.tvPeriod.text = if (alarm.hour < 12) "AM" else "PM"
            b.tvLabel.text = alarm.label
            b.tvDays.text = alarm.daysLabel()

            val inkColor = getColor(if (alarm.enabled) R.color.ink else R.color.faded_grey)
            b.tvTime.setTextColor(inkColor)
            b.tvPeriod.setTextColor(getColor(
                if (alarm.enabled) R.color.powder_blue else R.color.faded_grey))
            b.cardAlarm.setCardBackgroundColor(getColor(
                if (alarm.enabled) R.color.card else R.color.parchment))

            b.switchEnabled.setOnCheckedChangeListener(null)
            b.switchEnabled.isChecked = alarm.enabled
            b.switchEnabled.setOnCheckedChangeListener { _, _ -> toggleAlarm(alarm) }

            b.cardAlarm.setOnClickListener {
                AddAlarmSheet(existing = alarm, onSaved = { refresh() })
                    .show(supportFragmentManager, "edit_alarm")
            }
        }
    }

    companion object {
        private const val TAG = "Parlour"
    }
}
