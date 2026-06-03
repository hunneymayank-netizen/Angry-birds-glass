package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GameScore
import kotlinx.coroutines.delay
import kotlin.math.*

@Composable
fun AppContent(viewModel: GameViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val savedScores by viewModel.savedScores.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE3F2FD)) // Soft blue sky horizon base
    ) {
        AmbientBlurOrbs()

        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
            },
            label = "ScreenTransition"
        ) { screen ->
            when (screen) {
                "level_select" -> LevelSelectScreen(
                    savedScores = savedScores,
                    onLevelSelected = { idx -> viewModel.loadLevel(idx) },
                    isSoundOn = viewModel.isSoundEnabled.collectAsState().value,
                    onToggleSound = { viewModel.toggleSound() }
                )
                "game_play" -> GamePlayScreen(viewModel = viewModel)
            }
        }
    }
}

fun Modifier.frostedGlass(
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(24.dp),
    alpha: Float = 0.45f,
    borderWidth: androidx.compose.ui.unit.Dp = 1.dp
): Modifier = this
    .background(Color.White.copy(alpha = alpha), shape = shape)
    .border(borderWidth, Color.White.copy(alpha = 0.35f), shape = shape)

@Composable
fun AmbientBlurOrbs() {
    var shiftX by remember { mutableStateOf(0f) }
    var shiftY by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        var angle = 0.0
        while (true) {
            delay(32)
            angle += 0.015
            shiftX = sin(angle).toFloat() * 40f
            shiftY = cos(angle * 0.8).toFloat() * 30f
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x99FFA4B4), Color.Transparent),
                center = Offset(w * 0.15f + shiftX, h * 0.2f + shiftY),
                radius = w * 0.45f
            )
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x99FFF176), Color.Transparent),
                center = Offset(w * 0.85f - shiftX, h * 0.8f - shiftY),
                radius = w * 0.5f
            )
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x8090CAF9), Color.Transparent),
                center = Offset(w * 0.75f + shiftX * 0.5f, h * 0.45f + shiftY * 0.5f),
                radius = w * 0.4f
            )
        )
    }
}

@Composable
fun LevelSelectScreen(
    savedScores: Map<Int, GameScore>,
    onLevelSelected: (Int) -> Unit,
    isSoundOn: Boolean,
    onToggleSound: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Frosted Glass Header panel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(24.dp))
                .background(Color(0x50FFFFFF), RoundedCornerShape(24.dp))
                .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleSound,
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0x80FFFFFF), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                        .testTag("sound_toggle")
                ) {
                    Text(
                        text = if (isSoundOn) "🔊" else "🔇",
                        fontSize = 18.sp
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "STAGE SELECT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp,
                            color = Color(0xFF1E3A8A).copy(alpha = 0.6f)
                        )
                    )
                    Text(
                        text = "Angry Playful Birds",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = Color(0xFF1E3A8A)
                        )
                    )
                }

                IconButton(
                    onClick = {},
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0x80FFFFFF), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                ) {
                    Text(
                        text = "📊",
                        fontSize = 18.sp
                    )
                }
            }
        }

        // 2. Playful Mascot Instruction Banner (frosted)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .rotate(-1f)
                .shadow(12.dp, RoundedCornerShape(28.dp))
                .background(Color(0x60FFFFFF), RoundedCornerShape(28.dp))
                .border(1.5.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(28.dp))
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Canvas(modifier = Modifier.size(76.dp)) {
                    drawCuteBird(Offset(38f, 38f), 28f, isShaking = true, type = BirdType.CLASSIC)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "MEET THE PLAY SQUAD!",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = Color(0xFF1E3A8A)
                        )
                    )
                    Text(
                        text = "🚀 Breezy Blue hits Nitro!\n💥 Bomb Black triggers huge Exploding Blasts!\n☄️ Rapid Rose splits into THREE flyers simultaneously!",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = Color(0xFF1E3A8A).copy(alpha = 0.85f)
                        )
                    )
                }
            }
        }

        // 3. Level Selector Card Grid
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "TAP A CARTOON PUZZLE STAGE",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = Color(0xFF1E3A8A).copy(alpha = 0.7f)
                ),
                modifier = Modifier.padding(bottom = 14.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(0.95f),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
            ) {
                listOf(0, 1, 2).forEach { index ->
                    val scoreObj = savedScores[index]
                    val isUnlocked = index == 0 || (scoreObj != null && scoreObj.unlocked) || (savedScores[index - 1]?.stars ?: 0 > 0)
                    val stars = scoreObj?.stars ?: 0
                    val high = scoreObj?.highScore ?: 0

                    LevelCardItem(
                        index = index,
                        isUnlocked = isUnlocked,
                        stars = stars,
                        highScore = high,
                        onClick = {
                            if (isUnlocked) onLevelSelected(index)
                        }
                    )
                }
            }
        }

        // Kids banner
        Box(
            modifier = Modifier
                .background(Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = "🧸 100% Kids Friendly • Family Approved • Offline Fun!",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = Color(0xFF1E3A8A).copy(alpha = 0.7f)
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun LevelCardItem(
    index: Int,
    isUnlocked: Boolean,
    stars: Int,
    highScore: Int,
    onClick: () -> Unit
) {
    val themeColor = when (index) {
        0 -> Color(0xFF34C759)
        1 -> Color(0xFF5856D6)
        else -> Color(0xFFFF2D55)
    }

    Box(
        modifier = Modifier
            .width(105.dp)
            .height(155.dp)
            .shadow(if (isUnlocked) 8.dp else 2.dp, RoundedCornerShape(24.dp))
            .background(
                if (isUnlocked) Color(0xCCFFFFFF) else Color(0x40FFFFFF),
                RoundedCornerShape(24.dp)
            )
            .border(
                width = 1.5.dp,
                color = if (isUnlocked) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.25f),
                shape = RoundedCornerShape(24.dp)
            )
            .clip(RoundedCornerShape(24.dp))
            .clickable(enabled = isUnlocked) { onClick() }
            .testTag("level_card_$index")
    ) {
        if (!isUnlocked) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = "Locked level",
                tint = Color(0xFF1E3A8A).copy(alpha = 0.35f),
                modifier = Modifier
                    .size(34.dp)
                    .align(Alignment.Center)
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(themeColor)
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "STAGE ${index + 1}",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    )
                }

                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    repeat(3) { starIdx ->
                        val active = starIdx < stars
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Star $starIdx",
                            tint = if (active) Color(0xFFFFCC00) else Color(0x331E3A8A),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Canvas(modifier = Modifier.size(46.dp)) {
                    val c = size.width / 2
                    when (index) {
                        0 -> drawRoundTarget(Offset(c, c), 13f, expression = MonsterExpression.HAPPY)
                        1 -> drawRoundTarget(Offset(c, c), 13f, expression = MonsterExpression.SCARED)
                        2 -> drawRoundTarget(Offset(c, c), 13f, expression = MonsterExpression.SHOCKED)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x331E3A8A))
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (highScore > 0) "★★★ $highScore" else "NEW!",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 9.sp,
                            color = Color(0xFF1E3A8A)
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun GamePlayScreen(viewModel: GameViewModel) {
    val levelIndex by viewModel.currentLevelIndex.collectAsState()
    val score by viewModel.score.collectAsState()
    val birdsRemaining by viewModel.birdsRemaining.collectAsState()
    val status by viewModel.gameEndStatus.collectAsState()
    val starsAwarded by viewModel.starsAwarded.collectAsState()

    val birdX by viewModel.birdX.collectAsState()
    val birdY by viewModel.birdY.collectAsState()
    val birdState by viewModel.birdState.collectAsState()
    val currentBirdType by viewModel.currentBirdType.collectAsState()
    val activeBirds by viewModel.activeBirds.collectAsState()
    val trail by viewModel.trail.collectAsState()
    val blocks by viewModel.blocks.collectAsState()
    val monsters by viewModel.monsters.collectAsState()
    val effects by viewModel.effects.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(birdState) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val logicalTouchX = offset.x / size.width * 800f
                            val logicalTouchY = offset.y / size.height * 450f
                            val dstSling = (logicalTouchX - viewModel.slingX) * (logicalTouchX - viewModel.slingX) +
                                    (logicalTouchY - viewModel.slingY) * (logicalTouchY - viewModel.slingY)
                            if (dstSling < 1500f || birdState == "idle") {
                                viewModel.startDragging()
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val logicalTouchX = change.position.x / size.width * 800f
                            val logicalTouchY = change.position.y / size.height * 450f
                            viewModel.updateDrag(logicalTouchX, logicalTouchY)
                        },
                        onDragEnd = {
                            viewModel.releaseDrag()
                        },
                        onDragCancel = {
                            viewModel.releaseDrag()
                        }
                    )
                }
                .testTag("game_viewport")
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasW = size.width
                val canvasH = size.height

                val scaleX = canvasW / 800f
                val scaleY = canvasH / 450f

                withTransform({
                    scale(scaleX, scaleY, pivot = Offset(0f, 0f))
                }) {
                    drawTerrainSky()

                    if (birdState == "dragging") {
                        drawTrajectoryDots(viewModel.slingX, viewModel.slingY, birdX, birdY)
                    }

                    trail.forEachIndexed { idx, pt ->
                        val alpha = (idx.toFloat() / trail.size) * 0.45f
                        drawCircle(
                            color = Color.White.copy(alpha = alpha),
                            radius = 3.5f + (idx * 0.12f),
                            center = Offset(pt.first, pt.second)
                        )
                    }

                    drawSlingshotBack(viewModel.slingX, viewModel.slingY, birdX, birdY, birdState)

                    if (birdState == "flying") {
                        activeBirds.forEach { birdInstance ->
                            if (birdInstance.isAlive) {
                                drawCuteBird(
                                    center = Offset(birdInstance.x, birdInstance.y),
                                    radius = 16f,
                                    isShaking = false,
                                    type = birdInstance.type
                                )
                            }
                        }
                    } else if (birdState == "idle" || birdState == "dragging") {
                        drawCuteBird(
                            center = Offset(birdX, birdY),
                            radius = 18f,
                            isShaking = birdState == "dragging",
                            type = currentBirdType
                        )
                    }

                    drawSlingshotFront(viewModel.slingX, viewModel.slingY, birdX, birdY, birdState)

                    blocks.forEach { block ->
                        if (block.life > 0) {
                            val halfW = block.width / 2
                            val halfH = block.height / 2
                            drawRoundRect(
                                color = block.getColor(),
                                topLeft = Offset(block.x - halfW, block.y - halfH),
                                size = Size(block.width, block.height),
                                cornerRadius = CornerRadius(6f, 6f)
                            )
                            drawRoundRect(
                                color = Color(0xFF1E293B),
                                topLeft = Offset(block.x - halfW, block.y - halfH),
                                size = Size(block.width, block.height),
                                cornerRadius = CornerRadius(6f, 6f),
                                style = Stroke(width = 2f)
                            )
                            drawBlockAesthetics(block)
                        }
                    }

                    monsters.forEach { monster ->
                        if (!monster.isPopped) {
                            drawRoundTarget(
                                center = Offset(monster.x, monster.y),
                                radius = monster.radius,
                                expression = monster.expression
                            )
                        }
                    }

                    effects.forEach { effect ->
                        if (effect.text != null) {
                            drawComicWordBubble(effect)
                        } else {
                            if (effect.isStar) {
                                drawConfettiStar(Offset(effect.x, effect.y), 6f, effect.color, effect.age / effect.maxAge)
                            }
                        }
                    }
                }
            }
        }

        // 4. Overlaid frosted statistics bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .shadow(6.dp, RoundedCornerShape(20.dp))
                .background(Color(0x7FFFFFFF), RoundedCornerShape(20.dp))
                .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "STAGE ${levelIndex + 1}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E3A8A).copy(alpha = 0.6f)
                        )
                    )
                    Text(
                        text = "Score: $score",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1E3A8A)
                        )
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color(0x331E3A8A), RoundedCornerShape(16.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "READY:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1E3A8A),
                            fontSize = 9.sp
                        )
                    )
                    repeat(birdsRemaining) {
                        Canvas(modifier = Modifier.size(18.dp)) {
                            drawCuteBird(Offset(9f, 9f), 8f, isShaking = false, type = currentBirdType)
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { viewModel.forceResetLevel() },
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color.White, CircleShape)
                            .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                            .testTag("restart_level")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Restart Level",
                            tint = Color(0xFFFF2D55),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.navigateToLevelSelect() },
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color.White, CircleShape)
                            .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                            .testTag("map_nav")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Home,
                            contentDescription = "Map Select",
                            tint = Color(0xFF1E3A8A),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // 5. Special Active Power button
        AnimatedVisibility(
            visible = birdState == "flying" && activeBirds.any { it.isAlive && !it.isAbilityUsed },
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp)
        ) {
            val promoText = when (currentBirdType) {
                BirdType.SPEED_BOOST -> "NITRO BOOST! ⚡"
                BirdType.RAPID_FIRE -> "TRIPLE SPLIT! ☄️"
                BirdType.BOMB -> "DETONATE BOMB! 💥"
                else -> "CHIRP GREET! 🌟"
            }
            val containerColor = when (currentBirdType) {
                BirdType.SPEED_BOOST -> Color(0xFF34C759)
                BirdType.RAPID_FIRE -> Color(0xFFFF2D55)
                BirdType.BOMB -> Color(0xFF5856D6)
                else -> Color(0xFFFF9500)
            }

            Button(
                onClick = { viewModel.triggerSpecialAbility() },
                colors = ButtonDefaults.buttonColors(containerColor = containerColor),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .width(220.dp)
                    .height(52.dp)
                    .border(2.dp, Color.White, RoundedCornerShape(24.dp))
                    .shadow(12.dp, RoundedCornerShape(24.dp))
                    .testTag("special_ability_tap")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "⚡",
                        fontSize = 20.sp
                    )
                    Text(
                        text = promoText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                    )
                }
            }
        }

        // 6. Overlaid screens
        AnimatedVisibility(
            visible = status != "none",
            enter = fadeIn() + scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                if (status == "victory") {
                    VictoryDialog(
                        score = score,
                        stars = starsAwarded,
                        onNextLevel = {
                            val next = levelIndex + 1
                            if (next < 3) viewModel.loadLevel(next) else viewModel.navigateToLevelSelect()
                        },
                        onReplay = { viewModel.forceResetLevel() },
                        onMap = { viewModel.navigateToLevelSelect() },
                        isLastLevel = levelIndex == 2
                    )
                } else if (status == "defeat") {
                    DefeatDialog(
                        onReplay = { viewModel.forceResetLevel() },
                        onMap = { viewModel.navigateToLevelSelect() }
                    )
                }
            }
        }
    }
}

// DrawScope utilities
private fun DrawScope.drawTerrainSky() {
    drawCircle(Color(0xFFE2E8F0).copy(alpha = 0.5f), 170f, Offset(440f, 410f))
    drawCircle(Color(0xFFCBD5E1).copy(alpha = 0.5f), 220f, Offset(200f, 410f))

    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF4ADE80), Color(0xFF22C55E))
        ),
        topLeft = Offset(-10f, 400f),
        size = Size(820f, 60f)
    )

    drawLine(
        color = Color(0xFF16A34A),
        start = Offset(-2f, 400f),
        end = Offset(802f, 400f),
        strokeWidth = 3f
    )
}

private fun DrawScope.drawTrajectoryDots(slingX: Float, slingY: Float, birdX: Float, birdY: Float) {
    val dx = slingX - birdX
    val dy = slingY - birdY
    val pullSpeedFactor = 0.22f
    val initVx = dx * pullSpeedFactor
    val initVy = dy * pullSpeedFactor

    var simX = slingX
    var simY = slingY
    var simVx = initVx
    var simVy = initVy

    repeat(15) { step ->
        simX += simVx
        simY += simVy
        simVy += 0.22f

        if (simY <= 400f) {
            drawCircle(
                color = Color(0xFFFFD54F).copy(alpha = (1f - (step / 15f)).coerceIn(0.1f, 1f)),
                radius = (3.5f - step * 0.08f).coerceAtLeast(1f),
                center = Offset(simX, simY)
            )
        }
    }
}

private fun DrawScope.drawSlingshotBack(
    slingX: Float,
    slingY: Float,
    birdX: Float,
    birdY: Float,
    birdState: String
) {
    val brownColor = Color(0xFF78350F)
    val elasticColor = Color(0xFF334155)

    drawLine(brownColor, Offset(slingX, slingY), Offset(slingX, 405f), strokeWidth = 9f, cap = StrokeCap.Round)
    drawLine(brownColor, Offset(slingX, slingY), Offset(slingX - 18f, slingY - 28f), strokeWidth = 7f, cap = StrokeCap.Round)

    if (birdState == "dragging") {
        drawLine(
            color = elasticColor,
            start = Offset(slingX - 18f, slingY - 24f),
            end = Offset(birdX - 4f, birdY),
            strokeWidth = 5f,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawSlingshotFront(
    slingX: Float,
    slingY: Float,
    birdX: Float,
    birdY: Float,
    birdState: String
) {
    val brownColor = Color(0xFF78350F)
    val elasticColor = Color(0xFF475569)

    drawLine(brownColor, Offset(slingX, slingY), Offset(slingX + 18f, slingY - 28f), strokeWidth = 7f, cap = StrokeCap.Round)

    if (birdState == "dragging") {
        drawLine(
            color = elasticColor,
            start = Offset(slingX + 18f, slingY - 24f),
            end = Offset(birdX + 4f, birdY),
            strokeWidth = 5f,
            cap = StrokeCap.Round
        )
        drawRoundRect(
            color = Color(0xFFB45309),
            topLeft = Offset(birdX - 8f, birdY - 14f),
            size = Size(16f, 28f),
            cornerRadius = CornerRadius(4f, 4f)
        )
    }
}

private fun DrawScope.drawCuteBird(center: Offset, radius: Float, isShaking: Boolean, type: BirdType) {
    var finalCenter = center
    if (isShaking) {
        val randOffset = Offset(
            (Math.random().toFloat() - 0.5f) * 3f,
            (Math.random().toFloat() - 0.5f) * 3f
        )
        finalCenter += randOffset
    }

    drawCircle(type.color, radius, finalCenter)

    val underbellyColor = when (type) {
        BirdType.CLASSIC -> Color(0xFFFFCDD2)
        BirdType.SPEED_BOOST -> Color(0xFFE0F7FA)
        BirdType.RAPID_FIRE -> Color(0xFFFCE4EC)
        BirdType.BOMB -> Color(0xFF90A4AE)
    }
    drawCircle(underbellyColor, radius * 0.62f, finalCenter + Offset(0f, radius * 0.35f))

    val eyeYOffset = -radius * 0.2f
    val eyeRadius = radius * 0.25f

    drawCircle(Color.White, eyeRadius, finalCenter + Offset(radius * 0.12f, eyeYOffset))
    drawCircle(Color.White, eyeRadius, finalCenter + Offset(radius * 0.5f, eyeYOffset))
    drawCircle(Color.Black, 2.5f, finalCenter + Offset(radius * 0.16f, eyeYOffset))
    drawCircle(Color.Black, 2.5f, finalCenter + Offset(radius * 0.54f, eyeYOffset))

    drawLine(
        color = Color(0xFF1E293B),
        start = finalCenter + Offset(-4f, eyeYOffset - 5f),
        end = finalCenter + Offset(radius * 0.7f, eyeYOffset - 3f),
        strokeWidth = 3f,
        cap = StrokeCap.Round
    )

    val beakPath = Path().apply {
        moveTo(finalCenter.x + radius * 0.32f, finalCenter.y + radius * 0.05f)
        lineTo(finalCenter.x + radius * 0.95f, finalCenter.y + radius * 0.2f)
        lineTo(finalCenter.x + radius * 0.32f, finalCenter.y + radius * 0.35f)
        close()
    }
    drawPath(beakPath, Color(0xFFFFB300))

    when (type) {
        BirdType.SPEED_BOOST -> {
            val speedTrail = Path().apply {
                moveTo(finalCenter.x - radius, finalCenter.y - 4f)
                lineTo(finalCenter.x - radius - 12f, finalCenter.y - 12f)
                lineTo(finalCenter.x - radius - 16f, finalCenter.y - 2f)
                lineTo(finalCenter.x - radius - 8f, finalCenter.y + 6f)
                close()
            }
            drawPath(speedTrail, Color(0xFFFFCC00))
        }
        BirdType.BOMB -> {
            drawLine(
                color = Color(0xFFE2E8F0),
                start = finalCenter - Offset(0f, radius),
                end = finalCenter - Offset(radius * 0.2f, radius + 12f),
                strokeWidth = 2.5f,
                cap = StrokeCap.Round
            )
            drawCircle(
                color = Color(0xFFFF3B30),
                radius = 3.5f,
                center = finalCenter - Offset(radius * 0.2f, radius + 12f)
            )
        }
        BirdType.RAPID_FIRE -> {
            drawCircle(
                color = Color(0xFFFF2D55).copy(alpha = 0.5f),
                radius = radius * 0.4f,
                center = finalCenter + Offset(-radius * 0.7f, 0f)
            )
        }
        else -> {}
    }
}

private fun DrawScope.drawRoundTarget(center: Offset, radius: Float, expression: MonsterExpression) {
    if (expression == MonsterExpression.POPPED) return

    val targetColor = when (expression) {
        MonsterExpression.HAPPY -> Color(0xFF818CF8)
        MonsterExpression.SCARED -> Color(0xFF6366F1)
        else -> Color(0xFF4F46E5)
    }

    drawCircle(targetColor, radius, center)
    drawCircle(Color.White.copy(alpha = 0.25f), radius * 0.22f, center + Offset(-radius * 0.35f, -radius * 0.35f))

    val eyeY = -radius * 0.12f
    val eyeSpacing = radius * 0.32f
    drawCircle(Color.White, radius * 0.28f, center + Offset(-eyeSpacing, eyeY))
    drawCircle(Color.White, radius * 0.28f, center + Offset(eyeSpacing, eyeY))
    drawCircle(Color.Black, 2.2f, center + Offset(-eyeSpacing, eyeY))
    drawCircle(Color.Black, 2.2f, center + Offset(eyeSpacing, eyeY))

    drawRoundRect(
        color = Color(0xFFFBBF24),
        topLeft = Offset(center.x - 6f, center.y + 1f),
        size = Size(12f, 7f),
        cornerRadius = CornerRadius(3f, 3f)
    )

    when (expression) {
        MonsterExpression.HAPPY -> {
            drawArc(
                color = Color(0xFF1E293B),
                startAngle = 10f,
                sweepAngle = 160f,
                useCenter = false,
                topLeft = Offset(center.x - 4f, center.y + radius * 0.3f),
                size = Size(8f, 5f),
                style = Stroke(width = 2f, cap = StrokeCap.Round)
            )
        }
        MonsterExpression.SCARED -> {
            drawCircle(
                color = Color(0xFF1E293B),
                radius = 3f,
                center = center + Offset(0f, radius * 0.45f)
            )
        }
        MonsterExpression.SHOCKED -> {
            drawRoundRect(
                color = Color(0xFF1E293B),
                topLeft = Offset(center.x - 4.5f, center.y + radius * 0.35f),
                size = Size(9f, 5f),
                cornerRadius = CornerRadius(1.5f, 1.5f)
            )
        }
        else -> {}
    }
}

private fun DrawScope.drawBlockAesthetics(block: PhysicsBlock) {
    val halfW = block.width / 2
    val halfH = block.height / 2
    val colorAccent = block.getColor().copy(alpha = 0.5f)

    if (block.material == MaterialType.WOOD) {
        drawLine(
            color = colorAccent,
            start = Offset(block.x - halfW + 6f, block.y - halfH + 6f),
            end = Offset(block.x + halfW - 6f, block.y - halfH + 6f),
            strokeWidth = 2f
        )
    } else if (block.material == MaterialType.ICE) {
        drawLine(
            color = Color.White.copy(alpha = 0.6f),
            start = Offset(block.x - halfW + 4f, block.y - halfH + 4f),
            end = Offset(block.x + halfW - 4f, block.y + halfH - 4f),
            strokeWidth = 1.5f
        )
    } else {
        drawCircle(colorAccent, 2.5f, Offset(block.x - halfW + 8f, block.y - 3f))
        drawCircle(colorAccent, 2f, Offset(block.x + halfW - 8f, block.y + 4f))
    }
}

private fun DrawScope.drawComicWordBubble(effect: ParticleEffect) {
    if (effect.text == null) return
    val ageRatio = effect.age / effect.maxAge
    val scale = if (ageRatio < 0.15f) (ageRatio / 0.15f) else (1f - ageRatio).coerceAtLeast(0.1f)

    withTransform({
        scale(scale, scale, pivot = Offset(effect.x, effect.y))
        rotate(12f * (1f - ageRatio * 2f), pivot = Offset(effect.x, effect.y))
    }) {
        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 25f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = android.graphics.Paint.Align.CENTER
        }
        val strokePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 25f
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 5.5f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = android.graphics.Paint.Align.CENTER
        }

        drawContext.canvas.nativeCanvas.drawText(effect.text, effect.x, effect.y, strokePaint)
        drawContext.canvas.nativeCanvas.drawText(effect.text, effect.x, effect.y, textPaint)
    }
}

private fun DrawScope.drawConfettiStar(center: Offset, r: Float, color: Color, ratio: Float) {
    val scale = (1f - ratio)
    val opacityColor = color.copy(alpha = scale)
    val path = Path().apply {
        var angle = 0.0
        val increment = Math.PI / 5
        repeat(10) { step ->
            val curRadius = if (step % 2 == 0) r * scale else (r * 0.4f) * scale
            val x = center.x + curRadius * cos(angle).toFloat()
            val y = center.y + curRadius * sin(angle).toFloat()
            if (step == 0) moveTo(x, y) else lineTo(x, y)
            angle += increment
        }
        close()
    }
    drawPath(path, opacityColor)
}

@Composable
fun VictoryDialog(
    score: Int,
    stars: Int,
    onNextLevel: () -> Unit,
    onReplay: () -> Unit,
    onMap: () -> Unit,
    isLastLevel: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .shadow(24.dp, RoundedCornerShape(32.dp))
            .background(Color(0xE6FFFFFF), RoundedCornerShape(32.dp))
            .border(2.dp, Color.White, RoundedCornerShape(32.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "✨ STAGE CLEAR! ✨",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    color = Color(0xFF34C759)
                ),
                textAlign = TextAlign.Center
            )

            Text(
                text = "You popped all cheeky targets!",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E3A8A).copy(alpha = 0.7f)
                ),
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            Row(
                modifier = Modifier.padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(3) { starIdx ->
                    val active = starIdx < stars
                    val scale by animateFloatAsState(
                        targetValue = if (active) 1.2f else 0.8f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy),
                        label = "StarAnim"
                    )
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "Star feedback",
                        tint = if (active) Color(0xFFFFCC00) else Color(0x221E3A8A),
                        modifier = Modifier
                            .size(52.dp)
                            .scale(scale)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .background(Color(0xFFFEF3C7), RoundedCornerShape(16.dp))
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Total Score: $score",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFD97706),
                        fontSize = 16.sp
                    )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!isLastLevel) {
                    Button(
                        onClick = onNextLevel,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("next_level_button"),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "NEXT LEVEL",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                            )
                            Icon(Icons.Filled.ArrowForward, "Next")
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onReplay,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9500)),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("replay_button"),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Filled.Refresh, "Retry", modifier = Modifier.size(16.dp))
                            Text("REPLAY", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black, fontSize = 12.sp))
                        }
                    }

                    Button(
                        onClick = onMap,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5856D6)),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("map_button"),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Filled.Home, "Map", modifier = Modifier.size(16.dp))
                            Text("MAPS", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black, fontSize = 12.sp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DefeatDialog(
    onReplay: () -> Unit,
    onMap: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .shadow(24.dp, RoundedCornerShape(32.dp))
            .background(Color(0xE6FFFFFF), RoundedCornerShape(32.dp))
            .border(2.dp, Color.White, RoundedCornerShape(32.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(modifier = Modifier.size(68.dp)) {
                drawCuteBird(Offset(34f, 34f), 28f, isShaking = false, type = BirdType.CLASSIC)
            }

            Text(
                text = "AWW, SO CLOSE!",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    color = Color(0xFFFF2D55)
                ),
                modifier = Modifier.padding(top = 16.dp),
                textAlign = TextAlign.Center
            )

            Text(
                text = "Keep trying! You will knock down all structures next turn!",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E3A8A).copy(alpha = 0.7f)
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 22.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onReplay,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759)),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(48.dp)
                        .testTag("defeat_replay_button"),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Filled.Refresh, "Retry")
                        Text(
                            "TRY AGAIN",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = 13.sp)
                        )
                    }
                }

                Button(
                    onClick = onMap,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E8E93)),
                    modifier = Modifier
                        .weight(0.8f)
                        .height(48.dp)
                        .testTag("defeat_map_button"),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(
                        "MAPS",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    )
                }
            }
        }
    }
}
