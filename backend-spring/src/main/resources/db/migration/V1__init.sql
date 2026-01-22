create extension if not exists "pgcrypto";

create table if not exists users (
  id uuid primary key default gen_random_uuid(),
  email text not null,
  password_hash text not null,
  display_name text,
  first_name text,
  last_name text,
  country text,
  birth_year int,
  birth_month int,
  birth_day int,
  phone_country text,
  phone_digits text,
  contact_method text,
  role text not null default 'USER',
  status text not null default 'ACTIVE',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create unique index if not exists ux_users_email on users ((lower(email)));

create unique index if not exists ux_users_phone
  on users (phone_country, phone_digits)
  where phone_country is not null and phone_digits is not null;

create table if not exists vehicles (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references users(id) on delete cascade,
  license_number text not null,
  make text not null,
  model text not null,
  year int,
  blacklisted boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create unique index if not exists ux_vehicles_license on vehicles ((upper(license_number)));
create index if not exists ix_vehicles_user on vehicles (user_id);

create table if not exists refresh_sessions (
  sid uuid primary key default gen_random_uuid(),
  user_id uuid not null references users(id) on delete cascade,
  refresh_token_hash bytea not null,
  created_at timestamptz not null default now(),
  expires_at timestamptz not null,
  last_used_at timestamptz,
  revoked_at timestamptz,
  revoked_reason text,
  replaced_by_sid uuid,
  ip_last inet,
  ua_last text
);

create unique index if not exists ux_refresh_sessions_token_hash on refresh_sessions (refresh_token_hash);
create index if not exists ix_refresh_sessions_user on refresh_sessions (user_id);
create index if not exists ix_refresh_sessions_expires on refresh_sessions (expires_at);
