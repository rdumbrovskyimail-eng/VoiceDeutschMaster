const { onRequest } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const { GoogleGenAI } = require("@google/genai");

const GEMINI_API_KEY = defineSecret("GEMINI_API_KEY");

exports.getEphemeralToken = onRequest(
  { secrets: [GEMINI_API_KEY] },
  async (req, res) => {
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

      res.status(200).json({
        token: token.name,
        expiresAt: expireTime
      });

    } catch (error) {
      console.error("getEphemeralToken error:", error);
      res.status(500).json({ error: "Internal server error", details: error.message });
    }
  }
);