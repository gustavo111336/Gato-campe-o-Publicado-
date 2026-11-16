package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

class GameAudio(private val context: Context) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  // Limit concurrent AudioTracks to avoid AudioFlinger track-pool exhaustion crashes during intense gameplay
  private val trackSemaphore = Semaphore(2)

  var isSoundEnabled: Boolean = true
  var isHapticEnabled: Boolean = true

  private val vibrator: Vibrator? by lazy {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
      } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
      }
    } catch (_: Exception) {
      null
    }
  }

  fun playCatCollectSound(isChampion: Boolean = false) {
    if (!isSoundEnabled) return
    scope.launch {
      if (isChampion) {
        playVictoryMelodySync()
      } else {
        playToneSequence(
          listOf(
            Pair(587.33f, 80),
            Pair(783.99f, 90),
            Pair(987.77f, 150)
          )
        )
      }
    }
  }

  fun playMeowNotificationSound() {
    if (!isSoundEnabled) return
    scope.launch {
      playToneSequence(
        listOf(
          Pair(680f, 95),
          Pair(880f, 90),
          Pair(620f, 110),
          Pair(760f, 140)
        )
      )
    }
  }

  fun playPowerUpSound() {
    if (!isSoundEnabled) return
    scope.launch {
      playToneSequence(
        listOf(
          Pair(523.25f, 60),
          Pair(659.25f, 60),
          Pair(783.99f, 60),
          Pair(1046.50f, 120)
        )
      )
    }
  }

  fun playGhostAlertSound() {
    if (!isSoundEnabled) return
    scope.launch {
      val sampleRate = 22050
      val durationMs = 240
      val numSamples = (sampleRate * (durationMs / 1000.0)).toInt().coerceAtLeast(1)
      val buffer = ShortArray(numSamples)
      for (i in 0 until numSamples) {
        val t = i.toDouble() / sampleRate
        val freq = 180.0 + 30.0 * sin(2.0 * PI * 6.0 * t)
        val envelope = 1.0 - (i.toDouble() / numSamples)
        val sample = (sin(2.0 * PI * freq * t) * envelope * 20000).toInt()
        buffer[i] = sample.toShort()
      }
      playPcmBuffer(buffer, sampleRate)
    }
  }

  fun playGhostHitSound() {
    if (!isSoundEnabled) return
    scope.launch {
      playToneSequence(
        listOf(
          Pair(300.0f, 70),
          Pair(200.0f, 90),
          Pair(130.0f, 160)
        )
      )
    }
  }

  fun playDashSound() {
    if (!isSoundEnabled) return
    scope.launch {
      playToneSequence(
        listOf(
          Pair(400.0f, 45),
          Pair(800.0f, 65)
        )
      )
    }
  }

  fun playVictoryMelody() {
    if (!isSoundEnabled) return
    scope.launch {
      playVictoryMelodySync()
    }
  }

  private fun playVictoryMelodySync() {
    val notes = listOf(
      Pair(523.25f, 100),
      Pair(659.25f, 100),
      Pair(783.99f, 100),
      Pair(1046.5f, 180),
      Pair(880.0f, 100),
      Pair(1046.5f, 320)
    )
    playToneSequence(notes)
  }

  fun playSonicMeowSound() {
    if (!isSoundEnabled) return
    scope.launch {
      playToneSequence(
        listOf(
          Pair(880.0f, 60),
          Pair(660.0f, 80),
          Pair(990.0f, 110)
        )
      )
    }
  }

  fun playFreezeSound() {
    if (!isSoundEnabled) return
    scope.launch {
      playToneSequence(
        listOf(
          Pair(1174.66f, 70),
          Pair(1396.91f, 90),
          Pair(1567.98f, 130)
        )
      )
    }
  }

  fun playAchievementSound() {
    if (!isSoundEnabled) return
    scope.launch {
      playToneSequence(
        listOf(
          Pair(659.25f, 70),
          Pair(783.99f, 70),
          Pair(987.77f, 70),
          Pair(1318.51f, 220)
        )
      )
    }
  }

  private fun playToneSequence(tones: List<Pair<Float, Int>>) {
    if (!trackSemaphore.tryAcquire()) return
    try {
      val sampleRate = 22050
      // Concatenate tones into a SINGLE PCM buffer so only 1 AudioTrack is allocated per sound effect!
      val totalSamples = tones.sumOf { (_, ms) -> (sampleRate * (ms / 1000.0)).toInt().coerceAtLeast(1) }
      if (totalSamples <= 0) return
      val combined = ShortArray(totalSamples)
      var offset = 0
      for ((freq, durationMs) in tones) {
        val count = (sampleRate * (durationMs / 1000.0)).toInt().coerceAtLeast(1)
        for (i in 0 until count) {
          if (offset >= totalSamples) break
          val t = i.toDouble() / sampleRate
          val envelope = 1.0 - (i.toDouble() / count) * 0.7
          val sample = (sin(2.0 * PI * freq * t) * envelope * 20000).toInt()
          combined[offset++] = sample.toShort()
        }
      }
      playPcmBufferInternal(combined, sampleRate)
    } finally {
      trackSemaphore.release()
    }
  }

  private fun playPcmBuffer(buffer: ShortArray, sampleRate: Int) {
    if (!trackSemaphore.tryAcquire()) return
    try {
      playPcmBufferInternal(buffer, sampleRate)
    } finally {
      trackSemaphore.release()
    }
  }

  private fun playPcmBufferInternal(buffer: ShortArray, sampleRate: Int) {
    var track: AudioTrack? = null
    try {
      val minBuf = AudioTrack.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT
      )
      if (minBuf <= 0) return

      val byteSize = max(buffer.size * 2, minBuf)
      track = AudioTrack.Builder()
        .setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        )
        .setAudioFormat(
          AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        )
        .setBufferSizeInBytes(byteSize)
        .setTransferMode(AudioTrack.MODE_STATIC)
        .build()

      if (track.state != AudioTrack.STATE_INITIALIZED) {
        return
      }

      val written = track.write(buffer, 0, buffer.size)
      if (written > 0) {
        track.play()
        val waitMs = ((buffer.size.toDouble() / sampleRate) * 1000).toLong().coerceIn(30L, 1200L) + 25L
        Thread.sleep(waitMs)
      }
    } catch (_: Throwable) {
      // Graceful fallback on devices/emulators with unavailable audio hardware
    } finally {
      try {
        if (track?.playState == AudioTrack.PLAYSTATE_PLAYING) {
          track.stop()
        }
      } catch (_: Throwable) {
      }
      try {
        track?.release()
      } catch (_: Throwable) {
      }
    }
  }

  fun vibrateCollect() {
    if (!isHapticEnabled) return
    vibrate(40)
  }

  fun vibrateGhostHit() {
    if (!isHapticEnabled) return
    vibrate(180)
  }

  fun vibrateDash() {
    if (!isHapticEnabled) return
    vibrate(25)
  }

  fun vibrateVictory() {
    if (!isHapticEnabled) return
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val timings = longArrayOf(0, 100, 80, 150, 100, 300)
        val amplitudes = intArrayOf(0, 180, 0, 220, 0, 255)
        vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
      } else {
        @Suppress("DEPRECATION")
        vibrator?.vibrate(300)
      }
    } catch (_: Throwable) {
    }
  }

  private fun vibrate(durationMs: Long) {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
      } else {
        @Suppress("DEPRECATION")
        vibrator?.vibrate(durationMs)
      }
    } catch (_: Throwable) {
    }
  }
}
