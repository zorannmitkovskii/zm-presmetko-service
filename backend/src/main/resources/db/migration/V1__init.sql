-- PRS-1.5: основни табели, улога app_runtime и RLS шаблон (техничка спец. 2.0, дел 6.3).
-- Миграциите ги извршува сопственикот на шемата; апликацијата се поврзува како app_runtime.

-- Улогата за апликацијата: без BYPASSRLS, не е сопственик на табели.
-- Локално/во тестови ја креира init скриптата (со LOGIN и лозинка); тука само осигуруваме дека постои
-- и дека не може да ја заобиколи RLS.
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'app_runtime') THEN
        CREATE ROLE app_runtime NOLOGIN;
    END IF;
END
$$;
ALTER ROLE app_runtime NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE;

-- Тековна компанија од SET LOCAL app.company_id (TenantAwareTransactionManager).
-- Ако контекстот не е поставен враќа NULL, па политиките враќаат 0 редови (fail-closed).
CREATE FUNCTION app_current_company_id() RETURNS uuid
    LANGUAGE sql STABLE
AS $$
    SELECT nullif(current_setting('app.company_id', true), '')::uuid
$$;

-- Корисници: глобални (еден корисник може да е член во повеќе компании), без company_id.
CREATE TABLE users (
    id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    idp_subject  text        NOT NULL UNIQUE, -- sub од Keycloak
    email        text        NOT NULL UNIQUE,
    display_name text        NOT NULL,
    created_at   timestamptz NOT NULL DEFAULT now()
);

-- Компании: коренот на tenant-от; изолацијата е по id (= company_id).
CREATE TABLE companies (
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name       text        NOT NULL,
    edb        varchar(13) NOT NULL UNIQUE, -- ЕДБ; не се логира
    embs       varchar(7),
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE memberships (
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id uuid        NOT NULL REFERENCES companies (id),
    user_id    uuid        NOT NULL REFERENCES users (id),
    role       text        NOT NULL, -- шаблон на улога, дозволите доаѓаат со PRS-5
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (company_id, user_id)
);
CREATE INDEX memberships_user_id_idx ON memberships (user_id);

-- RLS шаблон: секоја tenant табела ги добива овие три наредби.
ALTER TABLE companies ENABLE ROW LEVEL SECURITY;
ALTER TABLE companies FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON companies
    USING (id = app_current_company_id())
    WITH CHECK (id = app_current_company_id());

ALTER TABLE memberships ENABLE ROW LEVEL SECURITY;
ALTER TABLE memberships FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON memberships
    USING (company_id = app_current_company_id())
    WITH CHECK (company_id = app_current_company_id());

-- Привилегии за app_runtime (DML, без DDL), и за идни табели.
GRANT USAGE ON SCHEMA public TO app_runtime;
GRANT SELECT, INSERT, UPDATE, DELETE ON users, companies, memberships TO app_runtime;
GRANT EXECUTE ON FUNCTION app_current_company_id() TO app_runtime;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO app_runtime;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO app_runtime;
