insert into users (id, email, password_hash, display_name, role, status)
values (gen_random_uuid(), '${admin_email}', '${admin_password_hash}', 'Parallax Admin', 'ADMIN', 'ACTIVE')
on conflict ((lower(email))) do nothing;
