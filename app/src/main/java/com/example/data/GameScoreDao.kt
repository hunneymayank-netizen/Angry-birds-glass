package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameScoreDao {
    @Query("SELECT * FROM game_scores ORDER BY levelIndex ASC")
    fun getAllScores(): Flow<List<GameScore>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScore(score: GameScore)

    @Query("UPDATE game_scores SET unlocked = :unlocked WHERE levelIndex = :levelIndex")
    suspend fun unlockLevel(levelIndex: Int, unlocked: Boolean)
}
