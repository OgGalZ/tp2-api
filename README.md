# TP2 — API REST avancées avec Spring Boot

Projet unique regroupant les 4 exercices : calculatrice, conversion de devises, BMI avec JWT, documentation/Docker/tests.

**Stack** : Java 21 · Spring Boot 3.5 · Spring Data JPA · PostgreSQL 16 · Spring Security + JJWT · Spring Retry · Caffeine · springdoc-openapi · Docker · JUnit 5 / MockMvc

## Lancer le projet

### Option A — En local (PostgreSQL Homebrew)

```bash
cd tp2-api
mvn spring-boot:run
```

Par défaut, l'API se connecte à `jdbc:postgresql://localhost:5432/tp2` avec ton utilisateur macOS, sans mot de passe. Les tables sont créées automatiquement.

### Option B — Tout en Docker (API + base)

```bash
cd tp2-api
docker compose up --build
```

La base Docker est exposée sur le port **5433** pour ne pas entrer en conflit avec le PostgreSQL Homebrew. **Arrête d'abord l'option A** si elle tourne, car les deux utilisent le port 8080.

### Option C — Image seule (livrable « docker build / docker run »)

```bash
docker build -t tp2-api .
docker run --rm -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/tp2 \
  -e DB_USERNAME=$USER \
  tp2-api
```

## Liens utiles

| | URL |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |

## Tests

```bash
mvn test
```

Les tests utilisent une base H2 en mémoire : ils ne dépendent ni de PostgreSQL ni d'Internet.

**Couverture** : 17 tests. Ils couvrent un succès, une erreur de validation, la division par zéro, la pagination, le CRUD, le cache, le 503 avec rollback, le 429, le 401, le 403, la rotation du refresh token et le vrai retry avec backoff.

## Postman

Importe `postman/TP2.postman_collection.json`. Elle contient 45 requêtes classées par exercice, avec tous les codes d'erreur demandés. Exécute les dossiers **dans l'ordre** : les ids et les jetons sont enregistrés automatiquement dans les variables de la collection.

- **429** : clic droit sur le dossier « Rate limiting », puis *Run folder*, avec **25 itérations** et un délai de 0 ms.
- **503** : relance l'API avec une URL externe injoignable, puis appelle `GET /api/currency/convert` sur une paire pas encore en cache :
  ```bash
  EXCHANGE_API_URL=http://localhost:1 mvn spring-boot:run
  ```

## Structure

```
src/main/java/fr/efrei/tp2
├── common/       ApiError + GlobalExceptionHandler (@RestControllerAdvice)
├── calculator/   Exercice 1
├── currency/     Exercice 2 (WebClient, @Cacheable, @Retryable, @Transactional)
├── ratelimit/    Filtre token bucket 20 req/min/IP
├── auth/         Utilisateurs, login, refresh tokens
├── security/     JWT, filtre, Spring Security
├── bmi/          Exercice 3
└── config/       OpenAPI
docs/schema.sql   Structure de la base
```
