# JWT — JSON Web Token

## Vad är det?

JWT är ett sätt att bevisa vem du är utan att servern behöver komma ihåg dig. Istället för att lagra sessioner i en databas skickar servern ett signerat "bevis" till klienten, som klienten sedan visar vid varje request.

---

## Strukturen

En JWT ser ut så här:

```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0QHRlc3QuY29tIiwiaWF0IjoxNzg1NzYxNzcyfQ.itfwLmuHIlvx04wnU1wLb8Gqfo6QdFE9L2h_HlBIZto
```

Det är tre Base64-kodade delar separerade med `.`:

```
HEADER.PAYLOAD.SIGNATURE
```

### Header
```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```
Berättar vilken algoritm som använts för att signera.

### Payload (Claims)
```json
{
  "sub": "test@test.com",
  "iat": 1785761772,
  "exp": 1785848172
}
```
- `sub` — subject, dvs vem tokenen tillhör (vanligtvis email eller userId)
- `iat` — issued at, när den skapades (Unix timestamp)
- `exp` — expires, när den slutar gälla

> **OBS:** Payload är inte krypterad — bara kodad. Vem som helst kan läsa den. Lägg aldrig lösenord eller känslig data här.

### Signature
```
HMACSHA256(
  base64(header) + "." + base64(payload),
  SECRET_KEY
)
```
Signaturen är det enda som är hemligt. Servern signerar med en hemlig nyckel. Om någon ändrar payload:en stämmer inte signaturen längre — och servern förkastar tokenen.

---

## Flödet

```
1. Klienten loggar in:
   POST /api/auth/login  { email, password }

2. Servern verifierar mot DB, skapar JWT och svarar:
   { "token": "eyJ..." }

3. Klienten sparar tokenen (t.ex. i minnet eller Keychain på iOS).

4. Vid varje efterföljande request skickar klienten:
   Authorization: Bearer eyJ...

5. Servern verifierar signaturen → vet vem du är → svarar med data.
```

---

## I ditt HRV-projekt

### Skapa token (vid login/register)
```java
String token = Jwts.builder()
    .subject(user.getEmail())
    .issuedAt(new Date())
    .expiration(new Date(System.currentTimeMillis() + 86400000)) // 24h
    .signWith(secretKey)
    .compact();
```

### Validera token (vid varje request)
```java
Claims claims = Jwts.parser()
    .verifyWith(secretKey)
    .build()
    .parseSignedClaims(token)
    .getPayload();

String email = claims.getSubject(); // "test@test.com"
```

### Spring Security-filter
Varje HTTP-request passerar `JwtAuthFilter` som:
1. Läser `Authorization: Bearer <token>` headern
2. Validerar signaturen
3. Sätter användaren i `SecurityContextHolder` så att Spring vet vem som anropar

---

## Varför JWT och inte sessioner?

| Sessions | JWT |
|----------|-----|
| Servern lagrar state i DB/minne | Servern lagrar inget — stateless |
| Svårt att skala horisontellt | Fungerar med flera servrar direkt |
| Invalideras enkelt (radera session) | Kan inte invalideras utan extra logik |
| Bra för webbappar med cookies | Bra för API:er och mobilappar |

---

## Säkerhetsregler

- **Hemlig nyckel** (`JWT_SECRET`) måste vara lång och slumpmässig — aldrig hårdkodad i koden
- Sätt rimlig **expiration** (t.ex. 24h för access token)
- Använd **HTTPS** — annars kan tokenen stjälas i transit
- Lägg aldrig lösenord eller personnummer i payload

---

## Testa din JWT

Gå till [jwt.io](https://jwt.io) och klistra in din token. Du kan se header och payload direkt i browsern.

Tokenen du fick nyss:
```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0QHRlc3QuY29tIiwiaWF0IjoxNzg1NzYxNzcyLCJleHAiOjE3ODU4NDgxNzJ9.itfwLmuHIlvx04wnU1wLb8Gqfo6QdFE9L2h_HlBIZto
```
Sub: `test@test.com` · Expires: 24h efter att den skapades.
