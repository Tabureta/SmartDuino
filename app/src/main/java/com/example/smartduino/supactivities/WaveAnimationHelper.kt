package com.example.smartduino.supactivities

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.os.Handler
import android.os.Looper

class WaveAnimationHelper(
    private val container: FrameLayout,
    private val waveDrawableRes: Int
) {
    private val waveViews = mutableListOf<View>()
    private val handler = Handler(Looper.getMainLooper())
    var isAnimating = false
    private var waveInterval: Long = 800
    private var animationDuration: Long = 1000

    private val waveRunnable = object : Runnable {
        override fun run() {
            createWave()
            if (isAnimating) {
                handler.postDelayed(this, waveInterval)
            }
        }
    }

    fun startWaveAnimation() {
        if (isAnimating) return

        isAnimating = true
        handler.post(waveRunnable)
    }

    fun stopWaveAnimation() {
        isAnimating = false
        handler.removeCallbacks(waveRunnable)
        clearWaves()
    }

    fun setWaveInterval(interval: Long) {
        waveInterval = interval
    }

    fun setAnimationDuration(duration: Long) {
        animationDuration = duration
    }

    private fun createWave() {
        val waveView = ImageView(container.context).apply {
            setImageResource(waveDrawableRes)
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.CENTER }
            scaleX = 0.2f
            scaleY = 0.2f
            alpha = 0.8f
        }

        container.addView(waveView)
        waveViews.add(waveView)

        waveView.animate()
            .scaleX(4f)
            .scaleY(4f)
            .alpha(0f)
            .setDuration(animationDuration)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                container.removeView(waveView)
                waveViews.remove(waveView)
            }
            .start()
    }

    private fun clearWaves() {
        waveViews.forEach { container.removeView(it) }
        waveViews.clear()
    }

    fun cleanup() {
        stopWaveAnimation()
    }
}