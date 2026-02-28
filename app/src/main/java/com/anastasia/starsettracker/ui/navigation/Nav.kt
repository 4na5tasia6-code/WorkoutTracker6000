package com.anastasia.starsettracker.ui.navigation

sealed class Tab(val route: String, val label: String) {
    data object Today : Tab("today", "Today")
    data object Stats : Tab("stats", "Stats")
    data object Machines : Tab("machines", "Machines")
}
