package com.ktp.iptv.data

import android.content.Context
import androidx.room.Room
import com.ktp.iptv.data.db.AppDatabase
import com.ktp.iptv.data.db.FavoriteDao

interface AppContainer {
    val playlistRepository: PlaylistRepository
    val favoriteDao: FavoriteDao
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    private val db by lazy {
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "ktp_iptv.db"
        ).fallbackToDestructiveMigration().build()
    }

    override val favoriteDao: FavoriteDao by lazy { db.favoriteDao() }

    override val playlistRepository: PlaylistRepository by lazy {
        PlaylistRepository(favoriteDao)
    }
}
