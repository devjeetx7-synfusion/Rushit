-- RushIt — Firebase is the identity provider.
--
-- This migration is FORWARD ONLY and NON-DESTRUCTIVE with respect to user data:
--   * no table is dropped, no row is deleted, no column holding user content is dropped;
--   * existing rows keep their primary keys, usernames, names, avatars and phone numbers;
--   * legacy rows that were created under Supabase Auth keep firebase_uid = NULL and are simply
--     unreachable by the new policies until an administrator links them deliberately.
--
-- What it changes is who owns a row: previously `public.profiles.id` equalled `auth.users.id` and
-- Row Level Security compared `auth.uid()` with it. Firebase Authentication now issues the token, so
-- the verified Firebase UID arrives in the JWT's `sub` claim, which is text — not a UUID. Comparing
-- it with `auth.uid()` would either fail to cast or silently match nothing, so every policy is
-- rewritten around `(select auth.jwt() ->> 'sub')` against `profiles.firebase_uid text`.
--
-- Apply with:  supabase db push
-- or paste into the SQL editor. Review the whole file before running it against production, and take
-- a backup first: `supabase db dump -f before_firebase_identity.sql`.
--
-- Prerequisite, configured outside SQL (Supabase Dashboard → Authentication → Third-Party Auth →
-- Firebase): register the Firebase project ID. Until that is done, Firebase ID tokens are rejected
-- before they reach the database and every request below runs as `anon`.
--
-- Nothing here needs, or grants, the service-role key. The Android app ships only the anon key.
--
-- No BEGIN/COMMIT here on purpose: `supabase db push` and the SQL editor already run a migration in
-- a transaction, and an explicit one would commit early. To apply by hand inside a transaction, wrap
-- this whole file yourself.

-- ---------------------------------------------------------------------------------------------
-- 0. Identity helpers
-- ---------------------------------------------------------------------------------------------

-- The verified Firebase UID of the caller, or NULL for anonymous requests.
--
-- `sub` is set by the JWT issuer and cannot be supplied by the client; Supabase verifies the token
-- signature against the registered Firebase project before PostgREST ever runs. `stable` plus the
-- subquery at each call site keeps the evaluation to once per query rather than once per row.
create or replace function public.current_firebase_uid()
returns text
language sql
stable
security invoker
set search_path = ''
as $$
    select nullif(auth.jwt() ->> 'sub', '');
$$;

comment on function public.current_firebase_uid() is
    'Verified Firebase UID from the JWT sub claim. NULL when the caller is not authenticated.';

-- The internal profiles.id (UUID) of the caller, used by child tables.
--
-- SECURITY DEFINER so the lookup is not itself filtered by the profiles policy (which would make the
-- policies on user_settings/user_devices/user_activity_logs depend on a second RLS evaluation).
create or replace function public.current_profile_id()
returns uuid
language sql
stable
security definer
set search_path = public
as $$
    select p.id
      from public.profiles p
     where p.firebase_uid = public.current_firebase_uid()
     limit 1;
$$;

comment on function public.current_profile_id() is
    'Internal profiles.id owned by the calling Firebase UID. NULL when unlinked or anonymous.';

-- ---------------------------------------------------------------------------------------------
-- 1. profiles — own the row by Firebase UID, keep the UUID as an internal key
-- ---------------------------------------------------------------------------------------------

alter table public.profiles add column if not exists firebase_uid text;

-- The client never sends `id`; the database owns it, so two devices cannot race for the same key and
-- a client can never choose an id that collides with somebody else's row.
alter table public.profiles alter column id set default gen_random_uuid();

-- One row per Firebase account. Partial: legacy rows keep NULL until they are linked explicitly.
drop index if exists public.profiles_firebase_uid_unique;
create unique index if not exists profiles_firebase_uid_unique
    on public.profiles (firebase_uid)
    where firebase_uid is not null;

-- A Firebase UID is opaque text with no length contract; guard only against nonsense.
alter table public.profiles drop constraint if exists profiles_firebase_uid_not_blank;
alter table public.profiles add constraint profiles_firebase_uid_not_blank
    check (firebase_uid is null or btrim(firebase_uid) <> '');

-- No new row may be created without an identity: a NULL-uid row would be invisible to its owner and
-- would defeat the unique index above. Legacy rows are untouched (this is a BEFORE INSERT trigger).
create or replace function public.require_profile_identity()
returns trigger
language plpgsql
set search_path = public
as $$
begin
    if new.firebase_uid is null or btrim(new.firebase_uid) = '' then
        raise exception 'profiles.firebase_uid is required for new rows'
            using errcode = '23502';
    end if;
    return new;
end;
$$;

revoke all on function public.require_profile_identity() from public;

drop trigger if exists profiles_require_identity on public.profiles;
create trigger profiles_require_identity
    before insert on public.profiles
    for each row execute function public.require_profile_identity();

-- ---------------------------------------------------------------------------------------------
-- 2. Foreign keys: the profile is the parent, auth.users is no longer involved
-- ---------------------------------------------------------------------------------------------

-- profiles.id referenced auth.users, which no longer creates users. The column stays (legacy rows
-- depend on it) but the FK must go: a Firebase sign-up has no auth.users row to point at.
do $$
declare
    c record;
begin
    for c in
        select conname
          from pg_constraint
         where conrelid = 'public.profiles'::regclass
           and contype = 'f'
           and confrelid = 'auth.users'::regclass
    loop
        execute format('alter table public.profiles drop constraint %I', c.conname);
    end loop;
end $$;

-- Child rows must still cascade from the profile, and their old auth.users FK would reject every
-- Firebase user (there is no auth.users row to satisfy it). NOT VALID keeps pre-existing child rows
-- that have no profile from blocking the migration; the constraint is validated below only when that
-- is provably safe, so no orphan row is ever deleted.
--
-- The three tables are written out rather than looped over so that each statement is greppable and a
-- failure names its own table.

do $$
declare
    c record;
begin
    for c in
        select conname
          from pg_constraint
         where conrelid = 'public.user_settings'::regclass
           and contype = 'f'
           and confrelid <> 'public.profiles'::regclass
    loop
        execute format('alter table public.user_settings drop constraint %I', c.conname);
    end loop;

    if not exists (
        select 1 from pg_constraint
         where conrelid = 'public.user_settings'::regclass
           and contype = 'f'
           and confrelid = 'public.profiles'::regclass
    ) then
        alter table public.user_settings
            add constraint user_settings_user_id_fkey
            foreign key (user_id) references public.profiles (id) on delete cascade not valid;
    end if;

    if not exists (
        select 1 from public.user_settings c
          left join public.profiles p on p.id = c.user_id
         where p.id is null
    ) then
        alter table public.user_settings validate constraint user_settings_user_id_fkey;
    else
        raise notice 'RushIt: user_settings has rows without a profile; FK left NOT VALID';
    end if;
end $$;

do $$
declare
    c record;
begin
    for c in
        select conname
          from pg_constraint
         where conrelid = 'public.user_devices'::regclass
           and contype = 'f'
           and confrelid <> 'public.profiles'::regclass
    loop
        execute format('alter table public.user_devices drop constraint %I', c.conname);
    end loop;

    if not exists (
        select 1 from pg_constraint
         where conrelid = 'public.user_devices'::regclass
           and contype = 'f'
           and confrelid = 'public.profiles'::regclass
    ) then
        alter table public.user_devices
            add constraint user_devices_user_id_fkey
            foreign key (user_id) references public.profiles (id) on delete cascade not valid;
    end if;

    if not exists (
        select 1 from public.user_devices c
          left join public.profiles p on p.id = c.user_id
         where p.id is null
    ) then
        alter table public.user_devices validate constraint user_devices_user_id_fkey;
    else
        raise notice 'RushIt: user_devices has rows without a profile; FK left NOT VALID';
    end if;
end $$;

do $$
declare
    c record;
begin
    for c in
        select conname
          from pg_constraint
         where conrelid = 'public.user_activity_logs'::regclass
           and contype = 'f'
           and confrelid <> 'public.profiles'::regclass
    loop
        execute format('alter table public.user_activity_logs drop constraint %I', c.conname);
    end loop;

    if not exists (
        select 1 from pg_constraint
         where conrelid = 'public.user_activity_logs'::regclass
           and contype = 'f'
           and confrelid = 'public.profiles'::regclass
    ) then
        alter table public.user_activity_logs
            add constraint user_activity_logs_user_id_fkey
            foreign key (user_id) references public.profiles (id) on delete cascade not valid;
    end if;

    if not exists (
        select 1 from public.user_activity_logs c
          left join public.profiles p on p.id = c.user_id
         where p.id is null
    ) then
        alter table public.user_activity_logs validate constraint user_activity_logs_user_id_fkey;
    else
        raise notice 'RushIt: user_activity_logs has rows without a profile; FK left NOT VALID';
    end if;
end $$;

-- ---------------------------------------------------------------------------------------------
-- 3. Supabase Auth triggers are retired
-- ---------------------------------------------------------------------------------------------

-- handle_new_user() provisioned rows from auth.users. Firebase creates users now, so the trigger
-- can only ever fire for legacy Supabase Auth sign-ups, which no longer happen in the app.
drop trigger if exists on_auth_user_created on auth.users;
drop function if exists public.handle_new_user();

-- The previous account-deletion RPC deleted the auth.users row, which cascaded. A Firebase account
-- has no such row, so it is replaced (below) with one that deletes the caller's own profile.
drop function if exists public.delete_user_account();

-- ---------------------------------------------------------------------------------------------
-- 4. Row Level Security — ownership by verified Firebase UID
-- ---------------------------------------------------------------------------------------------

alter table public.profiles enable row level security;
alter table public.user_settings enable row level security;
alter table public.user_devices enable row level security;
alter table public.user_activity_logs enable row level security;

-- Drop every policy written against auth.uid(); they are either always-false (text vs uuid) or, in
-- the case of the original public-read policy, far too permissive.
do $$
declare
    p record;
begin
    for p in
        select schemaname, tablename, policyname
          from pg_policies
         where schemaname = 'public'
           and tablename in ('profiles', 'user_settings', 'user_devices', 'user_activity_logs')
    loop
        execute format('drop policy %I on %I.%I', p.policyname, p.schemaname, p.tablename);
    end loop;
end $$;

-- profiles: a person may read and change only the row whose firebase_uid is their verified subject.
-- NULL-uid legacy rows match nothing, which is intended: they await an explicit, audited link.
create policy "profiles_select_own" on public.profiles
    for select to authenticated
    using ((select public.current_firebase_uid()) = firebase_uid);

create policy "profiles_insert_own" on public.profiles
    for insert to authenticated
    with check ((select public.current_firebase_uid()) = firebase_uid);

-- USING and WITH CHECK together: the row must already be theirs, and must still be theirs after the
-- write, so ownership cannot be reassigned to another account.
create policy "profiles_update_own" on public.profiles
    for update to authenticated
    using ((select public.current_firebase_uid()) = firebase_uid)
    with check ((select public.current_firebase_uid()) = firebase_uid);

-- Deleting one's own profile is part of account deletion; it cascades to settings, devices and logs.
create policy "profiles_delete_own" on public.profiles
    for delete to authenticated
    using ((select public.current_firebase_uid()) = firebase_uid);

-- Child tables: ownership flows through the profile row the caller owns.
create policy "user_settings_select_own" on public.user_settings
    for select to authenticated
    using (user_id = (select public.current_profile_id()));

create policy "user_settings_insert_own" on public.user_settings
    for insert to authenticated
    with check (user_id = (select public.current_profile_id()));

create policy "user_settings_update_own" on public.user_settings
    for update to authenticated
    using (user_id = (select public.current_profile_id()))
    with check (user_id = (select public.current_profile_id()));

create policy "user_devices_select_own" on public.user_devices
    for select to authenticated
    using (user_id = (select public.current_profile_id()));

create policy "user_devices_insert_own" on public.user_devices
    for insert to authenticated
    with check (user_id = (select public.current_profile_id()));

create policy "user_devices_update_own" on public.user_devices
    for update to authenticated
    using (user_id = (select public.current_profile_id()))
    with check (user_id = (select public.current_profile_id()));

create policy "user_devices_delete_own" on public.user_devices
    for delete to authenticated
    using (user_id = (select public.current_profile_id()));

create policy "user_activity_logs_select_own" on public.user_activity_logs
    for select to authenticated
    using (user_id = (select public.current_profile_id()));

create policy "user_activity_logs_insert_own" on public.user_activity_logs
    for insert to authenticated
    with check (user_id = (select public.current_profile_id()));

-- Activity rows stay append-only for clients: no UPDATE or DELETE policy exists.
-- There is no policy for `anon` on any of these tables, so anonymous access is denied outright.

-- ---------------------------------------------------------------------------------------------
-- 5. Administrator authorization
-- ---------------------------------------------------------------------------------------------

-- Membership is data, never a claim the client can mint. Only a server-side actor (service role,
-- SQL editor, or a reviewed function) can write this table: RLS is on and no policy grants access,
-- and every privilege is revoked from the API roles.
create table if not exists public.admin_roles (
    firebase_uid text primary key,
    role text not null default 'admin',
    granted_at timestamptz not null default now(),
    granted_by text,
    constraint admin_roles_role_check check (role in ('admin', 'owner', 'support'))
);

alter table public.admin_roles enable row level security;
revoke all on table public.admin_roles from anon;
revoke all on table public.admin_roles from authenticated;

-- True only for a caller whose verified Firebase UID is listed above. SECURITY DEFINER is required:
-- the caller has no read access to admin_roles, which is the point.
create or replace function public.is_admin()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
    select exists (
        select 1
          from public.admin_roles a
         where a.firebase_uid = public.current_firebase_uid()
           and a.role in ('admin', 'owner')
    );
$$;

-- Admins may read every profile (support tooling). They get no write policy and no access to child
-- tables: an administrator can look, but cannot edit or delete somebody's account data from a client.
create policy "profiles_admin_read" on public.profiles
    for select to authenticated
    using ((select public.is_admin()));

-- ---------------------------------------------------------------------------------------------
-- 6. Account deletion (fallback when the Edge Function is not deployed)
-- ---------------------------------------------------------------------------------------------

-- The app deletes the Firebase account in-process and then deletes its own profile row, which
-- cascades. This function exists for the case where the profile purge must be retried from the
-- device: it authorizes on the verified Firebase UID and can only ever touch the caller's own row.
create or replace function public.delete_user_account()
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
    caller text;
    profile_id uuid;
begin
    caller := public.current_firebase_uid();
    if caller is null then
        raise exception 'Not authenticated' using errcode = '42501';
    end if;

    profile_id := public.current_profile_id();
    if profile_id is null then
        raise exception 'No profile is linked to this identity' using errcode = 'P0002';
    end if;

    delete from public.profiles where id = profile_id;
end;
$$;

revoke all on function public.delete_user_account() from public;
revoke all on function public.delete_user_account() from anon;
grant execute on function public.delete_user_account() to authenticated;

-- ---------------------------------------------------------------------------------------------
-- 7. Column privileges — defence in depth behind RLS
-- ---------------------------------------------------------------------------------------------

-- RLS decides which rows a role may touch; grants decide which columns. Both are needed: without the
-- column list, a client could write firebase_uid and hand its profile to another account.
revoke all on table public.profiles from anon;
revoke all on table public.profiles from authenticated;

grant select (id, firebase_uid, full_name, username, avatar_url, phone_number, phone_verified,
              onboarding_completed, created_at, updated_at)
    on table public.profiles to authenticated;
-- `id` is absent: the database default owns it. `phone_verified` is absent: a client must never be
-- able to mark its own phone as verified.
grant insert (firebase_uid, full_name, avatar_url)
    on table public.profiles to authenticated;
grant update (full_name, username, avatar_url, onboarding_completed)
    on table public.profiles to authenticated;

-- Deleting one's own profile is allowed by RLS; the column grants above do not restrict DELETE.
grant delete on table public.profiles to authenticated;

revoke all on table public.user_settings from anon;
revoke all on table public.user_settings from authenticated;
grant select (user_id, theme, language, notifications_enabled, updated_at)
    on table public.user_settings to authenticated;
grant insert (user_id) on table public.user_settings to authenticated;
grant update (theme, language, notifications_enabled)
    on table public.user_settings to authenticated;

revoke all on table public.user_devices from anon;
revoke all on table public.user_devices from authenticated;
grant select (id, user_id, device_name, platform, app_version, last_active_at, created_at)
    on table public.user_devices to authenticated;
grant insert (user_id, device_name, platform, app_version, last_active_at)
    on table public.user_devices to authenticated;
grant update (device_name, platform, app_version, last_active_at)
    on table public.user_devices to authenticated;
grant delete on table public.user_devices to authenticated;

revoke all on table public.user_activity_logs from anon;
revoke all on table public.user_activity_logs from authenticated;
grant select (id, user_id, event_type, metadata, created_at)
    on table public.user_activity_logs to authenticated;
grant insert (user_id, event_type, metadata)
    on table public.user_activity_logs to authenticated;

-- Function privileges: identity helpers are readable by signed-in callers only, and nothing here is
-- executable by `anon`.
revoke all on function public.current_firebase_uid() from public;
revoke all on function public.current_firebase_uid() from anon;
grant execute on function public.current_firebase_uid() to authenticated;

revoke all on function public.current_profile_id() from public;
revoke all on function public.current_profile_id() from anon;
grant execute on function public.current_profile_id() to authenticated;

revoke all on function public.is_admin() from public;
revoke all on function public.is_admin() from anon;
grant execute on function public.is_admin() to authenticated;

-- username_available(text) from the previous migration is unchanged: it answers one yes/no question,
-- is SECURITY DEFINER, and reveals no row data. It is re-granted here so a partial rollback of the
-- earlier grants cannot silently break onboarding.
revoke all on function public.username_available(text) from public;
revoke all on function public.username_available(text) from anon;
grant execute on function public.username_available(text) to authenticated;

-- ---------------------------------------------------------------------------------------------
-- Post-migration verification (run these by hand; each must return what the comment says)
-- ---------------------------------------------------------------------------------------------
--
-- 1. The legacy rows are intact and unlinked, not deleted:
--      select count(*) filter (where firebase_uid is null) as legacy,
--             count(*) filter (where firebase_uid is not null) as linked
--        from public.profiles;
--
-- 2. No policy still references auth.uid() (expected: zero rows):
--      select tablename, policyname from pg_policies
--       where schemaname = 'public' and coalesce(qual, '') || coalesce(with_check, '') like '%auth.uid()%';
--
-- 3. RLS is enabled everywhere it must be (expected: rowsecurity = true for all four):
--      select relname, relrowsecurity from pg_class
--       where relname in ('profiles','user_settings','user_devices','user_activity_logs','admin_roles');
--
-- 4. The Firebase UID is unique and enforced (expected: one unique index):
--      select indexname from pg_indexes where tablename = 'profiles' and indexdef like '%firebase_uid%';
--
-- 5. An authenticated request sees only its own row (run from the app, or with a Firebase token):
--      curl -s "$SUPABASE_URL/rest/v1/profiles?select=id,firebase_uid" \
--           -H "apikey: $ANON_KEY" -H "Authorization: Bearer $FIREBASE_ID_TOKEN"
--
-- Nothing below this line is executed automatically. Linking a legacy Supabase Auth account to a
-- Firebase account is a deliberate, audited act, because RushIt must never merge two people's data
-- on a matching email or phone number. Replace both values by hand, after verifying the ownership
-- out of band, then run it once:
--
--   update public.profiles
--      set firebase_uid = '<verified-firebase-uid>'
--    where id = '<legacy-supabase-uuid>'
--      and firebase_uid is null;
