package com.sassybutler.alarm

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.sassybutler.alarm.databinding.ActivityAlarmBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Wake-up screen (design: WakeUpScreen.tsx).
 *
 *  • Bypasses the lock screen, turns the screen on
 *  • Night-gradient backdrop, giant serif clock
 *  • "Butler speaking" card typewrites the actual spoken greeting
 *  • Circular pulsing Hush button; snooze link below
 */
class AlarmActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlarmBinding

    private var alarmId = -1

    private val clockHandler = Handler(Looper.getMainLooper())
    private val clockTick = object : Runnable {
        override fun run() {
            val now = Date()
            binding.tvTime.text = SimpleDateFormat("HH:mm", Locale.UK).format(now)
            binding.tvSeconds.text = SimpleDateFormat("ss", Locale.UK).format(now)
            clockHandler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ── Lock screen bypass (API 27+ preferred path) ──────────────
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )

        disableBack()
        binding = ActivityAlarmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        alarmId = intent.getIntExtra(AlarmReceiver.EXTRA_ALARM_ID, -1)

        binding.tvDate.text = SimpleDateFormat("EEEE, d MMMM", Locale.UK)
            .format(Date()).uppercase()
        binding.btnSnooze.text = "Snooze (${ButlerPrefs.snoozeMinutes(this)} min)"

        binding.btnHush.setOnClickListener {
            it.isEnabled = false
            sendServiceAction(AlarmForegroundService.ACTION_DISMISS_ALARM)
            finish()
        }
        binding.btnSnooze.setOnClickListener {
            sendServiceAction(AlarmForegroundService.ACTION_SNOOZE_ALARM)
            finish()
        }

        typewrite(intent.getStringExtra(AlarmForegroundService.EXTRA_GREETING)
            ?: "Good morning. Consciousness is now required.")
        startPulse(binding.pulseRing1, 0L)
        startPulse(binding.pulseRing2, 1250L)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.getStringExtra(AlarmForegroundService.EXTRA_GREETING)?.let { typewrite(it) }
    }

    override fun onResume() {
        super.onResume()
        clockHandler.post(clockTick)
    }

    override fun onPause() {
        clockHandler.removeCallbacks(clockTick)
        super.onPause()
    }

    // User cannot simply go Back to dismiss the alarm
    private fun disableBack() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { /* no-op — dismiss explicitly */ }
        })
    }

    /** Design's 42 ms/char typewriter for the butler's line. */
    private fun typewrite(line: String) {
        lifecycleScope.launch {
            val quoted = "“$line”"
            for (i in 1..quoted.length) {
                binding.tvGreeting.text = quoted.substring(0, i)
                delay(42)
            }
        }
    }

    /** Expanding, fading ring behind the Hush button (design pulse-ring). */
    private fun startPulse(ring: View, startDelay: Long) {
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2500
            this.startDelay = startDelay
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { anim ->
                val f = anim.animatedFraction
                val scale = 1f + 0.7f * f
                ring.scaleX = scale
                ring.scaleY = scale
                ring.alpha = 0.5f * (1f - f)
            }
            start()
        }
    }

    private fun sendServiceAction(action: String) {
        startService(Intent(this, AlarmForegroundService::class.java).apply {
            this.action = action
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
        })
    }
}
