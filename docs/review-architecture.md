# Architecture Review Report

**Project:** WeGo - Travel Planning Platform
**Date:** 2026-02-10
**Reviewer:** arch-reviewer (automated)
**Scope:** Controller/Service/Repository layering, DI, Entity/DTO separation, exception handling, SOLID/DRY, naming conventions

---

## Executive Summary

The WeGo project demonstrates a well-structured Spring Boot application with clear layering (Controller -> Service -> Repository), centralized permission checking (`PermissionChecker`), and unified exception handling for both API and Web controllers. The codebase follows consistent naming conventions and makes good use of DTOs to avoid entity leakage.

Key concerns:
- **TripController.java is 1664 lines** -- the largest controller and a DRY/SRP violation hotspot
- **Web controllers directly access repositories**, bypassing the service layer in several places
- **Duplicated `getCurrentUser()` method** across 8+ web controllers
- **Duplicated permission-checking boilerplate** (find member, check role) in web controllers instead of delegating to services
- **Inconsistent auth patterns** between web controllers (OAuth2User + email lookup) and API controllers (UserPrincipal + @CurrentUser)

---

## Detailed Findings

### 1. Layering Violations

#### 1.1 Controller directly accesses Repository (bypasses Service layer)

**Severity:** :red_circle: Critical

| File | Line | Repository Used | Should Use |
|------|------|-----------------|------------|
| `TripController.java` | 75 | `PlaceRepository` injected | `PlaceService` or `ActivityService` |
| `TripController.java` | 1085-1100 | `placeRepository.findByGooglePlaceId()`, `placeRepository.save()` | Service method |
| `TripController.java` | 1306-1328 | Same pattern in `updateActivity` | Service method |
| `ExpenseWebController.java` | 61 | `TripMemberRepository` injected | `TripService.getTripMembers()` |
| `InviteController.java` | 48-52 | `InviteLinkRepository`, `TripRepository`, `TripMemberRepository` injected | `InviteLinkService`, `TripService` |
| `ProfileController.java` | 37-39 | `TripMemberRepository`, `DocumentRepository`, `ExpenseRepository` | `ProfileService` or aggregate in `UserService` |

**Description:** Web controllers directly inject and call repositories, violating the Controller -> Service -> Repository layering principle. The `TripController` creates and saves `Place` entities directly (lines 1082-1106, 1304-1328), which is business logic that belongs in the service layer.

**Recommendation:** Extract Place creation/lookup into `ActivityService` or a new `PlaceService`. Extract profile statistics into `UserService.getProfileStats()`. Remove direct repository dependencies from controllers.

---

#### 1.2 Business logic in Controller layer

**Severity:** :red_circle: Critical

| File | Lines | Description |
|------|-------|-------------|
| `TripController.java` | 270-278 | Trip duration calculation (`ChronoUnit.DAYS.between`) |
| `TripController.java` | 280-284 | Days-until-trip calculation |
| `TripController.java` | 306-313 | Average expense per member calculation |
| `TripController.java` | 436-474 | Weather coordinate extraction with priority logic |
| `TripController.java` | 523-548 | Activity grouping by date + sorting |
| `TripController.java` | 752-764 | Expense grouping by date with TreeMap |
| `TripController.java` | 770-773 | Per-person expense average calculation |
| `TripController.java` | 1108-1158 | Activity form parsing: date calculation, transport validation, manual minutes validation |
| `ExpenseWebController.java` | 550-625 | `buildSplits()` - expense split creation logic |
| `SettlementWebController.java` | 100-107 | Per-person average calculation |
| `HomeController.java` | 72-76 | `daysUntil` calculation with entity mutation (`trip.setDaysUntil`) |

**Description:** Controllers contain significant business logic including date calculations, data grouping, financial calculations, and validation. This logic should reside in the service layer for reusability and testability.

**Recommendation:** Move grouping/calculation logic into service methods. For example, `TripService.getTripDetailData(tripId, userId)` could return a rich DTO with pre-computed `tripDays`, `tripNights`, `daysUntil`, `averageExpense`, etc.

---

### 2. DRY Violations

#### 2.1 Duplicated `getCurrentUser()` method

**Severity:** :yellow_circle: Warning

Identical or near-identical `getCurrentUser()` methods exist in **8 web controllers**:

| File | Lines |
|------|-------|
| `TripController.java` | 1657-1663 |
| `ExpenseWebController.java` | 661-667 |
| `InviteController.java` | 175-181 |
| `ProfileController.java` | 160-174 |
| `SettlementWebController.java` | 128-134 |
| `TodoWebController.java` | 123-134 |
| `GlobalExpenseController.java` | 80-94 |
| `GlobalDocumentController.java` | 112-126 |

**Description:** Each web controller has its own copy of `getCurrentUser(principal)` that extracts email from `OAuth2User` and calls `userService.getUserByEmail()`. Some variants add null/exception handling, others don't.

**Recommendation:** Extract into a shared base class (e.g., `BaseWebController`) or a utility class (e.g., `WebAuthHelper`). Alternatively, create a custom `@CurrentWebUser` annotation resolver that directly resolves `User` from the principal.

---

#### 2.2 Duplicated permission check boilerplate

**Severity:** :yellow_circle: Warning

The pattern of finding the current member and checking edit/owner role is duplicated across multiple methods in `TripController`:

```java
TripResponse.MemberSummary currentMember = trip.getMembers().stream()
    .filter(m -> m.getUserId().equals(user.getId()))
    .findFirst()
    .orElse(null);
boolean canEdit = currentMember != null &&
    (currentMember.getRole() == Role.OWNER ||
     currentMember.getRole() == Role.EDITOR);
```

Found in: `showTripDetail` (line 253), `showActivities` (line 509), `showActivityDetail` (line 610), `showMembersPage` (line 678), `showActivityCreateForm` (line 1001), `showActivityEditForm` (line 1206), `duplicateActivity` (line 916), `createActivity` (line 1070), `updateActivity` (line 1293), `showEditForm` (line 1499), `updateTrip` (line 1557).

Also in `TodoWebController.java` (line 92) and `ExpenseWebController.java` (lines 101, 634-653 as helper methods).

**Recommendation:** `ExpenseWebController` already has extracted `findCurrentMember()` and `canEdit()` helpers. Promote these to the base class or a shared utility. Even better, have the service layer return a view-model that includes the user's permission level.

---

#### 2.3 Duplicated trip-fetch-and-null-check pattern

**Severity:** :yellow_circle: Warning

Nearly every web controller method repeats:
```java
TripResponse trip;
try {
    trip = tripService.getTrip(id, user.getId());
} catch (Exception e) {
    return "redirect:/dashboard?error=trip_not_found";
}
if (trip == null) {
    return "redirect:/dashboard?error=trip_not_found";
}
```

This appears in **15+ methods** across `TripController`, `ExpenseWebController`, `TodoWebController`, `SettlementWebController`.

**Recommendation:** Extract into a shared helper or use a `@PreAuthorize`-style interceptor. Note that `TripService.getTrip()` already throws `ResourceNotFoundException` if not found, so the `null` check is likely redundant.

---

#### 2.4 Duplicated activity create/update form logic

**Severity:** :yellow_circle: Warning

| File | Lines | Description |
|------|-------|-------------|
| `TripController.java` | 1081-1158 | `createActivity()` - Place find/create, date parse, transport validation |
| `TripController.java` | 1304-1380 | `updateActivity()` - Nearly identical Place find/create, date parse, transport validation |

**Description:** The `createActivity` and `updateActivity` methods share ~80% identical code for place lookup, date calculation, transport mode parsing, and manual minutes validation.

**Recommendation:** Extract shared logic into a private helper method like `buildActivityFromFormParams()` or move the entire Place find-or-create + request building into the service layer.

---

### 3. Single Responsibility Principle (SRP) Violations

#### 3.1 TripController is a God Controller (1664 lines)

**Severity:** :red_circle: Critical

**File:** `/Users/mark/WeGo/src/main/java/com/wego/controller/web/TripController.java`

**Description:** `TripController` handles:
- Trip CRUD (list, create, edit, view)
- Activity CRUD (list, create, edit, delete, duplicate)
- Activity detail view
- Expense list view
- Document list and upload form
- Member management page
- Transport recalculation
- Weather coordinate calculation
- Search coordinate calculation

It injects 8 dependencies: `TripService`, `UserService`, `ActivityService`, `TodoService`, `ExpenseService`, `DocumentService`, `InviteLinkService`, `PlaceRepository`.

**Recommendation:** Split into focused controllers:
- `TripController` - Trip CRUD only (~300 lines)
- `ActivityWebController` - Activity CRUD, duplicate, transport recalc (~500 lines)
- Keep `ExpenseWebController` as-is (already separate, but expenses list is still in TripController)
- `DocumentWebController` - Document list/upload (move from TripController)
- `MemberWebController` - Member page (move from TripController)

---

#### 3.2 TripService handles cover image upload logic

**Severity:** :blue_circle: Suggestion

**File:** `/Users/mark/WeGo/src/main/java/com/wego/service/TripService.java` (lines 510-699)

**Description:** `TripService` contains ~190 lines of cover image validation and upload logic including magic byte validation, file extension validation, and storage path management. This is a separate concern from trip business logic.

**Recommendation:** Extract into a `CoverImageService` or `ImageUploadService`.

---

### 4. Inconsistent Authentication Patterns

#### 4.1 Web vs API auth divergence

**Severity:** :yellow_circle: Warning

| Layer | Auth Pattern | Principal Type | User Resolution |
|-------|-------------|----------------|-----------------|
| Web Controllers | `@AuthenticationPrincipal OAuth2User` | `OAuth2User` | `principal.getAttribute("email")` -> `userService.getUserByEmail()` |
| API Controllers | `@CurrentUser UserPrincipal` | `UserPrincipal` | `principal.getId()` (direct UUID) |
| `HomeController` (dashboard) | `@CurrentUser UserPrincipal` | `UserPrincipal` | `principal.getId()` |

**Description:** Most web controllers use the OAuth2User pattern requiring an email lookup, while API controllers and `HomeController` use the custom `@CurrentUser` annotation which directly provides the user ID. This inconsistency means web controllers make an extra DB query per request.

**Recommendation:** Migrate all web controllers to use `@CurrentUser UserPrincipal` like `HomeController` does. This eliminates the redundant `getUserByEmail()` call and the duplicated `getCurrentUser()` methods.

---

### 5. Entity/DTO Separation

#### 5.1 Entity mutation in controller

**Severity:** :yellow_circle: Warning

**File:** `/Users/mark/WeGo/src/main/java/com/wego/controller/web/HomeController.java` (line 74)

```java
trip.setDaysUntil(ChronoUnit.DAYS.between(today, trip.getStartDate()));
```

**Description:** `HomeController.dashboard()` mutates the `TripResponse` DTO's `daysUntil` field. While this is a DTO (not an entity), the mutation pattern in a controller is a code smell. If the service returns this DTO from a `@Transactional` context and JPA dirty-checking is active, entity mutations could be persisted accidentally.

**Recommendation:** Compute `daysUntil` in the service layer or in the DTO's factory method.

---

#### 5.2 InviteController uses Entity directly in view model

**Severity:** :yellow_circle: Warning

**File:** `/Users/mark/WeGo/src/main/java/com/wego/controller/web/InviteController.java` (line 93)

```java
model.addAttribute("trip", trip);  // Trip is an Entity, not a DTO
```

**Description:** `InviteController` fetches the `Trip` entity directly from `TripRepository` and passes it to the view model. All other controllers pass `TripResponse` (DTO). This exposes the JPA entity to the template layer.

**Recommendation:** Use `TripService.getTrip()` or convert to a minimal DTO before passing to the view.

---

### 6. Exception Handling

#### 6.1 Exception handling is well-structured (positive finding)

**Severity:** :large_blue_circle: Info

The project has a clean dual exception handler setup:
- `GlobalExceptionHandler` (`@RestControllerAdvice(basePackages = "com.wego.controller.api")`) - Returns `ApiResponse` JSON
- `WebExceptionHandler` (`@ControllerAdvice(basePackages = "com.wego.controller.web")`) - Returns error view pages
- Custom `ErrorController` for Spring's default error handling

This is well-designed and correctly scoped.

---

#### 6.2 Overly broad exception catching in web controllers

**Severity:** :yellow_circle: Warning

Multiple web controller methods catch `Exception` broadly:

| File | Line | Method |
|------|------|--------|
| `TripController.java` | 205 | `createTrip()` catches `Exception` |
| `TripController.java` | 243 | `showTripDetail()` catches `Exception` for trip fetch |
| `TripController.java` | 1166 | `createActivity()` catches `Exception` |
| `TripController.java` | 1389 | `updateActivity()` catches `Exception` |

**Description:** These catch blocks handle exceptions that would be better handled by the `WebExceptionHandler`. The catch blocks often set model attributes and return the same form view, but `WebExceptionHandler` would redirect to the error page. The controllers are essentially duplicating error handling.

**Recommendation:** Let `WebExceptionHandler` handle unexpected exceptions. Only catch specific expected exceptions (e.g., `ValidationException`) when you need to return to the same form with error messages.

---

### 7. Magic Numbers

#### 7.1 Hardcoded values in controllers

**Severity:** :blue_circle: Suggestion

| File | Line | Value | Description |
|------|------|-------|-------------|
| `TripController.java` | 88 | `50` | Page size for trip list |
| `TripController.java` | 163 | `"TWD"` | Default currency |
| `TripController.java` | 379-382 | `25.0330, 121.5654, 50000` | Default Taipei coordinates and search radius |
| `TripController.java` | 431-432 | `25.0339, 121.5645` | Default Taipei 101 coordinates |
| `TripController.java` | 1133, 1355 | `2880` | Max transport minutes (48 hours) |
| `TripController.java` | 1453 | `50` | Max API calls for transport recalculation |
| `TripApiController.java` | 100 | `50` | Max page size |
| `InviteLinkService.java` | 43 | `10` | MAX_MEMBERS_PER_TRIP (also in TripService:59) |

**Recommendation:** Extract into constants with descriptive names, or into configuration properties. Note that `MAX_MEMBERS_PER_TRIP` is duplicated between `TripService` and `InviteLinkService`.

---

### 8. Dependency Injection

#### 8.1 No circular dependency issues detected

**Severity:** :large_blue_circle: Info

All dependencies follow a clean DAG:
- Controllers -> Services -> Repositories/Domain
- `InviteLinkService` -> `TripService` (one-way)
- No bidirectional service dependencies found

#### 8.2 InviteLinkService duplicates MAX_MEMBERS_PER_TRIP

**Severity:** :blue_circle: Suggestion

**Files:**
- `/Users/mark/WeGo/src/main/java/com/wego/service/TripService.java` (line 59)
- `/Users/mark/WeGo/src/main/java/com/wego/service/InviteLinkService.java` (line 43)

**Description:** Both services define `MAX_MEMBERS_PER_TRIP = 10`. If one changes, the other must also change.

**Recommendation:** Extract to a shared configuration property or constant class.

---

### 9. Naming Conventions

#### 9.1 Naming is generally consistent (positive finding)

- Entities: Singular (`User`, `Trip`, `Activity`)
- Services: `{Domain}Service` (`TripService`, `ActivityService`)
- Web Controllers: `{Domain}Controller` or `{Domain}WebController`
- API Controllers: `{Domain}ApiController`
- DTOs: `Create{Entity}Request`, `Update{Entity}Request`, `{Entity}Response`

#### 9.2 Minor naming inconsistencies

**Severity:** :blue_circle: Suggestion

| Issue | Location |
|-------|----------|
| `ErrorController` (web package) vs Spring's `ErrorController` interface | Could be `CustomErrorController` for clarity |
| `GlobalExceptionHandler` only handles API | Name suggests it handles all exceptions |
| `GlobalExpenseController` / `GlobalDocumentController` prefix "Global" is unusual | Could be `ExpenseOverviewController` / `DocumentOverviewController` |

---

## Summary Statistics

| Category | Critical | Warning | Suggestion | Info |
|----------|:--------:|:-------:|:----------:|:----:|
| Layering Violations | 2 | 0 | 0 | 0 |
| DRY Violations | 0 | 4 | 0 | 0 |
| SRP Violations | 1 | 0 | 1 | 0 |
| Auth Inconsistency | 0 | 1 | 0 | 0 |
| Entity/DTO Separation | 0 | 2 | 0 | 0 |
| Exception Handling | 0 | 1 | 0 | 1 |
| Magic Numbers | 0 | 0 | 1 | 0 |
| Dependency Injection | 0 | 0 | 1 | 1 |
| Naming Conventions | 0 | 0 | 1 | 1 |
| **Total** | **3** | **8** | **4** | **3** |

### Top 3 Priority Actions

1. **Split TripController** (1664 lines) into 4-5 focused controllers -- reduces complexity and makes the codebase maintainable
2. **Move Place creation/lookup and form-parsing logic** from TripController into the service layer -- fixes the most impactful layering violation
3. **Extract shared `getCurrentUser()` and permission-check patterns** into a base class or utility -- eliminates the most pervasive DRY violation across 8+ controllers
