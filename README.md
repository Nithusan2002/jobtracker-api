# JobTracker API

REST-API for å spore egne jobbsøknader (bedrift, stilling, søknadsdato, status, lenke og notater).

## Teknologier

- **Kotlin + Spring Boot 3.3** — for et lite, typesikkert og velkjent Spring-økosystem uten mye boilerplate.
- **Spring Data JPA + Hibernate** — CRUD mot databasen uten håndskrevet SQL.
- **PostgreSQL** — ekte relasjonsdatabase, ikke en in-memory-database som forsvinner ved restart.
- **Gradle (Kotlin DSL)** — bygg-verktøy, samme språk som resten av prosjektet.

Ingen autentisering, brukerhåndtering, frontend eller LLM-integrasjon — bevisst utelatt fra scope.

## Datamodell

Én entitet, `Application`:

| Felt              | Type      | Påkrevd | Beskrivelse                                  |
|-------------------|-----------|---------|-----------------------------------------------|
| `id`              | Long      | auto    | Genereres av databasen                        |
| `companyName`     | String    | ja      | Navn på bedriften                             |
| `jobTitle`        | String    | ja      | Stillingstittel                               |
| `applicationDate` | LocalDate | ja      | Dato søknaden ble sendt (`YYYY-MM-DD`)        |
| `status`          | enum      | ja      | `SENT`, `INTERVIEW`, `REJECTED`, `OFFER`      |
| `jobListingUrl`   | String    | nei     | Lenke til stillingsannonsen                   |
| `notes`           | String    | nei     | Fritekstnotater                               |

## Kjøre prosjektet lokalt

### 1. Start database

```bash
docker run --name jobtracker-db \
  -e POSTGRES_PASSWORD=devpass \
  -e POSTGRES_DB=jobtracker \
  -p 5432:5432 \
  -d postgres
```

### 2. Konfigurasjon

`src/main/resources/application.yml` peker allerede mot `jdbc:postgresql://localhost:5432/jobtracker` med bruker `postgres` / passord `devpass` (matcher Docker-kommandoen over). Endre der ved behov.

Databasetabellen opprettes automatisk ved oppstart (`spring.jpa.hibernate.ddl-auto: update`) — ingen migrasjonsverktøy nødvendig for dette lille prosjektet.

### 3. Start appen

```bash
./gradlew bootRun
```

API-et kjører på `http://localhost:8080`.

## Endepunkter

Alle under `/api/applications`.

### `GET /api/applications` — liste alle

```bash
curl http://localhost:8080/api/applications
```

### `GET /api/applications/{id}` — hent én

```bash
curl http://localhost:8080/api/applications/1
```

### `POST /api/applications` — opprett ny

```bash
curl -X POST http://localhost:8080/api/applications \
  -H "Content-Type: application/json" \
  -d '{
    "companyName": "Computas",
    "jobTitle": "Backend-utvikler",
    "applicationDate": "2026-08-01",
    "status": "SENT",
    "jobListingUrl": "https://example.com/job/123",
    "notes": "Søkte via finn.no"
  }'
```

### `PUT /api/applications/{id}` — oppdater eksisterende

```bash
curl -X PUT http://localhost:8080/api/applications/1 \
  -H "Content-Type: application/json" \
  -d '{
    "companyName": "Computas",
    "jobTitle": "Backend-utvikler",
    "applicationDate": "2026-08-01",
    "status": "INTERVIEW",
    "jobListingUrl": "https://example.com/job/123",
    "notes": "Fikk intervju 15. august"
  }'
```

### `DELETE /api/applications/{id}` — slett

```bash
curl -X DELETE http://localhost:8080/api/applications/1
```

## Statuskoder

- `200 OK` — vellykket GET/PUT
- `201 Created` — vellykket POST
- `204 No Content` — vellykket DELETE
- `400 Bad Request` — valideringsfeil (f.eks. manglende `companyName`)
- `404 Not Found` — søknad med gitt id finnes ikke
