# MVVM Architecture — Separation of UI and Data/Sensor Logic

## Summary
Introduced a basic MVVM pattern across all Fragments to achieve clear separation between UI (XML layouts + ViewBinding) and data/sensor logic. Fragments now only handle view binding and user interaction; all state management, data loading, and business logic live in ViewModel classes.

## Changes

### New dependency
- Added `androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.0` (`libs.lifecycle.viewmodel.ktx`) in `app/build.gradle.kts` and `gradle/libs.versions.toml`.

### New package: `viewmodel/`
9 ViewModel classes, one per Fragment:

| ViewModel | State class | Responsibility |
|---|---|---|
| `InicioViewModel` | `InicioUiState` | Podcast loading, search/filter, favorites |
| `PodcastsViewModel` | `PodcastsUiState` | User podcasts, status/category filters, profile name |
| `FavoritesViewModel` | `FavoritesUiState` | Favorites loading, search |
| `DetailViewModel` | `DetailUiState` | Podcast loading, favorite toggle, audio URL resolution |
| `ListsViewModel` | `ListsUiState` | Playlist CRUD, podcast loading |
| `ProfileViewModel` | `ProfileUiState` | Profile load/update, sign out |
| `UserProfileViewModel` | `UserProfileUiState` | User profile + podcasts with category filter |
| `UploadViewModel` | `UploadUiState` | Category loading, publish flow (audio upload, cover upload, DB insert) |
| `DraftsViewModel` | `DraftsUiState` | Drafts load/delete |

### Refactored: `PodcastAdapter`
- Removed all `SupabaseService` calls from the adapter (was calling `isFavorited`, `addFavorite`, `removeFavorite` internally)
- Added `favoriteIds: Set<String>` constructor parameter and `updateFavoriteIds(Set<String>)` method
- Favorite toggle now delegates to `onFavoriteClick` callback (handled by ViewModel)

### Refactored: All 9 Fragments
- Now use `private val viewModel: XxxViewModel by viewModels()` from `fragment-ktx`
- State observed via `flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)`
- All `SupabaseService` calls replaced by ViewModel method calls
- UI logic (loading visibility, text binding, adapter creation) driven by state changes
- `onFavoriteClick` on adapters delegated to `viewModel.toggleFavorite()` where relevant

### Architecture pattern
```
XML Layouts → Fragment (View binding + event forwarding)
                  ↓
            ViewModel (StateFlow + business logic)
                  ↓
            SupabaseService (data layer, unchanged)
```

- Fragments still handle: view inflation, ViewBinding, `onClick` listeners, navigation, dialogs/toasts
- ViewModels handle: data loading, filtering, state management, favorite toggling, publish flows
- Sensors (`ProximitySensorManager`, `ShakeDetector`) remain in `AudioForegroundService` (unchanged — already cleanly separated)
- No DI, no repository layer, no use cases — kept minimal per project conventions
