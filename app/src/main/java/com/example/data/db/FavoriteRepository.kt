package com.example.data.db

import kotlinx.coroutines.flow.Flow

class FavoriteRepository(private val favoriteDao: FavoriteDao) {
    val allFavorites: Flow<List<FavoriteBhajan>> = favoriteDao.getAllFavorites()

    suspend fun insert(bhajan: FavoriteBhajan) {
        favoriteDao.insertFavorite(bhajan)
    }

    suspend fun deleteById(videoId: String) {
        favoriteDao.deleteFavoriteById(videoId)
    }

    fun isFavorite(videoId: String): Flow<Boolean> {
        return favoriteDao.isFavoriteFlow(videoId)
    }

    suspend fun isFavoriteDirect(videoId: String): Boolean {
        return favoriteDao.isFavoriteDirect(videoId)
    }
}
