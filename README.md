# Urja Meter Ops REST API Service

> A clean, production-ready, documented Spring Boot REST API service and operational web client built over the legacy **Urja Meter Ops** utility portal (`https://urja-ops.flockenergy.tech`).

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot 3.4](https://img.shields.io/badge/Spring_Boot-3.4.2-green.svg)](https://spring.io/projects/spring-boot)
[![OpenAPI 3.1](https://img.shields.io/badge/OpenAPI-3.1.0-blue.svg)](./openapi.json)
[![Tests Passing](https://img.shields.io/badge/Tests-14%20Passing-brightgreen.svg)]()

---

## Quick Navigation

* 📖 **[PROTOCOL.md](./PROTOCOL.md)** — Comprehensive write-up of how Urja Meter Ops actually works under the hood: auth, SvelteKit internals, CSRF protection, endpoints, hidden HMAC-SHA256 signing, and legacy data quirks.
* 📄 **[openapi.json](./openapi.json)** — Full OpenAPI 3.1.0 specification of this API.
* 💭 **[REFLECTION.md](./REFLECTION.md)** — Answers to all evaluation reflection prompts (assumptions, toughest parts, mistakes, self-critique).
* 🌐 **Interactive Dashboard & Swagger UI** — Built into the service at `http://localhost:8080` and `http://localhost:8080/swagger-ui.html`.

---

## 1. Executive Summary & What Was Built

The legacy **Urja Meter Ops** portal is an internal utility application used by field and operations staff to monitor smart meters, grid distribution transformers (DTs), and electricity consumption. While usable in a desktop browser, **it offers no programmatic API**, forcing data and product teams into manual copy-pasting.

We reverse-engineered the portal and built **a unified, resilient API wrapper and query service** in **Java 21 & Spring Boot 3.4** that provides:

1. **Clean, Strongly-Typed REST API**:
   * **Smart Meters API** (`/api/v1/meters`): Paginated listing, full-text search, and multi-attribute filtering (by make, phase, status, DT code).
   * **Geospatial Proximity Search** (`/api/v1/meters?nearLat=...&nearLng=...&radiusKm=...`): Answers *"what meters are near this location?"* using Haversine spherical distance calculations.
   * **Meter Deep Dive & Telemetry** (`/api/v1/meters/{id}` & `/api/v1/meters/{id}/consumption`): Canonical nameplate data, geographical coordinates, breadcrumbs, and normalized 30-minute interval readings with voltage anomaly analytics.
   * **Distribution Transformers** (`/api/v1/transformers`): Transformer loading metrics, feeder associations, and connected meter inventories.
   * **Network Topology Tree** (`/api/v1/hierarchy`): Reconstructed 7-level distribution grid hierarchy from Zone down to DT and meters.
   * **Grid Health Analytics** (`/api/v1/analytics/overview`): Real-time fleet health score %, make breakdown, phase distribution, and status breakdown.
2. **Automated Session & HMAC Signing Engine**:
   * Manages Better-Auth session tokens (`__Secure-better-auth.session_token`).
   * Automatically satisfies SvelteKit CSRF origin checks.
   * Retrieves signing secrets from `/portal/keys` and computes dynamic HMAC-SHA256 signatures for authenticated bulk data exports.
3. **High-Performance In-Memory Index & Caching**:
   * Proactively synchronizes all 403 meters on startup via HMAC bulk export.
   * Serves queries in **<1ms** with Caffeine caching.
   * Graceful fallback to live portal endpoints if sync is in progress.
4. **Modern Embedded Web Client** (Optional Extension):
   * A responsive SPA served directly from `http://localhost:8080`.
   * Interactive **Leaflet.js map** with status-coded meter markers.
   * **Chart.js** 7-day energy & voltage telemetry charts.
   * Interactive collapsible tree view of the 7-level network hierarchy.

---

## 2. Project Architecture & Code Structure

```
Flock/
├── pom.xml                                   # Maven configuration (Java 21, Spring Boot 3.4.2)
├── mvnw / mvnw.cmd                           # Self-contained Maven Wrapper
├── openapi.json                              # Exported OpenAPI 3.1 specification
├── PROTOCOL.md                               # Reverse-engineering protocol & discoveries
├── REFLECTION.md                             # Reflection & self-critique
├── README.md                                 # Front door documentation
├── src/
│   ├── main/
│   │   ├── java/com/flockenergy/urja/
│   │   │   ├── UrjaMeterOpsApplication.java  # Application entry point
│   │   │   ├── client/                       # Upstream portal integration
│   │   │   │   ├── UrjaPortalClient.java     # Interface for portal calls
│   │   │   │   ├── UrjaPortalClientImpl.java # HTTP client with retry & error handling
│   │   │   │   ├── UrjaSessionManager.java   # Better-Auth cookie & CSRF management
│   │   │   │   └── UrjaHmacSigner.java       # HMAC-SHA256 export request signer
│   │   │   ├── config/                       # Properties, Cache, OpenAPI, Web MVC
│   │   │   ├── controller/                   # REST API controllers
│   │   │   │   ├── MeterController.java      # /api/v1/meters
│   │   │   │   ├── TransformerController.java# /api/v1/transformers
│   │   │   │   ├── HierarchyController.java  # /api/v1/hierarchy
│   │   │   │   ├── AnalyticsController.java  # /api/v1/analytics/overview
│   │   │   │   └── SyncController.java       # /api/v1/sync
│   │   │   ├── dto/                          # Raw upstream response DTOs
│   │   │   ├── exception/                    # RFC 7807 ProblemDetail exception handler
│   │   │   ├── model/                        # Canonical domain & response models
│   │   │   └── service/                      # Business logic, query engine, tree builder
│   │   └── resources/
│   │       ├── application.yml               # Service configuration & defaults
│   │       └── static/
│   │           └── index.html                # Modern single-page web dashboard
│   └── test/java/com/flockenergy/urja/       # Unit & integration test suite (14 tests)
```

---

## 3. How to Install, Configure, and Run

### Prerequisites
* **Java 21** or higher (`java -version`).
* Maven is optional (the repository includes the Maven Wrapper `./mvnw`).

### 3.1 Running Locally
Clone the repository and run:

```bash
# 1. Run all unit and integration tests
./mvnw clean test

# 2. Start the API service
./mvnw spring-boot:run
```

The application will start on **`http://localhost:8080`**.  
On startup, it will automatically authenticate with the Urja portal, download the complete 403-meter dataset via HMAC bulk export, and index the entire fleet.

### 3.2 Configuration Options
Configuration parameters can be overridden via `application.yml` or standard environment variables:

| Environment Variable | Description | Default Value |
| :--- | :--- | :--- |
| `PORT` or `SERVER_PORT` | HTTP port for the API | `8080` |
| `URJA_PORTAL_URL` | Base URL of the Urja Portal | `https://urja-ops.flockenergy.tech` |
| `URJA_PORTAL_USERNAME`| Operator login email | `operator@urja.local` |
| `URJA_PORTAL_PASSWORD`| Operator login password | `urja-ops-2026` |

Example running on a custom port:
```bash
PORT=9090 ./mvnw spring-boot:run
```

---

## 4. API Endpoints & Sample Requests

### 4.1 Query & Filter Smart Meters
**`GET /api/v1/meters`**

Filter by make, phase, installation status, DT code, text search, or geospatial radius:

```bash
# Find all three-phase Faulty meters made by Genus
curl -s "http://localhost:8080/api/v1/meters?make=Genus&phase=three&status=Faulty" | jq .

# Geospatial radius search: find all smart meters within 5 km of Jaipur center (26.9389, 75.8309)
curl -s "http://localhost:8080/api/v1/meters?nearLat=26.9389&nearLng=75.8309&radiusKm=5.0" | jq .
```

**Sample Response:**
```json
{
  "content": [
    {
      "meterId": "J100000",
      "serialNo": "SE33962",
      "make": "HPL",
      "phaseType": "single",
      "installStatus": "Decommissioned",
      "installType": "Whole Current",
      "build": "legacy",
      "dtCode": "DT-001",
      "geo": {
        "lat": 26.938961002479868,
        "lng": 75.83095696146852
      },
      "hierarchy": {
        "zone": { "name": "Jaipur Zone 1", "code": "Z-01" },
        "circle": { "name": "Circle 1", "code": "C-01" },
        "division": { "name": "Division 1", "code": "D-01" },
        "subdivision": { "name": "Subdivision 1", "code": "SD-01" },
        "substation": { "name": "Substation 1", "code": "SS-01" },
        "feeder": { "name": "Feeder 1", "code": "F-001" },
        "dt": { "name": "Malviya Nagar DT 1", "code": "DT-001" },
        "formattedBreadcrumb": "Jaipur Zone 1 › Circle 1 › Division 1 › Subdivision 1 › Substation 1 › Feeder 1 › Malviya Nagar DT 1"
      }
    }
  ],
  "page": 1,
  "size": 1,
  "totalElements": 403,
  "totalPages": 403,
  "isFirst": true,
  "isLast": false
}
```

---

### 4.2 Meter Detail & Consumption Analytics
**`GET /api/v1/meters/{id}`**

```bash
curl -s "http://localhost:8080/api/v1/meters/J100000" | jq .
```

**Sample Response:**
```json
{
  "meter": {
    "meterId": "J100000",
    "serialNo": "SE33962",
    "make": "HPL",
    "phaseType": "single",
    "installStatus": "Decommissioned",
    "dtCode": "DT-001"
  },
  "consumptionSummary": {
    "readingCount": 337,
    "firstTimestamp": "23/06/2026 23:30",
    "latestTimestamp": "30/06/2026 23:30",
    "firstKwh": 48438.74,
    "latestKwh": 48580.79,
    "totalEnergyConsumedKwh": 142.05,
    "latestKvah": 52467.25,
    "latestVoltR": 220.0,
    "minVoltage": 220.0,
    "maxVoltage": 240.0,
    "avgVoltage": 229.8,
    "voltageStatus": "NORMAL",
    "anomalyFlags": []
  },
  "readings": [
    {
      "timestamp": "23/06/2026 23:30",
      "isoTimestamp": "2026-06-23T23:30:00",
      "kwh": 48438.74,
      "kvah": 52313.84,
      "voltR": 226.0
    }
  ]
}
```

---

### 4.3 Distribution Transformers
**`GET /api/v1/transformers`** and **`GET /api/v1/transformers/{code}`**

```bash
curl -s "http://localhost:8080/api/v1/transformers/DT-001" | jq .
```

**Sample Response:**
```json
{
  "transformer": {
    "code": "DT-001",
    "name": "Malviya Nagar DT 1",
    "feederCode": "F-001",
    "capacityKva": 100,
    "meterCount": 10,
    "installedMeters": 3,
    "faultyMeters": 3,
    "decommissionedMeters": 4,
    "status": "ATTENTION_REQUIRED"
  },
  "connectedMeters": [
    { "meterId": "J100000", "make": "HPL", "installStatus": "Decommissioned" }
  ]
}
```

---

### 4.4 Reconstructed Network Hierarchy Tree
**`GET /api/v1/hierarchy`**

```bash
curl -s "http://localhost:8080/api/v1/hierarchy" | jq '.[0] | {id, name, level, meterCount}'
```

```json
{
  "id": "Z-01",
  "name": "Jaipur Zone 1",
  "level": "ZONE",
  "meterCount": 143
}
```

---

### 4.5 Operational Overview & KPIs
**`GET /api/v1/analytics/overview`**

```bash
curl -s "http://localhost:8080/api/v1/analytics/overview" | jq .
```

```json
{
  "totalMeters": 403,
  "installedMeters": 238,
  "faultyMeters": 90,
  "decommissionedMeters": 75,
  "healthScorePercentage": 72.6,
  "totalTransformers": 40,
  "totalCapacityKva": 8601,
  "makeDistribution": {
    "HPL": 90,
    "Genus": 88,
    "Secure": 85,
    "Allied": 76,
    "L&T": 64
  },
  "phaseDistribution": {
    "single": 277,
    "three": 126
  }
}
```

---

## 5. Assumptions, Design Decisions & Trade-Offs

### 1. In-Memory Indexing via Bulk Export vs N+1 Upstream Calls
* **The Problem:** The portal has 403 meters across 21 pages. Fetching meter details, coordinates, and hierarchy individually via `GET /meters/:id` would require over 1,200 separate HTTP calls, taking over 60 seconds and placing severe load on the legacy portal.
* **Our Decision:** We leveraged the discovered `/portal/export` endpoint with cryptographic HMAC-SHA256 signing to retrieve the entire grid dataset in a single call. We store this in concurrent, thread-safe in-memory maps (`ConcurrentHashMap`).
* **Trade-Off:** Provides sub-millisecond query latency and enables spatial radius searches that the portal cannot do. The trade-off is memory consumption (under 5 MB for 403 meters in JVM heap). For 5 million meters, this would be backed by PostgreSQL / Redis.

### 2. Dual Schema Normalization (`legacy` vs `v2`)
* In the portal, 238 meters use a legacy key-value array format (`data: [{parameterName, parameterValue}]`), while 165 meters use `v2` with an escaped JSON string (`classData: "{\"installed_meter\":{...}}"`).
* **Our Decision:** Our service abstracts this legacy duality away completely. External API consumers receive a clean, strongly-typed `Meter` object with uniform attributes regardless of which generation the physical hardware belongs to.

### 3. Session Resilience & Sliding Expiration
* Better-Auth cookies expire after 1 hour (`Max-Age=3600`).
* **Our Decision:** `UrjaSessionManager` tracks expiration proactively, refreshing the session if it is within 5 minutes of expiry. Furthermore, `UrjaPortalClientImpl` implements transparent retry: if any call receives a `401 Unauthorized`, it invalidates the cached session token, re-authenticates, and retries the request once before propagating errors.

---

## 6. What Was Intentionally Left Out

* **Write / Mutation Endpoints:** The assignment explicitly stated *"Treat the portal as read-only. You don't need to (and shouldn't try to) attack or break it"*. Consequently, we intentionally did not expose endpoints to modify meter parameters or simulate meter decommissioning upstream.
* **Heavy External Database:** To ensure the project can be cloned and run immediately by any reviewer with zero configuration, we intentionally omitted external database dependencies (like MySQL or Oracle) in favor of high-performance in-memory indexing with Caffeine cache.

---

## 7. What We'd Improve With More Time

1. **Persistent Historical Timeseries Store:** Integrate PostgreSQL + TimescaleDB to persist interval consumption readings beyond the 7-day sliding window.
2. **PostGIS Spatial R-Tree:** Transition from in-memory Haversine distance calculation to PostGIS `ST_DWithin` queries for scaling to millions of smart meters.
3. **Webhook Subscriptions:** Expose `/api/v1/webhooks` to alert upstream dispatch systems when a meter enters a `Faulty` status or experiences voltage drop below 205V.
4. **Resilience4j Circuit Breakers:** Add formal circuit breaker metrics and fallback telemetry snapshots.

---

## 8. Verification & Running the Tests

All unit and integration tests can be run via:
```bash
./mvnw clean test
```

### Test Suite Summary:
* `UrjaHmacSignerTest`: Validates HMAC-SHA256 generation, hex encoding, and deterministic hashing.
* `GeoLocationTest`: Tests coordinate validation and Haversine distance calculations.
* `ConsumptionSummaryTest`: Tests interval reading consumption aggregation, voltage statistics, and under-voltage anomaly detection.
* `MeterServiceTest`: Tests multi-criteria filtering (make, phase, status, DT code) and spatial radius sorting.
* `UrjaMeterOpsApplicationTests`: Full Spring Boot integration test validating OpenAPI docs and sync status endpoints.

---

## 9. Deliverables Map

* **Source Code:** Under `/src/main/java/com/flockenergy/urja/`
* **README:** [README.md](./README.md)
* **PROTOCOL:** [PROTOCOL.md](./PROTOCOL.md)
* **OpenAPI 3.1 Spec:** [openapi.json](./openapi.json)
* **Reflection & Review Questions:** [REFLECTION.md](./REFLECTION.md)
* **Web Client Dashboard:** Located at `src/main/resources/static/index.html` (served at `http://localhost:8080`)
