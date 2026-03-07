// Путь: src/test/java/com/voicedeutsch/master/presentation/navigation/NavRoutesTest.kt
package com.voicedeutsch.master.presentation.navigation

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class NavRoutesTest {

    // ── Individual route type checks ──────────────────────────────────────

    @Test
    fun onboarding_isNavRoute() {
        val route: NavRoute = NavRoute.Onboarding
        assertTrue(route is NavRoute.Onboarding)
    }

    @Test
    fun dashboard_isNavRoute() {
        val route: NavRoute = NavRoute.Dashboard
        assertTrue(route is NavRoute.Dashboard)
    }

    @Test
    fun session_isNavRoute() {
        val route: NavRoute = NavRoute.Session
        assertTrue(route is NavRoute.Session)
    }

    @Test
    fun knowledgeMap_isNavRoute() {
        val route: NavRoute = NavRoute.KnowledgeMap
        assertTrue(route is NavRoute.KnowledgeMap)
    }

    @Test
    fun book_isNavRoute() {
        val route: NavRoute = NavRoute.Book
        assertTrue(route is NavRoute.Book)
    }

    @Test
    fun settings_isNavRoute() {
        val route: NavRoute = NavRoute.Settings
        assertTrue(route is NavRoute.Settings)
    }

    @Test
    fun statistics_isNavRoute() {
        val route: NavRoute = NavRoute.Statistics
        assertTrue(route is NavRoute.Statistics)
    }

    @Test
    fun bookManager_isNavRoute() {
        val route: NavRoute = NavRoute.BookManager
        assertTrue(route is NavRoute.BookManager)
    }

    @Test
    fun runtimeTests_isNavRoute() {
        val route: NavRoute = NavRoute.RuntimeTests
        assertTrue(route is NavRoute.RuntimeTests)
    }

    @Test
    fun comprehensiveTests_isNavRoute() {
        val route: NavRoute = NavRoute.ComprehensiveTests
        assertTrue(route is NavRoute.ComprehensiveTests)
    }

    // ── Singleton identity ─────────────────────────────────────────────────

    @Test
    fun onboarding_sameInstance_sameObject() {
        assertSame(NavRoute.Onboarding, NavRoute.Onboarding)
    }

    @Test
    fun dashboard_sameInstance_sameObject() {
        assertSame(NavRoute.Dashboard, NavRoute.Dashboard)
    }

    @Test
    fun session_sameInstance_sameObject() {
        assertSame(NavRoute.Session, NavRoute.Session)
    }

    @Test
    fun settings_sameInstance_sameObject() {
        assertSame(NavRoute.Settings, NavRoute.Settings)
    }

    // ── All routes are distinct ────────────────────────────────────────────

    @Test
    fun allRoutes_totalCount_isTen() {
        val routes = allRoutes()
        assertEquals(10, routes.size)
    }

    @Test
    fun allRoutes_allDistinct_noDuplicates() {
        val routes = allRoutes()
        assertEquals(routes.size, routes.distinct().size)
    }

    @Test
    fun onboarding_notEqualsDashboard() {
        assertNotEquals(NavRoute.Onboarding, NavRoute.Dashboard)
    }

    @Test
    fun session_notEqualsKnowledgeMap() {
        assertNotEquals(NavRoute.Session, NavRoute.KnowledgeMap)
    }

    @Test
    fun book_notEqualsBookManager() {
        assertNotEquals(NavRoute.Book, NavRoute.BookManager)
    }

    @Test
    fun runtimeTests_notEqualsComprehensiveTests() {
        assertNotEquals(NavRoute.RuntimeTests, NavRoute.ComprehensiveTests)
    }

    @Test
    fun statistics_notEqualsSettings() {
        assertNotEquals(NavRoute.Statistics, NavRoute.Settings)
    }

    // ── when exhaustive matching ───────────────────────────────────────────

    @Test
    fun whenExhaustive_onboarding_returnsCorrectName() {
        val name = routeName(NavRoute.Onboarding)
        assertEquals("Onboarding", name)
    }

    @Test
    fun whenExhaustive_dashboard_returnsCorrectName() {
        val name = routeName(NavRoute.Dashboard)
        assertEquals("Dashboard", name)
    }

    @Test
    fun whenExhaustive_session_returnsCorrectName() {
        val name = routeName(NavRoute.Session)
        assertEquals("Session", name)
    }

    @Test
    fun whenExhaustive_knowledgeMap_returnsCorrectName() {
        val name = routeName(NavRoute.KnowledgeMap)
        assertEquals("KnowledgeMap", name)
    }

    @Test
    fun whenExhaustive_book_returnsCorrectName() {
        val name = routeName(NavRoute.Book)
        assertEquals("Book", name)
    }

    @Test
    fun whenExhaustive_settings_returnsCorrectName() {
        val name = routeName(NavRoute.Settings)
        assertEquals("Settings", name)
    }

    @Test
    fun whenExhaustive_statistics_returnsCorrectName() {
        val name = routeName(NavRoute.Statistics)
        assertEquals("Statistics", name)
    }

    @Test
    fun whenExhaustive_bookManager_returnsCorrectName() {
        val name = routeName(NavRoute.BookManager)
        assertEquals("BookManager", name)
    }

    @Test
    fun whenExhaustive_runtimeTests_returnsCorrectName() {
        val name = routeName(NavRoute.RuntimeTests)
        assertEquals("RuntimeTests", name)
    }

    @Test
    fun whenExhaustive_comprehensiveTests_returnsCorrectName() {
        val name = routeName(NavRoute.ComprehensiveTests)
        assertEquals("ComprehensiveTests", name)
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private fun allRoutes(): List<NavRoute> = listOf(
        NavRoute.Onboarding,
        NavRoute.Dashboard,
        NavRoute.Session,
        NavRoute.KnowledgeMap,
        NavRoute.Book,
        NavRoute.Settings,
        NavRoute.Statistics,
        NavRoute.BookManager,
        NavRoute.RuntimeTests,
        NavRoute.ComprehensiveTests,
    )

    private fun routeName(route: NavRoute): String = when (route) {
        is NavRoute.Onboarding -> "Onboarding"
        is NavRoute.Dashboard -> "Dashboard"
        is NavRoute.Session -> "Session"
        is NavRoute.KnowledgeMap -> "KnowledgeMap"
        is NavRoute.Book -> "Book"
        is NavRoute.Settings -> "Settings"
        is NavRoute.Statistics -> "Statistics"
        is NavRoute.BookManager -> "BookManager"
        is NavRoute.RuntimeTests -> "RuntimeTests"
        is NavRoute.ComprehensiveTests -> "ComprehensiveTests"
    }
}
