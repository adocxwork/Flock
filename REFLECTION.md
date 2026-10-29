# Engineering Reflection: Urja Meter Ops API

## 1. What assumptions did you make?
* **Single Tenant Utility Scope:** We assumed the portal instance corresponds to a single electric distribution utility (Jaipur Discom network) where the operator credentials provide full read-only operational visibility over all feeders and distribution transformers.
* **Bulk Export Completeness & Stability:** We observed that `/portal/export` reliably returns all 403 meters across the grid in one request regardless of `page=1`. We assumed this bulk path remains the most authoritative, low-overhead way to keep our API's query engine synchronized without hammering the legacy server with 400+ individual HTTP requests.
* **Smart Meter Reading Semantics:** We assumed active energy (`kWh`) readings represent cumulative registers that monotonically increase, making `latestKwh - firstKwh` an accurate representation of total consumed energy across the 7-day interval window. For voltage, nominal single-phase voltage was assumed to be 220V–240V, and values outside 205V–255V were treated as potential grid quality anomalies.

---

## 2. Which part was the most difficult, and how did you get unstuck?
The most challenging part of the investigation was **navigating the SvelteKit CSRF origin protection and reverse-engineering the HMAC-SHA256 signing mechanism for bulk exports**:

* **CSRF Origin Protection:** When initially attempting to log in via curl/HTTP client, the server returned `403 Forbidden: Cross-site POST form submissions are forbidden`. By inspecting SvelteKit’s form submission pipeline in `DHWw2r--.js`, we realized SvelteKit enforces strict `Origin` and `Referer` checks on form actions. Explicitly setting `Origin: https://urja-ops.flockenergy.tech` immediately unlocked seamless session authentication.
* **HMAC Request Signature:** When examining `4.2Bgc2kUI.js` (the Transformers component), we noticed the export button didn't simply download a static file; it queried `/portal/keys` for a dynamic `signingSecret` and invoked WebCrypto (`crypto.subtle`) to sign a newline-separated string `[method, path, queryString, timestamp].join('\n')`. Replicating this signature algorithm in Java using `Mac.getInstance("HmacSHA256")` allowed us to access the complete, clean dataset directly.

---

## 3. If you had another day, what would you improve?
* **Persistent Storage & Historical Trend Store:** Currently, the dataset is synchronized into high-performance in-memory indexes (ConcurrentHashMap) backed by Caffeine cache. With another day, we would integrate SQLite / PostgreSQL via Spring Data JPA and Flyway migrations to store multi-month interval time-series readings, enabling historical degradation tracking and seasonal consumption forecasting.
* **Dynamic Geofencing & PostGIS Spatial Queries:** While our current Haversine formula implementation provides instant distance calculation and radius filtering for hundreds of meters, scaling to millions of meters would require spatial R-trees or PostGIS spatial indexing (`ST_DWithin`).
* **Webhooks & Change Data Capture:** We would add webhook subscription endpoints (`/api/v1/webhooks`) so external services could receive immediate notifications whenever a meter enters a `Faulty` state or records an under-voltage anomaly.
* **Resilience Rate Limiting & Circuit Breakers:** Add Resilience4j circuit breakers to gracefully fall back to cached snapshots if the upstream legacy portal experiences downtime or network partitions.

---

## 4. What mistake did you make while solving this?
* **Early Search Assumption:** In our initial probe, we expected the `/portal/meters/search` endpoint to support standard multi-field query parameters like `?dtCode=DT-001` or `?make=HPL`. Testing confirmed that the portal's search backend only searches the literal meter ID and serial number strings. If you passed `DT-001`, it returned 0 results!
* **The Fix:** Rather than restricting our users to the legacy portal's limited search capabilities, we designed our service to ingest the full bulk dataset via the authenticated export endpoint and build a rich local in-memory index. This enabled multi-attribute filtering (make, phase, status, DT code) and spatial radius search with sub-millisecond query latency.

---

## 5. If you were reviewing your own submission, what would you criticise?
* **In-Memory Scale Boundary:** Storing all 403 meters in memory is lightning fast (<1ms latency) and appropriately sized for the 403 meters in this utility portal. However, if the dataset grew from hundreds of meters to 5 million smart meters across a state utility, holding all raw telemetry objects in JVM heap would lead to excessive garbage collection pressure. We should clearly document this architectural boundary and the transition path to a distributed datastore (e.g. Postgres + Redis).
* **Scheduled Polling vs Real-Time Push:** Because the legacy portal does not provide WebSockets or event webhooks, our sync mechanism polls every 15 minutes. A potential critique is that changes made in the legacy portal in between sync cycles might have a minor staleness window, though users can always trigger an on-demand sync via `POST /api/v1/sync`.
