package com.voicedeutsch.master.presentation.screen.settings

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.voicedeutsch.master.data.local.database.dao.BookDao
import com.voicedeutsch.master.data.local.datastore.UserPreferencesDataStore
import com.voicedeutsch.master.domain.repository.UserRepository
import com.voicedeutsch.master.presentation.theme.Background
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.voicedeutsch.master.util.NetworkMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

data class RuntimeTestResult(
    val name: String,
    val status: TestStatus,
    val message: String,
    val durationMs: Long,
)

enum class TestStatus { PENDING, RUNNING, PASS, FAIL, SKIP }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuntimeTestScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val userRepository: UserRepository = koinInject()
    val preferencesDataStore: UserPreferencesDataStore = koinInject()
    val networkMonitor: NetworkMonitor = koinInject()
    val bookDao: BookDao = koinInject()

    var results by remember { mutableStateOf<List<RuntimeTestResult>>(emptyList()) }
    var isRunning by remember { mutableStateOf(false) }

    val junitTestClasses = remember {
        listOf(
            "SrsCalculatorTest" to "Алгоритм интервального повторения (SRS)",
            "EvaluateAnswerUseCaseTest" to "Оценка ответов пользователя",
            "FunctionRegistryTest" to "Реестр функций Gemini",
            "FunctionRouterTest" to "Маршрутизация function calls",
            "ContextBuilderTest" to "Сборка контекста для сессии",
            "AppDatabaseTest" to "Room база данных: миграции, CRUD",
            "StrategySelectorTest" to "Выбор стратегии обучения",
            "VoiceSessionManagerTest" to "Управление голосовой сессией",
            "MasterPromptTest" to "Системный промпт Gemini",
        )
    }

    fun runAllTests() {
        isRunning = true
        results = emptyList()
        scope.launch {
            val tests = mutableListOf<RuntimeTestResult>()

            // 1. Network
            tests.add(runTest("Сеть (Network Monitor)") {
                val online = networkMonitor.isOnline()
                if (online) "Подключение активно" else throw Exception("Нет сети")
            })

            // 2. Room DB read/write
            tests.add(runTest("Room БД (read/write)") {
                val userId = withContext(Dispatchers.IO) { userRepository.getActiveUserId() }
                if (userId != null) {
                    val profile = withContext(Dispatchers.IO) { userRepository.getUserProfile(userId) }
                    "Пользователь: ${profile?.name ?: "N/A"}, ID: $userId"
                } else {
                    "Нет активного пользователя (это нормально до онбординга)"
                }
            })

            // 3. DataStore read/write
            tests.add(runTest("DataStore (read/write)") {
                val config = withContext(Dispatchers.IO) { preferencesDataStore.loadGeminiConfig() }
                "model=${config.modelName}, voice=${config.voiceName}"
            })

            // 4. Microphone
            tests.add(runTest("Микрофон (запись 500мс)") {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                if (!hasPermission) throw Exception("Нет разрешения RECORD_AUDIO")

                withContext(Dispatchers.IO) {
                    val bufferSize = AudioRecord.getMinBufferSize(
                        16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
                    )
                    if (bufferSize <= 0) throw Exception("Невалидный буфер: $bufferSize")

                    @Suppress("MissingPermission")
                    val recorder = AudioRecord(
                        MediaRecorder.AudioSource.VOICE_RECOGNITION,
                        16000, AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT, bufferSize,
                    )
                    try {
                        recorder.startRecording()
                        val buffer = ShortArray(bufferSize / 2)
                        val read = recorder.read(buffer, 0, buffer.size)
                        recorder.stop()
                        if (read > 0) "Записано $read сэмплов" else throw Exception("read=$read")
                    } finally {
                        recorder.release()
                    }
                }
            })

            // 5. BookDao
            tests.add(runTest("BookDao (user books)") {
                val books = withContext(Dispatchers.IO) { bookDao.getAllBooks() }
                "Найдено ${books.size} пользовательских книг"
            })

            // 6. Firebase connectivity (basic check)
            tests.add(runTest("Firebase (auth check)") {
                val auth = com.google.firebase.Firebase.auth
                val user = auth.currentUser
                if (user != null) "UID: ${user.uid} (anonymous=${user.isAnonymous})"
                else "Нет авторизованного пользователя"
            })

            results = tests
            isRunning = false
        }
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text("Runtime-тесты") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { runAllTests() }, enabled = !isRunning) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Запустить")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
        ) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Runtime тесты
                item {
                    Text(
                        "RUNTIME-ТЕСТЫ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    if (results.isEmpty() && !isRunning) {
                        Button(
                            onClick = { runAllTests() },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Запустить runtime-тесты") }
                    }
                }

                if (isRunning) {
                    item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
                }

                items(results) { result -> TestResultCard(result) }

                // JUnit тесты
                item {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "JUNIT-ТЕСТЫ (src/test)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Эти тесты запускаются через Gradle (./gradlew test).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                items(junitTestClasses) { testInfo ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "JUnit",
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.width(48.dp),
                            )
                            Column(Modifier.weight(1f)) {
                                Text(testInfo.first, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(testInfo.second, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TestResultCard(result: RuntimeTestResult) {
    val (icon, color) = when (result.status) {
        TestStatus.PASS    -> "PASS" to Color(0xFF22C55E)
        TestStatus.FAIL    -> "FAIL" to Color(0xFFEF4444)
        TestStatus.SKIP    -> "SKIP" to Color(0xFFF59E0B)
        TestStatus.RUNNING -> "..." to MaterialTheme.colorScheme.primary
        TestStatus.PENDING -> "..." to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                icon,
                color = color,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.width(40.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(result.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    result.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "${result.durationMs}ms",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private suspend fun runTest(name: String, block: suspend () -> String): RuntimeTestResult {
    val start = System.currentTimeMillis()
    return try {
        val message = block()
        RuntimeTestResult(name, TestStatus.PASS, message, System.currentTimeMillis() - start)
    } catch (e: Exception) {
        RuntimeTestResult(name, TestStatus.FAIL, e.message ?: "Unknown error", System.currentTimeMillis() - start)
    }
}
