package com.voicedeutsch.master.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.voicedeutsch.master.data.local.database.converter.Converters
import com.voicedeutsch.master.data.local.database.dao.BookProgressDao
import com.voicedeutsch.master.data.local.database.dao.GrammarRuleDao
import com.voicedeutsch.master.data.local.database.dao.KnowledgeDao
import com.voicedeutsch.master.data.local.database.dao.MistakeDao
import com.voicedeutsch.master.data.local.database.dao.PhraseDao
import com.voicedeutsch.master.data.local.database.dao.ProgressDao
import com.voicedeutsch.master.data.local.database.dao.SessionDao
import com.voicedeutsch.master.data.local.database.dao.UserDao
import com.voicedeutsch.master.data.local.database.dao.WordDao
import com.voicedeutsch.master.data.local.database.entity.BookProgressEntity
import com.voicedeutsch.master.data.local.database.entity.DailyStatisticsEntity
import com.voicedeutsch.master.data.local.database.entity.GrammarRuleEntity
import com.voicedeutsch.master.data.local.database.entity.MistakeLogEntity
import com.voicedeutsch.master.data.local.database.dao.AchievementDao
import com.voicedeutsch.master.data.local.database.entity.AchievementEntity
import com.voicedeutsch.master.data.local.database.entity.UserAchievementEntity
import com.voicedeutsch.master.data.local.database.entity.PhraseEntity
import com.voicedeutsch.master.data.local.database.entity.PhraseKnowledgeEntity
import com.voicedeutsch.master.data.local.database.entity.PronunciationRecordEntity
import com.voicedeutsch.master.data.local.database.entity.RuleKnowledgeEntity
import com.voicedeutsch.master.data.local.database.entity.SessionEntity
import com.voicedeutsch.master.data.local.database.entity.SessionEventEntity
import com.voicedeutsch.master.data.local.database.entity.UserEntity
import com.voicedeutsch.master.data.local.database.entity.WordEntity
import com.voicedeutsch.master.data.local.database.entity.WordKnowledgeEntity

@Database(
    entities = [
        UserEntity::class,
        WordEntity::class,
        WordKnowledgeEntity::class,
        GrammarRuleEntity::class,
        RuleKnowledgeEntity::class,
        PhraseEntity::class,
        PhraseKnowledgeEntity::class,
        SessionEntity::class,
        SessionEventEntity::class,
        BookProgressEntity::class,
        DailyStatisticsEntity::class,
        PronunciationRecordEntity::class,
        MistakeLogEntity::class,
        AchievementEntity::class,
        UserAchievementEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun wordDao(): WordDao
    abstract fun knowledgeDao(): KnowledgeDao
    abstract fun grammarRuleDao(): GrammarRuleDao
    abstract fun phraseDao(): PhraseDao
    abstract fun sessionDao(): SessionDao
    abstract fun bookProgressDao(): BookProgressDao
    abstract fun progressDao(): ProgressDao
    abstract fun mistakeDao(): MistakeDao
    abstract fun achievementDao(): AchievementDao

    companion object {
        const val DATABASE_NAME = "voice_deutsch_master.db"
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS achievements (
                        id TEXT NOT NULL PRIMARY KEY,
                        name_ru TEXT NOT NULL,
                        name_de TEXT NOT NULL,
                        description_ru TEXT NOT NULL,
                        icon TEXT NOT NULL,
                        condition_json TEXT NOT NULL,
                        category TEXT NOT NULL,
                        created_at INTEGER NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS user_achievements (
                        id TEXT NOT NULL PRIMARY KEY,
                        user_id TEXT NOT NULL,
                        achievement_id TEXT NOT NULL,
                        earned_at INTEGER NOT NULL,
                        announced INTEGER NOT NULL DEFAULT 0,
                        created_at INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                        FOREIGN KEY (achievement_id) REFERENCES achievements(id) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_user_achievements_user_id ON user_achievements(user_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_user_achievements_achievement_id ON user_achievements(achievement_id)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_user_achievements_user_id_achievement_id ON user_achievements(user_id, achievement_id)")
            }
        }
    }
}