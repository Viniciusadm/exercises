package com.archieapps.exercises

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.archieapps.exercises.core.data.PlanDraft
import com.archieapps.exercises.core.net.dto.PlanDayDto
import com.archieapps.exercises.core.net.dto.PlanExerciseDto
import com.archieapps.exercises.core.net.dto.PlanMuscleDto
import com.archieapps.exercises.core.net.dto.PlanMusclesDto
import com.archieapps.exercises.core.net.dto.PlanProposalDto
import com.archieapps.exercises.ui.design.ExercisesTheme
import com.archieapps.exercises.ui.feature.plan.PlanActions
import com.archieapps.exercises.ui.feature.plan.PlanScreen
import com.archieapps.exercises.ui.feature.plan.PlanStep
import com.archieapps.exercises.ui.feature.plan.PlanUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PlanReviewInstrumentedTest {
    @get:Rule
    val compose = createComposeRule()

    private fun item(ref: String, name: String, isNew: Boolean) = PlanExerciseDto(
        ref = ref,
        exerciseId = if (isNew) null else 1,
        name = name,
        isNew = isNew,
        muscles = PlanMusclesDto(primary = listOf(PlanMuscleDto("gluteo-medio", "Glúteo médio"))),
        targetSets = 3,
        targetReps = 10,
        restSeconds = 60,
    )

    private fun state(vararg items: PlanExerciseDto) = PlanUiState(
        step = PlanStep.REVIEW,
        draft = PlanDraft(
            proposal = PlanProposalDto(
                planUuid = "5f3ab1e6-2c4d-4f0a-9b6e-8d1c2a3b4c5d",
                source = "deterministic",
                daysPerWeek = 1,
                setsBudgetPerDay = 20,
                days = listOf(
                    PlanDayDto(
                        day = 1,
                        focus = "Pernas",
                        name = "Pernas · Dia 1",
                        totalSets = items.sumOf { it.targetSets },
                        estimatedMinutes = 30,
                        exercises = items.toList(),
                    )
                ),
            )
        ),
    )

    private fun actions(
        onSetSets: (Int, String, Int) -> Unit = { _, _, _ -> },
        onRemove: (Int, String) -> Unit = { _, _ -> },
        onConfirm: () -> Unit = {},
    ) = PlanActions(
        onBack = {},
        onSetObjective = {},
        onSetDays = {},
        onSetMinutes = {},
        onToggleEquipment = {},
        onClearEquipment = {},
        onGenerate = {},
        onBackToForm = {},
        onDiscard = {},
        onSetSets = onSetSets,
        onSetReps = { _, _, _ -> },
        onRemove = onRemove,
        onOpenSwap = { _, _ -> },
        onCloseSwap = {},
        onApplySwap = {},
        onConfirm = onConfirm,
        onOpenWorkouts = {},
        onPlanAnother = {},
        onDismissMessage = {},
    )

    @Test
    fun the_new_exercise_badge_shows_only_for_the_item_that_is_not_in_the_catalog() {
        compose.setContent {
            ExercisesTheme {
                PlanScreen(
                    state = state(
                        item("cat:1", "Cadeira abdutora", isNew = false),
                        item("canon:face-pull", "Face pull", isNew = true),
                    ),
                    actions = actions(),
                )
            }
        }

        compose.onNodeWithText("Cadeira abdutora").assertIsDisplayed()
        compose.onNodeWithText("Face pull").assertIsDisplayed()
        compose.onAllNodesWithText("novo no seu catálogo").assertCountEquals(1)
    }

    @Test
    fun confirming_is_blocked_while_the_plan_has_a_repeated_exercise() {
        compose.setContent {
            ExercisesTheme {
                PlanScreen(
                    state = state(
                        item("cat:1", "Cadeira abdutora", isNew = false),
                        item("cat:1", "Cadeira abdutora", isNew = false),
                    ),
                    actions = actions(),
                )
            }
        }

        compose.onNodeWithText("Criar treino").assertIsNotEnabled()
        compose.onNodeWithText("Há exercício repetido no plano.").assertIsDisplayed()
    }

    @Test
    fun a_healthy_plan_can_be_confirmed_and_reaches_the_callback() {
        var confirmed = 0

        compose.setContent {
            ExercisesTheme {
                PlanScreen(
                    state = state(item("cat:1", "Cadeira abdutora", isNew = false)),
                    actions = actions(onConfirm = { confirmed++ }),
                )
            }
        }

        compose.onNodeWithText("Criar treino").assertIsEnabled().performClick()

        assertEquals(1, confirmed)
    }

    @Test
    fun the_stepper_and_the_remove_button_actually_reach_their_callbacks() {
        val setsCalls = mutableListOf<Triple<Int, String, Int>>()
        val removals = mutableListOf<Pair<Int, String>>()

        compose.setContent {
            ExercisesTheme {
                PlanScreen(
                    state = state(item("cat:1", "Cadeira abdutora", isNew = false)),
                    actions = actions(
                        onSetSets = { day, ref, sets -> setsCalls += Triple(day, ref, sets) },
                        onRemove = { day, ref -> removals += day to ref },
                    ),
                )
            }
        }

        compose.onAllNodesWithText("+")[0].performClick()
        compose.onNodeWithText("Remover").performClick()

        assertEquals(listOf(Triple(1, "cat:1", 4)), setsCalls)
        assertEquals(listOf(1 to "cat:1"), removals)
    }
}
