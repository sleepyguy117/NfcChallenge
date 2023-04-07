package com.challenge.nfc

import android.animation.Animator
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.appcompat.app.AppCompatActivity
import com.challenge.nfc.databinding.ActivitySuccessBinding

class SuccessActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySuccessBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySuccessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val reloadAmount = intent!!.getIntExtra(RELOAD_AMOUNT, Constants.RELOAD_AMOUNT1)

        binding.checkmarkAnimation.addAnimatorListener(object: Animator.AnimatorListener {
            override fun onAnimationStart(p0: Animator) {
                startVibrate()
            }
            override fun onAnimationEnd(p0: Animator) {
                finish()
            }
            override fun onAnimationCancel(p0: Animator) {
            }
            override fun onAnimationRepeat(p0: Animator) {
            }
        })


        binding.successLabel.text = String.format(getString(R.string.successfully_added_format, reloadAmount))
        binding.checkmarkAnimation.playAnimation()

    }

    override fun onPause() {
        super.onPause()
        cancelVibrate()
    }

    private fun startVibrate() {
        val pattern = longArrayOf(0, 1000)
        val amplitudes = intArrayOf(0, 100)

        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (vibrator.hasAmplitudeControl()) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, 0))
            } else {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
            }
        } else {
            vibrator.vibrate(pattern, 0)
        }
    }

    private fun cancelVibrate() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.cancel()
    }

    companion object {
        const val RELOAD_AMOUNT = "RELOAD_AMOUNT"
    }
}
