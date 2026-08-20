package com.archieapps.exercises.ui.nav

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.archieapps.exercises.core.di.AppContainer
import com.archieapps.exercises.core.rest.OpenSessionBus
import com.archieapps.exercises.ui.design.IronBackground
import com.archieapps.exercises.ui.feature.anatomy.AnatomyScreen
import com.archieapps.exercises.ui.feature.licenses.LicensesScreen
import com.archieapps.exercises.ui.feature.plan.PlanActions
import com.archieapps.exercises.ui.feature.plan.PlanScreen
import com.archieapps.exercises.ui.feature.plan.PlanViewModel
import com.archieapps.exercises.ui.feature.anatomy.AnatomyViewModel
import com.archieapps.exercises.ui.feature.home.HomeScreen
import com.archieapps.exercises.ui.feature.home.HomeViewModel
import com.archieapps.exercises.ui.feature.settings.SettingsScreen
import com.archieapps.exercises.ui.feature.settings.SettingsViewModel
import com.archieapps.exercises.ui.feature.catalog.CatalogScreen
import com.archieapps.exercises.ui.feature.catalog.CatalogViewModel
import com.archieapps.exercises.ui.feature.history.HistoryScreen
import com.archieapps.exercises.ui.feature.history.HistoryViewModel
import com.archieapps.exercises.ui.feature.progress.ProgressScreen
import com.archieapps.exercises.ui.feature.progress.ProgressViewModel
import com.archieapps.exercises.ui.feature.session.SessionActions
import com.archieapps.exercises.ui.feature.templates.TemplateEditorScreen
import com.archieapps.exercises.ui.feature.templates.TemplateEditorViewModel
import com.archieapps.exercises.ui.feature.workout.WorkoutEditorActions
import com.archieapps.exercises.ui.feature.workout.WorkoutEditorScreen
import com.archieapps.exercises.ui.feature.workout.WorkoutEditorViewModel
import com.archieapps.exercises.ui.feature.session.SessionRoute
import com.archieapps.exercises.ui.feature.session.SessionViewModel

@Composable
fun ExercisesNavHost(container: AppContainer, modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = TopLevelDestination.entries.any { it.route == currentRoute } || currentRoute == null

    LaunchedEffect(navController) {
        OpenSessionBus.requestedAt.collect { requestedAt ->
            if (requestedAt == null) return@collect

            if (navController.currentDestination?.route != Routes.SESSION) {
                navController.navigate(Routes.SESSION) { launchSingleTop = true }
            }

            OpenSessionBus.consume()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = IronBackground,
        bottomBar = {
            if (showBottomBar) {
                IronBottomBar(
                    current = currentRoute,
                    onSelect = { destination -> navController.switchTo(destination) },
                    onStartWorkout = { navController.navigate(Routes.SESSION) },
                )
            }
        },
    ) { padding ->
        val layoutDirection = LocalLayoutDirection.current
        val contentPadding = PaddingValues(
            start = padding.calculateStartPadding(layoutDirection),
            top = padding.calculateTopPadding(),
            end = padding.calculateEndPadding(layoutDirection),
            bottom = padding.calculateBottomPadding() + 16.dp,
        )

        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.HOME.route,
            enterTransition = { fadeIn(tween(180)) },
            exitTransition = { fadeOut(tween(140)) },
        ) {
            composable(TopLevelDestination.HOME.route) {
                val viewModel: HomeViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            HomeViewModel(
                                container.homeRepository,
                                container.authRepository,
                                container.anatomyRepository,
                                container.bodyGeometry,
                            )
                        }
                    }
                )
                val state by viewModel.state.collectAsStateWithLifecycle()

                LifecycleResumeEffect(Unit) {
                    viewModel.refresh()
                    onPauseOrDispose { }
                }

                HomeScreen(
                    state = state,
                    onRefresh = viewModel::refresh,
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onStartWorkout = { navController.navigate(Routes.SESSION) },
                    onOpenAnatomy = { navController.navigate(Routes.ANATOMY) },
                    contentPadding = contentPadding,
                )
            }

            composable(TopLevelDestination.HISTORY.route) {
                val viewModel: HistoryViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { HistoryViewModel(container.workoutRepository, container.templateRepository) }
                    }
                )
                val state by viewModel.state.collectAsStateWithLifecycle()

                LifecycleResumeEffect(Unit) {
                    viewModel.refresh()
                    onPauseOrDispose { }
                }

                HistoryScreen(
                    state = state,
                    onToggle = viewModel::toggleExpanded,
                    onLoadMore = viewModel::loadMore,
                    onCreateTemplate = viewModel::createTemplate,
                    onEdit = { workout -> navController.navigate(Routes.workout(workout.id)) },
                    onDelete = viewModel::delete,
                    onDismissMessage = viewModel::dismissMessage,
                    contentPadding = contentPadding,
                )
            }

            composable(TopLevelDestination.PROGRESS.route) {
                val viewModel: ProgressViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { ProgressViewModel(container.exerciseRepository, container.workoutRepository) }
                    }
                )
                val state by viewModel.state.collectAsStateWithLifecycle()

                ProgressScreen(
                    state = state,
                    onSelect = viewModel::select,
                    onOpenAnatomy = { navController.navigate(Routes.ANATOMY) },
                    contentPadding = contentPadding,
                )
            }

            composable(TopLevelDestination.CATALOG.route) {
                val viewModel: CatalogViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            CatalogViewModel(
                                container.exerciseRepository,
                                container.templateRepository,
                                container.anatomyRepository,
                            )
                        }
                    }
                )
                val state by viewModel.state.collectAsStateWithLifecycle()

                CatalogScreen(
                    state = state,
                    onQueryChange = viewModel::onQueryChange,
                    onMuscleFilter = viewModel::onMuscleFilter,
                    onShowTemplates = viewModel::showTemplates,
                    onCreate = viewModel::startCreate,
                    onEdit = viewModel::startEdit,
                    onFormChange = viewModel::updateForm,
                    onSubmitForm = viewModel::submitForm,
                    onCancelForm = viewModel::cancelForm,
                    onDeleteTemplate = viewModel::deleteTemplate,
                    onOpenTemplate = { id -> navController.navigate(Routes.template(id)) },
                    onPlanWorkout = { navController.navigate(Routes.PLAN) },
                    onDismissMessage = viewModel::dismissMessage,
                    contentPadding = contentPadding,
                )
            }

            composable(
                route = Routes.SESSION,
                enterTransition = { slideInVertically(tween(240)) { it / 3 } + fadeIn(tween(240)) },
                exitTransition = { slideOutVertically(tween(200)) { it / 3 } + fadeOut(tween(200)) },
            ) {
                val viewModel: SessionViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            SessionViewModel(
                                container.sessionRepository,
                                container.workoutRepository,
                                container.templateRepository,
                                container.exerciseRepository,
                                container.anatomyRepository,
                                container.bodyGeometry,
                                container.settings,
                                container.restTimer,
                            )
                        }
                    }
                )
                val state by viewModel.state.collectAsStateWithLifecycle()

                SessionRoute(
                    state = state,
                    actions = SessionActions(
                        onOpenExercise = viewModel::openExercise,
                        onCloseExercise = viewModel::closeExercise,
                        onAddExercise = viewModel::addExercise,
                        onRemoveExercise = viewModel::removeExercise,
                        onAddSet = viewModel::addSet,
                        onRemoveSet = viewModel::removeSet,
                        onChangeReps = viewModel::changeReps,
                        onChangeWeight = viewModel::changeWeight,
                        onSetWeight = viewModel::setWeight,
                        onChangeDuration = viewModel::changeDuration,
                        onToggleSet = viewModel::toggleSetDone,
                        onAddRest = viewModel::addRest,
                        onSkipRest = viewModel::skipRest,
                        onFinish = viewModel::finish,
                        onDiscard = {
                            viewModel.discard()
                            navController.popBackStack()
                        },
                        onDismissMessage = viewModel::dismissMessage,
                    ),
                    onClose = { navController.popBackStack() },
                    onRepeatLast = viewModel::repeatLastWorkout,
                    onStartTemplate = viewModel::startFromTemplate,
                    onRepeatWorkout = viewModel::repeatWorkout,
                    onStartEmpty = viewModel::startEmpty,
                    onManageTemplates = { navController.navigate(Routes.template(null)) },
                    onPlanWorkout = { navController.navigate(Routes.PLAN) },
                    onDone = {
                        viewModel.acknowledgeOutcome()
                        navController.popBackStack()
                    },
                    contentPadding = contentPadding,
                )
            }

            composable(
                route = Routes.WORKOUT_PATTERN,
                arguments = listOf(navArgument("id") { type = NavType.IntType }),
            ) { entry ->
                val workoutId = entry.arguments?.getInt("id") ?: 0
                val viewModel: WorkoutEditorViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            WorkoutEditorViewModel(
                                workoutId,
                                container.workoutRepository,
                                container.exerciseRepository,
                            )
                        }
                    }
                )
                val state by viewModel.state.collectAsStateWithLifecycle()

                LaunchedEffect(state.closed) {
                    if (state.closed) navController.popBackStack()
                }

                WorkoutEditorScreen(
                    state = state,
                    actions = WorkoutEditorActions(
                        onNameChange = viewModel::setName,
                        onNotesChange = viewModel::setNotes,
                        onShiftDate = viewModel::shiftDate,
                        onChangeDuration = viewModel::changeDuration,
                        onAddExercise = viewModel::addExercise,
                        onRemoveExercise = viewModel::removeExercise,
                        onAddSet = viewModel::addSet,
                        onRemoveSet = viewModel::removeSet,
                        onChangeReps = viewModel::changeReps,
                        onChangeWeight = viewModel::changeWeight,
                        onSetWeight = viewModel::setWeight,
                        onChangeDurationSeconds = viewModel::changeDurationSeconds,
                        onToggleSet = viewModel::toggleSet,
                        onSave = viewModel::save,
                        onDelete = viewModel::delete,
                        onDismissMessage = viewModel::dismissMessage,
                    ),
                    onClose = { navController.popBackStack() },
                    contentPadding = contentPadding,
                )
            }

            composable(
                route = Routes.TEMPLATE_PATTERN,
                arguments = listOf(navArgument("id") { type = NavType.IntType }),
            ) { entry ->
                val templateId = entry.arguments?.getInt("id")?.takeIf { it > 0 }
                val viewModel: TemplateEditorViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            TemplateEditorViewModel(
                                templateId,
                                container.templateRepository,
                                container.exerciseRepository,
                            )
                        }
                    }
                )
                val state by viewModel.state.collectAsStateWithLifecycle()

                LaunchedEffect(state.closed) {
                    if (state.closed) navController.popBackStack()
                }

                TemplateEditorScreen(
                    state = state,
                    onNameChange = viewModel::setName,
                    onAddExercise = viewModel::addExercise,
                    onRemoveItem = viewModel::removeItem,
                    onMoveItem = viewModel::moveItem,
                    onUpdateItem = viewModel::updateItem,
                    onSave = viewModel::save,
                    onDelete = viewModel::delete,
                    onDismissMessage = viewModel::dismissMessage,
                    contentPadding = contentPadding,
                )
            }

            composable(Routes.ANATOMY) {
                val viewModel: AnatomyViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { AnatomyViewModel(container.anatomyRepository, container.bodyGeometry) }
                    }
                )
                val state by viewModel.state.collectAsStateWithLifecycle()

                AnatomyScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onSelectRegion = viewModel::selectRegion,
                    onSelectView = viewModel::selectView,
                    onSelectRegionId = viewModel::selectRegionId,
                    onOpenSuggestions = viewModel::openSuggestions,
                    onAdopt = viewModel::adopt,
                    onDismissMessage = viewModel::dismissMessage,
                    contentPadding = contentPadding,
                )
            }

            composable(Routes.PLAN) {
                val viewModel: PlanViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            PlanViewModel(
                                container.planRepository,
                                container.templateRepository,
                                container.exerciseRepository,
                                container.anatomyRepository,
                                container.sessionRepository,
                                container.settings,
                            )
                        }
                    }
                )
                val state by viewModel.state.collectAsStateWithLifecycle()

                PlanScreen(
                    state = state,
                    actions = PlanActions(
                        onBack = { navController.popBackStack() },
                        onSetObjective = viewModel::setObjective,
                        onSetDays = viewModel::setDaysPerWeek,
                        onSetMinutes = viewModel::setMinutesPerDay,
                        onToggleEquipment = viewModel::toggleEquipment,
                        onClearEquipment = viewModel::clearEquipment,
                        onGenerate = viewModel::generate,
                        onBackToForm = viewModel::backToForm,
                        onDiscard = viewModel::discard,
                        onSetSets = viewModel::setSets,
                        onSetReps = viewModel::setReps,
                        onRemove = viewModel::remove,
                        onOpenSwap = viewModel::openSwap,
                        onCloseSwap = viewModel::closeSwap,
                        onApplySwap = viewModel::applySwap,
                        onConfirm = viewModel::confirm,
                        onOpenWorkouts = {
                            navController.popBackStack()
                            navController.navigate(Routes.SESSION)
                        },
                        onPlanAnother = viewModel::reset,
                        onDismissMessage = viewModel::dismissMessage,
                    ),
                    contentPadding = contentPadding,
                )
            }

            composable(Routes.LICENSES) {
                LicensesScreen(
                    onBack = { navController.popBackStack() },
                    contentPadding = contentPadding,
                )
            }

            composable(Routes.SETTINGS) {
                val viewModel: SettingsViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            SettingsViewModel(
                                container.settings,
                                container.exerciseRepository,
                                container.sessionRepository,
                                container.authRepository,
                            )
                        }
                    }
                )
                val state by viewModel.state.collectAsStateWithLifecycle()

                SettingsScreen(
                    state = state,
                    onSyncCatalog = viewModel::syncCatalog,
                    onFlushPending = viewModel::flushPending,
                    onRestChange = viewModel::setDefaultRest,
                    onKeepScreenOnChange = viewModel::setKeepScreenOn,
                    onLogout = viewModel::logout,
                    onClose = { navController.popBackStack() },
                    onOpenLicenses = { navController.navigate(Routes.LICENSES) },
                    contentPadding = contentPadding,
                )
            }
        }
    }
}

private fun NavHostController.switchTo(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
