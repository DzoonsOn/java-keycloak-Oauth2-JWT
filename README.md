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



\## 10. Testowanie Keycloak i Spring Security w Postmanie



Do testowania uwierzytelniania i autoryzacji wykorzystujemy \*\*Postman\*\*. Pozwala on uzyskać token JWT z Keycloak i sprawdzić zabezpieczone endpointy Spring Boot bez konieczności tworzenia frontendu.



\### 10.1. Konfiguracja Keycloak



W panelu administracyjnym Keycloak przejdź do:



\*\*Clients → chat-frontend → Settings\*\*



Upewnij się, że:



\- Client authentication: `OFF`

\- Standard flow: `ON`

\- Direct access grants: `OFF`

\- PKCE: `S256`



W polu \*\*Valid redirect URIs\*\* dodaj:



```text

https://oauth.pstmn.io/v1/callback

```



Adres callback musi odpowiadać adresowi używanemu w Postmanie.



\### 10.2. Konfiguracja Postmana



Utwórz nowe żądanie:



```http

GET http://localhost:8081/api/me

```



Przejdź do zakładki \*\*Authorization\*\* i wybierz \*\*OAuth 2.0\*\*.



Ustaw następujące wartości:



| Parametr | Wartość |

|---|---|

| Token Name | jan-token |

| Grant Type | Authorization Code (With PKCE) |

| Callback URL | https://oauth.pstmn.io/v1/callback |

| Auth URL | http://localhost:8080/realms/chat-realm/protocol/openid-connect/auth |

| Access Token URL | http://localhost:8080/realms/chat-realm/protocol/openid-connect/token |

| Client ID | chat-frontend |

| Client Secret | Puste |

| Code Challenge Method | S256 |

| Scope | openid |



\### 3. Logowanie użytkownika



1\. Kliknij \*\*Get New Access Token\*\*.

2\. Zostaniesz przekierowany na stronę logowania Keycloak.

3\. Zaloguj się jako `jan`, używając ustawionego wcześniej hasła.

4\. Po poprawnym logowaniu Postman otrzyma access token JWT.

5\. Kliknij \*\*Use Token\*\*.

6\. Kliknij \*\*Send\*\*, aby wysłać żądanie do Spring Boot.



Przykładowa odpowiedź:



```json

{

&#x20; "id": "identyfikator-uzytkownika",

&#x20; "username": "jan",

&#x20; "roles": \[

&#x20;   "ROLE\_USER"

&#x20; ]

}

```



\### 4. Testowanie endpointów



Po uzyskaniu tokenu można przetestować uprawnienia użytkowników.



| Endpoint | jan (USER) | adminchat (USER, ADMIN) |

|---|---|---|

| GET /api/public/hello | 200 OK | 200 OK |

| GET /api/user/hello | 200 OK | 200 OK |

| GET /api/admin/hello | 403 Forbidden | 200 OK |

| GET /api/me | 200 OK | 200 OK |



\*\*Test użytkownika USER:\*\*



```http

GET http://localhost:8081/api/user/hello

Authorization: Bearer <ACCESS\_TOKEN>

```



Oczekiwana odpowiedź:



```text

200 OK

Witaj USER!

```



\*\*Test endpointu administratora z tokenem USER:\*\*



```http

GET http://localhost:8081/api/admin/hello

Authorization: Bearer <ACCESS\_TOKEN>

```



Oczekiwana odpowiedź:



```text

403 Forbidden

```



Następnie uzyskaj nowy token, logując się jako `adminchat`, i wywołaj ten sam endpoint.



Oczekiwana odpowiedź:



```text

200 OK

Witaj ADMIN!

```



\### 10.5. Weryfikacja odpowiedzi HTTP



| Status | Znaczenie |

|---|---|

| 200 OK | Token poprawny i dostęp przyznany |

| 401 Unauthorized | Brak tokenu, nieważny token lub błąd uwierzytelnienia |

| 403 Forbidden | Poprawny token, ale brak wymaganych uprawnień |



\### 10.6. Testy negatywne



Warto również przetestować następujące przypadki:



1\. Wywołanie `/api/me` bez tokenu – oczekiwany `401`.

2\. Wywołanie `/api/me` z niepoprawnym tokenem – oczekiwany `401`.

3\. Próba dostępu użytkownika `jan` do `/api/admin/hello` – oczekiwany `403`.

4\. Wywołanie endpointu z wygasłym tokenem – oczekiwany `401`.

5\. Wywołanie `/api/public/hello` bez tokenu – oczekiwany `200`.



Weryfikacja tych scenariuszy pozwala sprawdzić, czy Spring Security poprawnie obsługuje JWT i uprawnienia użytkowników.



\### 10.7. Ważne informacje



\- Token JWT należy przesyłać w nagłówku `Authorization: Bearer`.

\- Do wywoływania backendu używamy \*\*access tokenu\*\*, a nie ID tokenu.

\- Tokenów i haseł nie należy umieszczać w repozytorium.

\- Role `USER` i `ADMIN` muszą być poprawnie mapowane z `realm\_access.roles` na uprawnienia Spring Security.

\- Backend powinien dodatkowo sprawdzać odbiorcę tokenu (`aud`), aby przyjmować wyłącznie tokeny przeznaczone dla jego API.

\- Testy wymagają uruchomionego Keycloak i Spring Boot oraz zaimplementowanych endpointów.


## 11. Logowanie przez Google (OAuth2 + Keycloak)

Aplikacja Secure Chat umożliwia logowanie użytkowników za pomocą konta Google. Keycloak pełni funkcję pośrednika między Google a backendem Spring Boot.

### 11.1. Utworzenie klienta OAuth2 w Google Cloud

1. Otwórz [Google Cloud Console](https://console.cloud.google.com/).
2. Utwórz projekt `secure-chat`.
3. Przejdź do [Google Auth Platform](https://console.cloud.google.com/auth/overview).
4. W sekcji **Branding** skonfiguruj nazwę aplikacji i adres e-mail.
5. W sekcji **Audience** wybierz `External` i dodaj użytkowników testowych, jeśli aplikacja jest w trybie testowym.
6. Przejdź do [Clients](https://console.cloud.google.com/auth/clients).
7. Kliknij **Create client**.

Ustaw:

| Parametr | Wartość |
|---|---|
| Application type | Web application |
| Name | `secure-chat-keycloak` |

W sekcji **Authorized redirect URIs** dodaj:

```text
http://localhost:8080/realms/chat-realm/broker/google/endpoint
```

Kliknij **Create**.

Google wygeneruje `Client ID` oraz `Client Secret`.

### 11.2. Konfiguracja Google w Keycloak

1. Otwórz [Keycloak Admin Console](http://localhost:8080/admin/).
2. Wybierz realm `chat-realm`.
3. Przejdź do **Identity Providers**.
4. Kliknij **Add provider → Google**.
5. Wprowadź dane:

| Parametr | Wartość |
|---|---|
| Alias | `google` |
| Display name | `Google` |
| Client ID | Z Google Cloud |
| Client Secret | Z Google Cloud |
| Enabled | ON |

6. Kliknij **Save**.

Upewnij się, że adres **Redirect URI** w Keycloak jest identyczny z adresem dodanym w Google Cloud.

### 11.3. Testowanie logowania Google

Otwórz ekran logowania Keycloak:

[Keycloak Account Console](http://localhost:8080/realms/chat-realm/account/)

Następnie:

1. Kliknij **Google** pod formularzem logowania.
2. Wybierz konto Google.
3. Zaloguj się i zaakceptuj wymagane zgody.
4. Po pomyślnym logowaniu nastąpi powrót do Keycloak.

Przy pierwszym logowaniu Keycloak może poprosić o uzupełnienie danych użytkownika.

### 11.4. Weryfikacja użytkownika

W panelu administracyjnym Keycloak:

1. Wybierz **chat-realm → Users**.
2. Znajdź użytkownika zalogowanego przez Google.
3. Sprawdź zakładkę **Identity provider links**.
4. Powinno istnieć powiązanie z dostawcą `google`.
5. Przejdź do **Role mapping → Assign role**.
6. Przypisz rolę `USER`.

Po ponownym zalogowaniu token JWT powinien zawierać przypisaną rolę, o ile mapowanie ról do access tokenu jest włączone.

### 11.5. Testowanie w Postmanie

Logowanie przez Google można także przetestować bez frontendu, korzystając z Postmana.

1. Utwórz żądanie `GET http://localhost:8081/api/me`.
2. W **Authorization** wybierz `OAuth 2.0`.
3. Ustaw **Authorization Code (With PKCE)**.
4. Użyj Client ID `chat-frontend`.
5. Ustaw adresy autoryzacji i tokenów z realm `chat-realm`.
6. Kliknij **Get New Access Token**.
7. Na ekranie Keycloak wybierz **Google**.
8. Zaloguj się kontem Google.
9. Kliknij **Use Token**, a następnie **Send**.

W Keycloak klient `chat-frontend` musi mieć dodany adres callback Postmana do **Valid redirect URIs**.

Przykładowa odpowiedź:

```json
{
  "id": "user-id",
  "username": "google-user",
  "roles": ["ROLE_USER"]
}
```

### 11.6. Przydatne linki

- [Google Cloud Console](https://console.cloud.google.com/)
- [Google Auth Platform – Branding](https://console.cloud.google.com/auth/branding)
- [Google Auth Platform – Clients](https://console.cloud.google.com/auth/clients)
- [Google Auth Platform – Audience](https://console.cloud.google.com/auth/audience)
- [Keycloak Admin Console](http://localhost:8080/admin/)
- [Keycloak Account Console](http://localhost:8080/realms/chat-realm/account/)

### Uwagi bezpieczeństwa

- Nie zapisuj `Client Secret` w repozytorium GitHub.
- Backend Spring Boot weryfikuje token JWT wystawiony przez Keycloak, a nie bezpośrednio przez Google.
- Użytkownik zalogowany przez Google nie powinien automatycznie otrzymywać uprawnień `ADMIN`.
- W środowisku produkcyjnym używaj HTTPS i dokładnie określonych adresów przekierowania.
