\# Secure Chat – Backend aplikacji czatu z Keycloak



\## 1. Opis projektu



\*\*Secure Chat\*\* to aplikacja backendowa umożliwiająca bezpieczną komunikację między użytkownikami. Projekt wykorzystuje Spring Boot oraz Keycloak do zarządzania uwierzytelnianiem i autoryzacją.



Głównym celem projektu jest stworzenie systemu czatu, w którym użytkownicy mogą zakładać konta, logować się, prowadzić prywatne rozmowy oraz przeglądać historię wiadomości.



Za zarządzanie tożsamością użytkowników odpowiada Keycloak, który udostępnia mechanizmy OAuth 2.0 i OpenID Connect. Backend Spring Boot działa jako OAuth2 Resource Server i weryfikuje tokeny JWT.



\## 2. Technologie



| Technologia | Zastosowanie |

|---|---|

| Java 21 | Język programowania |

| Spring Boot 4.1.1 | Framework backendowy |

| Spring Security | Autoryzacja i zabezpieczanie API |

| OAuth2 Resource Server | Weryfikacja tokenów JWT |

| Keycloak | Uwierzytelnianie i zarządzanie użytkownikami |

| PostgreSQL | Przechowywanie danych |

| Docker Compose | Uruchamianie infrastruktury |

| Maven | Zarządzanie zależnościami |

| Postman | Testowanie endpointów |

| WebSocket | Komunikacja w czasie rzeczywistym – planowana |



\## 3. Architektura systemu



System składa się z trzech głównych elementów:



\*\*Keycloak\*\* – odpowiada za rejestrację, logowanie użytkowników, zarządzanie rolami oraz wystawianie tokenów JWT.



\*\*Spring Boot\*\* – obsługuje logikę biznesową aplikacji, zabezpiecza endpointy REST API i odpowiada za komunikację między użytkownikami.



\*\*PostgreSQL\*\* – przechowuje dane aplikacji, takie jak konwersacje, wiadomości oraz identyfikatory uczestników rozmów.



W przyszłości zostanie dodany frontend umożliwiający komunikację z backendem poprzez REST API oraz WebSocket.



\### Przepływ uwierzytelniania



1\. Użytkownik otwiera aplikację czatu.

2\. Frontend przekierowuje użytkownika do Keycloak.

3\. Użytkownik loguje się za pomocą loginu i hasła.

4\. Keycloak uwierzytelnia użytkownika.

5\. Frontend uzyskuje access token JWT poprzez Authorization Code Flow z PKCE.

6\. Frontend przesyła token do Spring Boot w nagłówku `Authorization: Bearer`.

7\. Spring Security sprawdza podpis tokenu, wystawcę oraz czas ważności.

8\. Backend odczytuje role użytkownika i sprawdza jego uprawnienia.

9\. Użytkownik otrzymuje dostęp do dozwolonych zasobów.



\## 4. Konfiguracja Keycloak



\### 4.1. Uruchomienie Keycloak



Keycloak zostaje uruchomiony w kontenerze Docker wraz z bazą PostgreSQL.



Konfiguracja lokalna:



\- Keycloak: `http://localhost:8080`

\- Spring Boot: `http://localhost:8081`

\- PostgreSQL: wewnętrzna sieć Docker



Do uruchamiania infrastruktury wykorzystywany jest plik `docker-compose.yml`.



Polecenie uruchomienia:



```bash

docker compose up -d

```



Panel administracyjny Keycloak:



`http://localhost:8080/admin/`



Konfiguracja Docker `start-dev` jest przeznaczona wyłącznie do środowiska deweloperskiego.



\### 4.2. Utworzenie Realm



Realm jest logicznie wydzieloną przestrzenią Keycloak, zawierającą użytkowników, klientów, role i konfigurację zabezpieczeń.



Aby utworzyć realm:



1\. Zalogować się do Keycloak Admin Console.

2\. Rozwinąć listę realmów.

3\. Wybrać \*\*Create realm\*\*.

4\. Wprowadzić nazwę `chat-realm`.

5\. Ustawić `Enabled = On`.

6\. Zatwierdzić przyciskiem \*\*Create\*\*.



Adres utworzonego realmu:



`http://localhost:8080/realms/chat-realm`



Realm `master` pozostaje przeznaczony do administracji serwerem Keycloak.



\### 4.3. Utworzenie klienta



W realm `chat-realm` należy utworzyć klienta reprezentującego aplikację frontendową.



Ścieżka:



\*\*Clients → Create client\*\*



Konfiguracja:



| Parametr | Wartość |

|---|---|

| Client type | OpenID Connect |

| Client ID | chat-frontend |

| Client authentication | Off |

| Standard flow | On |

| Implicit flow | Off |

| Direct access grants | Off |

| PKCE | S256 |



Klient `chat-frontend` jest klientem publicznym, ponieważ frontend działający w przeglądarce nie może bezpiecznie przechowywać `client\_secret`.



Przykładowe ustawienia dla frontendu działającego na porcie 3000:



\- Root URL: `http://localhost:3000`

\- Home URL: `http://localhost:3000`

\- Valid redirect URIs: `http://localhost:3000/\*`

\- Web origins: `http://localhost:3000`

\- Valid post logout redirect URIs: `http://localhost:3000/\*`



W środowisku produkcyjnym należy stosować dokładnie określone adresy przekierowań i HTTPS.



\### 4.4. Tworzenie ról



System wykorzystuje role realmu (Realm roles).



W panelu administracyjnym:



\*\*Realm roles → Create role\*\*



Należy utworzyć:



\*\*USER\*\*



Standardowy użytkownik aplikacji. Może korzystać z funkcji czatu, przesyłać wiadomości i przeglądać swoje konwersacje.



\*\*ADMIN\*\*



Administrator aplikacji. Docelowo będzie posiadać dostęp do funkcji administracyjnych, takich jak moderacja i zarządzanie wybranymi zasobami.



Role aplikacyjne nie są równoważne administracyjnym rolom Keycloak, takim jak `realm-admin`.



\### 4.5. Tworzenie użytkowników



W panelu administracyjnym:



\*\*Users → Add user\*\*



Należy utworzyć dwóch użytkowników testowych:



| Użytkownik | Role |

|---|---|

| jan | USER |

| adminchat | USER, ADMIN |



Dla każdego użytkownika należy:



1\. Utworzyć konto.

2\. Włączyć opcję `Enabled`.

3\. Przejść do zakładki \*\*Credentials\*\*.

4\. Ustawić unikalne hasło.

5\. Ustawić `Temporary = Off` dla testowego logowania bez wymuszenia zmiany hasła.

6\. Otworzyć \*\*Role mapping\*\*.

7\. Wybrać \*\*Assign role\*\*.

8\. Przypisać odpowiednie role.



\### 4.6. Role w tokenie JWT



Keycloak może umieszczać role realmu w claimie:



```json

{

&#x20; "preferred\_username": "jan",

&#x20; "realm\_access": {

&#x20;   "roles": \["USER"]

&#x20; }

}

```



Dla użytkownika `adminchat` lista będzie zawierać `USER` oraz `ADMIN`.



Należy upewnić się, że role trafiają do access tokenu, korzystając z odpowiedniego mapowania i client scope `roles`.



\## 5. Konfiguracja Spring Boot



\### 5.1. Zależności Maven



Projekt korzysta z następujących zależności:



\- Spring Web MVC

\- Spring Security

\- OAuth2 Resource Server

\- Zależności testowe Spring Boot



Do weryfikowania tokenów Keycloak nie jest wymagany osobny adapter Keycloak.



\### 5.2. Konfiguracja application.properties



```properties

spring.application.name=secure

server.port=8081



spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8080/realms/chat-realm

```



Właściwość `issuer-uri` pozwala Spring Security odczytać konfigurację OpenID Connect oraz klucze potrzebne do sprawdzania tokenów JWT.



\### 5.3. Konfiguracja Spring Security



Należy utworzyć klasę `SecurityConfig`, która:



1\. Konfiguruje `SecurityFilterChain`.

2\. Włącza uwierzytelnianie tokenem Bearer JWT.

3\. Wprowadza bezstanową obsługę żądań.

4\. Udostępnia publiczne endpointy.

5\. Ogranicza dostęp do endpointów użytkownika na podstawie roli `USER`.

6\. Ogranicza dostęp do endpointów administracyjnych na podstawie roli `ADMIN`.



Przykładowe reguły:



\- `/api/public/\*\*` – dostęp publiczny

\- `/api/user/\*\*` – wymagana rola USER

\- `/api/admin/\*\*` – wymagana rola ADMIN

\- pozostałe endpointy – wymagane uwierzytelnienie



\### 5.4. Mapowanie ról Keycloak



Spring Security domyślnie nie zamienia ról z `realm\_access.roles` na role Spring Security.



Należy zaimplementować klasę `KeycloakRoleConverter`.



Jej zadaniem jest przekształcanie ról:



\- `USER` → `ROLE\_USER`

\- `ADMIN` → `ROLE\_ADMIN`



Dzięki temu można wykorzystywać metody:



```java

.hasRole("USER")

.hasRole("ADMIN")

```



Dodatkowo backend powinien docelowo walidować claim `aud`, aby przyjmować wyłącznie tokeny przeznaczone dla jego API.



\## 6. Endpointy testowe



W pierwszym etapie implementowane są następujące endpointy:



| Metoda | Endpoint | Dostęp |

|---|---|---|

| GET | /api/public/hello | Publiczny |

| GET | /api/user/hello | USER |

| GET | /api/admin/hello | ADMIN |

| GET | /api/me | Zalogowany użytkownik |



Endpoint `/api/me` umożliwia pobranie informacji o uwierzytelnionym użytkowniku, takich jak identyfikator, nazwa oraz role.



\## 7. Testowanie przez Postman



Ponieważ frontend nie został jeszcze zaimplementowany, testowanie logowania odbywa się przez Postman.



W zakładce \*\*Authorization → OAuth 2.0\*\* należy ustawić:



| Parametr | Wartość |

|---|---|

| Grant Type | Authorization Code (With PKCE) |

| Auth URL | http://localhost:8080/realms/chat-realm/protocol/openid-connect/auth |

| Access Token URL | http://localhost:8080/realms/chat-realm/protocol/openid-connect/token |

| Client ID | chat-frontend |

| Client Secret | Puste |

| Code Challenge Method | S256 |

| Scope | openid |



Adres callback używany przez Postman musi być dodany w konfiguracji `Valid redirect URIs` klienta Keycloak.



\### Testy autoryzacji



Dla użytkownika `jan`:



\- `/api/me` → 200 OK

\- `/api/user/hello` → 200 OK

\- `/api/admin/hello` → 403 Forbidden



Dla użytkownika `adminchat` z rolami USER i ADMIN:



\- `/api/me` → 200 OK

\- `/api/user/hello` → 200 OK

\- `/api/admin/hello` → 200 OK



Brak tokenu przy próbie dostępu do chronionego endpointu powinien powodować odpowiedź `401 Unauthorized`.



\## 8. Dalszy rozwój backendu czatu



\### Etap 1. Uwierzytelnianie i autoryzacja



\- \[x] Uruchomienie Keycloak

\- \[x] Utworzenie `chat-realm`

\- \[x] Utworzenie `chat-frontend`

\- \[x] Utworzenie ról USER i ADMIN

\- \[x] Utworzenie użytkowników testowych

\- \[x] Konfiguracja weryfikacji JWT w Spring Boot

\- \[x] Test działania `/api/me`

\- \[ ] Pełna weryfikacja reguł USER i ADMIN

\- \[ ] Walidacja audience tokenu dla backendu



\### Etap 2. Baza danych aplikacji



\- \[ ] Dodanie Spring Data JPA

\- \[ ] Konfiguracja PostgreSQL dla danych czatu

\- \[ ] Utworzenie encji Conversation

\- \[ ] Utworzenie encji Message

\- \[ ] Powiązanie użytkowników z identyfikatorami Keycloak

\- \[ ] Dodanie migracji bazy danych



\### Etap 3. Obsługa wiadomości



\- \[ ] Tworzenie prywatnych konwersacji

\- \[ ] Wysyłanie wiadomości

\- \[ ] Pobieranie historii wiadomości

\- \[ ] Sprawdzanie uczestnictwa użytkownika w konwersacji

\- \[ ] Obsługa paginacji wiadomości



\### Etap 4. WebSocket



\- \[ ] Dodanie obsługi WebSocket

\- \[ ] Uwierzytelnianie połączeń

\- \[ ] Wysyłanie wiadomości w czasie rzeczywistym

\- \[ ] Powiadomienia o nowych wiadomościach

\- \[ ] Obsługa statusów odczytu



\### Etap 5. Frontend



\- \[ ] Utworzenie interfejsu czatu

\- \[ ] Integracja z Keycloak przez Authorization Code Flow + PKCE

\- \[ ] Obsługa sesji i odświeżania tokenów

\- \[ ] Wyświetlanie listy rozmów

\- \[ ] Wysyłanie i odbieranie wiadomości

\- \[ ] Wylogowanie użytkownika



\## 9. Założenia bezpieczeństwa



Podczas dalszego rozwoju projektu należy przestrzegać następujących zasad:



\- Hasła użytkowników są zarządzane przez Keycloak, a nie zapisywane w bazie aplikacji.

\- Każde chronione żądanie REST API wymaga poprawnego access tokenu.

\- Role użytkowników są weryfikowane po stronie backendu.

\- Dostęp do rozmów musi być dodatkowo ograniczany do ich uczestników — sama rola USER nie jest wystarczająca.

\- Produkcyjne środowisko wykorzystuje HTTPS.

\- Dane uwierzytelniające do usług są przechowywane poza repozytorium.

\- Backend weryfikuje wystawcę, podpis, ważność i docelowo odbiorcę tokenu.

\- Uprawnienia administracyjne aplikacji są odseparowane od administracji Keycloak.



\## 10. Podsumowanie



Projekt Secure Chat wykorzystuje Keycloak jako centralny system zarządzania tożsamością użytkowników oraz Spring Boot jako backend odpowiedzialny za logikę aplikacji.



Obecny etap obejmuje uruchomienie infrastruktury, konfigurację realm i klienta, utworzenie użytkowników oraz podstawową integrację JWT z Spring Security.



Kolejnym etapem będzie zaprojektowanie modelu danych czatu, utworzenie tabel konwersacji i wiadomości oraz wdrożenie komunikacji w czasie rzeczywistym.



Docelowo system umożliwi bezpieczne logowanie użytkowników, prowadzenie prywatnych rozmów, przechowywanie historii wiadomości oraz zarządzanie uprawnieniami.

