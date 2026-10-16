# Пресметко: backend (zm-presmetko-service)

е-Фактура и сметководство за Македонија. Ова репо е Gradle проект (Kotlin DSL) со модули:

- `backend/`: Spring Boot 3.5, Java 21, Spring Modulith. Еден JAR, два профила: `api` (HTTP) и `worker` (задачи, scheduler, УЈП).
- `ujp-contract/`: чиста Java библиотека (без Spring) со договорот кон УЈП; ја користат `backend` и `ujp-sim`.

Соседни репоа (во истата родителска папка `C:\Saas\Projects\presmetko`):
`zm-presmetko-fe` (React веб), `ujp-sim` (симулатор на УЈП), `presmetko-infra` (docker compose, Keycloak).

## Спецификации и тикети

Спецификациите се НАДВОР од репото, во `C:\Saas\Docs\presmetko`:

- `presmetko-funkcionalna-specifikacija.html`: функционална спец. 2.0
- `presmetko-tehnicka-specifikacija.html`: техничка спец. 2.0 (4.3 структура, 6.3 RLS, 10 заокружување, 16 околини, 21 начин на работа)
- `presmetko-arhitektura.html`, `tiketi/` (тикети `PRS-n.m`), `dizajn/` (HTML демо на екраните)

Секоја промена е врзана за тикет `PRS-n.m`; ID-то оди во PR насловот и описот.

## Команди

```bash
./gradlew build                      # spotlessCheck + компајлирање (-Werror) + сите тестови (треба Docker)
./gradlew spotlessApply              # форматирање (palantir-java-format)
./gradlew :backend:test --tests '*ModularityTest'   # само Modulith verify
./gradlew :backend:bootRun           # API (профили local,api) против docker compose од presmetko-infra
./gradlew :backend:bootRun -Pprofiles=local,worker  # worker
./gradlew dependencyCheckAggregate   # OWASP аудит (бара NVD_API_KEY)
```

Локалната околина: `docker compose up -d` во `../presmetko-infra`.

## Архитектонски правила (задолжителни)

### Модули (Spring Modulith)

- Модули: `identity`, `company`, `bureau`, `catalog`, `referencedata`, `invoicing`, `importing`, `ujp`, `documents`, `audit`, `notifications`, `shared` (под `mk.presmetko`). (`importing` наместо `import`, кој е клучен збор во Java.)
- Модул НЕ пристапува до табелите и до `internal` пакетите на друг модул. Комуникацијата е само преку неговиот јавен пакет (`<modul>/api`) или преку application events.
- Структура внатре во модул: `api/` (јавен интерфејс), `domain/`, `internal/` (сервиси, Spring Data JDBC репозиториуми), `web/` (REST, DTO).
- `shared` е отворен модул: tenant context, пари/заокружување, грешки, безбедност. Таму не оди бизнис логика.
- `ModularityTest` (`ApplicationModules.verify()`) мора да поминува.

### Пари и заокружување

- Пари се секогаш `BigDecimal`, во базата `NUMERIC(19,4)`. Никогаш `double`/`float`.
- Правило R2026-1, `RoundingMode.HALF_UP` (`mk.presmetko.shared.money.MoneyRounding`): износи на ставки 4 децимали, збирови на документ 2 децимали, `docGrossAmountR` и `docFinalAmount` цел број.
- Jackson е конфигуриран со `USE_BIG_DECIMAL_FOR_FLOATS` и `WRITE_BIGDECIMAL_AS_PLAIN`.

### Multi-tenancy и RLS

- Секоја tenant табела има `company_id uuid NOT NULL` и RLS:
  ```sql
  ALTER TABLE x ENABLE ROW LEVEL SECURITY;
  ALTER TABLE x FORCE ROW LEVEL SECURITY;
  CREATE POLICY tenant_isolation ON x
      USING (company_id = app_current_company_id())
      WITH CHECK (company_id = app_current_company_id());
  ```
- Апликацијата се поврзува како `app_runtime` (без `BYPASSRLS`, не е сопственик). Flyway работи како сопственик на шемата.
- `TenantAwareTransactionManager` извршува `SET LOCAL app.company_id` на почетокот на секоја трансакција од `TenantContext`. Пристап до tenant податоци само во трансакција (`@Transactional`); без контекст RLS враќа 0 редови.
- `company_id` во контекстот доаѓа САМО од проверено членство на корисникот (или од `company_id` на задачата во worker преку `TenantJobRunner`), НИКОГАШ директно од барањето (header, параметар, тело).
- Секоја нова tenant табела добива интеграциски тест за изолација (види `RowLevelSecurityIT`). Endpoint со ID на ресурс од друга компанија враќа 404.
- Исклучоци (не се tenant по `company_id`): `users`, `bureau_clients`, `bureau_assignments`, `ujp_credentials`; пристап само преку нивните модули со експлицитни проверки.

### Безбедност и логирање

- Никогаш не логирај ЕДБ/ЕМБС (TIN), износи ни УЈП креденцијали (ни во исклучоци, ни во MDC).
- УЈП креденцијалите постојат само во `worker` профилот (`application-worker.yml`, env променливи). `api` никогаш не ги чита.
- Тајни никогаш во репото; локалните тест-вредности се само во `application-local.yml` и `presmetko-infra`.

### База и миграции

- Flyway: `backend/src/main/resources/db/migration/V<n>__<opis>.sql`. Применета миграција не се менува; секоја промена е нова миграција.
- Spring Data JDBC со експлицитен SQL (без JPA/lazy loading).

## Именување и стил

- Пакети `mk.presmetko.<modul>...`; класи и код на англиски, UI текст и коментари може на македонски.
- Тестови: `*Test` (unit), `*IT` (интеграциски, Testcontainers); сите се извршуваат со `./gradlew build`.
- Форматирање: Spotless (palantir-java-format). Пред PR: `./gradlew spotlessApply build`.
- PR: шаблонот бара линк до тикет и листа на тестови.
