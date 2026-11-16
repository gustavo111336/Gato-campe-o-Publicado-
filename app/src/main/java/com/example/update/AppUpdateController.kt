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
  val storeUrl: String = "https://github.com/gustavo111336/Gato-campe-o-Publicado-/tree/main/app/apk"
)

class AppUpdateController(context: Context) {
  private val appContext = context.applicationContext
  private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(appContext)
  private var launcher: ActivityResultLauncher<IntentSenderRequest>? = null
  private val prefs = appContext.getSharedPreferences("app_update_prefs", Context.MODE_PRIVATE)

  private val defaultGitHubFolderUrl = "https://github.com/gustavo111336/Gato-campe-o-Publicado-/tree/main/app/apk"
  private val defaultPlayUrl = "https://play.google.com/store/apps/details?id=${appContext.packageName}"

  private val _state = MutableStateFlow(
    AppUpdateUiState(
      storeUrl = defaultGitHubFolderUrl
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
          message = "⚠️ Falha ao instalar atualização."
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
      message = "🔍 Verificando commits no GitHub..."
    )

    // Check latest commit SHA on GitHub repository main branch
    Thread {
      try {
        val apiUrl = URL("https://api.github.com/repos/gustavo111336/Gato-campe-o-Publicado-/commits/main")
        val conn = (apiUrl.openConnection() as HttpURLConnection).apply {
          requestMethod = "GET"
          setRequestProperty("Accept", "application/vnd.github+json")
          connectTimeout = 4000
          readTimeout = 4000
        }
        if (conn.responseCode == 200) {
          val json = conn.inputStream.bufferedReader().use { it.readText() }
          val remoteSha = extractJsonField(json, "sha") ?: ""
          val localSha = prefs.getString("github_last_sha", "") ?: ""

          if (remoteSha.isNotBlank()) {
            if (localSha.isBlank()) {
              prefs.edit().putString("github_last_sha", remoteSha).apply()
              _state.value = _state.value.copy(
                status = AppUpdateUiStatus.UP_TO_DATE,
                storeUrl = defaultGitHubFolderUrl,
                message = "✅ v${BuildConfig.VERSION_NAME} está atualizado com o GitHub!"
              )
              return@Thread
            } else if (remoteSha != localSha) {
              prefs.edit().putString("github_last_sha", remoteSha).apply()
              _state.value = _state.value.copy(
                status = AppUpdateUiStatus.UPDATE_AVAILABLE,
                storeUrl = defaultGitHubFolderUrl,
                message = "🚀 Novo commit no GitHub! Toque para baixar o APK na pasta."
              )
              return@Thread
            } else {
              _state.value = _state.value.copy(
                status = AppUpdateUiStatus.UP_TO_DATE,
                storeUrl = defaultGitHubFolderUrl,
                message = "✅ v${BuildConfig.VERSION_NAME} está sincronizado com o GitHub!"
              )
              return@Thread
            }
          }
        }
      } catch (e: Exception) {
        Log.w("AppUpdateController", "GitHub commit check error: ${e.message}")
      }

      // Fallback to Firestore remote config
      if (remoteRelease != null && remoteRelease.latestVersionCode > BuildConfig.VERSION_CODE) {
        val resolvedUrl = remoteRelease.storeUrl.ifBlank { defaultGitHubFolderUrl }
        _state.value = _state.value.copy(
          status = AppUpdateUiStatus.UPDATE_AVAILABLE,
          availableVersionCode = remoteRelease.latestVersionCode.toInt(),
          availableVersionName = remoteRelease.latestVersionName,
          storeUrl = resolvedUrl,
          message = "🚀 Nova versão v${remoteRelease.latestVersionName} disponível!"
        )
        return@Thread
      }

      // Fallback status
      _state.value = _state.value.copy(
        status = AppUpdateUiStatus.UP_TO_DATE,
        storeUrl = defaultGitHubFolderUrl,
        message = "✅ v${BuildConfig.VERSION_NAME} (Repositório GitHub) atualizado!"
      )
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
            message = "✅ Atualização baixada! Instalando..."
          )
          completeFlexibleUpdate()
        }
      }
    } catch (_: Exception) {
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
    val targetWebUrl = _state.value.storeUrl.ifBlank { defaultGitHubFolderUrl }
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetWebUrl)).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
      context.startActivity(intent)
    } catch (e: Exception) {
      Log.e("AppUpdateController", "Failed to open GitHub folder URL: ${e.message}")
    }
  }

  fun dispose() {
    try {
      appUpdateManager.unregisterListener(installStateListener)
    } catch (_: Exception) {
    }
  }
}
