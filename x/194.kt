// Path: src/test/java/com/voicedeutsch/master/util/NetworkMonitorTest.kt
package com.voicedeutsch.master.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class NetworkMonitorTest {

    private lateinit var context: Context
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var network: Network
    private lateinit var networkCapabilities: NetworkCapabilities
    private lateinit var sut: NetworkMonitor

    @BeforeEach
    fun setUp() {
        context = mockk()
        connectivityManager = mockk()
        network = mockk()
        networkCapabilities = mockk()

        every {
            context.getSystemService(Context.CONNECTIVITY_SERVICE)
        } returns connectivityManager

        sut = NetworkMonitor(context)
    }

    // ── isOnline — no active network ─────────────────────────────────────────

    @Test
    fun isOnline_noActiveNetwork_returnsFalse() {
        every { connectivityManager.activeNetwork } returns null
        assertFalse(sut.isOnline())
    }

    // ── isOnline — no capabilities ────────────────────────────────────────────

    @Test
    fun isOnline_activeNetworkButNoCapabilities_returnsFalse() {
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns null
        assertFalse(sut.isOnline())
    }

    // ── isOnline — WiFi ───────────────────────────────────────────────────────

    @Test
    fun isOnline_wifiTransport_returnsTrue() {
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns true
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns false
        assertTrue(sut.isOnline())
    }

    // ── isOnline — cellular ───────────────────────────────────────────────────

    @Test
    fun isOnline_cellularTransport_returnsTrue() {
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns false
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns true
        assertTrue(sut.isOnline())
    }

    // ── isOnline — both transports ────────────────────────────────────────────

    @Test
    fun isOnline_bothWifiAndCellular_returnsTrue() {
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns true
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns true
        assertTrue(sut.isOnline())
    }

    // ── isOnline — no transports ──────────────────────────────────────────────

    @Test
    fun isOnline_noWifiAndNoCellular_returnsFalse() {
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns false
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns false
        assertFalse(sut.isOnline())
    }

    // ── repeated calls ────────────────────────────────────────────────────────

    @Test
    fun isOnline_calledTwiceWithWifi_returnsTrueBothTimes() {
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns true
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns false

        assertTrue(sut.isOnline())
        assertTrue(sut.isOnline())
    }

    @Test
    fun isOnline_calledTwiceWithNoNetwork_returnsFalseBothTimes() {
        every { connectivityManager.activeNetwork } returns null

        assertFalse(sut.isOnline())
        assertFalse(sut.isOnline())
    }

    // ── WiFi short-circuit — cellular is not checked ──────────────────────────

    @Test
    fun isOnline_wifiTrue_cellularNotEvaluated_returnsTrue() {
        // When WiFi is true, the OR short-circuits; cellular check is irrelevant
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns true
        // Deliberately do NOT stub TRANSPORT_CELLULAR — confirms WiFi is checked first
        assertTrue(sut.isOnline())
    }

    // ── construction ─────────────────────────────────────────────────────────

    @Test
    fun construction_withContext_doesNotThrow() {
        assertNotNull(NetworkMonitor(context))
    }
}
