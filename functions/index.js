const { onRequest } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const { GoogleGenAI } = require("@google/genai");
const { initializeApp } = require("firebase-admin/app");
const { getAuth } = require("firebase-admin/auth");

// ✅ FIX: Инициализируем Firebase Admin для верификации токенов
initializeApp();

const GEMINI_API_KEY = defineSecret("GEMINI_API_KEY");

exports.getEphemeralToken = onRequest(
  { secrets: [GEMINI_API_KEY] },
  async (req, res) => {
    // CORS preflight
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

    // ✅ FIX: Верификация Firebase ID Token из заголовка Authorization
    // Без этого любой знающий URL мог бесконечно генерировать токены за ваш счёт
    const authHeader = req.headers.authorization || "";
    if (!authHeader.startsWith("Bearer ")) {
      res.status(401).json({ error: "Missing Authorization header" });
      return;
    }

    let verifiedUid;
    try {
      const idToken = authHeader.split("Bearer ")[1];
      const decodedToken = await getAuth().verifyIdToken(idToken);
      verifiedUid = decodedToken.uid;
    } catch (authError) {
      console.error("Auth verification failed:", authError.message);
      res.status(401).json({ error: "Invalid or expired auth token" });
      return;
    }

    try {
      const apiKey = GEMINI_API_KEY.value();
      const client = new GoogleGenAI({
        apiKey: apiKey,
        httpOptions: { apiVersion: "v1alpha" }
      });

      const expireTime = new Date(Date.now() + 30 * 60 * 1000).toISOString();

      const token = await client.authTokens.create({
        config: {
          uses: 1,
          expireTime: expireTime,
          liveConnectConstraints: {
            model: "gemini-2.5-flash-native-audio-preview",
            config: {
              responseModalities: ["AUDIO"]
            }
          },
          httpOptions: { apiVersion: "v1alpha" }
        }
      });

      console.log(`Token issued for uid: ${verifiedUid}`);

      res.status(200).json({
        token: token.name,   // ✅ Возвращаем полный resource name: "auth_tokens/XXX"
        expiresAt: expireTime
      });

    } catch (error) {
      console.error("getEphemeralToken error:", error);
      res.status(500).json({ error: "Internal server error", details: error.message });
    }
  }
);
