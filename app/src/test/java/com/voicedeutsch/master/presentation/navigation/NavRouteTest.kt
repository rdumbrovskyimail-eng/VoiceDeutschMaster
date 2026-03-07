package com.voicedeutsch.master.presentation.navigation

import org.junit.Assert.*
import org.junit.Test

class NavRouteTest {

    @Test
    fun `Onboarding is NavRoute`() {
        val route: NavRoute = NavRoute.Onboarding
        assertTrue(route is NavRoute.Onboarding)
    }

    @Test
    fun `Dashboard is NavRoute`() {
        val route: NavRoute = NavRoute.Dashboard
        assertTrue(route is NavRoute.Dashboard)
    }

    @Test
    fun `Session is NavRoute`() {
        val route: NavRoute = NavRoute.Session
        assertTrue(route is NavRoute.Session)
    }

    @Test
    fun `KnowledgeMap is NavRoute`() {
        val route: NavRoute = NavRoute.KnowledgeMap
        assertTrue(route is NavRoute.KnowledgeMap)
    }

    @Test
    fun `Book is NavRoute`() {
        val route: NavRoute = NavRoute.Book
        assertTrue(route is NavRoute.Book)
    }

    @Test
    fun `Settings is NavRoute`() {
        val route: NavRoute = NavRoute.Settings
        assertTrue(route is NavRoute.Settings)
    }

    @Test
    fun `Statistics is NavRoute`() {
        val route: NavRoute = NavRoute.Statistics
        assertTrue(route is NavRoute.Statistics)
    }

    @Test
    fun `BookManager is NavRoute`() {
        val route: NavRoute = NavRoute.BookManager
        assertTrue(route is NavRoute.BookManager)
    }

    @Test
    fun `RuntimeTests is NavRoute`() {
        val route: NavRoute = NavRoute.RuntimeTests
        assertTrue(route is NavRoute.RuntimeTests)
    }

    @Test
    fun `ComprehensiveTests is NavRoute`() {
        val route: NavRoute = NavRoute.ComprehensiveTests
        assertTrue(route is NavRoute.ComprehensiveTests)
    }

    @Test
    fun `all routes are distinct objects`() {
        val routes = listOf(
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
        assertEquals(10, routes.toSet().size)
    }

    @Test
    fun `Session is not Dashboard`() {
        assertNotEquals(NavRoute.Session, NavRoute.Dashboard)
    }
}
