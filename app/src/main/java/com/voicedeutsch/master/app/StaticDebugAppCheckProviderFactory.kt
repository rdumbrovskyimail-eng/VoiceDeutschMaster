package com.voicedeutsch.master.app

import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.AppCheckProvider
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.AppCheckToken

class StaticDebugAppCheckProviderFactory(
    private val debugToken: String
) : AppCheckProviderFactory {
    override fun create(firebaseApp: FirebaseApp): AppCheckProvider {
        return AppCheckProvider {
            Tasks.forResult(object : AppCheckToken() {
                override fun getToken(): String = debugToken
                override fun getExpireTimeMillis(): Long =
                    System.currentTimeMillis() + 3_600_000
            })
        }
    }
}