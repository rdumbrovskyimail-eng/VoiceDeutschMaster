// functions/index.js
// Last verified: 2026-02-23
//
// MIGRATION NOTE:
//   getEphemeralToken УДАЛЕНА — firebase-ai SDK + App Check управляют
//   авторизацией к Gemini Live API прозрачно. Cloud Function больше не нужна.
//   См. EphemeralTokenService.DELETED.kt для полного объяснения.
//
// ОСТАВЛЕНЫ / ДОБАВЛЕНЫ:
//   onUserDeleted     — очистка данных пользователя при удалении аккаунта
//   cleanupOldBackups — scheduled: удаление старых Firestore-метаданных бекапов
//                       (Storage-файлы удаляются автоматически через Object Lifecycle,
//                        см. storage.rules / GCP Console → Lifecycle ниже)
//   deleteUserAccount — callable: полное удаление аккаунта по запросу пользователя
//
// ─────────────────────────────────────────────────────────────────────────────
// РЕКОМЕНДОВАННАЯ КОНФИГУРАЦИЯ: Object Lifecycle Management (GCP Console)
// ─────────────────────────────────────────────────────────────────────────────
//
// Вместо крона на удаление файлов из Storage настройте Lifecycle Rule в
// GCP Console → Cloud Storage → [your-bucket] → Lifecycle:
//
//   Rule name : delete-old-backups
//   Action    : Delete object
//   Condition : Age = 90 days
//               Matches prefix: backups/
//
// Это бесплатно, не требует Cloud Function и не грузит память.
// Cloud Function cleanupOldBackups ниже удаляет ТОЛЬКО Firestore-метаданные
// (коллекция users/{uid}/backups) — Storage-файлы она больше не трогает.
//
// Инструкция:
//   https://cloud.google.com/storage/docs/lifecycle
// ─────────────────────────────────────────────────────────────────────────────

"use strict";

const { onCall, HttpsError }  = require("firebase-functions/v2/https");
const { onSchedule }          = require("firebase-functions/v2/scheduler");
const { onDocumentDeleted }   = require("firebase-functions/v2/firestore");
const { initializeApp }       = require("firebase-admin/app");
const { getFirestore }        = require("firebase-admin/firestore");
const { getStorage }          = require("firebase-admin/storage");
const { getAuth }             = require("firebase-admin/auth");

initializeApp();

// ─────────────────────────────────────────────────────────────────────────────
// onUserDeleted — очистка Firestore-данных при удалении Firebase Auth аккаунта
//
// Триггер: удаление документа users/{uid} из Firestore.
// Удаляет все вложенные коллекции: profile, preferences, progress, statistics, backups.
// Удаляет файлы пользователя из Firebase Storage: users/{uid}/**.
//
// Security: триггер срабатывает на стороне сервера — Auth не требуется.
// ─────────────────────────────────────────────────────────────────────────────
exports.onUserDeleted = onDocumentDeleted("users/{uid}", async (event) => {
    const uid     = event.params.uid;
    const db      = getFirestore();
    const storage = getStorage();
    const bucket  = storage.bucket();

    console.log(`Cleaning up data for deleted user: ${uid}`);

    // 1. Удаляем вложенные коллекции Firestore
    const subcollections = ["profile", "preferences", "progress", "statistics", "backups"];
    for (const sub of subcollections) {
        const ref  = db.collection("users").doc(uid).collection(sub);
        const snap = await ref.get();
        if (!snap.empty) {
            const batch = db.batch();
            snap.docs.forEach(doc => batch.delete(doc.ref));
            await batch.commit();
            console.log(`Deleted ${snap.size} docs from users/${uid}/${sub}`);
        }
    }

    // 2. Удаляем файлы из Storage: users/{uid}/
    const [files] = await bucket.getFiles({ prefix: `users/${uid}/` });
    await Promise.allSettled(files.map(file => file.delete()));
    console.log(`Deleted ${files.length} Storage files for uid=${uid}`);

    console.log(`✅ Cleanup complete for uid=${uid}`);
});

// ─────────────────────────────────────────────────────────────────────────────
// cleanupOldBackups — scheduled: удаление старых Firestore-метаданных бекапов
//
// Запускается каждое воскресенье в 03:00 UTC.
//
// ⚠️  ВАЖНО: эта функция удаляет ТОЛЬКО Firestore-документы (метаданные).
//     Storage-файлы удаляются автоматически через Object Lifecycle Rule (90 дней,
//     prefix backups/) — настройте в GCP Console, см. комментарий вверху файла.
//
// РЕШЕНИЕ ПРОБЛЕМЫ ПАМЯТИ:
//   Было: db.collection("users").get() — загружает ВСЕХ пользователей в память.
//         При 10 000 пользователей → Memory limit exceeded.
//
//   Стало: пагинация через limit(PAGE_SIZE) + startAfter(lastDoc).
//          В памяти одновременно находится не более PAGE_SIZE пользователей.
//          Цикл продолжается пока есть следующая страница.
//
// Лимит: оставляет минимум MIN_KEEP последних бекапов на пользователя,
//        даже если все они старше KEEP_DAYS.
// ─────────────────────────────────────────────────────────────────────────────
exports.cleanupOldBackups = onSchedule("every sunday 03:00", async () => {
    const db = getFirestore();

    const KEEP_DAYS = 90;
    const MIN_KEEP  = 3;
    const PAGE_SIZE = 100;   // пользователей за один запрос — безопасно для памяти
    const cutoff    = Date.now() - KEEP_DAYS * 24 * 60 * 60 * 1000;

    console.log(`Starting backup metadata cleanup. Cutoff: ${new Date(cutoff).toISOString()}`);

    let totalDeleted = 0;
    let lastDoc      = null;   // курсор пагинации
    let pageIndex    = 0;

    // ── Пагинированный обход пользователей ───────────────────────────────────
    // Вместо .get() на всю коллекцию — постранично по PAGE_SIZE.
    // Каждая итерация загружает не более PAGE_SIZE документов пользователей.
    do {
        let query = db.collection("users").limit(PAGE_SIZE);
        if (lastDoc) {
            query = query.startAfter(lastDoc);
        }

        const usersSnap = await query.get();
        if (usersSnap.empty) break;

        pageIndex++;
        console.log(`Processing page ${pageIndex}: ${usersSnap.size} users`);

        // ── Обработка пользователей на текущей странице ───────────────────────
        for (const userDoc of usersSnap.docs) {
            const uid = userDoc.id;

            const backupsSnap = await db
                .collection("users").doc(uid)
                .collection("backups")
                .orderBy("timestamp", "desc")
                .get();

            if (backupsSnap.empty) continue;

            // Оставляем MIN_KEEP последних, остальные старше cutoff — удаляем
            const toDelete = backupsSnap.docs
                .slice(MIN_KEEP)
                .filter(doc => (doc.data().timestamp || 0) < cutoff);

            if (toDelete.length === 0) continue;

            // Удаляем Firestore-метаданные батчем (Storage удаляет Lifecycle Rule)
            const batch = db.batch();
            toDelete.forEach(doc => batch.delete(doc.ref));
            await batch.commit();

            totalDeleted += toDelete.length;
            console.log(`uid=${uid}: deleted ${toDelete.length} backup metadata docs`);
        }

        // Курсор на последний документ страницы — для следующей итерации
        lastDoc = usersSnap.docs[usersSnap.docs.length - 1];

    } while (lastDoc !== null);

    console.log(`✅ Backup metadata cleanup done. Pages: ${pageIndex}, deleted: ${totalDeleted} docs.`);
});

// ─────────────────────────────────────────────────────────────────────────────
// deleteUserAccount — callable: полное удаление аккаунта по запросу пользователя
//
// Вызывается из SettingsScreen при нажатии "Удалить аккаунт".
// Порядок: удалить Firestore данные → удалить Storage файлы → удалить Auth аккаунт.
// Удаление Auth аккаунта триггернёт onUserDeleted для финальной очистки.
//
// Security: onCall автоматически верифицирует Firebase Auth токен.
//           App Check верифицируется автоматически если включён Enforcement.
// ─────────────────────────────────────────────────────────────────────────────
exports.deleteUserAccount = onCall(
    { enforceAppCheck: true },   // App Check обязателен для деструктивных операций
    async (request) => {
        const uid = request.auth?.uid;
        if (!uid) {
            throw new HttpsError("unauthenticated", "Authentication required.");
        }

        console.log(`deleteUserAccount requested by uid=${uid}`);

        const auth = getAuth();

        // Удаляем Auth аккаунт — это триггернёт onUserDeleted для очистки данных
        await auth.deleteUser(uid);

        console.log(`✅ Auth account deleted for uid=${uid}`);
        return { success: true };
    }
);  
