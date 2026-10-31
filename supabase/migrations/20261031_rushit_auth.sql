-- Rushit Authentication & User Profile Schema
-- This script creates the required tables and RLS policies for the Rushit App.

-- 1. Create User Profiles Table
CREATE TABLE public.profiles (
    id uuid REFERENCES auth.users ON DELETE CASCADE NOT NULL PRIMARY KEY,
    username text UNIQUE,
    full_name text,
    avatar_url text,
    phone_number text,
    is_verified boolean DEFAULT false NOT NULL,
    onboarding_completed boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at timestamp with time zone DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Enable RLS
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

-- Profiles Policies
CREATE POLICY "Public profiles are viewable by everyone."
    ON public.profiles FOR SELECT
    USING ( true );

CREATE POLICY "Users can insert their own profile."
    ON public.profiles FOR INSERT
    WITH CHECK ( auth.uid() = id );

CREATE POLICY "Users can update their own profile."
    ON public.profiles FOR UPDATE
    USING ( auth.uid() = id );

-- 2. Create User Settings Table
CREATE TABLE public.user_settings (
    user_id uuid REFERENCES public.profiles(id) ON DELETE CASCADE NOT NULL PRIMARY KEY,
    theme_preference text DEFAULT 'system',
    notifications_enabled boolean DEFAULT true,
    order_updates_enabled boolean DEFAULT true,
    promotions_enabled boolean DEFAULT false,
    language text DEFAULT 'en',
    created_at timestamp with time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at timestamp with time zone DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Enable RLS
ALTER TABLE public.user_settings ENABLE ROW LEVEL SECURITY;

-- User Settings Policies
CREATE POLICY "Users can view their own settings."
    ON public.user_settings FOR SELECT
    USING ( auth.uid() = user_id );

CREATE POLICY "Users can insert their own settings."
    ON public.user_settings FOR INSERT
    WITH CHECK ( auth.uid() = user_id );

CREATE POLICY "Users can update their own settings."
    ON public.user_settings FOR UPDATE
    USING ( auth.uid() = user_id );

-- 3. Create User Devices Table
CREATE TABLE public.user_devices (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id uuid REFERENCES public.profiles(id) ON DELETE CASCADE NOT NULL,
    device_id text NOT NULL,
    device_name text,
    fcm_token text,
    last_active timestamp with time zone DEFAULT timezone('utc'::text, now()),
    created_at timestamp with time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    UNIQUE(user_id, device_id)
);

-- Enable RLS
ALTER TABLE public.user_devices ENABLE ROW LEVEL SECURITY;

-- User Devices Policies
CREATE POLICY "Users can view their own devices."
    ON public.user_devices FOR SELECT
    USING ( auth.uid() = user_id );

CREATE POLICY "Users can insert their own devices."
    ON public.user_devices FOR INSERT
    WITH CHECK ( auth.uid() = user_id );

CREATE POLICY "Users can update their own devices."
    ON public.user_devices FOR UPDATE
    USING ( auth.uid() = user_id );

CREATE POLICY "Users can delete their own devices."
    ON public.user_devices FOR DELETE
    USING ( auth.uid() = user_id );

-- 4. Create User Activity Logs Table
CREATE TABLE public.user_activity_logs (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id uuid REFERENCES public.profiles(id) ON DELETE CASCADE NOT NULL,
    activity_type text NOT NULL,
    ip_address text,
    user_agent text,
    created_at timestamp with time zone DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Enable RLS
ALTER TABLE public.user_activity_logs ENABLE ROW LEVEL SECURITY;

-- Activity Logs Policies
CREATE POLICY "Users can view their own activity logs."
    ON public.user_activity_logs FOR SELECT
    USING ( auth.uid() = user_id );

CREATE POLICY "Users can insert their own activity logs."
    ON public.user_activity_logs FOR INSERT
    WITH CHECK ( auth.uid() = user_id );

-- 5. Trigger to automatically create a profile on user signup
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS trigger AS $$
BEGIN
  INSERT INTO public.profiles (id, username, full_name, avatar_url)
  VALUES (
    new.id,
    new.raw_user_meta_data->>'username',
    new.raw_user_meta_data->>'full_name',
    new.raw_user_meta_data->>'avatar_url'
  );
  
  INSERT INTO public.user_settings (user_id)
  VALUES (new.id);
  
  RETURN new;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE PROCEDURE public.handle_new_user();

-- 6. RPC for Account Deletion (Server-side bypass RLS)
CREATE OR REPLACE FUNCTION delete_user_account()
RETURNS void AS $$
BEGIN
  IF auth.uid() IS NULL THEN
    RAISE EXCEPTION 'Not authenticated';
  END IF;

  -- Delete from auth.users (will cascade to profiles and other tables)
  DELETE FROM auth.users WHERE id = auth.uid();
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
