package com.example.ui.navigation

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object LogFood : Screen("log_food")
    data object Diary : Screen("diary")
    data object Stats : Screen("stats")
    data object Profile : Screen("profile")
    data object Weight : Screen("weight")
    data object Activity : Screen("activity")
    data object RecipesAndPlans : Screen("recipes_and_plans")

    data object AiAssistant : Screen("ai_assistant")
    data object ManualEntry : Screen("manual_entry")
    data object AnalysisReview : Screen("analysis_review")
}

