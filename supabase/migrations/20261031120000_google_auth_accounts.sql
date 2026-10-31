-- RushIt — Google authentication, account provisioning and row level security.
--
-- Supersedes 20261031_rushit_auth.sql, which is left untouched as history. This migration is
-- idempotent: it creates what is missing and reconciles the earlier schema instead of assuming a
-- fresh database. It contains no destructive change to user-authored data; the only columns it drops
-- are ones RushIt deliberately stops collecting (device identifiers, IP address, user agent).
--
-- Apply with:  supabase db push      (or paste into the Supabase SQL editor, in order)
-- Nothing here requires the service-role key, and nothing here grants it to a client.

-- ---------------------------------------------------------------------------------------------
-- 0. Shared helpers
-- ---------------------------------------------------------------------------------------------

-- Keeps updated_at honest without trusting the client to send it.
create or replace function public.set_updated_at()
returns trigger
language plpgsql
set search_path = public
as $$
begin
    new.updated_at := now();
    return new;
end;
$$;

-- ---------------------------------------------------------------------------------------------
-- 1. profiles — one row per auth.users row, keyed by the Supabase user UUID
-- ---------------------------------------------------------------------------------------------

create table if not exists public.profiles (
    id uuid primary key references auth.users (id) on delete cascade,
    full_name text,
    username text,
    avatar_url text,
    phone_number text,
    phone_verified boolean not null default false,
    onboarding_completed boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

-- Reconcile the earlier revision: it used is_verified and had no phone_verified column.
do $$
begin
    if exists (
        select 1 from information_schema.columns
        where table_schema = 'public' and table_name = 'profiles' and column_name = 'is_verified'
    ) then
        if not exists (
            select 1 from information_schema.columns
            where table_schema = 'public' and table_name = 'profiles' and column_name = 'phone_verified'
        ) then
            alter table public.profiles rename column is_verified to phone_verified;
        else
            update public.profiles
               set phone_verified = true
             where phone_verified = false and is_verified = true;
            alter table public.profiles drop column is_verified;
        end if;
    end if;
end $$;

alter table public.profiles add column if not exists full_name text;
alter table public.profiles add column if not exists username text;
alter table public.profiles add column if not exists avatar_url text;
alter table public.profiles add column if not exists phone_number text;
alter table public.profiles add column if not exists phone_verified boolean not null default false;
alter table public.profiles add column if not exists onboarding_completed boolean not null default false;
alter table public.profiles add column if not exists created_at timestamptz not null default now();
alter table public.profiles add column if not exists updated_at timestamptz not null default now();

alter table public.profiles alter column phone_verified set default false;
alter table public.profiles alter column phone_verified set not null;
alter table public.profiles alter column onboarding_completed set default false;
alter table public.profiles alter column onboarding_completed set not null;
alter table public.profiles alter column created_at set default now();
alter table public.profiles alter column created_at set not null;
alter table public.profiles alter column updated_at set default now();
alter table public.profiles alter column updated_at set not null;

-- Ownership points at auth.users, not at email. Email is never a key in this schema.
do $$
declare
    constraint_record record;
begin
    for constraint_record in
        select conname
          from pg_constraint
         where conrelid = 'public.profiles'::regclass
           and contype = 'f'
           and confrelid <> 'auth.users'::regclass
    loop
        execute format('alter table public.profiles drop constraint %I', constraint_record.conname);
    end loop;

    if not exists (
        select 1 from pg_constraint
         where conrelid = 'public.profiles'::regclass
           and contype = 'f'
           and confrelid = 'auth.users'::regclass
    ) then
        alter table public.profiles
            add constraint profiles_id_fkey foreign key (id) references auth.users (id) on delete cascade;
    end if;
end $$;

-- A blank username is meaningless and would defeat uniqueness, so it is stored as NULL.
update public.profiles set username = null where username is not null and btrim(username) = '';

alter table public.profiles drop constraint if exists profiles_username_not_blank;
alter table public.profiles add constraint profiles_username_not_blank
    check (username is null or btrim(username) <> '');

-- The app writes normalized lowercase usernames; align anything written before that rule existed.
update public.profiles
   set username = lower(btrim(username))
 where username is not null and username <> lower(btrim(username));

-- Case-insensitive uniqueness. A plain UNIQUE(username) would let "Rushit" and "rushit" coexist.
-- The constraint must be dropped before its backing index, otherwise Postgres refuses.
-- If creating the index fails, two existing rows differ only by case and need a manual decision; the
-- migration stops rather than silently discarding somebody's username.
alter table public.profiles drop constraint if exists profiles_username_key;
drop index if exists public.profiles_username_key;
create unique index if not exists profiles_username_unique
    on public.profiles (lower(username))
    where username is not null;

-- A phone number may be linked later; it must never be duplicated across accounts.
alter table public.profiles drop constraint if exists profiles_phone_number_key;
do $$
begin
    if not exists (
        select 1 from pg_index i
          join pg_class c on c.oid = i.indexrelid
         where c.relname = 'profiles_phone_number_unique'
    ) then
        create unique index profiles_phone_number_unique
            on public.profiles (phone_number)
            where phone_number is not null;
    end if;
end $$;

drop trigger if exists profiles_set_updated_at on public.profiles;
create trigger profiles_set_updated_at
    before update on public.profiles
    for each row execute function public.set_updated_at();

-- ---------------------------------------------------------------------------------------------
-- 2. user_settings — defaults initialized when an account is created
-- ---------------------------------------------------------------------------------------------

create table if not exists public.user_settings (
    user_id uuid primary key references auth.users (id) on delete cascade,
    theme text not null default 'system',
    language text not null default 'en',
    notifications_enabled boolean not null default true,
    updated_at timestamptz not null default now()
);

do $$
begin
    if exists (
        select 1 from information_schema.columns
        where table_schema = 'public' and table_name = 'user_settings' and column_name = 'theme_preference'
    ) and not exists (
        select 1 from information_schema.columns
        where table_schema = 'public' and table_name = 'user_settings' and column_name = 'theme'
    ) then
        alter table public.user_settings rename column theme_preference to theme;
    end if;
end $$;

alter table public.user_settings add column if not exists theme text not null default 'system';
alter table public.user_settings add column if not exists language text not null default 'en';
alter table public.user_settings add column if not exists notifications_enabled boolean not null default true;
alter table public.user_settings add column if not exists updated_at timestamptz not null default now();

alter table public.user_settings alter column theme set default 'system';
alter table public.user_settings alter column theme set not null;
alter table public.user_settings alter column language set default 'en';
alter table public.user_settings alter column language set not null;
alter table public.user_settings alter column notifications_enabled set default true;
alter table public.user_settings alter column notifications_enabled set not null;
alter table public.user_settings alter column updated_at set default now();
alter table public.user_settings alter column updated_at set not null;

alter table public.user_settings drop constraint if exists user_settings_theme_check;
alter table public.user_settings add constraint user_settings_theme_check
    check (theme in ('system', 'light', 'dark'));

-- Settings belong to the auth user, so they survive even if a profile row is somehow absent.
do $$
declare
    constraint_record record;
begin
    for constraint_record in
        select conname
          from pg_constraint
         where conrelid = 'public.user_settings'::regclass
           and contype = 'f'
           and confrelid <> 'auth.users'::regclass
    loop
        execute format('alter table public.user_settings drop constraint %I', constraint_record.conname);
    end loop;

    if not exists (
        select 1 from pg_constraint
         where conrelid = 'public.user_settings'::regclass
           and contype = 'f'
           and confrelid = 'auth.users'::regclass
    ) then
        alter table public.user_settings
            add constraint user_settings_user_id_fkey
            foreign key (user_id) references auth.users (id) on delete cascade;
    end if;
end $$;

drop trigger if exists user_settings_set_updated_at on public.user_settings;
create trigger user_settings_set_updated_at
    before update on public.user_settings
    for each row execute function public.set_updated_at();

-- ---------------------------------------------------------------------------------------------
-- 3. user_devices — no hardware identifiers and no tokens
-- ---------------------------------------------------------------------------------------------

create table if not exists public.user_devices (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users (id) on delete cascade,
    device_name text,
    platform text,
    app_version text,
    last_active_at timestamptz,
    created_at timestamptz not null default now()
);

do $$
begin
    if exists (
        select 1 from information_schema.columns
        where table_schema = 'public' and table_name = 'user_devices' and column_name = 'last_active'
    ) and not exists (
        select 1 from information_schema.columns
        where table_schema = 'public' and table_name = 'user_devices' and column_name = 'last_active_at'
    ) then
        alter table public.user_devices rename column last_active to last_active_at;
    end if;
end $$;

alter table public.user_devices add column if not exists device_name text;
alter table public.user_devices add column if not exists platform text;
alter table public.user_devices add column if not exists app_version text;
alter table public.user_devices add column if not exists last_active_at timestamptz;
alter table public.user_devices add column if not exists created_at timestamptz not null default now();

-- RushIt does not need an install identifier or a push token to sign anybody in, and this release
-- stores no tokens of any kind. Dropping these columns is the point: less data, less exposure.
alter table public.user_devices drop constraint if exists user_devices_user_id_device_id_key;
alter table public.user_devices drop column if exists device_id;
alter table public.user_devices drop column if exists fcm_token;

alter table public.user_devices alter column created_at set default now();
alter table public.user_devices alter column created_at set not null;

do $$
declare
    constraint_record record;
begin
    for constraint_record in
        select conname
          from pg_constraint
         where conrelid = 'public.user_devices'::regclass
           and contype = 'f'
           and confrelid <> 'auth.users'::regclass
    loop
        execute format('alter table public.user_devices drop constraint %I', constraint_record.conname);
    end loop;

    if not exists (
        select 1 from pg_constraint
         where conrelid = 'public.user_devices'::regclass
           and contype = 'f'
           and confrelid = 'auth.users'::regclass
    ) then
        alter table public.user_devices
            add constraint user_devices_user_id_fkey
            foreign key (user_id) references auth.users (id) on delete cascade;
    end if;
end $$;

create index if not exists user_devices_user_id_idx on public.user_devices (user_id);

-- ---------------------------------------------------------------------------------------------
-- 4. user_activity_logs — minimal, non-sensitive account events
-- ---------------------------------------------------------------------------------------------

create table if not exists public.user_activity_logs (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users (id) on delete cascade,
    event_type text not null,
    metadata jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default now()
);

do $$
begin
    if exists (
        select 1 from information_schema.columns
        where table_schema = 'public' and table_name = 'user_activity_logs' and column_name = 'activity_type'
    ) and not exists (
        select 1 from information_schema.columns
        where table_schema = 'public' and table_name = 'user_activity_logs' and column_name = 'event_type'
    ) then
        alter table public.user_activity_logs rename column activity_type to event_type;
    end if;
end $$;

alter table public.user_activity_logs add column if not exists event_type text not null default '';
alter table public.user_activity_logs add column if not exists metadata jsonb not null default '{}'::jsonb;
alter table public.user_activity_logs add column if not exists created_at timestamptz not null default now();

-- IP addresses and user agents are collected by infrastructure logs already; keeping them here too
-- would only widen what a leaked row reveals.
alter table public.user_activity_logs drop column if exists ip_address;
alter table public.user_activity_logs drop column if exists user_agent;

alter table public.user_activity_logs alter column event_type drop default;
alter table public.user_activity_logs alter column created_at set default now();

alter table public.user_activity_logs drop constraint if exists user_activity_logs_event_type_check;
alter table public.user_activity_logs add constraint user_activity_logs_event_type_check
    check (btrim(event_type) <> '');

do $$
declare
    constraint_record record;
begin
    for constraint_record in
        select conname
          from pg_constraint
         where conrelid = 'public.user_activity_logs'::regclass
           and contype = 'f'
           and confrelid <> 'auth.users'::regclass
    loop
        execute format('alter table public.user_activity_logs drop constraint %I', constraint_record.conname);
    end loop;

    if not exists (
        select 1 from pg_constraint
         where conrelid = 'public.user_activity_logs'::regclass
           and contype = 'f'
           and confrelid = 'auth.users'::regclass
    ) then
        alter table public.user_activity_logs
            add constraint user_activity_logs_user_id_fkey
            foreign key (user_id) references auth.users (id) on delete cascade;
    end if;
end $$;

create index if not exists user_activity_logs_user_id_created_at_idx
    on public.user_activity_logs (user_id, created_at desc);

-- ---------------------------------------------------------------------------------------------
-- 5. Account provisioning trigger — safe under concurrency, cannot duplicate a row
-- ---------------------------------------------------------------------------------------------

-- Runs when Supabase creates the auth user, so a profile exists before the app's first read.
-- ON CONFLICT DO NOTHING makes a concurrent sign-in, or a re-run for an existing user, harmless.
--
-- The username is deliberately NOT seeded here: a duplicated username would abort account creation,
-- and usernames are chosen during onboarding.
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
    insert into public.profiles (id, full_name, avatar_url)
    values (
        new.id,
        nullif(btrim(coalesce(new.raw_user_meta_data ->> 'full_name', new.raw_user_meta_data ->> 'name', '')), ''),
        nullif(btrim(coalesce(new.raw_user_meta_data ->> 'avatar_url', new.raw_user_meta_data ->> 'picture', '')), '')
    )
    on conflict (id) do nothing;

    insert into public.user_settings (user_id)
    values (new.id)
    on conflict (user_id) do nothing;

    return new;
end;
$$;

-- A trigger function must not be callable directly by a client.
revoke all on function public.handle_new_user() from public;
revoke all on function public.handle_new_user() from anon;
revoke all on function public.handle_new_user() from authenticated;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
    after insert on auth.users
    for each row execute function public.handle_new_user();

-- ---------------------------------------------------------------------------------------------
-- 6. Username availability — the only cross-account read RushIt needs
-- ---------------------------------------------------------------------------------------------

-- RLS restricts profiles to their owner, so the app cannot check availability with a plain SELECT.
-- This function answers a yes/no question and reveals nothing else: no id, no email, no row data.
create or replace function public.username_available(p_username text)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
    select not exists (
        select 1
          from public.profiles
         where username is not null
           and lower(username) = lower(btrim(coalesce(p_username, '')))
    );
$$;

revoke all on function public.username_available(text) from public;
revoke all on function public.username_available(text) from anon;
grant execute on function public.username_available(text) to authenticated;

-- ---------------------------------------------------------------------------------------------
-- 7. Row Level Security
-- ---------------------------------------------------------------------------------------------

alter table public.profiles enable row level security;
alter table public.user_settings enable row level security;
alter table public.user_devices enable row level security;
alter table public.user_activity_logs enable row level security;

-- Drop the earlier revision's policies. "Public profiles are viewable by everyone" contradicts the
-- requirement that a user can read only their own profile, and its UPDATE policy had no WITH CHECK.
drop policy if exists "Public profiles are viewable by everyone." on public.profiles;
drop policy if exists "Users can insert their own profile." on public.profiles;
drop policy if exists "Users can update their own profile." on public.profiles;
drop policy if exists "Users can view their own settings." on public.user_settings;
drop policy if exists "Users can insert their own settings." on public.user_settings;
drop policy if exists "Users can update their own settings." on public.user_settings;
drop policy if exists "Users can view their own devices." on public.user_devices;
drop policy if exists "Users can insert their own devices." on public.user_devices;
drop policy if exists "Users can update their own devices." on public.user_devices;
drop policy if exists "Users can delete their own devices." on public.user_devices;
drop policy if exists "Users can view their own activity logs." on public.user_activity_logs;
drop policy if exists "Users can insert their own activity logs." on public.user_activity_logs;

-- (select auth.uid()) is wrapped in a subquery so Postgres evaluates it once per query, not once
-- per row. No policy exists for anon on any of these tables: anonymous access is denied outright.

drop policy if exists "profiles_select_own" on public.profiles;
create policy "profiles_select_own" on public.profiles
    for select to authenticated
    using ((select auth.uid()) = id);

drop policy if exists "profiles_insert_own" on public.profiles;
create policy "profiles_insert_own" on public.profiles
    for insert to authenticated
    with check ((select auth.uid()) = id);

-- USING and WITH CHECK together: the row must already be theirs, and the row they write must still
-- be theirs, so an id cannot be rewritten to point at another account.
drop policy if exists "profiles_update_own" on public.profiles;
create policy "profiles_update_own" on public.profiles
    for update to authenticated
    using ((select auth.uid()) = id)
    with check ((select auth.uid()) = id);

-- No DELETE policy on profiles: only the service role (account deletion) may remove a row.

drop policy if exists "user_settings_select_own" on public.user_settings;
create policy "user_settings_select_own" on public.user_settings
    for select to authenticated
    using ((select auth.uid()) = user_id);

drop policy if exists "user_settings_insert_own" on public.user_settings;
create policy "user_settings_insert_own" on public.user_settings
    for insert to authenticated
    with check ((select auth.uid()) = user_id);

drop policy if exists "user_settings_update_own" on public.user_settings;
create policy "user_settings_update_own" on public.user_settings
    for update to authenticated
    using ((select auth.uid()) = user_id)
    with check ((select auth.uid()) = user_id);

drop policy if exists "user_devices_select_own" on public.user_devices;
create policy "user_devices_select_own" on public.user_devices
    for select to authenticated
    using ((select auth.uid()) = user_id);

drop policy if exists "user_devices_insert_own" on public.user_devices;
create policy "user_devices_insert_own" on public.user_devices
    for insert to authenticated
    with check ((select auth.uid()) = user_id);

drop policy if exists "user_devices_update_own" on public.user_devices;
create policy "user_devices_update_own" on public.user_devices
    for update to authenticated
    using ((select auth.uid()) = user_id)
    with check ((select auth.uid()) = user_id);

drop policy if exists "user_devices_delete_own" on public.user_devices;
create policy "user_devices_delete_own" on public.user_devices
    for delete to authenticated
    using ((select auth.uid()) = user_id);

drop policy if exists "user_activity_logs_select_own" on public.user_activity_logs;
create policy "user_activity_logs_select_own" on public.user_activity_logs
    for select to authenticated
    using ((select auth.uid()) = user_id);

drop policy if exists "user_activity_logs_insert_own" on public.user_activity_logs;
create policy "user_activity_logs_insert_own" on public.user_activity_logs
    for insert to authenticated
    with check ((select auth.uid()) = user_id);

-- Activity rows are append-only for clients: no UPDATE or DELETE policy exists.

-- ---------------------------------------------------------------------------------------------
-- 8. Column privileges — defence in depth behind RLS
-- ---------------------------------------------------------------------------------------------

-- Even a correctly scoped UPDATE cannot touch a protected column, because the role has no grant for
-- it. phone_verified is the important one: a client must never be able to mark its own phone as
-- verified. id has no UPDATE grant, so account ownership cannot be reassigned.
revoke all on table public.profiles from anon;
revoke all on table public.profiles from authenticated;

grant select (id, full_name, username, avatar_url, phone_number, phone_verified, onboarding_completed, created_at, updated_at)
    on table public.profiles to authenticated;
grant insert (id, full_name, avatar_url)
    on table public.profiles to authenticated;
grant update (full_name, username, avatar_url, onboarding_completed)
    on table public.profiles to authenticated;

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

-- ---------------------------------------------------------------------------------------------
-- 9. Account deletion
-- ---------------------------------------------------------------------------------------------

-- Fallback path for account deletion when the delete-account Edge Function is not deployed.
-- Authorization comes from auth.uid(), which is derived from the caller's verified JWT and cannot be
-- supplied by the client. Deleting the auth.users row cascades to profiles, settings, devices and
-- activity logs. The preferred path remains supabase/functions/delete-account, which uses the Admin
-- API with a service-role key that never reaches the app.
create or replace function public.delete_user_account()
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
    caller uuid;
begin
    caller := auth.uid();
    if caller is null then
        raise exception 'Not authenticated' using errcode = '42501';
    end if;

    delete from auth.users where id = caller;
end;
$$;

revoke all on function public.delete_user_account() from public;
revoke all on function public.delete_user_account() from anon;
grant execute on function public.delete_user_account() to authenticated;
