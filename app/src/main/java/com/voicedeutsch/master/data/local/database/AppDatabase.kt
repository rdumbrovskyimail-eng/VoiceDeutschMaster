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
        MistakeLogEntity::class
    ],
    version = 1,
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

    companion object {
        const val DATABASE_NAME = "voice_deutsch_master.db"
    }
}