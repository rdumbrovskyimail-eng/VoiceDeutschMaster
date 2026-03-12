// Путь: src/test/java/com/voicedeutsch/master/data/mapper/UserMapperTest.kt
package com.voicedeutsch.master.data.mapper

import com.voicedeutsch.master.data.local.database.entity.UserEntity
import com.voicedeutsch.master.domain.model.user.CefrLevel
import com.voicedeutsch.master.domain.model.user.UserPreferences
import com.voicedeutsch.master.domain.model.user.UserProfile
import com.voicedeutsch.master.domain.model.user.VoiceSettings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UserMapperTest {

    private lateinit var json: Json

    @BeforeEach
    fun setUp() {
        json = Json { ignoreUnknownKeys = true }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildUserEntity(
        id: String = "user_1",
        name: String = "Max Mustermann",
        nativeLanguage: String = "ru",
        targetLanguage: String = "de",
        cefrLevel: String = "B1",
        cefrSubLevel: Int = 1,
        totalSessions: Int = 42,
        totalMinutes: Int = 630,
        totalWordsLearned: Int = 200,
        totalRulesLearned: Int = 30,
        age: Int? = 25,
        hobbies: String? = "reading, hiking",
        learningGoals: String? = "Business German",
        streakDays: Int = 7,
        lastSessionDate: Long? = 9000L,
        createdAt: Long = 1000L,
        updatedAt: Long = 2000L,
        preferencesJson: String = json.encodeToString(UserPreferences()),
        voiceSettingsJson: String = json.encodeToString(VoiceSettings()),
    ) = UserEntity(
        id = id,
        name = name,
        nativeLanguage = nativeLanguage,
        targetLanguage = targetLanguage,
        cefrLevel = cefrLevel,
        cefrSubLevel = cefrSubLevel,
        totalSessions = totalSessions,
        totalMinutes = totalMinutes,
        totalWordsLearned = totalWordsLearned,
        totalRulesLearned = totalRulesLearned,
        age = age,
        hobbies = hobbies,
        learningGoals = learningGoals,
        streakDays = streakDays,
        lastSessionDate = lastSessionDate,
        createdAt = createdAt,
        updatedAt = updatedAt,
        preferencesJson = preferencesJson,
        voiceSettingsJson = voiceSettingsJson,
    )

    private fun buildUserProfile(
        id: String = "user_1",
        name: String = "Max Mustermann",
        nativeLanguage: String = "ru",
        targetLanguage: String = "de",
        cefrLevel: CefrLevel = CefrLevel.B1,
        cefrSubLevel: String? = "B1.2",
        totalSessions: Int = 42,
        totalMinutes: Int = 630,
        totalWordsLearned: Int = 200,
        totalRulesLearned: Int = 30,
        age: Int? = 25,
        hobbies: String? = "reading, hiking",
        learningGoals: String? = "Business German",
        streakDays: Int = 7,
        lastSessionDate: Long? = 9000L,
        createdAt: Long = 1000L,
        updatedAt: Long = 2000L,
        preferences: UserPreferences = UserPreferences(),
        voiceSettings: VoiceSettings = VoiceSettings(),
    ) = UserProfile(
        id = id,
        name = name,
        nativeLanguage = nativeLanguage,
        targetLanguage = targetLanguage,
        cefrLevel = cefrLevel,
        cefrSubLevel = cefrSubLevel,
        totalSessions = totalSessions,
        totalMinutes = totalMinutes,
        totalWordsLearned = totalWordsLearned,
        totalRulesLearned = totalRulesLearned,
        age = age,
        hobbies = hobbies,
        learningGoals = learningGoals,
        streakDays = streakDays,
        lastSessionDate = lastSessionDate,
        createdAt = createdAt,
        updatedAt = updatedAt,
        preferences = preferences,
        voiceSettings = voiceSettings,
    )

    // ── UserEntity.toDomain ───────────────────────────────────────────────────

    @Test
    fun userEntity_toDomain_validData_mapsAllScalarFields() {
        val entity = buildUserEntity()
        with(UserMapper) {
            val domain = entity.toDomain(json)
            assertEquals(entity.id, domain.id)
            assertEquals(entity.name, domain.name)
            assertEquals(entity.nativeLanguage, domain.nativeLanguage)
            assertEquals(entity.targetLanguage, domain.targetLanguage)
            assertEquals(entity.cefrSubLevel, domain.cefrSubLevel)
            assertEquals(entity.totalSessions, domain.totalSessions)
            assertEquals(entity.totalMinutes, domain.totalMinutes)
            assertEquals(entity.totalWordsLearned, domain.totalWordsLearned)
            assertEquals(entity.totalRulesLearned, domain.totalRulesLearned)
            assertEquals(entity.age, domain.age)
            assertEquals(entity.hobbies, domain.hobbies)
            assertEquals(entity.learningGoals, domain.learningGoals)
            assertEquals(entity.streakDays, domain.streakDays)
            assertEquals(entity.lastSessionDate, domain.lastSessionDate)
            assertEquals(entity.createdAt, domain.createdAt)
            assertEquals(entity.updatedAt, domain.updatedAt)
        }
    }

    @Test
    fun userEntity_toDomain_validCefrLevel_mapsCorrectly() {
        val entity = buildUserEntity(cefrLevel = "B1")
        with(UserMapper) {
            assertEquals(CefrLevel.B1, entity.toDomain(json).cefrLevel)
        }
    }

    @Test
    fun userEntity_toDomain_allCefrLevels_mappedCorrectly() {
        CefrLevel.entries.forEach { level ->
            val entity = buildUserEntity(cefrLevel = level.name)
            with(UserMapper) {
                assertEquals(level, entity.toDomain(json).cefrLevel)
            }
        }
    }

    @Test
    fun userEntity_toDomain_validPreferencesJson_parsedToObject() {
        val prefs = UserPreferences()
        val entity = buildUserEntity(preferencesJson = json.encodeToString(prefs))
        with(UserMapper) {
            assertNotNull(entity.toDomain(json).preferences)
        }
    }

    @Test
    fun userEntity_toDomain_emptyPreferencesJson_fallsBackToDefault() {
        val entity = buildUserEntity(preferencesJson = "")
        with(UserMapper) {
            val domain = entity.toDomain(json)
            assertEquals(UserPreferences(), domain.preferences)
        }
    }

    @Test
    fun userEntity_toDomain_invalidPreferencesJson_fallsBackToDefault() {
        val entity = buildUserEntity(preferencesJson = "{not_valid_json!!}")
        with(UserMapper) {
            val domain = entity.toDomain(json)
            assertEquals(UserPreferences(), domain.preferences)
        }
    }

    @Test
    fun userEntity_toDomain_blankPreferencesJson_fallsBackToDefault() {
        val entity = buildUserEntity(preferencesJson = "   ")
        with(UserMapper) {
            val domain = entity.toDomain(json)
            assertEquals(UserPreferences(), domain.preferences)
        }
    }

    @Test
    fun userEntity_toDomain_validVoiceSettingsJson_parsedToObject() {
        val settings = VoiceSettings()
        val entity = buildUserEntity(voiceSettingsJson = json.encodeToString(settings))
        with(UserMapper) {
            assertNotNull(entity.toDomain(json).voiceSettings)
        }
    }

    @Test
    fun userEntity_toDomain_emptyVoiceSettingsJson_fallsBackToDefault() {
        val entity = buildUserEntity(voiceSettingsJson = "")
        with(UserMapper) {
            assertEquals(VoiceSettings(), entity.toDomain(json).voiceSettings)
        }
    }

    @Test
    fun userEntity_toDomain_invalidVoiceSettingsJson_fallsBackToDefault() {
        val entity = buildUserEntity(voiceSettingsJson = "INVALID_JSON")
        with(UserMapper) {
            assertEquals(VoiceSettings(), entity.toDomain(json).voiceSettings)
        }
    }

    @Test
    fun userEntity_toDomain_nullOptionalFields_preservedAsNull() {
        val entity = buildUserEntity(
            cefrSubLevel = 0,
            age = null,
            hobbies = null,
            learningGoals = null,
            lastSessionDate = null,
        )
        with(UserMapper) {
            val domain = entity.toDomain(json)
            assertNull(domain.cefrSubLevel)
            assertNull(domain.age)
            assertNull(domain.hobbies)
            assertNull(domain.learningGoals)
            assertNull(domain.lastSessionDate)
        }
    }

    @Test
    fun userEntity_toDomain_zeroCounters_preservedAsZero() {
        val entity = buildUserEntity(
            totalSessions = 0,
            totalMinutes = 0,
            totalWordsLearned = 0,
            totalRulesLearned = 0,
            streakDays = 0,
        )
        with(UserMapper) {
            val domain = entity.toDomain(json)
            assertEquals(0, domain.totalSessions)
            assertEquals(0, domain.totalMinutes)
            assertEquals(0, domain.totalWordsLearned)
            assertEquals(0, domain.totalRulesLearned)
            assertEquals(0, domain.streakDays)
        }
    }

    // ── UserProfile.toEntity ──────────────────────────────────────────────────

    @Test
    fun userProfile_toEntity_validData_mapsAllScalarFields() {
        val domain = buildUserProfile()
        with(UserMapper) {
            val entity = domain.toEntity(json)
            assertEquals(domain.id, entity.id)
            assertEquals(domain.name, entity.name)
            assertEquals(domain.nativeLanguage, entity.nativeLanguage)
            assertEquals(domain.targetLanguage, entity.targetLanguage)
            assertEquals(domain.cefrLevel.name, entity.cefrLevel)
            assertEquals(domain.cefrSubLevel, entity.cefrSubLevel)
            assertEquals(domain.totalSessions, entity.totalSessions)
            assertEquals(domain.totalMinutes, entity.totalMinutes)
            assertEquals(domain.totalWordsLearned, entity.totalWordsLearned)
            assertEquals(domain.totalRulesLearned, entity.totalRulesLearned)
            assertEquals(domain.age, entity.age)
            assertEquals(domain.hobbies, entity.hobbies)
            assertEquals(domain.learningGoals, entity.learningGoals)
            assertEquals(domain.streakDays, entity.streakDays)
            assertEquals(domain.lastSessionDate, entity.lastSessionDate)
            assertEquals(domain.createdAt, entity.createdAt)
            assertEquals(domain.updatedAt, entity.updatedAt)
            assertNotNull(entity.preferencesJson)
            assertNotNull(entity.voiceSettingsJson)
        }
    }

    @Test
    fun userProfile_toEntity_cefrLevelStoredAsEnumName() {
        CefrLevel.entries.forEach { level ->
            val domain = buildUserProfile(cefrLevel = level)
            with(UserMapper) {
                assertEquals(level.name, domain.toEntity(json).cefrLevel)
            }
        }
    }

    @Test
    fun userProfile_toEntity_preferencesSerializedToJson() {
        val domain = buildUserProfile(preferences = UserPreferences())
        with(UserMapper) {
            val entity = domain.toEntity(json)
            assertTrue(entity.preferencesJson.isNotBlank())
        }
    }

    @Test
    fun userProfile_toEntity_voiceSettingsSerializedToJson() {
        val domain = buildUserProfile(voiceSettings = VoiceSettings())
        with(UserMapper) {
            val entity = domain.toEntity(json)
            assertTrue(entity.voiceSettingsJson.isNotBlank())
        }
    }

    @Test
    fun userProfile_toEntity_nullOptionalFields_preservedAsNull() {
        val domain = buildUserProfile(
            age = null,
            hobbies = null,
            learningGoals = null,
            lastSessionDate = null,
        )
        with(UserMapper) {
            val entity = domain.toEntity(json)
            assertNull(entity.cefrSubLevel)
            assertNull(entity.age)
            assertNull(entity.hobbies)
            assertNull(entity.learningGoals)
            assertNull(entity.lastSessionDate)
        }
    }

    // ── UserProfile roundtrip ─────────────────────────────────────────────────

    @Test
    fun userProfile_roundtrip_entityToDomainToEntity_scalarFieldsMatch() {
        val original = buildUserEntity()
        with(UserMapper) {
            val domain = original.toDomain(json)
            val restored = domain.toEntity(json)
            assertEquals(original.id, restored.id)
            assertEquals(original.name, restored.name)
            assertEquals(original.nativeLanguage, restored.nativeLanguage)
            assertEquals(original.targetLanguage, restored.targetLanguage)
            assertEquals(original.cefrLevel, restored.cefrLevel)
            assertEquals(original.cefrSubLevel, restored.cefrSubLevel)
            assertEquals(original.totalSessions, restored.totalSessions)
            assertEquals(original.totalMinutes, restored.totalMinutes)
            assertEquals(original.totalWordsLearned, restored.totalWordsLearned)
            assertEquals(original.totalRulesLearned, restored.totalRulesLearned)
            assertEquals(original.age, restored.age)
            assertEquals(original.hobbies, restored.hobbies)
            assertEquals(original.learningGoals, restored.learningGoals)
            assertEquals(original.streakDays, restored.streakDays)
            assertEquals(original.lastSessionDate, restored.lastSessionDate)
            assertEquals(original.createdAt, restored.createdAt)
            assertEquals(original.updatedAt, restored.updatedAt)
        }
    }

    @Test
    fun userProfile_roundtrip_preferencesPreserved() {
        val original = buildUserEntity()
        with(UserMapper) {
            val domain = original.toDomain(json)
            val restored = domain.toEntity(json)
            val restoredDomain = restored.toDomain(json)
            assertEquals(domain.preferences, restoredDomain.preferences)
        }
    }

    @Test
    fun userProfile_roundtrip_voiceSettingsPreserved() {
        val original = buildUserEntity()
        with(UserMapper) {
            val domain = original.toDomain(json)
            val restored = domain.toEntity(json)
            val restoredDomain = restored.toDomain(json)
            assertEquals(domain.voiceSettings, restoredDomain.voiceSettings)
        }
    }

    @Test
    fun userProfile_roundtrip_invalidPreferencesJson_defaultPreservesOnRestore() {
        val original = buildUserEntity(preferencesJson = "GARBAGE")
        with(UserMapper) {
            val domain = original.toDomain(json)
            assertEquals(UserPreferences(), domain.preferences)
            val restored = domain.toEntity(json)
            val restoredDomain = restored.toDomain(json)
            assertEquals(UserPreferences(), restoredDomain.preferences)
        }
    }

    @Test
    fun userProfile_roundtrip_invalidVoiceSettingsJson_defaultPreservesOnRestore() {
        val original = buildUserEntity(voiceSettingsJson = "GARBAGE")
        with(UserMapper) {
            val domain = original.toDomain(json)
            assertEquals(VoiceSettings(), domain.voiceSettings)
            val restored = domain.toEntity(json)
            val restoredDomain = restored.toDomain(json)
            assertEquals(VoiceSettings(), restoredDomain.voiceSettings)
        }
    }

    @Test
    fun userProfile_roundtrip_nullOptionalFields_preservedAsNull() {
        val original = buildUserEntity(
            cefrSubLevel = 0,
            age = null,
            hobbies = null,
            learningGoals = null,
            lastSessionDate = null,
        )
        with(UserMapper) {
            val domain = original.toDomain(json)
            val restored = domain.toEntity(json)
            assertNull(restored.cefrSubLevel)
            assertNull(restored.age)
            assertNull(restored.hobbies)
            assertNull(restored.learningGoals)
            assertNull(restored.lastSessionDate)
        }
    }
}
