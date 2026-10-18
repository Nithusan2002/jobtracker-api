# JobTracker

[![CI](https://github.com/Nithusan2002/jobtracker-api/actions/workflows/ci.yml/badge.svg)](https://github.com/Nithusan2002/jobtracker-api/actions/workflows/ci.yml)

JobTracker er en enkel webapp og REST-API for å holde oversikt over jobbsøknader. Den lar deg registrere bedrift, stilling, søknadsdato, status, stillingslenke og notater, og passer som et lite porteføljeprosjekt for Kotlin/Spring Boot med PostgreSQL.

![JobTracker skjermbilde](docs/jobtracker-screenshot.png)

## Funksjoner

- Registrer nye jobbsøknader
- Se alle søknader i en ryddig tabell
- Søk på bedrift eller stilling
- Filtrer på status: `SENT`, `INTERVIEW`, `REJECTED`, `OFFER`
- Rediger eksisterende søknader
- Slett søknader
- REST-API under `/api/applications`
- Validering og tydelige `400`/`404`-feilsvar
- Egen offentlig demomodus med fiktive data, automatisk tilbakestilling og grenser mot misbruk

## Teknologistack

- Kotlin
- Spring Boot 3.3
- Spring Web
- Spring Data JPA / Hibernate
- PostgreSQL og Flyway-migreringer
- Gradle Kotlin DSL
- HTML, CSS og JavaScript

## Kjøre lokalt

Du trenger **Java 17** og **Docker** (for PostgreSQL og integrasjonstestene).
Gradle lastes ned automatisk av Gradle Wrapper; du trenger ikke installere Gradle.

### 1. Start PostgreSQL

```bash
docker run --name jobtracker-db \
  -e POSTGRES_PASSWORD=devpass \
  -e POSTGRES_DB=jobtracker \
  -p 127.0.0.1:5432:5432 \
  -d postgres:17-alpine
```

Hvis containeren allerede finnes:

```bash
docker start jobtracker-db
```

### 2. Start appen

```bash
./gradlew bootRun
```

Åpne webappen:

```text
http://localhost:8080/
```

API-et ligger her:

```text
http://localhost:8080/api/applications
```

Flyway oppretter skjemaet ved oppstart. Hibernate bruker `ddl-auto=validate` og endrer ikke tabellene.
Vanlig modus starter uten eksempeldata og sletter aldri data automatisk.

### Eksisterende database fra før Flyway

Ta sikkerhetskopi først. På en database med det tidligere Hibernate-skjemaet setter du
`FLYWAY_BASELINE_ON_MIGRATE=true` ved første oppstart. Flyway markerer utgangspunktet som versjon 0,
kjører V1 uten å overskrive tabellen og beholder radene. Fjern variabelen etterpå.
Uten dette eksplisitte valget nekter Flyway å migrere et ukjent, ikke-tomt skjema.
Hibernate validerer at skjemaet stemmer. Testene kontrollerer at eksisterende rader bevares.

### Offentlig demomodus

Bruk **en egen database uten personopplysninger**. Med `JOBTRACKER_DEMO_ENABLED=true`
slettes alle søknader i denne databasen ved oppstart og deretter hver halvtime,
og fem tydelig fiktive søknader legges inn på nytt. Ikke aktiver dette mot en privat database.

```bash
JOBTRACKER_DEMO_ENABLED=true ./gradlew bootRun
```

Demoen har ingen innlogging. Alle kan se, endre og slette oppføringene; dette opplyses på siden.
Den har maks 100 oppføringer, 120 API-forespørsler og 20 skriveforespørsler per minutt.
Forespørselsgrensene deles av alle besøkende i én appinstans, også bak en proxy.
Ingen IP-adresser eller brukeridentifikatorer lagres av begrensningen.
Ved `429` følger `Retry-After` med. Store skriveforespørsler over 16 KiB gir `413`,
og full demo gir `409`. Cross-site-skriving fra moderne nettlesere avvises.

Grenser og reset-intervall kan endres med Spring-egenskapene `jobtracker.demo.max-applications`,
`requests-per-minute`, `writes-per-minute` og `reset-interval` (standard `PT30M`).
Tilbakestilling og API-endringer bruker samme PostgreSQL-transaksjonslås, slik at samtidige
opprettelser ikke kan omgå maksgrensen og tilbakestilling ikke kan gi en halvferdig datasamling.
Kjør én appinstans; ratebegrensningen er ikke distribuert og erstatter ikke vern mot DDoS.

`GET /api/demo` forteller grensesnittet om demomodus. `GET /actuator/health` kontrollerer
også databasen, uten å eksponere tilkoblingsdetaljer.

## Arkitektur

```mermaid
flowchart LR
    Browser["Nettleser: HTML/CSS/JavaScript"] -->|"JSON /api/applications"| Controller[ApplicationController]
    Controller --> Service[ApplicationService]
    Service --> Repository[ApplicationRepository]
    Repository -->|"JPA / Hibernate"| Database[(PostgreSQL)]
    Controller -.->|"Validering og 404"| Errors[GlobalExceptionHandler]
```

Spring Boot serverer både webgrensesnittet og API-et fra samme app. Controlleren håndterer
HTTP og inputvalidering, servicen utfører CRUD-operasjonene, og repository-laget lagrer
data i PostgreSQL. Dette holder prosjektet enkelt å kjøre og deploye.

## Tester og CI

Med Java 17 og Docker i gang:

```bash
./gradlew test bootJar
```

Integrasjonstestene starter en separat PostgreSQL 17-container med Testcontainers.
Du trenger ikke starte den lokale databasen først, og testene bruker ikke dine lokale
søknadsdata. Første kjøring laster ned Gradle, avhengigheter og Docker-images.

Testene dekker alle fem API-endepunktene, lagring og oppdatering av data, tom liste,
`400` ved tomme påkrevde felt og ugyldig JSON/dato/status, samt `404` for ukjent ID.
Avviste opprettelser og oppdateringer skal ikke endre databasen.
Testene dekker også feltgrenser og lenkeprotokoller, demo-reset og maksgrense med samtidige
opprettelser, store forespørsler uten Content-Length, ratebegrensning og Retry-After,
samt migrering av en eksisterende database uten tap av rader.

Testrapporten ligger i `build/reports/tests/test/index.html`.
GitHub Actions kjører `./gradlew test bootJar --no-daemon` på push og pull requests
med Java 17, og laster opp testrapporter som artifacts. Workflowen kan også startes manuelt.

## Kjente begrensninger

- Ingen autentisering eller tilgang per bruker. Dette er en felles demo, ikke et sted for virkelige søknader.
- Søking og filtrering skjer i nettleseren. API-et henter hele listen, som begrenses til 100 rader i demoen.
- Ratebegrensning er global per appinstans. En ivrig besøkende kan midlertidig bruke opp budsjettet for andre.
- Tilbakestilling skjer hver halvtime mens appen kjører, og ved omstart. Hvis hosting stanser appen, gjenopprettes fiktive data neste gang den starter.

## Deploy

[Oppskrift for offentlig demo](docs/public-demo.md) beskriver oppsett og kontrollpunkter.
`render.yaml` oppretter én gratis webservice med Docker, demomodus og databasebasert helsesjekk.
Den oppretter **ikke** en database eller et betalt abonnement. Du må gi den `DATABASE_URL`
for en egen PostgreSQL-database. Ikke bruk en database som inneholder andre prosjekters data.

Appen støtter `postgresql://bruker:passord@vert/database?sslmode=require` (og `postgres://`),
og konverterer til JDBC ved oppstart. Prosentkodede passord og pluss-tegn håndteres uten dobbel dekoding.
Alternativt kan du bruke `SPRING_DATASOURCE_URL` (JDBC), `SPRING_DATASOURCE_USERNAME` og
`SPRING_DATASOURCE_PASSWORD`. Legg hemmeligheter i hostingens miljøvariabler, aldri i Git.

## Datamodell

| Felt | Type | Påkrevd | Beskrivelse |
| --- | --- | --- | --- |
| `id` | Long | auto | Genereres av databasen |
| `companyName` | String | ja | Navn på bedriften |
| `jobTitle` | String | ja | Stillingstittel |
| `applicationDate` | LocalDate | ja | Dato søknaden ble sendt |
| `status` | enum | ja | `SENT`, `INTERVIEW`, `REJECTED`, `OFFER` |
| `jobListingUrl` | String | nei | Lenke til stillingsannonsen |
| `notes` | String | nei | Fritekstnotater |

## API

Alle endepunkter ligger under `/api/applications`.

| Metode | Endepunkt | Beskrivelse |
| --- | --- | --- |
| `GET` | `/api/applications` | Hent alle søknader |
| `GET` | `/api/applications/{id}` | Hent én søknad |
| `POST` | `/api/applications` | Opprett søknad |
| `PUT` | `/api/applications/{id}` | Oppdater søknad |
| `DELETE` | `/api/applications/{id}` | Slett søknad |

### Eksempel: opprett søknad

```bash
curl -X POST http://localhost:8080/api/applications \
  -H "Content-Type: application/json" \
  -d '{
    "companyName": "Fjord Demo AS",
    "jobTitle": "Backend-utvikler",
    "applicationDate": "2026-08-11",
    "status": "SENT",
    "jobListingUrl": "https://example.com/job/123",
    "notes": "Fiktiv demosøknad"
  }'
```

### Statuskoder

- `200 OK` ved vellykket `GET` og `PUT`
- `201 Created` ved vellykket `POST`
- `204 No Content` ved vellykket `DELETE`
- `400 Bad Request` ved valideringsfeil
- `404 Not Found` når søknaden ikke finnes
- `409 Conflict` når demoen har nådd maks antall oppføringer
- `413 Payload Too Large` ved mer enn 16 KiB i demomodus
- `429 Too Many Requests` når demoens felles forespørselsgrense er brukt opp

## Lisens

MIT License. Se [LICENSE](LICENSE).
