# Audify - Session Summary

## Overview
This document summarizes the changes made to connect the **Audify** Android app to Supabase, fix authentication flows, and display user data in the UI.

## Files Modified

### 1. `app/src/main/java/com/example/audify/SupabaseService.kt`
Central Supabase client/service. Changes:

- **`registerUser`** — Changed return type from `Result<Unit>` to `Result<String>` (returns the new user's UUID from `auth.users`). Gets the user ID via `currentUserOrNull()` after signup.
- **`createProfile(userId, name)`** — New function. Inserts a row into the `profiles` table after registration (stores the full name from "Nombre Completo").
- **`getCurrentUserEmail()`** — New function. Returns the authenticated user's email from the Supabase session.
- **`getProfile()`** — Rewritten. Returns `Profile` directly (never fails). Queries the `profiles` table; if no profile exists or name is empty, auto-creates one using the email prefix as the default name. All errors are caught and return a fallback `Profile` instead of propagating.
- **`updateProfileName(name)`** — New function. Updates the user's name in the `profiles` table.
- **`signOut()`** — New function. Calls `client.auth.signOut()` to properly disconnect the user.
- **`loginUser`** — Now calls `getProfile()` after successful login to ensure every user has a profile row on first login.
- **`Profile` data class** — Added to map the `profiles` table (id, name, avatar_url, created_at).

### 2. `app/src/main/java/com/example/audify/LoginActivity.kt`
- Added navigation to `MainActivity` on successful login (line 64-65) — previously only showed a toast.
- Other logic unchanged (validation, password toggle, forgot password placeholder).

### 3. `app/src/main/java/com/example/audify/RegisterActivity.kt`
- After successful registration, now calls `SupabaseService.createProfile(userId, name)` to save the user's full name from "Nombre Completo" to the `profiles` table (line 88).
- Navigates to `LoginActivity` after registration.

### 4. `app/src/main/java/com/example/audify/MainActivity.kt`
- **`loadDrawerUserData()`** — New method. Reads the drawer header views (`txtDrawerAvatar`, `txtDrawerNombre`, `txtDrawerCorreo`) and populates them with the real user's name and email from Supabase.
- **Sidebar logout** — Now calls `SupabaseService.signOut()` before navigating to `LoginActivity`.
- Loads data inside `lifecycleScope.launch` with try/catch fallback to email prefix.

### 5. `app/src/main/java/com/example/audify/ui/profile/ProfileFragment.kt`
- **`loadUserData()`** — Rewritten. Loads real user name + email from `SupabaseService.getProfile()` and `getCurrentUserEmail()` instead of hardcoded strings. Shows avatar initial from name.
- **Save button** — Now calls `SupabaseService.updateProfileName(name)` to persist name changes to the `profiles` table.
- **Logout button** — Now calls `SupabaseService.signOut()` before navigating to `LoginActivity`.

## Database / Supabase Configuration Needed

### RLS Policy for `profiles` INSERT
The `profiles` table has RLS enabled but was **missing an INSERT policy**, which caused `createProfile()` to fail silently. Add this in Supabase SQL Editor:

```sql
create policy "Usuarios insertan su perfil"
on profiles for insert
with check (auth.uid() = id);
```

### Disable Email Confirmation
Go to **Supabase Dashboard → Authentication → Settings → General** and toggle **"Confirm email" OFF** to allow registration without email verification.

## New Registration Flow
1. User fills "Nombre Completo", "Correo", "Contraseña", "Confirmar Contraseña"
2. `registerUser(email, password)` creates the user in `auth.users`
3. `createProfile(userId, name)` inserts into `profiles` with the full name
4. User is redirected to `LoginActivity`
5. After login, `loginUser()` calls `getProfile()` which ensures the profile exists
6. `MainActivity` and `ProfileFragment` display the user's name and email from the database

## Old Account Handling
For users who registered before the INSERT policy was added (no profile row exists):
- `getProfile()` auto-creates a profile with the email prefix as name on first login
- The user can edit their full name in the Profile screen and save it
