const { onRequest } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const fetch = require("node-fetch");

const GEMINI_API_KEY = defineSecret("GEMINI_API_KEY");

/**
 * HTTPS Function: выдаёт Ephemeral Token для Gemini Live API.
 *
 * Android вызывает: POST https://<region>-<project>.cloudfunctions.net/getEphemeralToken
 * Body: { "userId": "..." }
 * Response: { "token": "...", "expiresAt": "..." }
 */
exports.getEphemeralToken = onRequest(
  { secrets: [GEMINI_API_KEY] },
  async (req, res) => {
    // CORS
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

      const googleResponse = await fetch(
        `https://generativelanguage.googleapis.com/v1beta/${model}:generateEphemeralToken?key=${apiKey}`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            ttl: "3600s",
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
      const token = data.token;
      const expiresAt = data.expireTime;

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