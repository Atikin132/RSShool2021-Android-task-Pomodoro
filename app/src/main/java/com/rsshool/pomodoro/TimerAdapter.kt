package com.rsshool.pomodoro

import android.graphics.Color
import android.graphics.drawable.AnimationDrawable
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rsshool.pomodoro.databinding.TimerItemBinding

class TimerAdapter(
    private val listener: TimerListener
) : ListAdapter<Timer, TimerAdapter.TimerViewHolder>(itemComparator) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimerViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = TimerItemBinding.inflate(layoutInflater, parent, false)
        return TimerViewHolder(binding, listener)
    }

    override fun onBindViewHolder(holder: TimerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private companion object {

        private val itemComparator = object : DiffUtil.ItemCallback<Timer>() {

            override fun areItemsTheSame(oldItem: Timer, newItem: Timer): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Timer, newItem: Timer): Boolean {
                return oldItem.currentMs == newItem.currentMs &&
                        oldItem.isStarted == newItem.isStarted
            }

            override fun getChangePayload(oldItem: Timer, newItem: Timer) = Any()
        }

    }


    class TimerViewHolder(
        private val binding: TimerItemBinding,
        private val listener: TimerListener,
    ) : RecyclerView.ViewHolder(binding.root) {

        private var timer: CountDownTimer? = null

        fun bind(timer: Timer) {
            if (timer.currentMs <= 100L) {
                binding.timer.text = "00:00:00"
                binding.startPauseButton.setBackgroundColor(Color.GRAY)
                binding.startPauseButton.isEnabled = false
                binding.timerItemLayout.setCardBackgroundColor(timer.endColor)
                binding.deleteButton.setBackgroundColor(timer.endColor)
                binding.completeIndicator.isInvisible = true
            } else {
                binding.timer.text = timer.currentMs.displayTime()
                val current = timer.minValue - timer.currentMs
                binding.completeIndicator.isVisible = true
                binding.completeIndicator.setPeriod(timer.minValue)
                binding.completeIndicator.setCurrent(current)
                if (timer.isStarted) {
                    startTimer(timer)
                    binding.completeIndicator.isVisible = true
                } else {
                    stopTimer(timer)
                    if (timer.currentMs == timer.minValue) {
                        binding.completeIndicator.isInvisible = true
                        binding.startPauseButton.setBackgroundColor(timer.endColor)
                        binding.startPauseButton.isEnabled = true
                        binding.timerItemLayout.setCardBackgroundColor(Color.WHITE)
                        binding.deleteButton.setBackgroundColor(Color.WHITE)
                    }
                }
            }
            initButtonsListeners(timer)
        }

        private fun initButtonsListeners(timer: Timer) {
            binding.startPauseButton.setOnClickListener {
                if (timer.isStarted) {
                    listener.stop(timer.id, timer.currentMs)
                } else {
                    listener.start(timer.id)
                }
            }
            binding.deleteButton.setOnClickListener { listener.delete(timer.id) }
        }

        private fun startTimer(timer1: Timer) {
            binding.startPauseButton.text = "Stop"
            timer?.cancel()
            timer = getCountDownTimer(timer1)
            timer?.start()

            if (timer1.currentMs > 100L) {
                binding.blinkingIndicator.isInvisible = false
                (binding.blinkingIndicator.background as? AnimationDrawable)?.start()
            }
        }

        private fun stopTimer(timer1: Timer) {
            binding.startPauseButton.text = "Start"

            timer?.cancel()

            binding.blinkingIndicator.isInvisible = true
            (binding.blinkingIndicator.background as? AnimationDrawable)?.stop()
        }

        private fun getCountDownTimer(timer1: Timer): CountDownTimer {
            return object : CountDownTimer(timer1.currentMs, UNIT_TEN_MS) {
                override fun onTick(millisUntilFinished: Long) {
                    timer1.currentMs = millisUntilFinished
                    val current = timer1.minValue - millisUntilFinished
                    binding.timer.text = (timer1.currentMs).displayTime()
                    binding.completeIndicator.setPeriod(timer1.minValue)
                    binding.completeIndicator.setCurrent(current)
                }

                override fun onFinish() {
                    binding.startPauseButton.text = "Start"
                    timer?.cancel()
                    binding.startPauseButton.isEnabled = false
                    binding.startPauseButton.setBackgroundColor(Color.GRAY)
                    binding.timerItemLayout.setCardBackgroundColor(timer1.endColor)
                    binding.deleteButton.setBackgroundColor(timer1.endColor)
                    binding.blinkingIndicator.isInvisible = true
                    (binding.blinkingIndicator.background as? AnimationDrawable)?.stop()
                    binding.completeIndicator.isInvisible = true
                }
            }
        }


        private fun Long.displayTime(): String {
            if (this <= 0L) {
                return START_TIME
            }
            val h = this / 1000 / 3600
            val m = this / 1000 % 3600 / 60
            val s = this / 1000 % 60
            return "${displaySlot(h)}:${displaySlot(m)}:${displaySlot(s)}"
        }

        private fun displaySlot(count: Long): String {
            return if (count / 10L > 0) {
                "$count"
            } else {
                "0$count"
            }
        }

        private companion object {
            private const val START_TIME = "00:00:00"
            private const val UNIT_TEN_MS = 10L
        }
    }

}


