package com.geepscontrol.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

fun createDatabaseBuilder(context: Context): RoomDatabase.Builder<GeepsDatabase> =
    Room.databaseBuilder<GeepsDatabase>(
        context = context.applicationContext,
        name = context.applicationContext.getDatabasePath(DATABASE_NAME).absolutePath,
    )

fun buildDatabase(
    context: Context,
    name: String = DATABASE_NAME,
): GeepsDatabase =
    Room
        .databaseBuilder<GeepsDatabase>(
            context = context.applicationContext,
            name = context.applicationContext.getDatabasePath(name).absolutePath,
        ).enableMultiInstanceInvalidation()
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
