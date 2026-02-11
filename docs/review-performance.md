# Performance Review Report

**Project:** WeGo
**Date:** 2026-02-10
**Reviewer:** perf-reviewer (Claude Opus 4.6)

---

## Executive Summary

The WeGo codebase demonstrates **good foundational performance awareness** with several well-implemented patterns: flat entity design avoiding JPA lazy-loading pitfalls, batch place lookups in `ActivityService` to prevent N+1 queries, Caffeine caching for statistics, two-tier caching for exchange rates, circuit breaker on external API calls, Bucket4j rate limiting, and `open-in-view: false`.

However, there are **significant performance issues** in high-traffic paths: the `getUserTrips` method triggers N+1 queries for member summaries, several high-frequency query columns lack database indexes, external API clients each create their own `RestTemplate` instead of sharing a properly-configured bean, static assets have no Cache-Control headers, Thymeleaf caching is disabled in production config, and the in-memory `CacheService`/`RateLimitService` lack eviction strategies that could lead to memory leaks under heavy use.

**Summary Statistics:**
- Critical: 2
- Warning: 8
- Suggestion: 6
- Total: 16

---

## 1. Database & Query Performance

### 1.1 N+1 Query in `TripService.getUserTrips`

- **Severity:** RED Critical
- **File:** `/Users/mark/WeGo/src/main/java/com/wego/service/TripService.java:185-195`
- **Description:** `getUserTrips()` calls `getMemberSummaries(trip.getId())` inside a `.map()` on every trip in the paginated result. Each call to `getMemberSummaries` triggers 2 queries: one for `findByTripId` (TripMember) and one for `findAllById` (User). For a page of 20 trips, this produces **40+ additional queries**.
- **Recommendation:** Create a custom repository method that batch-fetches all `TripMember` rows for the trip IDs in the page, and batch-fetches all referenced `User` entities in a single query. Then assemble member summaries in-memory.

### 1.2 N+1 Query in `SettlementService.calculateSettlement`

- **Severity:** YELLOW Warning
- **File:** `/Users/mark/WeGo/src/main/java/com/wego/service/SettlementService.java:111-181`
- **Description:** `calculateSettlement` loads all expenses and splits in 2 queries, which is efficient. However, each `convertToBaseCurrency` call may trigger an individual API call to `ExchangeRateService.getRate` per unique currency pair. With N different currencies, this produces N external API calls (mitigated by in-memory caching but not batched).
- **Recommendation:** Pre-fetch all needed exchange rates in one batch before iterating expenses.

### 1.3 Missing Database Indexes

- **Severity:** RED Critical
- **File:** Database schema (entity annotations, no JPA `@Index` annotations found)
- **Description:** The design document lists intended indexes, but since the project uses `ddl-auto: update`, JPA only creates indexes from entity annotations. The following high-frequency query columns lack `@Table(indexes=...)` annotations:
  - `activities.trip_id` + `day` + `sort_order` (used by every activity query)
  - `expenses.trip_id` + `created_at` (used by every expense listing)
  - `expense_splits.expense_id` (used by settlement calculation)
  - `expense_splits.user_id` + `is_settled` (used by settlement queries)
  - `todos.trip_id` + `status` + `due_date` (used by todo listing)
  - `documents.trip_id` + `created_at` (used by document listing)
  - `trip_members.trip_id` + `user_id` (unique constraint exists but may not be an index)
  - `invite_links.token` (unique constraint exists but should verify)
- **Recommendation:** Add `@Table(indexes = @Index(...))` annotations to all entity classes for the columns listed above, or create a Flyway/Liquibase migration. Without these indexes, every list query on trips with many records will do full table scans.

### 1.4 Unbounded List Queries

- **Severity:** YELLOW Warning
- **File:** Multiple repositories
- **Description:** Several repository methods return `List<>` without pagination:
  - `ActivityRepository.findByTripIdOrderByDayAscSortOrderAsc` (all activities for a trip)
  - `ExpenseRepository.findByTripIdOrderByCreatedAtDesc` (all expenses for a trip)
  - `ExpenseSplitRepository.findByTripId` (all splits for a trip)
  - `TodoRepository.findByTripIdOrderedByDueDateAndStatus` (all todos)
  - `DocumentRepository.findByTripIdOrderByCreatedAtDesc` (all documents)
- **Recommendation:** For display-facing methods, add `Pageable` support. The paginated variants exist for some (Expense, Todo) but the service layer often calls the unbounded version. For internal calculations (settlement), unbounded is acceptable since trip data is naturally bounded by member limits.

### 1.5 `deleteByTripId` Cascade in `TripService.deleteTrip`

- **Severity:** YELLOW Warning
- **File:** `/Users/mark/WeGo/src/main/java/com/wego/service/TripService.java:272-322`
- **Description:** Trip deletion executes 9 sequential database operations: deleting expense splits, expenses, fetching+deleting documents (with storage calls), activities, todos, invite links, members, cover image, and the trip itself. The document deletion also iterates each document individually for storage cleanup.
- **Recommendation:** Use `@Modifying` bulk delete queries (already done for `ExpenseSplitRepository.deleteByTripId`). Consider making the storage cleanup asynchronous since it involves external HTTP calls. The current approach could hold a transaction open for an extended period.

---

## 2. Caching

### 2.1 No Caching on `TripService.getTrip`

- **Severity:** YELLOW Warning
- **File:** `/Users/mark/WeGo/src/main/java/com/wego/service/TripService.java:157-170`
- **Description:** `getTrip` is called on every trip detail page view but has no caching. It queries the trip, counts members, checks permissions, and fetches member summaries -- all requiring 3+ queries per request.
- **Recommendation:** Add `@Cacheable` for trip data with eviction on update/delete. The permission check should remain outside the cache (as done correctly in `StatisticsService`).

### 2.2 No Caching on `PermissionChecker`

- **Severity:** YELLOW Warning
- **File:** `/Users/mark/WeGo/src/main/java/com/wego/domain/permission/PermissionChecker.java` (referenced throughout)
- **Description:** `PermissionChecker.canView/canEdit/canDelete` are called on almost every request. Each call queries `TripMemberRepository.findByTripIdAndUserId`. For a single page load showing trip details with activities, expenses, and documents, the same permission check may be called 4+ times with the same parameters.
- **Recommendation:** Add a short-lived (30s) cache for `TripMember` lookup by tripId+userId, or use request-scoped caching with `@RequestScope`.

### 2.3 CacheService Memory Leak Risk

- **Severity:** YELLOW Warning
- **File:** `/Users/mark/WeGo/src/main/java/com/wego/service/CacheService.java:22`
- **Description:** The custom `CacheService` uses a `ConcurrentHashMap` with no maximum size limit. Expired entries are only cleaned on `get()` or `size()` calls, not proactively. Under heavy use with many unique cache keys (e.g., weather for many locations), this map will grow unbounded until expired entries are accessed.
- **Recommendation:** Replace with the existing Caffeine cache infrastructure (already used for statistics). Caffeine handles eviction, max size, and async cleanup automatically. Alternatively, add a scheduled cleanup task.

### 2.4 Dual Caching Systems

- **Severity:** BLUE Suggestion
- **File:** `CacheService.java`, `CacheConfig.java`, `ExchangeRateService.java`
- **Description:** The project has three separate caching systems: (1) Spring Cache with Caffeine via `CacheConfig` for statistics, (2) custom `CacheService` with ConcurrentHashMap for weather, (3) manual ConcurrentHashMap caching in `ExchangeRateService`. This increases maintenance complexity and inconsistency.
- **Recommendation:** Consolidate on Spring Cache + Caffeine for all caching needs. Define additional cache names in `CacheConfig` for weather (6h TTL) and exchange rates (1h TTL, 24h fallback).

---

## 3. External API Calls

### 3.1 No `@Async` Usage for External API Calls

- **Severity:** YELLOW Warning
- **File:** Project-wide (grep for `@Async` returned no results)
- **Description:** All external API calls (Google Maps, OpenWeatherMap, ExchangeRate, Supabase Storage) are synchronous and block the request thread. The `batchRecalculateWithRateLimit` method in `TransportCalculationService` even includes `Thread.sleep(100)` between API calls, blocking the thread pool.
- **Recommendation:** For non-critical external calls (storage cleanup on trip delete, transport recalculation), use `@Async` with a dedicated thread pool. Add `@EnableAsync` configuration. The transport recalculation endpoint should return immediately and process in background.

### 3.2 Each External Client Creates Its Own `RestTemplate`

- **Severity:** BLUE Suggestion
- **File:** `GoogleMapsClientImpl.java:110-115`, `OpenWeatherMapClient.java:81-86`, `ExchangeRateApiClient.java:95-100`, `SupabaseStorageClient.java:50`
- **Description:** Each external client creates its own `RestTemplate` with `SimpleClientHttpRequestFactory`. This means no connection pooling is shared between clients, and each creates new TCP connections per request. `SupabaseStorageClient` creates a bare `new RestTemplate()` with no timeouts configured.
- **Recommendation:** Define a shared `RestTemplate` bean (or use `RestTemplateBuilder`) with connection pooling (Apache HttpClient or OkHttp), default timeouts, and proper connection management. Per-client timeout overrides can be applied at the request level.

### 3.3 Google Maps API Has No Response Caching

- **Severity:** BLUE Suggestion
- **File:** `/Users/mark/WeGo/src/main/java/com/wego/service/TransportCalculationService.java:186-241`
- **Description:** `calculateTransportWithWarnings` calls Google Maps API for every transport calculation. The same origin-destination pair may be queried multiple times (e.g., after reordering and then reverting). Google Maps API calls cost money per request.
- **Recommendation:** Cache direction results by `originLat:originLng:destLat:destLng:mode` key with a 24h TTL. This is especially valuable since coordinates don't change for existing places.

### 3.4 Circuit Breaker Only on ExchangeRate

- **Severity:** BLUE Suggestion
- **File:** `ExchangeRateApiClient.java:249-284`
- **Description:** The circuit breaker pattern is only implemented for `ExchangeRateApiClient`. `GoogleMapsClientImpl` and `OpenWeatherMapClient` have no circuit breaker, so repeated failures will keep attempting API calls.
- **Recommendation:** Add circuit breaker logic to `GoogleMapsClientImpl` and `OpenWeatherMapClient`, or use a library like Resilience4j which provides circuit breakers, retries, and rate limiters as cross-cutting concerns.

---

## 4. JPA & Hibernate Configuration

### 4.1 `hibernate.ddl-auto: update` in Production Config

- **Severity:** YELLOW Warning
- **File:** `/Users/mark/WeGo/src/main/resources/application.yml:28`
- **Description:** `spring.jpa.hibernate.ddl-auto: update` is set in the main `application.yml` with no profile override. This means Hibernate will attempt schema modifications on every production startup, which can cause issues with concurrent deployments and is generally dangerous for production data.
- **Recommendation:** Set to `validate` or `none` for production. Use Flyway or Liquibase for migration management. Keep `update` only in dev/test profiles.

### 4.2 SQL Logging Enabled by Default

- **Severity:** BLUE Suggestion
- **File:** `/Users/mark/WeGo/src/main/resources/application.yml:92-93`
- **Description:** `org.hibernate.SQL: DEBUG` and `BasicBinder: TRACE` are enabled in the main config. This generates significant log volume in production and can impact I/O performance.
- **Recommendation:** Move these to a `application-dev.yml` profile. In production, only enable on-demand for debugging.

---

## 5. Frontend & Static Assets

### 5.1 No Cache-Control Headers for Static Resources

- **Severity:** YELLOW Warning
- **File:** `/Users/mark/WeGo/src/main/java/com/wego/config/WebConfig.java:40-50`
- **Description:** Static resource handlers for `/css/**`, `/js/**`, `/images/**` are configured without any cache period. Every page load will re-request all CSS, JS, and image files from the server, significantly increasing page load times and server load.
- **Recommendation:** Add `.setCachePeriod(86400)` (1 day) or `.setCachePeriod(604800)` (1 week) to resource handlers, combined with content hashing in filenames for cache busting.

### 5.2 Thymeleaf Cache Disabled

- **Severity:** BLUE Suggestion
- **File:** `/Users/mark/WeGo/src/main/resources/application.yml:40`
- **Description:** `spring.thymeleaf.cache: false` is set in the main config. This means every template rendering re-parses the HTML file from disk. While useful for development, this should be `true` in production.
- **Recommendation:** Set to `true` in production (default) and only override to `false` in `application-dev.yml`.

---

## 6. Connection Pool & Session

### 6.1 HikariCP Pool Size

- **Severity:** BLUE Suggestion
- **File:** `/Users/mark/WeGo/src/main/resources/application.yml:19-23`
- **Description:** HikariCP is configured with `maximum-pool-size: 10`, `minimum-idle: 2`. This is reasonable for a small deployment. However, with the N+1 query issues identified above, a single request can consume a connection for an extended period (especially `getUserTrips` with 40+ queries).
- **Recommendation:** After fixing N+1 issues, the pool size is appropriate. If concurrent users grow beyond ~50, consider increasing to 15-20. Monitor with HikariCP metrics.

---

## Positive Findings

The following patterns are well-implemented and should be maintained:

1. **Flat entity design (no `@OneToMany`/`@ManyToOne`)** -- eliminates lazy-loading N+1 at the JPA level and gives full control over query patterns.
2. **`open-in-view: false`** -- prevents accidental lazy loading in view layer.
3. **Batch place lookup in `ActivityService.buildPlaceLookup`** -- correctly uses `findAllById` to prevent N+1 when mapping activities to responses.
4. **`@Transactional(readOnly = true)`** on read methods -- enables Hibernate flush-mode optimizations.
5. **Caffeine cache with `recordStats()`** -- enables monitoring cache hit rates.
6. **Statistics caching with proper eviction** -- `StatisticsService` caches computed results and evicts on data changes.
7. **Exchange rate two-tier caching** -- primary + fallback cache for resilience.
8. **Circuit breaker on ExchangeRate API** -- prevents cascading failures.
9. **Bucket4j rate limiting with Caffeine-backed IP tracking** -- properly bounded with max size and TTL.
10. **`@Modifying` bulk delete for `ExpenseSplitRepository.deleteByTripId`** -- avoids loading entities just to delete them.
11. **Pagination support in repositories** -- `Pageable` variants exist for trip and expense queries.
12. **`Document.countByTripIds` batch query** -- explicitly designed to avoid N+1 for global document overview.

---

## Priority Action Items

| Priority | Issue | Impact |
|----------|-------|--------|
| 1 | Add database indexes (1.3) | All list queries will be slow without indexes |
| 2 | Fix N+1 in `getUserTrips` (1.1) | Dashboard/trip list is highest-traffic page |
| 3 | Add Cache-Control for static assets (5.1) | Reduces server load and improves page load time |
| 4 | Cache PermissionChecker lookups (2.2) | Reduces 4+ redundant queries per page load |
| 5 | Enable Thymeleaf caching for production (5.2) | Free performance win |
| 6 | Consolidate caching systems (2.4) | Reduces memory leak risk and maintenance burden |
| 7 | Add `@Async` for background operations (3.1) | Prevents thread blocking on external calls |
| 8 | Fix `ddl-auto: update` for production (4.1) | Prevents schema corruption risk |
