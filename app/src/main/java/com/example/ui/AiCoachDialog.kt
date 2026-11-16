package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.AiTipUiState

@Composable
fun AiCoachDialog(
  uiState: GameUiState,
  aiTipState: AiTipUiState,
  onAskAi: (String?) -> Unit,
  onDismiss: () -> Unit
) {
  var customQuestion by remember { mutableStateOf("") }

  LaunchedEffect(Unit) {
    if (aiTipState is AiTipUiState.Idle || aiTipState is AiTipUiState.Error) {
      onAskAi(null)
    }
  }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight(0.82f)
        .padding(4.dp)
        .shadow(24.dp, RoundedCornerShape(24.dp))
        .border(2.dp, Color(0xFF22D3EE), RoundedCornerShape(24.dp))
        .testTag("ai_coach_dialog"),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "🤖 IA Guia Felina",
              fontSize = 18.sp,
              fontWeight = FontWeight.Black,
              color = Color(0xFF67E8F9)
            )
            Text(
              text = "Análise tática em tempo real da sua partida",
              fontSize = 11.sp,
              color = Color(0xFF94A3B8)
            )
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Fechar IA", tint = Color.White)
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quick prompts
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          OutlinedButton(
            onClick = { onAskAi("Me dê uma dica rápida para a situação atual da partida!") },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            border = BorderStroke(1.dp, Color(0xFF22D3EE)),
            modifier = Modifier.weight(1f)
          ) {
            Text("💡 Dica Agora", fontSize = 11.sp, color = Color(0xFF67E8F9))
          }
          OutlinedButton(
            onClick = { onAskAi("Como escapar rápido quando os fantasmas estão muito perto?") },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            border = BorderStroke(1.dp, Color(0xFFC084FC)),
            modifier = Modifier.weight(1f)
          ) {
            Text("👻 Fugir", fontSize = 11.sp, color = Color(0xFFE9D5FF))
          }
          OutlinedButton(
            onClick = { onAskAi("Quais itens devo priorizar para subir de nível e vencer?") },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            border = BorderStroke(1.dp, Color(0xFFFDE047)),
            modifier = Modifier.weight(1f)
          ) {
            Text("🎁 Itens", fontSize = 11.sp, color = Color(0xFFFDE047))
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Live streaming answer card
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = Color(0xFF1E293B),
          border = BorderStroke(1.dp, Color(0xFF334155)),
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
        ) {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(14.dp)
              .verticalScroll(rememberScrollState())
          ) {
            when (aiTipState) {
              is AiTipUiState.Idle -> {
                Text(
                  text = "Toque em 'Dica Agora' ou envie sua dúvida abaixo!",
                  fontSize = 13.sp,
                  color = Color(0xFFCBD5E1)
                )
              }
              is AiTipUiState.Loading -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color(0xFF22D3EE),
                    strokeWidth = 2.5.dp
                  )
                  Spacer(modifier = Modifier.width(10.dp))
                  Text(
                    text = "Analisando sua arena e fantasmas...",
                    fontSize = 13.sp,
                    color = Color(0xFF67E8F9)
                  )
                }
              }
              is AiTipUiState.Streaming -> {
                Text(
                  text = aiTipState.text,
                  fontSize = 14.sp,
                  color = Color.White,
                  lineHeight = 20.sp
                )
              }
              is AiTipUiState.Success -> {
                Text(
                  text = aiTipState.text,
                  fontSize = 14.sp,
                  color = Color.White,
                  lineHeight = 20.sp
                )
              }
              is AiTipUiState.Error -> {
                Text(
                  text = "⚠️ Erro da IA: ${aiTipState.message}",
                  fontSize = 13.sp,
                  color = Color(0xFFF87171),
                  fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                  onClick = { onAskAi(customQuestion.trim().ifEmpty { null }) },
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.height(48.dp).testTag("ai_retry_button")
                ) {
                  Text("🔄 Tentar Novamente", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Custom question input
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedTextField(
            value = customQuestion,
            onValueChange = { customQuestion = it },
            placeholder = { Text("Pergunte algo para a IA...", fontSize = 12.sp, color = Color(0xFF64748B)) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White,
              focusedBorderColor = Color(0xFF22D3EE),
              unfocusedBorderColor = Color(0xFF334155)
            ),
            modifier = Modifier.weight(1f)
          )
          IconButton(
            onClick = {
              val q = customQuestion.trim()
              if (q.isNotEmpty()) {
                onAskAi(q)
                customQuestion = ""
              }
            },
            modifier = Modifier.size(48.dp)
          ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar pergunta", tint = Color(0xFF22D3EE))
          }
        }
      }
    }
  }
}
