package com.voicedeutsch.master.app

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.AppCheckProvider
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.AppCheckToken
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Callable
import java.util.concurrent.Executors

class StaticDebugAppCheckProviderFactory(
    private val debugToken: String
) : AppCheckProviderFactory {

    override fun create(firebaseApp: FirebaseApp): AppCheckProvider {
        return StaticDebugAppCheckProvider(debugToken, firebaseApp)
    }
}

private class StaticDebugAppCheckProvider(
    private val debugToken: String,
    private val firebaseApp: FirebaseApp
) : AppCheckProvider {

    private val executor = Executors.newSingleThreadExecutor()

    override fun getToken(): Task<AppCheckToken> {
        return Tasks.call(executor, Callable {
            exchangeDebugToken()
        })
    }

    private fun exchangeDebugToken(): AppCheckToken {
        val projectId = firebaseApp.options.projectId
            ?: throw IllegalStateException("Project ID not found")
        val appId = firebaseApp.options.applicationId

        val url = URL(
            "https://firebaseappcheck.googleapis.com/v1/projects/$projectId/apps/$appId:exchangeDebugToken?key=${firebaseApp.options.apiKey}"
        )

        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val body = JSONObject().apply {
                put("debugToken", debugToken)
            }

            connection.outputStream.use { os ->
                os.write(body.toString().toByteArray())
            }

            val responseCode = connection.responseCode
            if (responseCode != 200) {
                val error = connection.errorStream?.bufferedReader()?.readText() ?: "unknown"
                Log.e("StaticDebugAppCheck", "Exchange failed: $responseCode - $error")
                throw Exception("Token exchange failed: $responseCode - $error")
            }

            val response = connection.inputStream.bufferedReader().readText()
            val json = JSONObject(response)
            val token = json.getString("token")
            val ttl = json.getString("ttl").removeSuffix("s").toDouble()
            val expireTime = System.currentTimeMillis() + (ttl * 1000).toLong()

            Log.d("StaticDebugAppCheck", "✅ Token exchanged successfully")

            return object : AppCheckToken() {
                override fun getToken(): String = token
                override fun getExpireTimeMillis(): Long = expireTime
            }
        } finally {
            connection.disconnect()
        }
    }
}