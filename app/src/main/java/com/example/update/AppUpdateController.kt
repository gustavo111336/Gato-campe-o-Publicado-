package com.example.update

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.example.BuildConfig
import com.example.data.AppReleaseInfo
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.HttpURLConnection
import java.net.URL

enum class AppUpdateUiStatus {
  IDLE,
  CHECKING,
  UP_TO_DATE,
  UPDATE_AVAILABLE,
  DOWNLOADING,
  DOWNLOADED,
  INSTALLING,
  ERROR
}

data class AppUpdateUiState(
  val status: AppUpdateUiStatus = AppUpdateUiStatus.IDLE,
  val currentVersionName: String = BuildConfig.VERSION_NAME,
  val currentVersionCode: Int = BuildConfig.VERSION_CODE,
  val availableVersionCode: Int? = null,
  val availableVersionName: String? = null,
  val downloadProgressPercent: Int = 0,
  val message: String = "Versão atual: v${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
  val storeUrl: String = "https://github.com/gustavo111336/Gato-campe-o-Publicado-/releases"
)

class AppUpdateController(context: Context) {
  private val appContext = context.applicationContext
  private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(appContext)
  private var launcher: ActivityResultLauncher<IntentSenderRequest>? = null

  private val defaultGitHubReleasesUrl = "https://github.com/gustavo111336/Gato-campe-o-Publicado-/releases"
  private val defaultPlayUrl = "https://play.google.com/store/apps/details?id=${appContext.packageName}"

  private val _state = MutableStateFlow(
    AppUpdateUiState(
      storeUrl = defaultGitHubReleasesUrl
    )
  )
  val state = _state.asStateFlow()

  private val installStateListener = InstallStateUpdatedListener { installState ->
    when (installState.installStatus()) {
      InstallStatus.DOWNLOADING -> {
        val total = installState.totalBytesToDownload()
        val downloaded = installState.bytesDownloaded()
        val pct = if (total > 0L) ((downloaded * 100L) / total).toInt().coerceIn(0, 100) else 0
        _state.value = _state.value.copy(
          status = AppUpdateUiStatus.DOWNLOADING,
          downloadProgressPercent = pct,
          message = "⬇️ Baixando atualização... $pct%"
        )
      }
      InstallStatus.DOWNLOADED -> {
        _state.value = _state.value.copy(
          status = AppUpdateUiStatus.DOWNLOADED,
          downloadProgressPercent = 100,
          message = "✅ Atualização baixada! Instalando..."
        )
        completeFlexibleUpdate()
      }
      InstallStatus.INSTALLING -> {
        _state.value = _state.value.copy(
          status = AppUpdateUiStatus.INSTALLING,
          message = "⚙️ Instalando nova versão..."
        )
      }
      InstallStatus.INSTALLED -> {
        _state.value = _state.value.copy(
          status = AppUpdateUiStatus.UP_TO_DATE,
          downloadProgressPercent = 100,
          message = "✅ App atualizado com sucesso!"
        )
      }
      InstallStatus.FAILED -> {
        _state.value = _state.value.copy(
          status = AppUpdateUiStatus.ERROR,
          message = "⚠️ Falha ao instalar atualização (código ${installState.installErrorCode()})."
        )
      }
      else -> Unit
    }
  }

  init {
    try {
      appUpdateManager.registerListener(installStateListener)
    } catch (e: Exception) {
      Log.w("AppUpdateController", "Unable to register install listener: ${e.message}")
    }
  }

  fun attachLauncher(resultLauncher: ActivityResultLauncher<IntentSenderRequest>) {
    launcher = resultLauncher
  }

  fun onUpdateFlowResult(resultCode: Int) {
    if (resultCode != Activity.RESULT_OK) {
      _state.value = _state.value.copy(
        status = AppUpdateUiStatus.UPDATE_AVAILABLE,
        message = "⏸️ Atualização pausada."
      )
    }
  }

  fun checkForUpdate(
    activity: Activity?,
    autoTrigger: Boolean,
    remoteRelease: AppReleaseInfo? = null
  ) {
    _state.value = _state.value.copy(
      status = AppUpdateUiStatus.CHECKING,
      message = "🔍 Verificando atualizações no GitHub e Play Store..."
    )

    // 1. Check GitHub public repository releases API
    Thread {
      try {
        val apiUrl = URL("https://api.github.com/repos/gustavo111336/Gato-campe-o-Publicado-/releases/latest")
        val conn = (apiUrl.openConnection() as HttpURLConnection).apply {
          requestMethod = "GET"
          setRequestProperty("Accept", "application/vnd.github+json")
          connectTimeout = 4000
          readTimeout = 4000
        }
        if (conn.responseCode == 200) {
          val json = conn.inputStream.bufferedReader().use { it.readText() }
          val tagName = extractJsonField(json, "tag_name") ?: ""
          val htmlUrl = extractJsonField(json, "html_url") ?: defaultGitHubReleasesUrl
          val cleanTag = tagName.removePrefix("v").trim()
          val cleanCurrent = BuildConfig.VERSION_NAME.trim()

          if (cleanTag.isNotBlank() && cleanTag != cleanCurrent) {
            _state.value = _state.value.copy(
              status = AppUpdateUiStatus.UPDATE_AVAILABLE,
              availableVersionName = cleanTag,
              storeUrl = htmlUrl,
              message = "🚀 Nova versão v$cleanTag disponível no GitHub!"
            )
            return@Thread
          }
        }
      } catch (e: Exception) {
        Log.w("AppUpdateController", "GitHub release query error: ${e.message}")
      }

      // 2. Fallback to Firestore remote config if GitHub query didn't find newer version
      if (remoteRelease != null && remoteRelease.latestVersionCode > BuildConfig.VERSION_CODE) {
        val resolvedUrl = remoteRelease.storeUrl.ifBlank { defaultGitHubReleasesUrl }
        _state.value = _state.value.copy(
          status = AppUpdateUiStatus.UPDATE_AVAILABLE,
          availableVersionCode = remoteRelease.latestVersionCode.toInt(),
          availableVersionName = remoteRelease.latestVersionName,
          storeUrl = resolvedUrl,
          message = "🚀 Nova versão v${remoteRelease.latestVersionName} disponível!"
        )
        return@Thread
      }

      // 3. Fallback to Play In-App Updates
      try {
        appUpdateManager.appUpdateInfo
          .addOnSuccessListener { info ->
            handlePlayUpdateInfo(info, autoTrigger)
          }
          .addOnFailureListener {
            _state.value = _state.value.copy(
              status = AppUpdateUiStatus.UP_TO_DATE,
              storeUrl = defaultGitHubReleasesUrl,
              message = "✅ v${BuildConfig.VERSION_NAME} (GitHub Público) está atualizado!"
            )
          }
      } catch (_: Exception) {
        _state.value = _state.value.copy(
          status = AppUpdateUiStatus.UP_TO_DATE,
          storeUrl = defaultGitHubReleasesUrl,
          message = "✅ Versão instalada: v${BuildConfig.VERSION_NAME}."
        )
      }
    }.start()
  }

  private fun extractJsonField(json: String, key: String): String? {
    val pattern = "\"$key\"\\s*:\\s*\"([^\"]+)\"".toRegex()
    return pattern.find(json)?.groupValues?.get(1)
  }

  fun onResumeCheck(autoTrigger: Boolean) {
    try {
      appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
        if (info.installStatus() == InstallStatus.DOWNLOADED) {
          _state.value = _state.value.copy(
            status = AppUpdateUiStatus.DOWNLOADED,
            downloadProgressPercent = 100,
            message = "✅ Atualização pronta! Instalando..."
          )
          completeFlexibleUpdate()
        } else if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
          startUpdateFlow(info, AppUpdateType.IMMEDIATE)
        }
      }
    } catch (_: Exception) {
    }
  }

  private fun handlePlayUpdateInfo(info: AppUpdateInfo, autoTrigger: Boolean) {
    when {
      info.installStatus() == InstallStatus.DOWNLOADED -> {
        _state.value = _state.value.copy(
          status = AppUpdateUiStatus.DOWNLOADED,
          downloadProgressPercent = 100,
          message = "✅ Atualização baixada! Instalando..."
        )
        completeFlexibleUpdate()
      }
      info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE -> {
        val availCode = info.availableVersionCode()
        val chosenType = when {
          info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> AppUpdateType.IMMEDIATE
          info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> AppUpdateType.FLEXIBLE
          else -> null
        }
        _state.value = _state.value.copy(
          status = AppUpdateUiStatus.UPDATE_AVAILABLE,
          availableVersionCode = availCode,
          message = "🚀 Nova versão (build $availCode) disponível na Google Play!"
        )
        if (autoTrigger && chosenType != null) {
          startUpdateFlow(info, chosenType)
        }
      }
      else -> {
        if (_state.value.status != AppUpdateUiStatus.UPDATE_AVAILABLE) {
          _state.value = _state.value.copy(
            status = AppUpdateUiStatus.UP_TO_DATE,
            availableVersionCode = BuildConfig.VERSION_CODE,
            availableVersionName = BuildConfig.VERSION_NAME,
            message = "✅ App atualizado (v${BuildConfig.VERSION_NAME})."
          )
        }
      }
    }
  }

  private fun startUpdateFlow(info: AppUpdateInfo, updateType: Int) {
    val activeLauncher = launcher ?: return
    try {
      val options = AppUpdateOptions.newBuilder(updateType).build()
      appUpdateManager.startUpdateFlowForResult(info, activeLauncher, options)
    } catch (e: Exception) {
      Log.e("AppUpdateController", "startUpdateFlowForResult failed: ${e.message}")
    }
  }

  private fun completeFlexibleUpdate() {
    try {
      appUpdateManager.completeUpdate()
    } catch (e: Exception) {
      Log.w("AppUpdateController", "completeUpdate failed: ${e.message}")
    }
  }

  fun openStoreForUpdate(context: Context) {
    val targetWebUrl = _state.value.storeUrl.ifBlank { defaultGitHubReleasesUrl }
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetWebUrl)).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
      context.startActivity(intent)
    } catch (e: Exception) {
      Log.e("AppUpdateController", "Failed to open GitHub releases URL: ${e.message}")
    }
  }

  fun dispose() {
    try {
      appUpdateManager.unregisterListener(installStateListener)
    } catch (_: Exception) {
    }
  }
}
