package com.voicedeutsch.master.voicecore.functions

/**
 * ════════════════════════════════════════════════════════════════════════════
 * ИЗМЕНЕНИЯ (Async Function Calling):
 *   ДОБАВЛЕНО: behavior в GeminiFunctionDeclaration — управляет
 *   блокирующим/неблокирующим выполнением функции в Live API.
 *
 *   ДОБАВЛЕНО: FunctionBehavior enum — BLOCKING, NON_BLOCKING, UNSPECIFIED.
 *   ДОБАВЛЕНО: FunctionResponseScheduling enum — INTERRUPT, WHEN_IDLE, SILENT.
 * ════════════════════════════════════════════════════════════════════════════
 */

data class GeminiFunctionDeclaration(
    val name: String,
    val description: String,
    val parameters: GeminiParameters? = null,
    /**
     * Поведение функции при вызове в Live API:
     * - BLOCKING (default): модель ждёт результат, блокируя диалог.
     * - NON_BLOCKING: модель продолжает разговор, пока функция выполняется.
     */
    val behavior: FunctionBehavior = FunctionBehavior.BLOCKING,
)

data class GeminiParameters(
    val type: String = "object",
    val properties: Map<String, GeminiProperty> = emptyMap(),
    val required: List<String> = emptyList(),
)

data class GeminiProperty(
    val type: String,
    val description: String = "",
    val enum: List<String>? = null,
)

/**
 * Определяет, блокирует ли вызов функции генерацию модели.
 *
 * BLOCKING — модель приостанавливает генерацию до получения результата.
 * NON_BLOCKING — модель продолжает разговор; результат обрабатывается
 *                асинхронно с FunctionResponseScheduling.
 * UNSPECIFIED — как BLOCKING (дефолт API).
 */
enum class FunctionBehavior {
    BLOCKING,
    NON_BLOCKING,
    UNSPECIFIED,
}

/**
 * Определяет, как модель обрабатывает результат NON_BLOCKING функции.
 *
 * INTERRUPT — модель прерывает текущую речь и сразу сообщает результат.
 * WHEN_IDLE — модель дожидается завершения текущего ответа.
 * SILENT — модель запоминает результат, но не озвучивает его.
 */
enum class FunctionResponseScheduling {
    INTERRUPT,
    WHEN_IDLE,
    SILENT,
}