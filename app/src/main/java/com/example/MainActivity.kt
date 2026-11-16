package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.MultiplayerRepository
import com.example.firebase.configureAppCheck
import com.example.model.Difficulty
import com.example.model.GameStatus
import com.example.model.Vector2D
import com.example.notifications.CatNotificationHelper
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme
import com.example.update.AppUpdateController
import com.example.update.AppUpdateUiStatus
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // CRITICAL: Must initialize App Check before any Firebase AI Logic / Auth / Firestore call or ViewModel
    configureAppCheck(this, intent)
    CatNotificationHelper.ensureChannel(this)
    enableEdgeToEdge()

    setContent {
      MyApplicationTheme(darkTheme = true) {
        val context = LocalContext.current
        val viewModel: GameViewModel = viewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val aiTipState by viewModel.aiTipState.collectAsStateWithLifecycle()

        var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
        DisposableEffect(Unit) {
          val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
          }
          Firebase.auth.addAuthStateListener(listener)
          onDispose {
            Firebase.auth.removeAuthStateListener(listener)
          }
        }

        val activityScope = rememberCoroutineScope()
        val lifecycleOwner = LocalLifecycleOwner.current
        val updateController = remember(context) { AppUpdateController(context) }
        val updateState by updateController.state.collectAsStateWithLifecycle()

        val updateFlowLauncher = rememberLauncherForActivityResult(
          contract = ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
          updateController.onUpdateFlowResult(result.resultCode)
        }

        LaunchedEffect(updateFlowLauncher) {
          updateController.attachLauncher(updateFlowLauncher)
        }

        LaunchedEffect(updateState) {
          val isAvail = updateState.status == AppUpdateUiStatus.UPDATE_AVAILABLE ||
            updateState.status == AppUpdateUiStatus.DOWNLOADING ||
            updateState.status == AppUpdateUiStatus.DOWNLOADED ||
            updateState.status == AppUpdateUiStatus.INSTALLING
          val isDownloading = updateState.status == AppUpdateUiStatus.DOWNLOADING ||
            updateState.status == AppUpdateUiStatus.INSTALLING
          viewModel.syncAppUpdateUi(
            statusText = updateState.message,
            isAvailable = isAvail,
            isDownloading = isDownloading,
            progressPercent = updateState.downloadProgressPercent
          )
        }

        val runAppUpdateCheck: (Boolean) -> Unit = { autoTrigger ->
          activityScope.launch {
            val remoteRelease = if (Firebase.auth.currentUser != null) {
              runCatching {
                MultiplayerRepository(context).fetchAppReleaseConfig().getOrNull()
              }.getOrNull()
            } else {
              null
            }
            updateController.checkForUpdate(
              activity = this@MainActivity,
              autoTrigger = autoTrigger,
              remoteRelease = remoteRelease
            )
          }
        }

        LaunchedEffect(Unit) {
          if (uiState.autoUpdateEnabled) {
            runAppUpdateCheck(true)
          }
        }

        DisposableEffect(lifecycleOwner, uiState.autoUpdateEnabled) {
          val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
              updateController.onResumeCheck(uiState.autoUpdateEnabled)
            }
          }
          lifecycleOwner.lifecycle.addObserver(observer)
          onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
          }
        }

        DisposableEffect(updateController) {
          onDispose {
            updateController.dispose()
          }
        }

        val notificationPermissionLauncher = rememberLauncherForActivityResult(
          contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
          if (granted) {
            viewModel.sendMeowNotification(context)
          } else {
            viewModel.sendMeowNotification(context)
          }
        }

        val triggerMeowNotification: () -> Unit = {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !CatNotificationHelper.hasNotificationPermission(context)
          ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
          } else {
            viewModel.sendMeowNotification(context)
          }
        }

        // Scope OnlineMultiplayerViewModel strictly to authenticated user UID
        val activeUser = currentUser
        val onlineVm: OnlineMultiplayerViewModel? = if (activeUser != null) {
          key(activeUser.uid) {
            viewModel(
              key = activeUser.uid,
              factory = OnlineMultiplayerViewModel.factory(
                context = context,
                currentUserId = activeUser.uid,
                email = activeUser.email.orEmpty(),
                displayName = activeUser.displayName.orEmpty()
              )
            )
          }
        } else {
          LaunchedEffect(Unit) {
            viewModel.updateMultiplayerPlayers(null, emptyList(), "", "cat", 100, null, null)
          }
          null
        }

        // Sync active multiplayer room players (supports 100+ players, instant leave alert, and Elite voting ghost role)
        var currentActiveRoomDiff by remember { mutableStateOf("NORMAL") }
        if (activeUser != null && onlineVm != null) {
          val activeRoom by onlineVm.activeRoom.collectAsStateWithLifecycle()
          val roomPlayers by onlineVm.roomPlayersState.collectAsStateWithLifecycle()
          val leftAlert by onlineVm.leftPlayerAlert.collectAsStateWithLifecycle()

          LaunchedEffect(activeRoom, roomPlayers, leftAlert, activeUser.uid) {
            val room = activeRoom
            currentActiveRoomDiff = room?.difficulty ?: "NORMAL"
            val myRole = if (room != null && room.difficulty == "ELITE" && room.ghostPlayerUserId == activeUser.uid) {
              "ghost"
            } else {
              "cat"
            }
            viewModel.updateMultiplayerPlayers(
              roomCode = room?.code,
              players = roomPlayers,
              myUserId = activeUser.uid,
              myRole = myRole,
              maxPlayers = room?.maxPlayers?.toInt() ?: 100,
              winnerUsername = room?.winnerUsername?.takeIf { it.isNotBlank() },
              leftPlayerMessage = leftAlert
            )
            if (leftAlert != null) {
              onlineVm.consumeLeftPlayerAlert()
            }
          }

          val roomCode = activeRoom?.code
          val isPlaying = uiState.gameStatus == GameStatus.PLAYING || uiState.gameStatus == GameStatus.VICTORY
          LaunchedEffect(roomCode, isPlaying, uiState.gameStatus, uiState.selectedSkinId, activeRoom?.ghostPlayerUserId, activeRoom?.difficulty) {
            val room = activeRoom
            if (roomCode != null && isPlaying && room != null) {
              while (isActive && activeRoom != null && activeRoom?.status != "finished") {
                val won = uiState.gameStatus == GameStatus.VICTORY
                val role = if (activeRoom?.difficulty == "ELITE" && activeRoom?.ghostPlayerUserId == activeUser.uid) {
                  "ghost"
                } else {
                  "cat"
                }
                onlineVm.syncArenaProgress(
                  x = viewModel.engine.heroPosition.x,
                  y = viewModel.engine.heroPosition.y,
                  score = viewModel.engine.catsCollected,
                  role = role,
                  skinId = uiState.selectedSkinId,
                  winnerUserId = if (won) activeUser.uid else null,
                  winnerUsername = if (won) onlineVm.suggestedUsername() else null
                )
                if (won) break
                delay(450)
              }
            }
          }
        }

        // Handle Back button cleanly across all overlays and dialogs
        BackHandler(enabled = true) {
          when {
            uiState.showOnlineDialog -> viewModel.openOnlineDialog(false)
            uiState.showShopDialog -> viewModel.openShop(false)
            uiState.showAiDialog -> viewModel.openAiDialog(false)
            uiState.showCreatorDialog -> viewModel.openCreator(false)
            uiState.showSkinsDialog -> viewModel.openSkins(false)
            uiState.showModesDialog -> viewModel.openModes(false)
            uiState.showItemsDialog -> viewModel.openItems(false)
            uiState.showAchievementsDialog -> viewModel.openAchievements(false)
            uiState.showGalleryDialog -> viewModel.openGallery(false)
            uiState.showSettingsDialog -> viewModel.openSettings(false)
            uiState.showVictoryDialog -> viewModel.closeVictoryDialog()
            uiState.showGameOverDialog -> viewModel.closeGameOverDialog()
            uiState.gameStatus == GameStatus.PLAYING -> viewModel.onTogglePause()
            uiState.gameStatus == GameStatus.PAUSED -> viewModel.onTogglePause()
            else -> finish()
          }
        }

        Scaffold(
          modifier = Modifier.fillMaxSize(),
          containerColor = Color(0xFF0F172A),
          contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding)
              .background(Color(0xFF0F172A))
          ) {
            // Main Game World Canvas
            GameCanvas(
              engine = viewModel.engine,
              uiState = uiState,
              onTouchDirectDirection = { dir ->
                viewModel.inputVector = dir
              }
            )

            // Interactive Controls and HUD
            ControlsOverlay(
              uiState = uiState,
              onDirectionChanged = { dir ->
                viewModel.inputVector = dir
              },
              onDash = {
                viewModel.onTriggerDash()
              },
              onSonicWave = {
                viewModel.onTriggerSonicWave()
              },
              onPauseToggle = {
                viewModel.onTogglePause()
              },
              onOpenSettings = {
                viewModel.openSettings(true)
              },
              onOpenGallery = {
                viewModel.openGallery(true)
              },
              onOpenAchievements = {
                viewModel.openAchievements(true)
              },
              onOpenAi = {
                viewModel.openAiDialog(true)
              }
            )

            // Start Screen Overlay
            if (uiState.gameStatus == GameStatus.READY) {
              ReadyStartScreen(
                uiState = uiState,
                onStartNormal = { viewModel.startNormalGame() },
                onStartEndless = { viewModel.startEndlessGame() },
                onStartBots = { viewModel.startBotsGame() },
                onDifficultyChange = { viewModel.setDifficulty(it) },
                onOpenModes = { viewModel.openModes(true) },
                onOpenCreator = { viewModel.openCreator(true) },
                onOpenSkins = { viewModel.openSkins(true) },
                onOpenShop = { viewModel.openShop(true) },
                onOpenAchievements = { viewModel.openAchievements(true) },
                onOpenItems = { viewModel.openItems(true) },
                onOpenGallery = { viewModel.openGallery(true) },
                onOpenSettings = { viewModel.openSettings(true) },
                onOpenOnline = { viewModel.openOnlineDialog(true) },
                onOpenAi = { viewModel.openAiDialog(true) },
                onCheckUpdate = { runAppUpdateCheck(true) },
                onSendMeowNotification = triggerMeowNotification,
                onRestoreDefaultFair = { viewModel.restoreDefaultFairSettings() }
              )
            }

            // Pause Overlay Banner
            if (uiState.gameStatus == GameStatus.PAUSED) {
              PauseOverlay(
                onResume = { viewModel.onTogglePause() },
                onRestart = { viewModel.restartCurrentMode() },
                onReturnToMenu = { viewModel.returnToMenu() },
                onOpenSettings = { viewModel.openSettings(true) }
              )
            }

            // Dialogs
            if (uiState.showVictoryDialog) {
              VictoryDialog(
                uiState = uiState,
                onPlayAgain = { viewModel.restartCurrentMode() },
                onPlayEndless = { viewModel.playEndlessAfterVictory() },
                onReturnToMenu = { viewModel.returnToMenu() },
                onOpenGallery = { viewModel.openGallery(true) },
                onDismiss = { viewModel.closeVictoryDialog() }
              )
            }

            if (uiState.showGameOverDialog) {
              GameOverDialog(
                uiState = uiState,
                onPlayAgain = { viewModel.restartCurrentMode() },
                onUseRevive = { viewModel.useRevive() },
                onOpenShopForRevives = {
                  viewModel.closeGameOverDialog()
                  viewModel.openShop(true)
                },
                onReturnToMenu = { viewModel.returnToMenu() },
                onDismiss = { viewModel.closeGameOverDialog() }
              )
            }

            if (uiState.showModesDialog) {
              ModesSelectorDialog(
                uiState = uiState,
                isModeUnlocked = { mode -> viewModel.isModeEffectivelyUnlocked(mode, uiState) },
                onSelectMode = { mode -> viewModel.startMode(mode) },
                onOpenShop = {
                  viewModel.openModes(false)
                  viewModel.openShop(true)
                },
                onDismiss = { viewModel.openModes(false) }
              )
            }

            if (uiState.showItemsDialog) {
              ItemsCatalogDialog(
                uiState = uiState,
                onOpenShop = {
                  viewModel.openItems(false)
                  viewModel.openShop(true)
                },
                onDismiss = { viewModel.openItems(false) }
              )
            }

            if (uiState.showAchievementsDialog) {
              AchievementsDialog(
                uiState = uiState,
                onDismiss = { viewModel.openAchievements(false) }
              )
            }

            if (uiState.showGalleryDialog) {
              CatGalleryDialog(
                uiState = uiState,
                onDismiss = { viewModel.openGallery(false) }
              )
            }

            if (uiState.showSkinsDialog) {
              SkinsDialog(
                uiState = uiState,
                isSkinUnlocked = { skin -> viewModel.isSkinEffectivelyUnlocked(skin, uiState) },
                onSelectSkin = { skinId -> viewModel.selectSkin(skinId) },
                onOpenShop = {
                  viewModel.openSkins(false)
                  viewModel.openShop(true)
                },
                onDismiss = { viewModel.openSkins(false) }
              )
            }

            if (uiState.showShopDialog) {
              ShopDialog(
                uiState = uiState,
                tradableSkins = viewModel.tradableSkins(uiState),
                isSkinUnlocked = { skin -> viewModel.isSkinEffectivelyUnlocked(skin, uiState) },
                isModeUnlocked = { mode -> viewModel.isModeEffectivelyUnlocked(mode, uiState) },
                shopCostForSkin = { skin -> viewModel.shopCostForSkin(skin) },
                shopCostForMode = { mode -> viewModel.shopCostForMode(mode) },
                shopCostForItem = { item -> viewModel.shopCostForItem(item) },
                onBuySkin = { skinId, tradeSkinId -> viewModel.buySkin(skinId, tradeSkinId) },
                onBuyMode = { modeId, tradeSkinId -> viewModel.buyMode(modeId, tradeSkinId) },
                onBuyItem = { itemId, tradeSkinId -> viewModel.buyItem(itemId, tradeSkinId) },
                onBuyRevives = { quantity, cost, tradeSkinId -> viewModel.buyRevivePack(quantity, cost, tradeSkinId) },
                onDismiss = { viewModel.openShop(false) }
              )
            }

            if (uiState.showCreatorDialog) {
              CreatorStudioDialog(
                uiState = uiState,
                onSaveCustomMap = { mapCfg, playNow -> viewModel.saveCustomMap(mapCfg, playNow) },
                onSaveCustomItem = { itemCfg -> viewModel.saveCustomItem(itemCfg) },
                onDeleteCustomItem = { id -> viewModel.deleteCustomItem(id) },
                onToggleSpawnCustomItems = { viewModel.toggleSpawnCustomItems() },
                onDismiss = { viewModel.openCreator(false) }
              )
            }

            if (uiState.showAiDialog) {
              AiCoachDialog(
                uiState = uiState,
                aiTipState = aiTipState,
                onAskAi = { q -> viewModel.askAiTip(q) },
                onDismiss = { viewModel.openAiDialog(false) }
              )
            }

            if (uiState.showOnlineDialog) {
              OnlineMultiplayerDialog(
                currentUser = activeUser,
                onlineVm = onlineVm,
                uiState = uiState,
                onExportCloudSave = { viewModel.exportCloudSavePayload() },
                onApplyCloudSave = { profile -> viewModel.applyCloudSaveProfile(profile) },
                onClaimSharedSkin = { skinId -> viewModel.claimSharedSkin(skinId) },
                onStartMultiplayerMatch = {
                  val targetDiff = when (currentActiveRoomDiff) {
                    "CALMO" -> Difficulty.CALMO
                    "DESAFIO" -> Difficulty.DESAFIO
                    "ELITE" -> Difficulty.ELITE
                    else -> Difficulty.NORMAL
                  }
                  viewModel.setDifficulty(targetDiff)
                  viewModel.openOnlineDialog(false)
                  viewModel.startNormalGame()
                },
                onDismiss = { viewModel.openOnlineDialog(false) }
              )
            }

            if (uiState.showSettingsDialog) {
              SettingsDialog(
                uiState = uiState,
                updateState = updateState,
                onControlTypeChange = { viewModel.setControlType(it) },
                onToggleSound = { viewModel.toggleSound() },
                onToggleHaptic = { viewModel.toggleHaptic() },
                onToggle3dMode = { viewModel.toggle3dMode() },
                onToggle4dMode = { viewModel.toggle4dMode() },
                onDifficultyChange = { viewModel.setDifficulty(it) },
                onCustomGhostSpeedChange = { viewModel.setCustomGhostSpeed(it) },
                onArenaThemeChange = { viewModel.setArenaTheme(it) },
                onGhostCountChange = { viewModel.setGhostCount(it) },
                onHeroSpeedChange = { viewModel.setHeroSpeedOption(it) },
                onPowerUpFreqChange = { viewModel.setPowerUpFrequency(it) },
                onToggleSonicButton = { viewModel.toggleSonicButton() },
                onToggleGodMode = { viewModel.toggleGodMode() },
                onRestoreDefaultFair = { viewModel.restoreDefaultFairSettings() },
                onToggleAutoUpdate = { viewModel.setAutoUpdateEnabled(it) },
                onCheckUpdateNow = { runAppUpdateCheck(true) },
                onOpenUpdateStore = { updateController.openStoreForUpdate(this@MainActivity) },
                onSendMeowNotification = triggerMeowNotification,
                onDismiss = { viewModel.openSettings(false) }
              )
            }

            // Notification Feedback Toast
            AnimatedVisibility(
              visible = uiState.notificationFeedback != null,
              enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
              exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
              modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp, start = 20.dp, end = 20.dp)
            ) {
              uiState.notificationFeedback?.let { msg ->
                Surface(
                  color = Color(0xFF1E1B4B),
                  shape = RoundedCornerShape(16.dp),
                  border = BorderStroke(1.5.dp, Color(0xFFF472B6)),
                  modifier = Modifier.shadow(12.dp, RoundedCornerShape(16.dp))
                ) {
                  Text(
                    text = msg,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                  )
                }
              }
            }

            // Animated Achievement Unlocked Toast Banner
            AnimatedVisibility(
              visible = uiState.newlyUnlockedAchievement != null,
              enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
              exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
              modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp, start = 16.dp, end = 16.dp)
            ) {
              uiState.newlyUnlockedAchievement?.let { ach ->
                Surface(
                  color = Color(0xFF312E81),
                  shape = RoundedCornerShape(18.dp),
                  border = BorderStroke(2.dp, Color(0xFFFACC15)),
                  modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(18.dp))
                    .clickable { viewModel.dismissAchievementBanner() }
                    .testTag("achievement_banner")
                ) {
                  Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(ach.emoji, fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = "🏅 CONQUISTA DESBLOQUEADA!",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFDE047)
                      )
                      Text(
                        text = ach.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                      )
                      Text(
                        text = ach.rewardText,
                        fontSize = 11.sp,
                        color = Color(0xFF38BDF8)
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
