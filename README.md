# zm-presmetko-service

Backend за Пресметко (е-Фактура и сметководство). Gradle (Kotlin DSL) multi-module:

| Модул | Опис |
| --- | --- |
| `backend` | Spring Boot 3.5, Java 21, Spring Modulith. Еден JAR, профили `api` и `worker` |
| `ujp-contract` | чиста Java библиотека со договорот кон УЈП (ја користи и `ujp-sim`) |

Правила за развој (модули, пари, RLS, логирање): [CLAUDE.md](CLAUDE.md).
Спецификации и тикети: `C:\Saas\Docs\presmetko`.

## Предуслови

- JDK 21 (Gradle toolchain го наоѓа локално или го симнува автоматски)
- Docker (Testcontainers во тестовите и локалната околина)

## Команди

```bash
./gradlew build                                   # Spotless + компајлирање + тестови
./gradlew spotlessApply                           # форматирање
./gradlew :backend:bootRun                        # API (local,api), порта 8080
./gradlew :backend:bootRun -Pprofiles=local,worker  # worker, порта 8081
```

Локалната околина (Postgres, Keycloak, S3, Mailpit, ujp-sim): `docker compose up -d` во `../presmetko-infra`.
Портите и тест-корисниците се во README-то на `presmetko-infra`.

## Профили

| Профил | Намена |
| --- | --- |
| `api` | HTTP API; ги извршува Flyway миграциите; нема УЈП креденцијали |
| `worker` | задачи, scheduler, праќање кон УЈП; единствениот со УЈП креденцијали (`UJP_*` env) |
| `local` | поврзување со docker compose околината |

Пример во продукција: `java -jar backend.jar --spring.profiles.active=api` и `--spring.profiles.active=worker`.

## База и изолација на компании

- Flyway миграции во `backend/src/main/resources/db/migration`.
- Апликацијата се поврзува како `app_runtime` (без `BYPASSRLS`); миграциите како сопственик на шемата.
- `TenantAwareTransactionManager` поставува `SET LOCAL app.company_id` на почеток на секоја трансакција;
  RLS политиките не враќаат редови од друга компанија (тест: `RowLevelSecurityIT`).

## CI

`.github/workflows/ci.yml` на секој PR: `./gradlew build` (Spotless, тестови со Testcontainers,
`ApplicationModules.verify()`) и посебен job за OWASP dependency-check (бара secret `NVD_API_KEY`).
Заштитата на `main` се поставува рачно во GitHub (види README на `presmetko-infra`).
