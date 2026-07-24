# Navigation coherence fixes

## Problems fixed

### 1. UserProfileFragment sin entrada desde UI
- **File:** `app/src/main/java/com/example/audify/ui/detail/DetailFragment.kt`
- **Change:** Added `setOnClickListener` on `txtAuthor` to navigate to `userProfileFragment` with the podcast's `userId` as argument
- **Before:** `UserProfileFragment` was defined in the nav graph but no fragment navigated to it
- **After:** Tapping the author name in DetailFragment opens the user's profile showing their podcasts

### 2. Inconsistencia en botón "Atrás"
- **File:** `app/src/main/java/com/example/audify/ui/detail/DetailFragment.kt`
- **Change:** Replaced `requireActivity().onBackPressedDispatcher.onBackPressed()` with `Navigation.findNavController(requireView()).popBackStack()`
- **Before:** DetailFragment used the system back dispatcher while other fragments used `popBackStack()`
- **After:** All fragments consistently use `popBackStack()` for the back button

### 3. Auth redirect backstack pollution
- **Files:**
  - `app/src/main/java/com/example/audify/ui/upload/UploadFragment.kt` (6 occurrences)
  - `app/src/main/java/com/example/audify/ui/podcasts/PodcastsFragment.kt` (1 occurrence)
  - `app/src/main/java/com/example/audify/ui/favorites/FavoritesFragment.kt` (1 occurrence)
  - `app/src/main/java/com/example/audify/ui/profile/ProfileFragment.kt` (1 occurrence)
- **Change:** Added `requireActivity().finish()` after every `startActivity(Intent(requireContext(), LoginActivity::class.java))` when session is missing or expired
- **Before:** Pressing back from LoginActivity after an auth redirect returned to the same fragment that triggered the redirect, causing confusing loops
- **After:** MainActivity is finished when redirecting to login, so back from LoginActivity exits the app entirely

### 4. Bottom nav no respondía después de navegar a destinos fuera del menú (Subir, Listas, Borradores)
- **File:** `app/src/main/java/com/example/audify/MainActivity.kt`
- **Change:** Replaced `setupWithNavController` with a manual `OnDestinationChangedListener` + `OnItemSelectedListener`
  - Bottom nav listener now always pops back to start destination before navigating, ensuring clean backstack
  - Added `navigateToBottomNav()` helper for drawer items that correspond to bottom nav destinations (Inicio, Favoritos)
  - `OnDestinationChangedListener` properly syncs checked state, leaving no item selected when on a non-bottom-nav destination
- **Before:** `setupWithNavController` used `restoreState=true` which could show stale fragment state when returning from UploadFragment; also kept "Inicio" highlighted even when on UploadFragment
- **After:** Bottom nav always clears the backstack to start, navigates fresh (no state restore), and correctly shows no selection when on non-bottom-nav destinations

### Files modified
- `app/src/main/java/com/example/audify/MainActivity.kt`
- `app/src/main/java/com/example/audify/ui/detail/DetailFragment.kt`
- `app/src/main/java/com/example/audify/ui/upload/UploadFragment.kt`
- `app/src/main/java/com/example/audify/ui/podcasts/PodcastsFragment.kt`
- `app/src/main/java/com/example/audify/ui/favorites/FavoritesFragment.kt`
- `app/src/main/java/com/example/audify/ui/profile/ProfileFragment.kt`
