package com.example.ui

import androidx.compose.ui.graphics.Color

enum class BirdType(
    val displayName: String,
    val color: Color,
    val abilityName: String,
    val description: String
) {
    CLASSIC("Classic Red", Color(0xFFFF3B30), "Squeak", "Standard happy flyer"),
    SPEED_BOOST("Breezy Blue", Color(0xFF34C759), "Speedy Nitro", "Tap in flight to rocket forward!"),
    RAPID_FIRE("Rapid Rose", Color(0xFFFF2D55), "Triple Split", "Tap in flight to divide into three!"),
    BOMB("Bomb Black", Color(0xFF5856D6), "Comic Kaboom", "Tapped in flight or on impact: BIG BLAST!")
}

data class FlyingBird(
    val type: BirdType,
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var isAlive: Boolean = true,
    var isAbilityUsed: Boolean = false,
    val trail: MutableList<Pair<Float, Float>> = mutableListOf()
)

enum class MaterialType {
    WOOD, ICE, STONE
}

data class PhysicsBlock(
    val id: Int,
    var x: Float,          // Center coordinate
    var y: Float,          // Center coordinate
    val width: Float,
    val height: Float,
    var vx: Float = 0f,
    var vy: Float = 0f,
    val material: MaterialType,
    var life: Float = 100f, // Broken when <= 0
    var isStable: Boolean = false
) {
    fun getMass(): Float {
        return when (material) {
            MaterialType.WOOD -> 1.0f
            MaterialType.ICE -> 0.6f
            MaterialType.STONE -> 2.5f
        }
    }

    fun getColor(): Color {
        return when (material) {
            MaterialType.WOOD -> Color(0xFFE5A93B) // Warm golden wood
            MaterialType.ICE -> Color(0xFF8CE2F3)  // Bright cyan ice
            MaterialType.STONE -> Color(0xFF8A8A8A) // Soft slate stone
        }
    }
}

enum class MonsterExpression {
    HAPPY, SCARED, SHOCKED, POPPED
}

data class CheekyMonster(
    val id: Int,
    var x: Float,
    var y: Float,
    val radius: Float = 18f,
    var isPopped: Boolean = false,
    var expression: MonsterExpression = MonsterExpression.HAPPY,
    var vx: Float = 0f,
    var vy: Float = 0f
)

data class ParticleEffect(
    val x: Float,
    val y: Float,
    val text: String? = null, // e.g. "BOOM!"
    val color: Color,
    val isStar: Boolean = false,
    val vx: Float = 0f,
    val vy: Float = 0f,
    var age: Float = 0f, // Counts up to maxAge
    val maxAge: Float = 40f
)

data class LevelData(
    val levelIndex: Int,
    val title: String,
    val description: String,
    val blocks: List<PhysicsBlock>,
    val monsters: List<CheekyMonster>,
    val birdCount: Int = 3,
    val backgroundBrush: Color // Theme highlight
)
