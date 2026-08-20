package com.archieapps.exercises.ui.nav

import com.archieapps.exercises.R

enum class TopLevelDestination(
    val route: String,
    val label: String,
    val iconRes: Int,
) {
    HOME("home", "Hoje", R.drawable.ic_home),
    HISTORY("history", "Histórico", R.drawable.ic_calendar),
    PROGRESS("progress", "Progresso", R.drawable.ic_chart),
    CATALOG("catalog", "Exercícios", R.drawable.ic_list),
}

object Routes {
    const val LOGIN = "login"
    const val SETTINGS = "settings"
    const val SESSION = "session"
    const val ANATOMY = "anatomy"
    const val PLAN = "plan"
    const val LICENSES = "licenses"
    const val TEMPLATE_PATTERN = "template/{id}"
    const val WORKOUT_PATTERN = "workout/{id}/edit"

    fun workout(id: Int) = "workout/$id/edit"

    fun template(id: Int?) = "template/${id ?: 0}"
}
