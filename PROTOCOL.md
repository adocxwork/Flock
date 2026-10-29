# Reverse-Engineering Protocol: Urja Meter Ops Portal

**Target Application:** Urja Meter Ops (`https://urja-ops.flockenergy.tech`)  
**Investigated by:** Flock Energy Engineering Take-Home Solution  
**Status:** Fully Documented & Replicated

---

## 1. System Overview & Technology Stack

Urja Meter Ops is an internal distribution utility portal designed for operators to inspect smart meters, locate them within the distribution grid hierarchy, and observe half-hourly interval energy consumption.

Through client bundle analysis (`/_app/immutable/*`), network inspection, and route reverse engineering, we discovered the application architecture:

* **Frontend Framework:** SvelteKit 2.x with Svelte 5 (utilizing signals/runes `__sveltekit_uzbk0k`, `DHWw2r--.js`).
* **Styling:** Tailwind CSS.
* **Authentication Engine:** `Better-Auth` managing session tokens and sliding sessions.
* **Routing Architecture:**
  * Client/Server Hybrid: SvelteKit SSR load functions (`/__data.json`) paired with client-side fetch calls to internal JSON endpoints under `/portal/*`.
* **State / Session Storage:** No browser storage (`localStorage` and `sessionStorage` are empty); state is entirely cookie-governed.

---

## 2. Authentication & Session Protocol

### 2.1 The Login Flow (`POST /login`)
Authentication occurs via a SvelteKit form action:
* **Endpoint:** `POST https://urja-ops.flockenergy.tech/login`
* **Form Action Headers:**
  ```http
  POST /login HTTP/2
  Host: urja-ops.flockenergy.tech
  Content-Type: application/x-www-form-urlencoded
  Origin: https://urja-ops.flockenergy.tech
  Referer: https://urja-ops.flockenergy.tech/login
  x-sveltekit-action: true
  Accept: application/json
  ```
* **Payload:**
  ```http
  email=operator%40urja.local&password=urja-ops-2026
  ```

### 2.2 Security Quirks & CSRF Origin Enforcement
* **SvelteKit Origin Check:** The portal strictly validates the HTTP `Origin` header. If a client submits a login request without `Origin: https://urja-ops.flockenergy.tech`, the server immediately terminates the connection with:
  ```http
  HTTP/2 403 Forbidden
  Cross-site POST form submissions are forbidden
  ```
* **Response:**
  Upon valid credentials, the server responds with:
  ```json
  {"type": "redirect", "status": 303, "location": "/meters"}
  ```
  accompanied by the session cookie:
  ```http
  Set-Cookie: __Secure-better-auth.session_token=<token>; Max-Age=3600; Path=/; HttpOnly; Secure; SameSite=Lax
  ```

### 2.3 Session Characteristics
* **Cookie Name:** `__Secure-better-auth.session_token`
* **Security Flags:** `HttpOnly`, `Secure`, `SameSite=Lax`.
* **Lifespan:** `Max-Age=3600` (1 hour sliding window).
* **JavaScript Inaccessibility:** Because `HttpOnly` is set, `document.cookie` is blank in the browser.
* **Auth Helper Endpoints:**
  * `GET /api/auth/get-session`: Returns session object and user profile (`Ops Desk`, `operator@urja.local`, ID: `QQzA9lR6zhhqtbJrU2iC0Cp3rCIEYefi`).
  * `POST /api/auth/sign-out`: Invalidates server session and deletes the cookie.

---

## 3. Discovered Endpoints & API Reference

### 3.1 Meter Search (`GET /portal/meters/search`)
Used by the `/meters` view table.
* **Parameters:**
  * `q` (string): Query string. Only matches on `meterId` (e.g. `J100000`) or `serialNo` (e.g. `SE33962`).
  * `page` (integer): 1-based page index.
* **Ignored Parameters:** The portal completely ignores `pageSize` or `limit` parameters; it strictly pages at 20 items per page (403 total meters across 21 pages).
* **Sample Response:**
  ```json
  {
    "data": [
      {
        "meterId": "J100000",
        "serialNo": "SE33962",
        "make": "HPL",
        "phaseType": "single",
        "installStatus": "Decommissioned",
        "dtCode": "DT-001"
      }
    ],
    "total": 403,
    "page": 1,
    "pageSize": 20
  }
  ```

### 3.2 Geographical Location (`GET /portal/meters/:id/geo`)
Called dynamically when navigating to `/meters/:id`.
* **Sample Response:**
  ```json
  {
    "data": {
      "latitude": "26.938961002479868",
      "longitude": "75.83095696146852"
    }
  }
  ```
* **Quirk:** Coordinates are returned as strings rather than JSON numbers.
* **Error Response (`404`):** `{"error": "not_found", "message": "Meter not found"}`.

### 3.3 Consumption & Energy Readings (`GET /portal/meters/:id/energy`)
Called dynamically on the meter detail page.
* **Payload Size:** ~28 KB.
* **Data Count:** 337 half-hourly interval readings covering 7 full consecutive days.
* **Sample Record:**
  ```json
  {
    "data": [
      {
        "timestamp": "23/06/2026 23:30",
        "kwh": "48438.74",
        "kvah": "52313.84",
        "voltR": "226"
      }
    ]
  }
  ```
* **Quirks:**
  * Numbers are serialized as strings (`"kwh"`, `"kvah"`, `"voltR"`).
  * Timestamps use localized Indian utility format `DD/MM/YYYY HH:mm`.

### 3.4 Distribution Transformers (`GET /portal/dts`)
Used on `/transformers` view.
* **Parameters:** `page` (integer). 40 total DTs (20 per page across 2 pages).
* **Sample Response:**
  ```json
  {
    "data": [
      {
        "code": "DT-001",
        "name": "Malviya Nagar DT 1",
        "feederCode": "F-001",
        "capacityKva": 100
      }
    ],
    "total": 40,
    "page": 1,
    "pageSize": 20
  }
  ```

### 3.5 Server Load State (`GET /meters/:id/__data.json`)
SvelteKit server-rendered load data providing nameplate and grid hierarchy.
* Contains user context, meter parameters, and hierarchy mapping.

---

## 4. The Hidden Bulk Export & HMAC-SHA256 Signing Protocol

One of the most critical discoveries in the portal code (`4.2Bgc2kUI.js`) is the bulk export mechanism behind the **"Export all meters"** button on the Transformers page.

### 4.1 Key Exchange (`GET /portal/keys`)
Authenticated clients can request the cryptographic signing key:
```http
GET /portal/keys HTTP/2
Cookie: __Secure-better-auth.session_token=...
```
**Response:**
```json
{
  "data": {
    "signingSecret": "I3dZPPf5CgTp7JyGNMI8i6z8LFR7TmSR"
  }
}
```

### 4.2 Canonical Signature Calculation
The client creates an HMAC-SHA256 signature using the `signingSecret`:
```
canonical_message = method + "\n" + path + "\n" + queryString + "\n" + timestamp
```
Example:
```
GET
/portal/export
page=1
1730178668
```
Signature algorithm: `HmacSHA256(secret, canonical_message)` encoded in 64-character lowercase hexadecimal.

### 4.3 Export Request & Security Validations (`GET /portal/export`)
```http
GET /portal/export?page=1 HTTP/2
Cookie: __Secure-better-auth.session_token=...
x-timestamp: 1730178668
x-signature: 8fa7e4a1936be8f9...
```
* **Strict Security Enforcement:**
  * Missing headers, invalid signatures, or timestamps older than 5 minutes yield:
    ```json
    HTTP/2 401 Unauthorized
    {"error": "signature_invalid", "message": "Missing or invalid request signature."}
    ```
* **Surprise Behavior:**
  Although the frontend passes `?page=1`, the endpoint **completely ignores pagination and returns all 403 meters in a single payload**!
* **Sample Export Item:**
  ```json
  {
    "meterId": "J100000",
    "serialNo": "SE33962",
    "make": "HPL",
    "phaseType": "single",
    "installStatus": "Decommissioned",
    "installType": "Whole Current",
    "build": "legacy",
    "dtCode": "DT-001",
    "hierarchy": {
      "zone": { "name": "Jaipur Zone 1", "code": "Z-01" },
      "circle": { "name": "Circle 1", "code": "C-01" },
      "division": { "name": "Division 1", "code": "D-01" },
      "subdivision": { "name": "Subdivision 1", "code": "SD-01" },
      "substation": { "name": "Substation 1", "code": "SS-01" },
      "feeder": { "name": "Feeder 1", "code": "F-001" },
      "dt": { "name": "Malviya Nagar DT 1", "code": "DT-001" }
    },
    "geo": {
      "lat": 26.938961002479868,
      "lng": 75.83095696146852
    }
  }
  ```

---

## 5. Quirks, Surprises, and Inconsistencies Found

### 1. Dual Schema Architecture: `legacy` vs `v2` Builds
Across the 403 meters, two distinct generation schemes exist:
* **`legacy` (238 meters):**
  On the detail page (`/meters/:id/__data.json`), legacy meters expose parameters as a key-value object array:
  ```json
  "detail": { "data": [{"parameterName": "Meter ID", "parameterValue": "J100000"}, ...] }
  ```
* **`v2` (165 meters):**
  v2 meters do **not** use the parameter array. Instead, they provide a single string field `classData` containing escaped JSON with PascalCase keys:
  ```json
  "detail": { "classData": "{\"installed_meter\":{\"MeterId\":\"J100004\",\"SerialNo\":\"SE65293\",\"Make\":\"Genus\",\"PhaseType\":\"single\",\"InstallationStatus\":\"Faulty\",\"InstallationType\":\"CT Operated\"}}" }
  ```
  The legacy frontend had to use a try/catch JSON parser specifically for this branch!

### 2. Coordinate Type Divergence
* In `/portal/meters/:id/geo`, coordinates are strings (`"26.9389..."`).
* In `/portal/export`, coordinates are floating-point doubles (`26.9389`).

### 3. Search Limitations in Original Portal
* The portal search endpoint only searches meter IDs or serial numbers. Attempting to filter by DT code or make through the search box yields zero results.
* The portal offers no ability to query by phase, status, or location.

### 4. Network Hierarchy Inconsistencies
* In `/meters/:id/__data.json`, hierarchy keys have inconsistent naming (e.g. `"Sub Station"` with a space vs `"Substation"` in export).
* Breadcrumbs in the detail page combine name and code as `"Jaipur Zone 1 (Z-01)"`, requiring regex extraction if parsing from the page rather than export.

---

## 6. How Our Service Solves This

Our Spring Boot API service abstracts away every single one of these legacy headaches:
1. **Automated Session Management:** Transparently negotiates Better-Auth login and satisfies CSRF origin checks.
2. **HMAC Bulk Synchronization:** Uses `/portal/keys` and `/portal/export` to pull the complete 403-meter dataset into a high-performance in-memory index on startup.
3. **Canonical Normalization:** Normalizes `legacy` and `v2` records into a uniform, typed `Meter` model.
4. **Advanced Query Engine:** Enables multi-field filtering (`make`, `phase`, `status`, `dtCode`) and geospatial proximity search (`nearLat`, `nearLng`, `radiusKm`) that the legacy portal was completely incapable of answering.
5. **Reconstructed Network Tree:** Exposes the full 7-level distribution hierarchy from Zone to DT with pre-aggregated meter counts.
