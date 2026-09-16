package com.agydroid.data.local

import androidx.room.*
import com.agydroid.data.local.dao.*
import com.agydroid.data.local.entities.*

@Database(
    entities = [ProjectEntity::class, MessageEntity::class, BuildEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AgyDroidDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun messageDao(): MessageDao
    abstract fun buildDao(): BuildDao
}

class Converters {
    @TypeConverter
    fun fromProjectStatus(value: ProjectStatus): String = value.name

    @TypeConverter
    fun toProjectStatus(value: String): ProjectStatus =
        ProjectStatus.valueOf(value)

    @TypeConverter
    fun fromMessageRole(value: MessageRole): String = value.name

    @TypeConverter
    fun toMessageRole(value: String): MessageRole =
        MessageRole.valueOf(value)

    @TypeConverter
    fun fromBuildStatus(value: BuildStatus): String = value.name

    @TypeConverter
    fun toBuildStatus(value: String): BuildStatus =
        BuildStatus.valueOf(value)
}
