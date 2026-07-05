package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorite_bhajans ORDER BY timestamp DESC")
    fun getAllFavorites(): Flow<List<FavoriteBhajan>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(bhajan: FavoriteBhajan)

    @Query("DELETE FROM favorite_bhajans WHERE videoId = :videoId")
    suspend fun deleteFavoriteById(videoId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_bhajans WHERE videoId = :videoId LIMIT 1)")
    fun isFavoriteFlow(videoId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_bhajans WHERE videoId = :videoId LIMIT 1)")
    suspend fun isFavoriteDirect(videoId: String): Boolean
}
