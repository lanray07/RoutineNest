package com.routinenest.app.data

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module @InstallIn(SingletonComponent::class)
object Injection {
    @Provides @Singleton fun database(@ApplicationContext context: Context): NestDatabase = Room.databaseBuilder(context, NestDatabase::class.java, "routinenest.db").build()
}
