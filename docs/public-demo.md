# Publisere JobTracker som offentlig demo

Oppsettet er for én Spring Boot-app med en egen PostgreSQL-database. Appen og API-et
serveres fra samme domene. Det trengs ingen separat frontendhosting eller CORS-oppsett.

## Hosting og kostnad

`render.yaml` bruker en gratis Render-webservice. Opprett en separat PostgreSQL-database
hos en leverandør du har valgt, og legg tilkoblingen i Render som hemmelig miljøvariabel.
Det opprettes ingen database gjennom Blueprint-filen.

Render dokumenterer at gratis webservicer stanser etter 15 minutter uten trafikk;
første besøk kan derfor kreve en oppstart. Gratis Render Postgres utløper etter 30 dager
og er ikke valgt som database i denne konfigurasjonen.
Se [Render: gratis tjenester](https://render.com/docs/free).

Velg en varig database og avklar pris og eventuelle hvile-/lagringsgrenser før opprettelse.
Bruk en separat database eller et separat prosjekt, ikke en eksisterende appdatabase.
Denne PR-en oppretter ikke kontoer, abonnementer eller skytjenester.

## Før publisering

1. Kjør `./gradlew test bootJar` med Java 17 og Docker. Kontroller grønn GitHub Actions på committen som skal publiseres.
2. Opprett en **tom, dedikert** PostgreSQL-database. Ingen ekte jobbsøknader eller personopplysninger skal finnes her.
3. Åpne Render Dashboard, velg New → Blueprint og koble til repoet og branchen som skal publiseres (normalt main etter merge).
4. Kontroller at instanstypen er Free og at bare én webservice opprettes.
5. Legg inn `DATABASE_URL` direkte i Render. Bruk leverandørens SSL-tilkobling, for eksempel `postgresql://bruker:passord@vert:5432/jobtracker_demo?sslmode=require`. Prosentkod spesialtegn i brukernavn/passord. Ikke legg URL med passord i chat, PR eller kildekode.
6. Behold `JOBTRACKER_DEMO_ENABLED=true` og `/actuator/health` som helsesjekk. Ikke aktiver demomodus mot en privat database: den sletter alle søknader ved start og hver halvtime.
7. Publiser. Flyway oppretter tabellen. Hibernate validerer skjemaet, og demoen legger inn fem fiktive oppføringer.

Hvis hosten bruker en annen PostgreSQL-URL, kan JDBC-URL, brukernavn og passord settes separat
med `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` og `SPRING_DATASOURCE_PASSWORD`.
Tilkoblingsdetaljer vises ikke i helsesjekken. SQL-logging er slått av.

## Kontroller den publiserte adressen

- `/actuator/health` gir `200` og `{"status":"UP"}`. En mislykket databaseforbindelse skal ikke gi frisk status.
- Forsiden viser at demoen er offentlig, at data deles med andre og at de tilbakestilles.
- Opprett, søk etter, filtrer, rediger og slett en **fiktiv** oppføring på desktop og mobil.
- Sjekk at en `javascript:`-lenke og for lange tekster avvises med `400` og uten at data lagres.
- Sjekk at feilmeldinger vises ved `409`, `413` og `429`; unngå å bruke opp det felles forespørselsbudsjettet under vanlig visuell kontroll.
- Bekreft at data blir tilbakestilt etter en halvtime eller ved omstart, og at det ikke finnes konsollfeil.
- Legg først da til «Prøv demo» på porteføljen. Ikke presenter et lokalt skjermbilde som en publisert tjeneste.

## Tilbakerulling og vedlikehold

Stans webservicen ved problemer. Fjern en eventuell demolenk fra porteføljen til feilen er rettet.
Demoens data er bevisst midlertidige; en rollback av appkode skal ikke rulle tilbake eller slette
Flyways historikktabell. Ikke kjør den eldre `ddl-auto=update`-versjonen mot samme database.

Ved flere appinstanser må ratebegrensningen flyttes til et delt lager/gateway. PostgreSQL-låsen
beskytter allerede reset og kapasitet på tvers av instanser, men forespørselsbudsjettet gjør det ikke.
En felles demo kan fortsatt få upassende tekst eller bli midlertidig utilgjengelig når grensen nås;
den er ment som et lite porteføljebevis, ikke en flerbrukertjeneste for virkelige søknader.
