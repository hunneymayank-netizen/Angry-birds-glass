package com.example.data

import kotlinx.coroutines.flow.Flow

class GameRepository(private val gameScoreDao: GameScoreDao) {
    val allScores: Flow<List<GameScore>> = gameScoreDao.getAllScores()

    suspend fun insertScore(score: GameScore) {
        gameScoreDao.insertScore(score)
    }

    suspend fun unlockLevel(levelIndex: Int) {
        gameScoreDao.unlockLevel(levelIndex, true)
    }

    suspend fun initializeScoresIfEmpty() {
        // We will seed initial levels if they do not exist
        // Levels: 0, 1, 2
        // Level 0 is unlocked by default, others locked initially
        val initialScores = listOf(
            GameScore(levelIndex = 0, highScore = 0, stars = 0, unlocked = true),
            GameScore(levelIndex = 1, highScore = 0, stars = 0, unlocked = false),
            GameScore(levelIndex = 2, highScore = 0, stars = 0, unlocked = false)
        )
        for (score in initialScores) {
            gameScoreDao.insertScore(score)
        }
    }
}
