const { onRequest } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const fetch = require("node-fetch");

// Ключ хранится в Firebase Secret Manager — никогда не попадает в APK
const GEMINI_API_KEY = defineSecret("GEMINI_API_KEY");

/**
 * HTTPS Function: выдаёт Ephemeral Token для Gemini Live API.
 *
 * Android вызывает: POST https://<region>-<project>.cloudfunctions.net/getEphemeralToken
 * Body: { "userId": "..." }
 * Response: { "token": "...", "expiresAt": 1234567890 }
 *
 * Токен живёт ~1 час. Настоящий API ключ остаётся только здесь.
 */
exports.getEphemeralToken = onRequest(
  { secrets: [GEMINI_API_KEY] },
  async (req, res) => {
    // CORS для мобильных клиентов
    res.set("Access-Control-Allow-Origin", "*");
    if (req.method === "OPTIONS") {
      res.set("Access-Control-Allow-Methods", "POST");
      res.set("Access-Control-Allow-Headers", "Content-Type, Authorization");
      res.status(204).send("");
      return;
    }

    if (req.method !== "POST") {
      res.status(405).json({ error: "Method not allowed" });
      return;
    }

    try {
      const apiKey = GEMINI_API_KEY.value();
      const model = "models/gemini-2.5-flash-native-audio-preview";

      // Запрос временного токена у Google
      const googleResponse = await fetch(
        `https://generativelanguage.googleapis.com/v1beta/${model}:generateContent?key=${apiKey}`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            // Запрашиваем ephemeral token через специальный endpoint
            ephemeralTokenRequest: {
              expireTime: new Date(Date.now() + 60 * 60 * 1000).toISOString(), // +1 час
            },
          }),
        }
      );

      if (!googleResponse.ok) {
        const errorBody = await googleResponse.text();
        console.error("Google API error:", errorBody);
        res.status(502).json({ error: "Failed to get token from Google" });
        return;
      }

      const data = await googleResponse.json();
      const token = data.ephemeralToken?.token;
      const expiresAt = data.ephemeralToken?.expireTime;

      if (!token) {
        console.error("No token in response:", JSON.stringify(data));
        res.status(502).json({ error: "Invalid response from Google" });
        return;
      }

      res.status(200).json({ token, expiresAt });
    } catch (error) {
      console.error("getEphemeralToken error:", error);
      res.status(500).json({ error: "Internal server error" });
    }
  }
);
