# Supabase: crear tablas de listas y politicas RLS

Fecha: 2026-07-25

## Problema
Error en app/Supabase:
- `could not find table public.playlists`

## Causa
Las tablas de listas no existen en el esquema `public`.

## Solucion (ejecutar en Supabase SQL Editor)

```sql
-- Extensión necesaria para UUIDs
create extension if not exists pgcrypto;

-- =========================
-- Tabla playlists
-- =========================
create table if not exists public.playlists (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  name text not null,
  created_at timestamptz not null default now()
);

-- =========================
-- Tabla playlist_items
-- =========================
create table if not exists public.playlist_items (
  id uuid primary key default gen_random_uuid(),
  playlist_id uuid not null references public.playlists(id) on delete cascade,
  podcast_id text not null,
  created_at timestamptz not null default now(),
  unique (playlist_id, podcast_id)
);

-- Índices recomendados
create index if not exists idx_playlists_user_id on public.playlists(user_id);
create index if not exists idx_playlist_items_playlist_id on public.playlist_items(playlist_id);
create index if not exists idx_playlist_items_podcast_id on public.playlist_items(podcast_id);

-- =========================
-- RLS
-- =========================
alter table public.playlists enable row level security;
alter table public.playlist_items enable row level security;

-- Limpiar políticas previas (si existen)
drop policy if exists "playlists_select_own" on public.playlists;
drop policy if exists "playlists_insert_own" on public.playlists;
drop policy if exists "playlists_delete_own" on public.playlists;
drop policy if exists "playlists_update_own" on public.playlists;

drop policy if exists "playlist_items_select_own" on public.playlist_items;
drop policy if exists "playlist_items_insert_own" on public.playlist_items;
drop policy if exists "playlist_items_delete_own" on public.playlist_items;

-- Políticas para playlists
create policy "playlists_select_own"
on public.playlists for select
to authenticated
using (auth.uid() = user_id);

create policy "playlists_insert_own"
on public.playlists for insert
to authenticated
with check (auth.uid() = user_id);

create policy "playlists_delete_own"
on public.playlists for delete
to authenticated
using (auth.uid() = user_id);

create policy "playlists_update_own"
on public.playlists for update
to authenticated
using (auth.uid() = user_id)
with check (auth.uid() = user_id);

-- Políticas para playlist_items
create policy "playlist_items_select_own"
on public.playlist_items for select
to authenticated
using (
  exists (
    select 1
    from public.playlists p
    where p.id = playlist_items.playlist_id
      and p.user_id = auth.uid()
  )
);

create policy "playlist_items_insert_own"
on public.playlist_items for insert
to authenticated
with check (
  exists (
    select 1
    from public.playlists p
    where p.id = playlist_items.playlist_id
      and p.user_id = auth.uid()
  )
);

create policy "playlist_items_delete_own"
on public.playlist_items for delete
to authenticated
using (
  exists (
    select 1
    from public.playlists p
    where p.id = playlist_items.playlist_id
      and p.user_id = auth.uid()
  )
);
```

## Cambios de app relacionados
Archivo: `app/src/main/java/com/example/audify/SupabaseService.kt`

- Se agregó `MissingListsSchemaException` para traducir errores de tablas faltantes (`playlists`/`playlist_items`) a un mensaje claro en la UI.

## Resultado esperado
Tras ejecutar el SQL:
- La pantalla Listas carga correctamente.
- Se pueden crear listas y añadir/quitar podcasts sin errores de tabla inexistente.
