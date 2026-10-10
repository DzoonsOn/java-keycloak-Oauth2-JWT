# Secure Chat – Spring Boot + Keycloak

Backend aplikacji czatu wykorzystujący **Spring Boot 4.1.1**, **Java 21**, **Spring Security** oraz **Keycloak** do uwierzytelniania i autoryzacji użytkowników za pomocą JWT.

## 1. Uruchomienie Keycloak

Projekt wykorzystuje Docker Compose do uruchomienia Keycloak oraz PostgreSQL.

W głównym katalogu projektu, w którym znajduje się plik `docker-compose.yml`, wykonaj:

```bash
docker compose up -d
```

Sprawdź, czy kontenery działają:

```bash
docker compose ps
```

## 2. Panel administracyjny Keycloak

Otwórz w przeglądarce:

http://localhost:8080/admin/

Zaloguj się danymi skonfigurowanymi w `docker-compose.yml`:

- **Login:** `admin`
- **Hasło:** `admin123`

Dane służą wyłącznie do lokalnego środowiska deweloperskiego.

## 3. Utworzenie Realm

1. W panelu Keycloak rozwiń listę realmów (domyślnie `master`).
2. Kliknij **Create realm**.
3. Wpisz nazwę `chat-realm`.
4. Ustaw **Enabled: ON**.
5. Kliknij **Create**.

## 4. Utworzenie Client

Przejdź do **Clients → Create client**.

Ustaw:

- **Client type:** OpenID Connect
- **Client ID:** `chat-frontend`
- **Client authentication:** OFF
- **Standard flow:** ON
- **Direct access grants:** OFF
- **PKCE:** S256

Dla przykładowego frontendu działającego na porcie `3000` skonfiguruj:

- **Root URL:** `http://localhost:3000`
- **Valid redirect URIs:** `http://localhost:3000/*`
- **Web origins:** `http://localhost:3000`

Zapisz konfigurację.

Jeśli testujesz logowanie przez Postman, dodaj również jego adres callback do **Valid redirect URIs**.

## 5. Utworzenie ról

Przejdź do **Realm roles → Create role**.

Utwórz dwie role:

- `USER` – standardowy użytkownik czatu.
- `ADMIN` – administrator aplikacji.

## 6. Utworzenie użytkowników

Przejdź do **Users → Add user**.

Utwórz przykładowych użytkowników:

| Username | Role |
|---|---|
| `jan` | USER |
| `adminchat` | USER, ADMIN |

Dla każdego użytkownika:

1. W zakładce **Credentials** ustaw hasło.
2. Ustaw **Temporary: OFF**.
3. W zakładce **Role mapping → Assign role** przypisz odpowiednie role.

## 7. Konfiguracja Spring Boot

W pliku `src/main/resources/application.properties` dodaj:

```properties
spring.application.name=secure
server.port=8081
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8080/realms/chat-realm
```

Uruchom aplikację:

```bash
./mvnw spring-boot:run
```

Na Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

## 8. Testowanie API

Po zalogowaniu użytkownika przez Keycloak i uzyskaniu access tokenu JWT można przetestować zabezpieczone endpointy w Postmanie.

| Endpoint | Dostęp |
|---|---|
| `GET /api/public/hello` | Publiczny |
| `GET /api/user/hello` | USER |
| `GET /api/admin/hello` | ADMIN |
| `GET /api/me` | Zalogowany użytkownik |

Token należy przekazać w nagłówku:

```http
Authorization: Bearer <ACCESS_TOKEN>
```

Endpointy wymagają odpowiedniej konfiguracji `SecurityConfig` i mapowania ról Keycloak w Spring Security.

## 9. Zatrzymanie środowiska

Aby zatrzymać kontenery:

```bash
docker compose down
```

Dane PostgreSQL pozostają zachowane w wolumenie Dockera. Nie używaj `docker compose down -v`, jeśli chcesz zachować użytkowników i konfigurację Keycloak.