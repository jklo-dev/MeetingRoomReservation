-- Usuario ADMIN inicial, necesario porque crear usuarios exige rol ADMIN y sin este registro
-- no habria forma de empezar.
--
-- El hash corresponde a la contrasena 'admin' y se genero ejecutando BCryptPasswordEncoder
-- real, no se copio de ninguna fuente. Credenciales documentadas en el README: son aceptables
-- en una prueba tecnica e inaceptables en produccion.
INSERT INTO users (id, email, password_hash, first_name, last_name, role, created_at)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'admin@meetroom.local',
    '$2a$10$BqglZa2nSMmrOE0K9wjXeOFF4LGX33PiHQ944RSd6LmP3ciqY0kH2',
    'Admin',
    'Sistema',
    'ADMIN',
    now()
);
