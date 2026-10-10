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



\### 10.3. Logowanie użytkownika



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



\### 10.4. Testowanie endpointów



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

