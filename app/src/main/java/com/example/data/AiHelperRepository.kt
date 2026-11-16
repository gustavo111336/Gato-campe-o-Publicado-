package com.example.data

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

private const val TAG = "AiHelperRepository"

sealed interface AiTipUiState {
  data object Idle : AiTipUiState
  data object Loading : AiTipUiState
  data class Streaming(val text: String) : AiTipUiState
  data class Success(val text: String) : AiTipUiState
  data class Error(val message: String) : AiTipUiState
}

class AiHelperRepository {
  private val primaryModel by lazy {
    Firebase.ai.generativeModel("gemini-3.8-flash")
  }

  private val fallbackModel by lazy {
    Firebase.ai.generativeModel("gemini-3.5-flash-lite")
  }

  fun streamGameTip(prompt: String): Flow<AiTipUiState> = flow {
    emit(AiTipUiState.Loading)
    val sb = StringBuilder()
    var primaryError: Exception? = null

    try {
      withTimeout(60_000L) {
        primaryModel.generateContentStream(prompt).collect { chunk ->
          val piece = chunk.text.orEmpty()
          if (piece.isNotEmpty()) {
            sb.append(piece)
            emit(AiTipUiState.Streaming(sb.toString()))
          }
        }
      }
      if (sb.isBlank()) {
        val unaryPrimary = withTimeout(60_000L) {
          primaryModel.generateContent(prompt)
        }
        val unaryText = unaryPrimary.text.orEmpty()
        if (unaryText.isNotBlank()) {
          sb.append(unaryText)
        }
      }
    } catch (e: CancellationException) {
      if (e !is TimeoutCancellationException) throw e
      primaryError = e
      Log.w(TAG, "Primary AI model timed out, trying lite fallback", e)
    } catch (e: Exception) {
      primaryError = e
      Log.w(TAG, "Primary AI model failed (${e.message}), trying lite fallback", e)
    }

    if (sb.isNotBlank()) {
      emit(AiTipUiState.Success(sb.toString()))
      return@flow
    }

    // Fallback to gemini-3.5-flash-lite if primary failed or returned empty before streaming any text
    try {
      withTimeout(60_000L) {
        fallbackModel.generateContentStream(prompt).collect { chunk ->
          val piece = chunk.text.orEmpty()
          if (piece.isNotEmpty()) {
            sb.append(piece)
            emit(AiTipUiState.Streaming(sb.toString()))
          }
        }
      }
      if (sb.isBlank()) {
        val unary = withTimeout(60_000L) {
          fallbackModel.generateContent(prompt)
        }
        val unaryText = unary.text.orEmpty()
        if (unaryText.isNotBlank()) {
          sb.append(unaryText)
        }
      }
      if (sb.isNotBlank()) {
        emit(AiTipUiState.Success(sb.toString()))
      } else {
        val hint = primaryError?.localizedMessage?.let { " ($it)" }.orEmpty()
        emit(AiTipUiState.Error("A IA retornou uma resposta vazia$hint. Tente novamente."))
      }
    } catch (e: TimeoutCancellationException) {
      Log.e(TAG, "AI tip request timed out on fallback", e)
      emit(AiTipUiState.Error("Tempo esgotado ao consultar a IA. Verifique sua conexão com a internet."))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Firebase AI tip failed", e)
      val raw = e.localizedMessage ?: primaryError?.localizedMessage ?: "Falha ao consultar a IA no Firebase."
      val friendly = if (raw.contains("403", ignoreCase = true) || raw.contains("App Check", ignoreCase = true) || raw.contains("AppCheck", ignoreCase = true)) {
        "Falha de verificação App Check / acesso à IA ($raw). Verifique a internet ou abra o app pelo ambiente autorizado."
      } else {
        raw
      }
      emit(AiTipUiState.Error(friendly))
    }
  }
}
