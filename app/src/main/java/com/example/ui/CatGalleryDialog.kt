package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.CHAMPION_CATS

@Composable
fun CatGalleryDialog(
  uiState: GameUiState,
  onDismiss: () -> Unit
) {
  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight(0.85f)
        .padding(8.dp)
        .shadow(24.dp, RoundedCornerShape(24.dp))
        .border(2.dp, Color(0xFF38BDF8), RoundedCornerShape(24.dp))
        .testTag("cat_gallery_dialog"),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp)
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "📖 Livro dos 10 Felinos",
              fontSize = 18.sp,
              fontWeight = FontWeight.Black,
              color = Color(0xFF38BDF8)
            )
            Text(
              text = "Resgate todos os 10 para ser o Campeão!",
              fontSize = 11.sp,
              color = Color(0xFF94A3B8)
            )
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("close_gallery_button")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Fechar",
              tint = Color.White
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // List of 10 Cats
        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.weight(1f)
        ) {
          items(CHAMPION_CATS) { cat ->
            val isUnlocked = uiState.championCount > 0 || uiState.catsCollected >= cat.id
            CatCard(cat = cat, isUnlocked = isUnlocked)
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
          onClick = onDismiss,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Voltar ao Jogo 🐾", color = Color.White, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
fun CatCard(cat: com.example.model.CatBreed, isUnlocked: Boolean) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = if (isUnlocked) Color(0xFF1E293B) else Color(0xFF131C2E),
    border = androidx.compose.foundation.BorderStroke(
      width = 1.dp,
      color = if (cat.id == 10) Color(0xFFFDE047) else Color(0xFF334155)
    ),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Cat Avatar Badge
      Box(
        modifier = Modifier
          .size(48.dp)
          .background(cat.furColor, CircleShape)
          .border(2.dp, cat.earColor, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Text(cat.accessoryEmoji, fontSize = 22.sp)
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "${cat.id}. ${cat.name}",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = if (cat.id == 10) Color(0xFFFDE047) else Color.White
          )
          if (cat.id == 10) {
            Text(" 👑 O CAMPEÃO", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFFFACC15))
          }
        }
        Text(
          text = cat.title,
          fontSize = 11.sp,
          fontWeight = FontWeight.Medium,
          color = Color(0xFF38BDF8)
        )
        Text(
          text = cat.description,
          fontSize = 11.sp,
          color = Color(0xFF94A3B8),
          modifier = Modifier.padding(top = 2.dp)
        )
      }
    }
  }
}
