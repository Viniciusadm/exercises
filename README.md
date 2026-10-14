# Exercises

Exercises is an Android workout tracker: live sessions with sets, timers and pace, workout history, templates, generated workout plans, per-exercise progress and a muscle map. This repository holds the Android app. It is a client of Manage API, a private Laravel backend that is not published; this README describes the part of its contract the app consumes.

| Part | What it is |
| --- | --- |
| [`app`](app) | The Android app in Kotlin and Jetpack Compose, with Retrofit for the API and Room for the local cache. |
| [`tools/anatomy`](tools/anatomy) | One-off script that builds the body regions asset from [vulovix/body-muscles](https://github.com/vulovix/body-muscles). Attribution is in [`NOTICE`](NOTICE). |

Other clients of the same backend: [`calendar`](https://github.com/Viniciusadm/calendar), [`files`](https://github.com/Viniciusadm/files), [`oklo`](https://github.com/Viniciusadm/oklo) and [`finances`](https://github.com/Viniciusadm/finances).

## Requirements

- JDK 11+
- Android SDK with `compileSdk` 36; the app runs on Android 14+ (`minSdk` 34)
- Android Gradle Plugin 9.3.1 and Kotlin 2.2.10 (from [`gradle/libs.versions.toml`](gradle/libs.versions.toml))

## Building

The backend address is read at build time from `env.properties`, which is not versioned. Copy the example and set `BASE_URL`; without it the build points to `http://10.0.2.2:8001/`, the host machine seen from the emulator:

```bash
cp env.properties.example env.properties
```

Build and install the debug app:

```bash
./gradlew installDebug
```

The release build is signed with the local debug keystore (`~/.android/debug.keystore`) for sideloading.

## API contract

Every request sends `Accept: application/json`. Responses come in an envelope with `success`, `message`, `body`, `error`, `misc` and, on lists, `pagination` (`limit`, `current`, `last`, `count`, `total`).

Authentication: `POST api/auth/login` with `email` and `password` returns a token, sent afterwards as `Authorization: Bearer <token>`. A 401 or 403 sends the user back to login.

Access code: the backend guards some writes with a daily access code typed by the user. When a write is refused with a 400 that carries the gate message, the app asks for the 8-digit code on a shuffled keypad, checks it with `POST api/user/test` (`{"code": "..."}`) and sends it in the `code` header on the following requests. The app never derives the code.

Endpoints used:

| Area | Endpoints |
| --- | --- |
| Workouts | `GET api/workouts`, `GET api/workouts/{id}`, `POST api/workouts`, `PUT api/workouts/{id}`, `DELETE api/workouts/{id}`, `GET api/workouts/{id}/timeline`, `POST api/workouts/{id}/adjustments`, `GET api/workouts/stats`, `GET api/workouts/calendar`, `GET api/workout-exercises/latest` |
| Exercises | `GET api/exercises`, `GET api/exercises/meta`, `POST api/exercises`, `PUT api/exercises/{id}`, `DELETE api/exercises/{id}`, `GET api/exercises/{id}/history`, `GET api/exercises/{id}/muscles`, `PUT api/exercises/{id}/muscles` |
| Muscles | `GET api/muscles`, `GET api/muscles/heatmap`, `GET api/muscles/analysis`, `GET api/muscles/recommendations`, `POST api/muscles/recommendations/adopt` |
| Plans | `GET api/workout-plans/options`, `POST api/workout-plans`, `POST api/workout-plans/confirm` |
| Templates | `GET api/workout-templates`, `POST api/workout-templates`, `PUT api/workout-templates/{id}`, `DELETE api/workout-templates/{id}`, `POST api/workout-templates/from-workout/{id}` |

The request and response shapes are the DTOs in [`app/src/main/java/com/archieapps/exercises/core/net`](app/src/main/java/com/archieapps/exercises/core/net).

## Tests

```bash
./gradlew test
```

The unit tests cover the API client and its contract against recorded responses, the anatomy asset and geometry, workout plans and the derivations behind the home, progress and weight screens.
