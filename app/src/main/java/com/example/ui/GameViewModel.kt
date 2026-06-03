package com.example.ui

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GameDatabase
import com.example.data.GameRepository
import com.example.data.GameScore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val database = GameDatabase.getDatabase(application)
    private val repository = GameRepository(database.gameScoreDao())

    // UI Routing: "level_select", "game_play"
    private val _currentScreen = MutableStateFlow("level_select")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    // High scores and stars progression (from Room DB)
    private val _savedScores = MutableStateFlow<Map<Int, GameScore>>(emptyMap())
    val savedScores: StateFlow<Map<Int, GameScore>> = _savedScores.asStateFlow()

    // Active Level configuration and state parameters
    private val _currentLevelIndex = MutableStateFlow(0)
    val currentLevelIndex: StateFlow<Int> = _currentLevelIndex.asStateFlow()

    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score.asStateFlow()

    // Slingshot anchor location (logical screen coordinates: Width=800, Height=450)
    val slingX = 160f
    val slingY = 310f

    // Bird details (Backward compatible states for simple binders/tests)
    private val _birdX = MutableStateFlow(slingX)
    val birdX: StateFlow<Float> = _birdX.asStateFlow()

    private val _birdY = MutableStateFlow(slingY)
    val birdY: StateFlow<Float> = _birdY.asStateFlow()

    private val _birdVx = MutableStateFlow(0f)
    val birdVx: StateFlow<Float> = _birdVx.asStateFlow()

    private val _birdVy = MutableStateFlow(0f)
    val birdVy: StateFlow<Float> = _birdVy.asStateFlow()

    // Bird Flight Statuses: "idle", "dragging", "flying", "crashed"
    private val _birdState = MutableStateFlow("idle")
    val birdState: StateFlow<String> = _birdState.asStateFlow()

    // Extra birds remaining in queue (total launch attempts remaining is list.size - launchedCount)
    private val _birdsRemaining = MutableStateFlow(3)
    val birdsRemaining: StateFlow<Int> = _birdsRemaining.asStateFlow()

    // The roster queue of birds for the active level
    private val _birdsQueue = MutableStateFlow<List<BirdType>>(emptyList())
    val birdsQueue: StateFlow<List<BirdType>> = _birdsQueue.asStateFlow()

    private val _currentBirdQueueIndex = MutableStateFlow(0)
    val currentBirdQueueIndex: StateFlow<Int> = _currentBirdQueueIndex.asStateFlow()

    private val _currentBirdType = MutableStateFlow(BirdType.CLASSIC)
    val currentBirdType: StateFlow<BirdType> = _currentBirdType.asStateFlow()

    // Active Flying projectile list (Supports Triple Splits simultaneously!)
    private val _activeBirds = MutableStateFlow<List<FlyingBird>>(emptyList())
    val activeBirds: StateFlow<List<FlyingBird>> = _activeBirds.asStateFlow()

    // Trail dots for backward compatible drawings
    private val _trail = MutableStateFlow<List<Pair<Float, Float>>>(emptyList())
    val trail: StateFlow<List<Pair<Float, Float>>> = _trail.asStateFlow()

    // Active physical blocks
    private val _blocks = MutableStateFlow<List<PhysicsBlock>>(emptyList())
    val blocks: StateFlow<List<PhysicsBlock>> = _blocks.asStateFlow()

    // Active target monsters
    private val _monsters = MutableStateFlow<List<CheekyMonster>>(emptyList())
    val monsters: StateFlow<List<CheekyMonster>> = _monsters.asStateFlow()

    // Flying cartoon particle effects ("BOOM!", pops, stars)
    private val _effects = MutableStateFlow<List<ParticleEffect>>(emptyList())
    val effects: StateFlow<List<ParticleEffect>> = _effects.asStateFlow()

    private val _isSoundEnabled = MutableStateFlow(true)
    val isSoundEnabled: StateFlow<Boolean> = _isSoundEnabled.asStateFlow()

    // Ending status: "victory", "defeat", "none"
    private val _gameEndStatus = MutableStateFlow("none")
    val gameEndStatus: StateFlow<String> = _gameEndStatus.asStateFlow()

    private val _starsAwarded = MutableStateFlow(0)
    val starsAwarded: StateFlow<Int> = _starsAwarded.asStateFlow()

    // Local physics ticking Job
    private var gameLoopJob: Job? = null

    init {
        viewModelScope.launch {
            // Seed base items inside Room database on startup
            repository.allScores.collect { scores ->
                if (scores.isEmpty()) {
                    repository.initializeScoresIfEmpty()
                } else {
                    _savedScores.value = scores.associateBy { it.levelIndex }
                }
            }
        }
    }

    fun toggleSound() {
        _isSoundEnabled.value = !_isSoundEnabled.value
    }

    fun navigateToLevelSelect() {
        stopGameLoop()
        _currentScreen.value = "level_select"
    }

    fun loadLevel(levelIdx: Int) {
        _currentLevelIndex.value = levelIdx
        _score.value = 0
        _birdState.value = "idle"
        _birdX.value = slingX
        _birdY.value = slingY
        _birdVx.value = 0f
        _birdVy.value = 0f
        
        // Custom kid friendly sequences of bird types
        val queue = when (levelIdx) {
            0 -> listOf(BirdType.CLASSIC, BirdType.SPEED_BOOST, BirdType.RAPID_FIRE)
            1 -> listOf(BirdType.CLASSIC, BirdType.SPEED_BOOST, BirdType.RAPID_FIRE, BirdType.BOMB)
            else -> listOf(BirdType.SPEED_BOOST, BirdType.RAPID_FIRE, BirdType.BOMB, BirdType.BOMB)
        }
        _birdsQueue.value = queue
        _currentBirdQueueIndex.value = 0
        _birdsRemaining.value = queue.size
        _currentBirdType.value = queue.first()
        _activeBirds.value = emptyList()

        _trail.value = emptyList()
        _gameEndStatus.value = "none"
        _starsAwarded.value = 0
        _effects.value = emptyList()

        // Generate levels
        _blocks.value = generateBlocksForLevel(levelIdx)
        _monsters.value = generateMonstersForLevel(levelIdx)
        _currentScreen.value = "game_play"

        startGameLoop()
    }

    fun startDragging() {
        if (_birdState.value == "idle") {
            _birdState.value = "dragging"
            // Scared expressions when pulling slingshot
            _monsters.value = _monsters.value.map {
                if (!it.isPopped) it.copy(expression = MonsterExpression.SCARED) else it
            }
        }
    }

    fun updateDrag(dragX: Float, dragY: Float) {
        if (_birdState.value == "dragging") {
            val dx = dragX - slingX
            val dy = dragY - slingY
            val dist = sqrt(dx * dx + dy * dy)
            val maxPull = 80f
            if (dist > maxPull) {
                _birdX.value = slingX + (dx / dist) * maxPull
                _birdY.value = slingY + (dy / dist) * maxPull
            } else {
                _birdX.value = dragX
                _birdY.value = dragY
            }
        }
    }

    fun releaseDrag() {
        if (_birdState.value == "dragging") {
            val dx = slingX - _birdX.value
            val dy = slingY - _birdY.value
            val pullSpeedFactor = 0.22f
            val launchVx = dx * pullSpeedFactor
            val launchVy = dy * pullSpeedFactor

            _birdVx.value = launchVx
            _birdVy.value = launchVy
            _birdState.value = "flying"
            _trail.value = emptyList()

            // Initialize active flying projectiles list
            _activeBirds.value = listOf(
                FlyingBird(
                    type = _currentBirdType.value,
                    x = _birdX.value,
                    y = _birdY.value,
                    vx = launchVx,
                    vy = launchVy
                )
            )

            // Dynamic monster surprise
            _monsters.value = _monsters.value.map {
                if (!it.isPopped) it.copy(expression = MonsterExpression.SHOCKED) else it
            }

            addComicEffect(slingX, slingY, "LAUNCH!", _currentBirdType.value.color)
        }
    }

    // Kid-friendly Active ability triggering!
    fun triggerSpecialAbility() {
        val list = _activeBirds.value.toMutableList()
        val flying = list.firstOrNull { it.isAlive && !it.isAbilityUsed } ?: return

        when (flying.type) {
            BirdType.SPEED_BOOST -> {
                // Rocket Booster forward nitro!
                flying.vx *= 1.8f
                flying.vy *= 0.3f // fly flatter!
                flying.isAbilityUsed = true
                addComicEffect(flying.x, flying.y, "BOOST!", Color(0xFF34C759))
            }
            BirdType.RAPID_FIRE -> {
                // Triple Split branching vector
                flying.isAlive = false
                flying.isAbilityUsed = true
                
                val b1 = FlyingBird(
                    type = BirdType.RAPID_FIRE,
                    x = flying.x,
                    y = flying.y,
                    vx = flying.vx,
                    vy = flying.vy,
                    isAbilityUsed = true
                )
                val b2 = FlyingBird(
                    type = BirdType.RAPID_FIRE,
                    x = flying.x,
                    y = flying.y - 12f,
                    vx = flying.vx + 0.6f,
                    vy = flying.vy - 1.6f,
                    isAbilityUsed = true
                )
                val b3 = FlyingBird(
                    type = BirdType.RAPID_FIRE,
                    x = flying.x,
                    y = flying.y + 12f,
                    vx = flying.vx + 0.6f,
                    vy = flying.vy + 1.6f,
                    isAbilityUsed = true
                )

                list.remove(flying)
                list.add(b1)
                list.add(b2)
                list.add(b3)
                addComicEffect(flying.x, flying.y, "TRIPLE SPLIT!", Color(0xFFFF2D55))
            }
            BirdType.BOMB -> {
                // Trigger giant explosion damage!
                triggerRadialExplosion(flying.x, flying.y, 140f)
                flying.isAlive = false
                flying.isAbilityUsed = true
                addComicEffect(flying.x, flying.y, "KABOOM!", Color(0xFF5856D6))
            }
            else -> {
                // Standard cute squeaking cheer soundeffect representation
                addComicEffect(flying.x, flying.y, "CHIRP!", Color(0xFFFF3B30))
            }
        }

        _activeBirds.value = list
    }

    private fun triggerRadialExplosion(ex: Float, ey: Float, radius: Float) {
        val blocksCopy = _blocks.value.map { it.copy() }
        val monstersCopy = _monsters.value.map { it.copy() }
        var stateChanged = false
        var points = 0

        // Push and shatter blocks in range
        for (block in blocksCopy) {
            if (block.life <= 0) continue
            val dx = block.x - ex
            val dy = block.y - ey
            val dist = sqrt(dx * dx + dy * dy)
            if (dist < radius) {
                // Force coefficient
                val force = ((radius - dist) / radius) * 16f
                val angle = Math.atan2(dy.toDouble(), dx.toDouble()).toFloat()
                block.vx += Math.cos(angle.toDouble()).toFloat() * force
                block.vy += Math.sin(angle.toDouble()).toFloat() * force - 2f // launch up!
                block.isStable = false
                block.life -= (radius - dist) * 1.2f // serious damage
                points += 150
                stateChanged = true
            }
        }

        // Pop monsters in range
        for (monster in monstersCopy) {
            if (monster.isPopped) continue
            val dx = monster.x - ex
            val dy = monster.y - ey
            val dist = sqrt(dx * dx + dy * dy)
            if (dist < radius) {
                monster.isPopped = true
                monster.expression = MonsterExpression.POPPED
                points += 600
                stateChanged = true
                addComicEffect(monster.x, monster.y, "POP!", Color(0xFFD84315))
            }
        }

        if (stateChanged) {
            _blocks.value = blocksCopy.filter { it.life > 10f }
            _monsters.value = monstersCopy
        }
        if (points > 0) {
            _score.value += points
        }
    }

    fun forceResetLevel() {
        loadLevel(_currentLevelIndex.value)
    }

    private fun addComicEffect(x: Float, y: Float, text: String, color: Color) {
        val list = _effects.value.toMutableList()
        list.add(
            ParticleEffect(
                x = x,
                y = y,
                text = text,
                color = color,
                vx = (Math.random().toFloat() - 0.5f) * 4f,
                vy = -2.5f - (Math.random().toFloat() * 1.5f),
                isStar = false
            )
        )
        repeat(8) {
            list.add(
                ParticleEffect(
                    x = x,
                    y = y,
                    color = color.copy(alpha = 0.85f),
                    vx = (Math.random().toFloat() - 0.5f) * 8f,
                    vy = (Math.random().toFloat() - 0.5f) * 8f,
                    isStar = true,
                    maxAge = 25f + (Math.random().toFloat() * 15f)
                )
            )
        }
        _effects.value = list
    }

    private fun startGameLoop() {
        stopGameLoop()
        gameLoopJob = viewModelScope.launch {
            var trailTimer = 0
            while (true) {
                delay(16)
                tickPhysics()

                // Add trail dots for all active projectile coordinates
                if (_birdState.value == "flying") {
                    trailTimer++
                    if (trailTimer % 3 == 0) {
                        val active = _activeBirds.value.firstOrNull { it.isAlive }
                        if (active != null) {
                            val currentList = _trail.value.toMutableList()
                            currentList.add(Pair(active.x, active.y))
                            if (currentList.size > 25) currentList.removeAt(0)
                            _trail.value = currentList
                        }
                    }
                }
            }
        }
    }

    private fun stopGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = null
    }

    private fun tickPhysics() {
        val currentActive = _activeBirds.value
        val nextActiveList = mutableListOf<FlyingBird>()

        // 1. Advance active projectiles
        for (bird in currentActive) {
            if (!bird.isAlive) continue

            bird.x += bird.vx
            bird.y += bird.vy
            bird.vy += 0.22f // Gravity factor

            // Ground limit hit
            if (bird.y >= 405f) {
                bird.y = 405f
                bird.vx *= 0.5f
                bird.vy = -bird.vy * 0.25f // impact damping rebound

                // Bomb explodes immediately on surface impact!
                if (bird.type == BirdType.BOMB && !bird.isAbilityUsed) {
                    triggerRadialExplosion(bird.x, bird.y, 140f)
                    bird.isAlive = false
                    bird.isAbilityUsed = true
                    addComicEffect(bird.x, bird.y, "KABOOM!", Color(0xFF5856D6))
                }

                if (abs(bird.vx) < 0.2f && abs(bird.vy) < 0.2f) {
                    bird.isAlive = false
                }
            }

            // Screen boundary check
            if (bird.x > 815f || bird.x < -15f) {
                bird.isAlive = false
            }

            if (bird.isAlive) {
                nextActiveList.add(bird)
            }
        }

        // Keep local binders updated with primary active projectile (or backup to slingshot anchor)
        val primary = nextActiveList.firstOrNull()
        if (primary != null) {
            _birdX.value = primary.x
            _birdY.value = primary.y
            _birdVx.value = primary.vx
            _birdVy.value = primary.vy
        }

        _activeBirds.value = nextActiveList

        // Handle crash / next-attempt queue swap if all active projectiles are dead
        if (_birdState.value == "flying" && nextActiveList.isEmpty()) {
            crashActiveBirdGroup()
        }

        // 2. Resolve Obstacle Tower collisions for each projectile
        val activeBlocks = _blocks.value.map { it.copy() }
        val activeMonsters = _monsters.value.map { it.copy() }
        var stateChanged = false
        var pointsEarned = 0

        for (bird in nextActiveList) {
            val bx = bird.x
            val by = bird.y
            val br = 16f

            // Block collisions
            for (block in activeBlocks) {
                if (block.life <= 0) continue

                val halfW = block.width / 2
                val halfH = block.height / 2
                val closestX = bx.coerceIn(block.x - halfW, block.x + halfW)
                val closestY = by.coerceIn(block.y - halfH, block.y + halfH)

                val dx = bx - closestX
                val dy = by - closestY
                val distSq = dx * dx + dy * dy

                if (distSq < br * br) {
                    val speed = sqrt(bird.vx * bird.vx + bird.vy * bird.vy)
                    
                    // Deflect bird
                    bird.vx = -bird.vx * 0.5f
                    bird.vy = -bird.vy * 0.4f

                    // If Bomb, detonate on impact!
                    if (bird.type == BirdType.BOMB && !bird.isAbilityUsed) {
                        triggerRadialExplosion(bird.x, bird.y, 140f)
                        bird.isAlive = false
                        bird.isAbilityUsed = true
                        addComicEffect(bird.x, bird.y, "KABOOM!", Color(0xFF5856D6))
                        break
                    }

                    // Push block
                    val pushFactor = (2f / block.getMass()).coerceAtLeast(0.5f)
                    block.vx += dx * pushFactor * 0.25f
                    block.vy += dy * pushFactor * 0.25f
                    block.isStable = false

                    val impactForce = speed.coerceAtLeast(1f)
                    block.life -= impactForce * 20f

                    pointsEarned += 80
                    stateChanged = true

                    val textLabels = when (block.material) {
                        MaterialType.WOOD -> listOf("CRACK!", "CLATTER!", "BOOM!")
                        MaterialType.ICE -> listOf("SHATTER!", "ZING!", "SPLIT!")
                        MaterialType.STONE -> listOf("THUD!", "CRASH!", "BAM!")
                    }
                    addComicEffect(closestX, closestY, textLabels.random(), block.getColor())
                }
            }

            // Monster direct hit checks
            for (monster in activeMonsters) {
                if (monster.isPopped) continue

                val dx = bx - monster.x
                val dy = by - monster.y
                val dist = sqrt(dx * dx + dy * dy)
                if (dist < br + monster.radius) {
                    // direct target popped!
                    monster.isPopped = true
                    monster.expression = MonsterExpression.POPPED
                    pointsEarned += 600
                    stateChanged = true
                    addComicEffect(monster.x, monster.y, "POP!", Color(0xFFFF2D55))

                    // If Bomb, detonate!
                    if (bird.type == BirdType.BOMB && !bird.isAbilityUsed) {
                        triggerRadialExplosion(bird.x, bird.y, 140f)
                        bird.isAlive = false
                        bird.isAbilityUsed = true
                        addComicEffect(bird.x, bird.y, "KABOOM!", Color(0xFF5856D6))
                        break
                    }
                }
            }
        }

        // 3. Solve 2D Gravity & Tower Inter-collisions (Internal Stack Solver)
        for (i in activeBlocks.indices) {
            val block = activeBlocks[i]
            if (block.life <= 0) continue

            if (!block.isStable) {
                block.vy += 0.22f
                block.vx *= 0.94f
                block.vy *= 0.94f

                block.x += block.vx
                block.y += block.vy

                val groundLimitY = 405f - block.height / 2
                if (block.y >= groundLimitY) {
                    block.y = groundLimitY
                    block.vy = -block.vy * 0.15f
                    block.vx *= 0.72f
                    if (abs(block.vy) < 0.12f && abs(block.vx) < 0.12f) {
                        block.vy = 0f
                        block.vx = 0f
                        block.isStable = true
                    }
                    stateChanged = true
                }
            }
        }

        // Inter-Block stacking overlaps
        for (i in activeBlocks.indices) {
            val a = activeBlocks[i]
            if (a.life <= 0) continue

            for (j in i + 1 until activeBlocks.size) {
                val b = activeBlocks[j]
                if (b.life <= 0) continue

                val aw = a.width / 2
                val ah = a.height / 2
                val bw = b.width / 2
                val bh = b.height / 2

                val overlapX = (aw + bw) - abs(a.x - b.x)
                val overlapY = (ah + bh) - abs(a.y - b.y)

                if (overlapX > 0 && overlapY > 0) {
                    if (overlapX < overlapY) {
                        val push = overlapX / 2f
                        if (a.x < b.x) {
                            a.x -= push
                            b.x += push
                        } else {
                            a.x += push
                            b.x -= push
                        }
                        val totalVx = a.vx + b.vx
                        a.vx = totalVx * 0.4f
                        b.vx = totalVx * 0.4f
                        a.isStable = false
                        b.isStable = false
                    } else {
                        val push = overlapY / 2f
                        if (a.y < b.y) {
                            a.y -= push
                            b.y += push
                        } else {
                            a.y += push
                            b.y -= push
                        }
                        val totalVy = a.vy + b.vy
                        a.vy = totalVy * 0.25f
                        b.vy = totalVy * 0.25f
                        a.isStable = false
                        b.isStable = false
                    }
                    stateChanged = true
                }
            }
        }

        // Fall crashes on monsters
        for (monster in activeMonsters) {
            if (monster.isPopped) continue

            for (block in activeBlocks) {
                if (block.life <= 0) continue

                val halfW = block.width / 2
                val halfH = block.height / 2
                val closestX = monster.x.coerceIn(block.x - halfW, block.x + halfW)
                val closestY = monster.y.coerceIn(block.y - halfH, block.y + halfH)

                val dx = monster.x - closestX
                val dy = monster.y - closestY
                val distSq = dx * dx + dy * dy

                if (distSq < monster.radius * monster.radius) {
                    val fallSpeed = sqrt(block.vx * block.vx + block.vy * block.vy)
                    if (fallSpeed > 0.4f || !block.isStable) {
                        monster.isPopped = true
                        monster.expression = MonsterExpression.POPPED
                        pointsEarned += 600
                        stateChanged = true
                        addComicEffect(monster.x, monster.y, "CRUSH!", Color(0xFFFF2D55))
                    }
                }
            }
        }

        // Save states
        if (stateChanged) {
            _blocks.value = activeBlocks.filter { it.life > 10f }
            _monsters.value = activeMonsters
        }
        if (pointsEarned > 0) {
            _score.value += pointsEarned
        }

        // 4. Update Particle effects
        val currEffects = _effects.value.map {
            it.copy(
                age = it.age + 1f,
                x = it.x + it.vx,
                y = it.y + it.vy
            )
        }.filter { it.age < it.maxAge }
        _effects.value = currEffects

        // 5. Game Ending conditions (award stars using the formula requested by user!)
        if (_gameEndStatus.value == "none") {
            val allPopped = _monsters.value.all { it.isPopped }
            if (allPopped) {
                // Victory! Dynamically award stars based on performance and birds remaining
                val currentIdx = _currentBirdQueueIndex.value
                val size = _birdsQueue.value.size
                val birdsLeft = (size - 1 - currentIdx).coerceAtLeast(0)

                // Kid reward! Extra unlaunched bird remaining adds +1000 points to overall score!
                val birdsBonus = birdsLeft * 1000
                _score.value += birdsBonus

                val finalScore = _score.value
                val lvlIdx = _currentLevelIndex.value

                // Stars rating logic based on total score threshold (including birds bonus!)
                val stars = when (lvlIdx) {
                    0 -> { // Level 1
                        when {
                            finalScore >= 2500 -> 3
                            finalScore >= 1400 -> 2
                            else -> 1
                        }
                    }
                    1 -> { // Level 2
                        when {
                            finalScore >= 3000 -> 3
                            finalScore >= 1800 -> 2
                            else -> 1
                        }
                    }
                    else -> { // Level 3
                        when {
                            finalScore >= 3500 -> 3
                            finalScore >= 2000 -> 2
                            else -> 1
                        }
                    }
                }

                _starsAwarded.value = stars
                _gameEndStatus.value = "victory"
                stopGameLoop()

                // Save to Room DB persistence!
                saveLevelProgression(stars)
            } else if (_birdState.value == "crashed" && _birdsRemaining.value == 0) {
                // Wait for blocks to fully settle
                val anyMoving = _blocks.value.any { !it.isStable }
                if (!anyMoving) {
                    _gameEndStatus.value = "defeat"
                    stopGameLoop()
                }
            }
        }
    }

    private fun crashActiveBirdGroup() {
        _birdState.value = "crashed"
        _trail.value = emptyList()

        val nextIdx = _currentBirdQueueIndex.value + 1
        val totalSize = _birdsQueue.value.size

        if (nextIdx < totalSize) {
            _currentBirdQueueIndex.value = nextIdx
            _birdsRemaining.value = totalSize - nextIdx
            _currentBirdType.value = _birdsQueue.value[nextIdx]

            _birdX.value = slingX
            _birdY.value = slingY
            _birdVx.value = 0f
            _birdVy.value = 0f
            _birdState.value = "idle"

            // Put monsters back to smiling happy expressions!
            _monsters.value = _monsters.value.map {
                if (!it.isPopped) it.copy(expression = MonsterExpression.HAPPY) else it
            }
        } else {
            // Out of launch tries!
            _birdsRemaining.value = 0
        }
    }

    private fun saveLevelProgression(stars: Int) {
        val lvl = _currentLevelIndex.value
        val finalScore = _score.value
        viewModelScope.launch {
            val currentBest = _savedScores.value[lvl]
            val bestScore = maxOf(currentBest?.highScore ?: 0, finalScore)
            val bestStars = maxOf(currentBest?.stars ?: 0, stars)

            repository.insertScore(
                GameScore(
                    levelIndex = lvl,
                    highScore = bestScore,
                    stars = bestStars,
                    unlocked = true
                )
            )

            // Unlock next level
            val nextLvl = lvl + 1
            if (nextLvl < 3) {
                repository.unlockLevel(nextLvl)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopGameLoop()
    }

    // Level configuration builder
    private fun generateBlocksForLevel(lvl: Int): List<PhysicsBlock> {
        val list = mutableListOf<PhysicsBlock>()
        when (lvl) {
            0 -> {
                var id = 0
                list.add(PhysicsBlock(id++, x = 580f, y = 375f, width = 45f, height = 45f, material = MaterialType.WOOD))
                list.add(PhysicsBlock(id++, x = 580f, y = 330f, width = 45f, height = 45f, material = MaterialType.WOOD))
                list.add(PhysicsBlock(id++, x = 580f, y = 285f, width = 45f, height = 45f, material = MaterialType.WOOD))
                list.add(PhysicsBlock(id++, x = 580f, y = 255f, width = 90f, height = 15f, material = MaterialType.ICE))
            }
            1 -> {
                var id = 10
                list.add(PhysicsBlock(id++, x = 530f, y = 365f, width = 30f, height = 80f, material = MaterialType.ICE))
                list.add(PhysicsBlock(id++, x = 530f, y = 295f, width = 30f, height = 60f, material = MaterialType.WOOD))
                list.add(PhysicsBlock(id++, x = 650f, y = 365f, width = 30f, height = 80f, material = MaterialType.ICE))
                list.add(PhysicsBlock(id++, x = 650f, y = 295f, width = 30f, height = 60f, material = MaterialType.WOOD))
                list.add(PhysicsBlock(id++, x = 590f, y = 255f, width = 160f, height = 15f, material = MaterialType.ICE))
                list.add(PhysicsBlock(id++, x = 590f, y = 230f, width = 50f, height = 35f, material = MaterialType.STONE))
            }
            2 -> {
                var id = 30
                list.add(PhysicsBlock(id++, x = 500f, y = 365f, width = 40f, height = 80f, material = MaterialType.STONE))
                list.add(PhysicsBlock(id++, x = 680f, y = 365f, width = 40f, height = 80f, material = MaterialType.STONE))
                list.add(PhysicsBlock(id++, x = 590f, y = 375f, width = 35f, height = 45f, material = MaterialType.WOOD))
                list.add(PhysicsBlock(id++, x = 590f, y = 330f, width = 35f, height = 45f, material = MaterialType.WOOD))
                list.add(PhysicsBlock(id++, x = 590f, y = 300f, width = 190f, height = 20f, material = MaterialType.STONE))
                list.add(PhysicsBlock(id++, x = 540f, y = 255f, width = 30f, height = 70f, material = MaterialType.WOOD))
                list.add(PhysicsBlock(id++, x = 640f, y = 255f, width = 30f, height = 70f, material = MaterialType.WOOD))
                list.add(PhysicsBlock(id++, x = 590f, y = 210f, width = 120f, height = 15f, material = MaterialType.ICE))
            }
        }
        return list
    }

    private fun generateMonstersForLevel(lvl: Int): List<CheekyMonster> {
        val list = mutableListOf<CheekyMonster>()
        when (lvl) {
            0 -> {
                list.add(CheekyMonster(id = 0, x = 580f, y = 235f))
            }
            1 -> {
                list.add(CheekyMonster(id = 10, x = 590f, y = 375f))
                list.add(CheekyMonster(id = 11, x = 590f, y = 190f))
            }
            2 -> {
                list.add(CheekyMonster(id = 20, x = 540f, y = 375f))
                list.add(CheekyMonster(id = 21, x = 640f, y = 375f))
                list.add(CheekyMonster(id = 22, x = 590f, y = 185f))
            }
        }
        return list
    }
}
