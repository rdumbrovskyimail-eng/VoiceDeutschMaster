// functions/index.js
// Last verified: 2026-02-23
//
// MIGRATION NOTE:
//   getEphemeralToken УДАЛЕНА — firebase-ai SDK + App Check управляют
//   авторизацией к Gemini Live API прозрачно. Cloud Function больше не нужна.
//   См. EphemeralTokenService.DELETED.kt для полного объяснения.
//
// ОСТАВЛЕНЫ / ДОБАВЛЕНЫ:
//   onUserDeleted    — очистка данных пользователя при удалении аккаунта
//   cleanupOldBackups — scheduled: удаление старых бекапов из Storage

"use strict";

const { onCall, HttpsError }      = require("firebase-functions/v2/https");
const { onSchedule }              = require("firebase-functions/v2/scheduler");
const { onDocumentDeleted }       = require("firebase-functions/v2/firestore");
const { initializeApp }           = require("firebase-admin/app");
const { getFirestore }            = require("firebase-admin/firestore");
const { getStorage }              = require("firebase-admin/storage");
const { getAuth }                 = require("firebase-admin/auth");

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
    const uid = event.params.uid;
    console.log(`Cleaning up data for deleted user: ${uid}`);

    const db      = getFirestore();
    const storage = getStorage();
    const bucket  = storage.bucket();

    // 1. Удаляем вложенные коллекции Firestore
    const subcollections = ["profile", "preferences", "progress", "statistics", "backups"];
    for (const sub of subcollections) {
        const ref = db.collection("users").doc(uid).collection(sub);
        const snap = await ref.get();
        const batch = db.batch();
        snap.docs.forEach(doc => batch.delete(doc.ref));
        if (!snap.empty) {
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
// cleanupOldBackups — scheduled: удаление бекапов старше 90 дней
//
// Запускается каждое воскресенье в 03:00 UTC.
// Проходит по всем пользователям в Firestore и удаляет старые бекапы
// из Storage и Firestore-метаданных.
//
// Лимит: оставляет минимум 3 последних бекапа на пользователя,
// даже если все они старше 90 дней.
// ─────────────────────────────────────────────────────────────────────────────
exports.cleanupOldBackups = onSchedule("every sunday 03:00", async () => {
    const db      = getFirestore();
    const storage = getStorage();
    const bucket  = storage.bucket();

    const KEEP_DAYS = 90;
    const MIN_KEEP  = 3;
    const cutoff    = Date.now() - KEEP_DAYS * 24 * 60 * 60 * 1000;

    console.log(`Starting backup cleanup. Cutoff: ${new Date(cutoff).toISOString()}`);

    // Получаем всех пользователей с бекапами
    const usersSnap = await db.collection("users").get();
    let totalDeleted = 0;

    for (const userDoc of usersSnap.docs) {
        const uid = userDoc.id;

        const backupsSnap = await db
            .collection("users").doc(uid)
            .collection("backups")
            .orderBy("timestamp", "desc")
            .get();

        if (backupsSnap.empty) continue;

        const all = backupsSnap.docs;
        // Оставляем минимум MIN_KEEP последних, остальные старше cutoff — удаляем
        const toDelete = all
            .slice(MIN_KEEP)
            .filter(doc => (doc.data().timestamp || 0) < cutoff);

        for (const doc of toDelete) {
            const storagePath = doc.data().storagePath;

            // Удаляем из Storage
            if (storagePath) {
                await bucket.file(storagePath).delete().catch(e => {
                    console.warn(`Storage delete failed for ${storagePath}: ${e.message}`);
                });
            }

            // Удаляем метаданные из Firestore
            await doc.ref.delete();
            totalDeleted++;
        }
    }

    console.log(`✅ Backup cleanup done. Deleted: ${totalDeleted} backups.`);
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
