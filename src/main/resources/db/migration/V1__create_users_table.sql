CREATE TABLE users (
    id            uuid         NOT NULL,
    email         varchar(255) NOT NULL,
    password_hash varchar(60)  NOT NULL,
    first_name    varchar(100) NOT NULL,
    last_name     varchar(100) NOT NULL,
    role          varchar(20)  NOT NULL,
    created_at    timestamptz  NOT NULL,

    CONSTRAINT users_pk PRIMARY KEY (id),
    CONSTRAINT users_email_unique UNIQUE (email),
    -- Texto con CHECK en lugar de un ENUM nativo: ampliar un CHECK es una migracion trivial,
    -- alterar un ENUM de PostgreSQL no lo es.
    CONSTRAINT users_role_valid CHECK (role IN ('USER', 'ADMIN'))
);
