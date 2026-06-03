package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_scores")
data class GameScore(
    @PrimaryKey val levelIndex: Int,
    val highScore: Int,
    val stars: Int,
    val unlocked: Boolean
)
