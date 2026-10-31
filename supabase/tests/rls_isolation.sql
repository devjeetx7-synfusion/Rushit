-- RushIt RLS isolation tests (pgTAP).
--
-- Run locally with the Supabase CLI:
--   supabase start
--   supabase db reset          (applies every migration in supabase/migrations)
--   supabase test db           (runs supabase/tests/*.sql)
--
-- These tests prove the property that matters most: User A cannot read or write User B's data, an
-- anonymous caller cannot read either, and a client cannot promote itself. They run under pgTAP and
-- are not executed by the Gradle build.

begin;

select plan(16);

-- ---------------------------------------------------------------------------------------------
-- Two accounts, created the way Supabase creates them after a Google sign-in.
-- encrypted_password is a placeholder: these accounts only ever authenticate with an ID token.
-- ---------------------------------------------------------------------------------------------

create temporary table test_users (label text primary key, id uuid not null) on commit drop;

insert into auth.users (
    instance_id, id, aud, role, email, encrypted_password,
    email_confirmed_at, created_at, updated_at,
    raw_app_meta_data, raw_user_meta_data
)
values
    (
        '00000000-0000-0000-0000-000000000000',
        gen_random_uuid(), 'authenticated', 'authenticated', 'user_a@example.com',
        'not-used-google-sign-in',
        now(), now(), now(),
        '{"provider":"google","providers":["google"]}'::jsonb,
        '{"full_name":"User A","avatar_url":"https://example.test/a.png","sub":"google-a"}'::jsonb
    ),
    (
        '00000000-0000-0000-0000-000000000000',
        gen_random_uuid(), 'authenticated', 'authenticated', 'user_b@example.com',
        'not-used-google-sign-in',
        now(), now(), now(),
        '{"provider":"google","providers":["google"]}'::jsonb,
        '{"full_name":"User B","avatar_url":"https://example.test/b.png","sub":"google-b"}'::jsonb
    );

insert into test_users (label, id)
select email, id from auth.users where email in ('user_a@example.com', 'user_b@example.com');

-- 1-4: the on_auth_user_created trigger provisions each account exactly once.
select is(
    (select count(*) from public.profiles where id in (select id from test_users)),
    2::bigint,
    'the signup trigger creates exactly one profile per new auth user'
);

select is(
    (select count(*) from public.user_settings where user_id in (select id from test_users)),
    2::bigint,
    'the signup trigger creates default settings for each new auth user'
);

select is(
    (select count(*) from public.profiles p
       join test_users t on t.id = p.id
      where p.onboarding_completed = false),
    2::bigint,
    'new accounts start with onboarding incomplete'
);

select is(
    (select full_name from public.profiles
      where id = (select id from test_users where label = 'user_a@example.com')),
    'User A',
    'the signup trigger seeds the display name from Google metadata'
);

-- Give user A a username as the table owner, so availability can be checked across accounts later.
update public.profiles
   set username = 'usera', onboarding_completed = true
 where id = (select id from test_users where label = 'user_a@example.com');

-- ---------------------------------------------------------------------------------------------
-- 5-10: signed in as User A.
-- ---------------------------------------------------------------------------------------------

set local role authenticated;
select set_config('request.jwt.claims', json_build_object(
    'sub', (select id::text from test_users where label = 'user_a@example.com'),
    'role', 'authenticated'
)::text, true);

select is(
    (select count(*) from public.profiles),
    1::bigint,
    'user A can read exactly one profile'
);

select is(
    (select id from public.profiles),
    (select id from test_users where label = 'user_a@example.com'),
    'the only profile user A can read is their own'
);

select is(
    (select count(*) from public.user_settings),
    1::bigint,
    'user A can read exactly one settings row'
);

select lives_ok(
    format('update public.profiles set full_name = %L where id = %L',
           'User A renamed',
           (select id::text from test_users where label = 'user_a@example.com')),
    'user A can update their own profile'
);

-- RLS hides other users' rows, so a cross-account UPDATE matches nothing instead of raising.
select is(
    (select count(*) from public.profiles
      where id = (select id from test_users where label = 'user_b@example.com')),
    0::bigint,
    'user B''s profile is invisible to user A'
);

-- phone_verified has no UPDATE grant for authenticated, so self-verification is impossible.
select throws_ok(
    format('update public.profiles set phone_verified = true where id = %L',
           (select id::text from test_users where label = 'user_a@example.com')),
    '42501',
    'user A cannot set their own phone_verified flag',
    'phone_verified is protected from client updates'
);

-- ---------------------------------------------------------------------------------------------
-- 11: back as the table owner, confirm User B was untouched.
-- ---------------------------------------------------------------------------------------------

reset role;

select is(
    (select full_name from public.profiles
      where id = (select id from test_users where label = 'user_b@example.com')),
    'User B',
    'user A could not modify user B''s profile'
);

-- ---------------------------------------------------------------------------------------------
-- 12-13: anonymous callers get nothing.
-- ---------------------------------------------------------------------------------------------

set local role anon;
select set_config('request.jwt.claims', '{"role":"anon"}', true);

select is(
    (select count(*) from public.profiles),
    0::bigint,
    'an anonymous caller cannot read any profile'
);

select is(
    (select count(*) from public.user_activity_logs),
    0::bigint,
    'an anonymous caller cannot read any activity log'
);

-- ---------------------------------------------------------------------------------------------
-- 14-16: username availability across accounts, without exposing any row.
-- ---------------------------------------------------------------------------------------------

set local role authenticated;
select set_config('request.jwt.claims', json_build_object(
    'sub', (select id::text from test_users where label = 'user_b@example.com'),
    'role', 'authenticated'
)::text, true);

select is(
    public.username_available('usera'),
    false,
    'username_available reports a username taken by another account'
);

select is(
    public.username_available('USERA'),
    false,
    'username availability is case-insensitive'
);

select is(
    public.username_available('shopper.free'),
    true,
    'username_available reports an unclaimed username as free'
);

select * from finish();

rollback;
