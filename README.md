# CriminalIntent — Investigation Bureau

A Kotlin Android app with a dark navy case-file interface, amber controls, green solved labels, and photo evidence. Uses Fragments, RecyclerView, a ViewModel, Kotlin coroutines, and Room. All data stays on the device.

## Open in Android Studio

1. Extract the ZIP, then choose **Open** in Android Studio and select the `CriminalIntent` folder (the folder containing `settings.gradle.kts`).
2. Allow Gradle sync and dependency downloads. This project uses Android Gradle Plugin 8.9.2, Kotlin 2.0.21, Gradle 8.11.1, and SDK 35.
3. In Settings > Build, Execution, Deployment > Build Tools > Gradle, select Android Studio's bundled JDK (Java 17 or newer).
4. Install Android SDK Platform 35 if prompted. Use an emulator or phone running Android 8.0/API 26 or newer.
5. Select the `app` configuration and click Run. For camera testing, configure an emulator camera in Device Manager or use a physical phone.

A Gradle wrapper is included. Android Studio creates your machine-specific `local.properties`; do not commit that file. On macOS/Linux, run `chmod +x gradlew` before using the wrapper in a terminal.

## Using the app

- Tap **+ New case** or the app-bar **New case** action.
- Enter a title and field notes. Changes save automatically.
- Tap the date button to open a date-picker DialogFragment.
- Mark **Case solved** to show a green SOLVED label in the list.
- Tap **Capture evidence**, take a photo, and confirm it in the camera app.
- Tap **Share case report** to send a text report to another app through an implicit ACTION_SEND Intent. Photos stay local and are not included in the report.
- Tap **Delete case**, then confirm. The record and its current photo are removed.
- Use Android Back or the toolbar arrow to return to the list.

## Requirement map

| Assignment requirement | Implementation |
| --- | --- |
| Scrollable list with title and date | CrimeListFragment + RecyclerView + ListAdapter |
| Separate editable detail screen | CrimeDetailFragment, Fragment back stack |
| Add and delete | List button and app-bar menu; confirmed deletion |
| Persistence across restart | Room CaseDatabase, CrimeDao, case-files.db |
| Solved state visible in list | OPEN amber / SOLVED green status labels |
| Editable date | DateDialog, a DatePickerDialog inside DialogFragment |
| Device camera photo | TakePicture Activity Result contract + FileProvider |
| Implicit Intent | ACTION_SEND report with Android chooser |
| Rotation handling | Activity ViewModel, Room Flow, restored Fragment arguments and pending camera filename |

## Source guide

`app/src/main/java/com/example/criminalintent/CrimeStore.kt` contains the Room entity, DAO, database, application, and ViewModel. Database writes run in order on an application-owned background queue so navigation and rotation do not cancel pending saves.

`app/src/main/java/com/example/criminalintent/MainActivity.kt` contains the host Activity, list Fragment, RecyclerView adapter, detail Fragment, and date DialogFragment. Views are built in Kotlin to keep the example compact; theme values and FileProvider paths are XML resources.

`app/src/main/AndroidManifest.xml` registers the Activity, application, optional camera feature, and private FileProvider. Delegating capture to an installed camera app does not require a CAMERA permission. Images are sampled when decoded to reduce memory use. The app handles system-bar and keyboard insets.

## Test before submission

The project was generated and its configuration/source reviewed, but **has not been compiled or run here**. Local build tools could not initialize, SDK access was unavailable, and external downloads encountered a TLS connection error. Gradle sync, emulator testing, and camera behavior still need verification in Android Studio.

1. Add three cases with different titles/dates. Check list scrolling and tap each case.
2. Edit title/notes, return to list, and reopen. Check saved text.
3. Mark a case solved and confirm its green label. Unmark it and confirm OPEN.
4. Change a date. Rotate with the date dialog open and complete a selection.
5. Rotate the list and detail screens, including while editing text. Verify no crash or lost saved data.
6. Capture a photo. Rotate, navigate away and back, and check the photo. Cancel another capture and verify the prior photo remains.
7. Rotate while the camera is open; complete capture and verify attachment.
8. Force-stop the app after saves finish and reopen. Verify title, date, notes, solved state, and photo.
9. Share a report to an installed compatible app. On an emulator without a sharing app, check that absence is handled.
10. Delete a case, verify it disappears, then restart and verify it remains deleted.
11. Check a small-screen emulator and large font settings. The detail screen scrolls.

## Submit your own repository

Create a repository, commit the project, and push the branch you want graded. Example terminal commands from this folder:

```sh
git init
git switch -c assignment/criminal-intent
git add .
git commit -m "Build investigation-themed CriminalIntent"
git remote add origin YOUR_REPOSITORY_URL
git push -u origin assignment/criminal-intent
```

Submit the specific branch URL (for example, `https://github.com/YOUR_NAME/YOUR_REPO/tree/assignment/criminal-intent`) through the course portal. Each student submits individually. Do not make commits after submitting your final link.

## References

- Android Fragments: https://developer.android.com/guide/fragments
- Room: https://developer.android.com/training/data-storage/room
- Activity results: https://developer.android.com/training/basics/intents/result
- FileProvider: https://developer.android.com/reference/androidx/core/content/FileProvider
