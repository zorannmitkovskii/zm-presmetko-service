-- Исто како presmetko-infra/postgres/init: login улога за апликацијата, без BYPASSRLS.
CREATE ROLE app_runtime LOGIN PASSWORD 'app_runtime_test' NOSUPERUSER NOBYPASSRLS;
