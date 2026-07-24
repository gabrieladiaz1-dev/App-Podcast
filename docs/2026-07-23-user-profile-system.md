# 2026-07-23 — User Profile System with Username

## What changed

### SupabaseService.kt
- Added `username` field to `Profile` data class
- `createProfile()` now accepts a `username` parameter
- `getProfile()` includes username in self-healing logic (auto-creates/updates if missing)
- Added `isUsernameAvailable(username)` — checks uniqueness against existing profiles (excludes current user)
- Added `updateUsername(username)` — updates username for current user
- Added `getProfileByUserId(userId)` — fetches any user's profile by ID (for public profile view)

### RegisterActivity.kt + activity_register.xml
- Added username field (`etUsername`) between name and email fields
- Username validation: required, min 3 chars, only letters/digits/dots/underscores
- Username uniqueness check before registration via `isUsernameAvailable()`
- Confirmation dialog unchanged

### ProfileFragment.kt + fragment_profile.xml
- Added editable username field (`edtUsername`) above the name field
- Username validation on save (same rules as registration)
- Username uniqueness check if changed
- Name and username save independently via `updateProfileName()` + `updateUsername()`

### UserProfileFragment.kt + fragment_user_profile.xml (NEW)
- Public profile view for any user by userId
- Shows avatar, display name, @username, podcast count, categories
- Back button to navigate back
- Currently shows empty state (podcast loading not yet wired to Supabase)

### nav_graph.xml
- Added `userProfileFragment` destination with `userId` string argument

### strings.xml
- Added profile-related strings for username validation messages and user profile

## Supabase table requirements
The `profiles` table must have a `username` column (text, unique). Run this migration if not already present:
```sql
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS username TEXT DEFAULT '';
CREATE UNIQUE INDEX IF NOT EXISTS profiles_username_unique ON profiles (username) WHERE username != '';
```

## Notes
- `getProfileByUserId()` is for viewing other users' profiles
- `UserProfileFragment` is registered in nav graph but not yet navigated to from any screen (can be wired from podcast author tap, etc.)
