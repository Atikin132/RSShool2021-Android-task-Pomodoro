package com.rsshool.pomodoro

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import com.rsshool.pomodoro.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), TimerListener, LifecycleObserver {
    private lateinit var binding: ActivityMainBinding

    private val timerAdapter = TimerAdapter(this)
    private val timers = mutableListOf<Timer>()
    private var nextId = 0
    private var backPressed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        binding.addNewTimerButton.setOnClickListener {
            if (binding.minuteEdit.text.isEmpty()) {
                Toast.makeText(this, "Введите нужное количество минут", Toast.LENGTH_SHORT).show()
            } else {
                if (binding.minuteEdit.text.toString().toLong() > 5999L) {
                    Toast.makeText(this, "Введите что-нибудь меньше 100 часов", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    val seconds = binding.minuteEdit.text.toString().toLong() * 60000
                    timers.add(
                        Timer(
                            nextId++,
                            seconds,
                            seconds, 0L,
                            false,
                            resources.getColor(R.color.red_light),
                        )
                    )
                    timerAdapter.submitList(timers.toList())
                }
            }
        }

        binding.recycler.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = timerAdapter
        }

    }

    override fun onBackPressed() {
        backPressed = true
        val stopIntent = Intent(this, ForegroundService::class.java)
        stopIntent.putExtra(COMMAND_ID, COMMAND_STOP)
        startService(stopIntent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        val stopIntent = Intent(this, ForegroundService::class.java)
        stopIntent.putExtra(COMMAND_ID, COMMAND_STOP)
        startService(stopIntent)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackgrounded() {
        val startIntent = Intent(this, ForegroundService::class.java)
        startIntent.putExtra(COMMAND_ID, COMMAND_START)
        if (backPressed) {
            timers.clear()
        }
        if (timers.size > 0) {
            var id = 0
            for (i in 0 until timers.size) {
                if (timers[i].isStarted) {
                    id = i
                    break
                }
            }
            if (timers[id].isStarted && timers.size > 0) {
                startIntent.putExtra(STARTED_TIMER_TIME_MS, timers[id].currentMs)
                startIntent.putExtra(MINUTE_VALUE, timers[id].minValue)

                startService(startIntent)
            }
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppForegrounded() {
        if (timers.size > 0) {
            val stopIntent = Intent(this, ForegroundService::class.java)
            stopIntent.putExtra(COMMAND_ID, COMMAND_STOP)
            startService(stopIntent)
        }
    }

    override fun onResume() {
        super.onResume()
        binding.minuteEdit.clearFocus()
    }

    override fun start(id: Int) {
        changeTimer(id, null, true)
        changeOtherTimer(id)

    }

    override fun stop(id: Int, currentMs: Long) {
        changeTimer(id, currentMs, false)
    }

    override fun delete(id: Int) {
        timers.remove(timers.find { it.id == id })
        timerAdapter.submitList(timers.toList())
    }

    private fun changeTimer(id: Int, currentMs: Long?, isStarted: Boolean) {
        val newTimers = mutableListOf<Timer>()
        timers.forEach {
            if (it.id == id) {
                newTimers.add(
                    Timer(
                        it.id,
                        currentMs ?: it.currentMs, it.minValue, 0L,
                        isStarted, it.endColor
                    )
                )
            } else {
                newTimers.add(it)
            }
        }
        timerAdapter.submitList(newTimers)
        timers.clear()
        timers.addAll(newTimers)
    }

    private fun changeOtherTimer(id: Int) {
        val newTimers = mutableListOf<Timer>()
        timers.forEach {
            if (it.id != id) {
                newTimers.add(
                    Timer(
                        it.id,
                        null ?: it.currentMs, it.minValue, 0L,
                        false, it.endColor
                    )
                )
            } else {
                newTimers.add(it)
            }
        }
        timerAdapter.submitList(newTimers)
        timers.clear()
        timers.addAll(newTimers)
    }


}

data class Timer(
    val id: Int,
    var currentMs: Long,
    var minValue: Long,
    var startCurrentTimeMillis: Long,
    var isStarted: Boolean, val endColor: Int
)