# Module: incident

Core reporting module. Handles incident submission, PostGIS duplicate detection, image storage, status lifecycle, and real-time SSE push to connected reporters.

---

## Package structure

```
incident/
├── controller/
│   └── IncidentController.java      — all /incidents/* endpoints
├── dto/
│   ├── IncidentRequestDTO.java      — forceCreate flag bypasses ST_DWithin in tests
│   └── IncidentResponseDTO.java     — includes duplicate: true/false, reportCount
├── entity/
│   ├── Incident.java                — image URL, PostGIS geometry point, status, source
│   ├── IncidentReporter.java        — join table: incident ↔ user (original + confirmers)
│   ├── AssignmentWorkflow.java      — full audit trail of status transitions
│   ├── Assignment.java
│   ├── AssignmentStatus.java        — REPORTED → VERIFIED → ASSIGNED → IN_PROGRESS → RESOLVED
│   ├── Image.java
│   ├── IncidentSource.java          — MANUAL | DASHCAM
│   └── IssueType.java               — POTHOLE | CRACKED_SURFACE | FADED_LANE_MARKINGS | ...
├── exception/
│   └── IncidentServiceException.java (if present)
├── repository/
│   ├── IncidentRepository.java      — findAllReportedByUser, findNearestDuplicate (PostGIS)
│   └── IncidentReporterRepository.java — findUserIdsByIncidentId (SSE targeting)
└── service/
    ├── interfaces/
    │   ├── IncidentService.java
    │   └── ImageStorageService.java
    └── impl/
        ├── IncidentServiceImpl.java
        ├── IncidentSseService.java          — emitter registry, per-user SSE targeting
        └── LocalImageStorageServiceImpl.java — writes base64 image to uploads/incidents/
```

---

## Reporting flow

1. Client sends `POST /incidents/create` with base64 image, GPS coordinates, issue type, and description.
2. Apache Tika validates the MIME type of the decoded image bytes.
3. If `forceCreate` is false (normal mode), `IncidentRepository.findNearestDuplicate` runs a PostGIS `ST_DWithin` query with a **1 km radius** filtered by `IssueType`.
4. **No duplicate found:** incident saved, image written to `uploads/incidents/<uuid>.jpg`, `IncidentReporter` entry created for the submitting user. Response has `duplicate: false`.
5. **Duplicate found:** response returns `duplicate: true` with the existing incident's data. No new incident is created.
6. On duplicate confirmation (`POST /incidents/{id}/confirm`):
   - `reportCount` is incremented on the existing incident.
   - A new `IncidentReporter` entry is created for the confirming user (unique constraint on `(incident_id, user_id)` makes this idempotent).
   - `IncidentSseService.pushIncidentUpdate` sends an `incident-updated` SSE event to all users linked to that incident.

---

## Entities

### Incident
Stores image URL (not the image itself), PostGIS geometry point (`SRID=4326`), `IssueType`, `IncidentSource`, `AssignmentStatus`, and `reportCount`. The URL is constructed from `SERVICES_WEB_BASE_URL` at write time.

### IncidentReporter
Join table linking an incident to every user who has reported or confirmed it. Unique constraint on `(incident_id, user_id)` — a user can only be linked once. Used to:
- Scope SSE pushes to only relevant users.
- Include confirmed incidents in a user's own feed (`GET /incidents/my`).

### AssignmentWorkflow
Immutable audit log of every status transition. `UPDATEDBY` is nullable for the initial system-generated entry.

### IssueType (enum)
`POTHOLE`, `CRACKED_SURFACE`, `FADED_LANE_MARKINGS`, `DAMAGED_TRAFFIC_SIGN`, `BLOCKED_STORM_DRAIN`, `BROKEN_TRAFFIC_LIGHT`, `ACCIDENT`

No separate DB table — stored as a string column.

---

## Key queries

### `findAllReportedByUser`
```sql
SELECT DISTINCT i FROM Incident i
LEFT JOIN IncidentReporter ir ON ir.incident = i
WHERE i.user.userId = :userId OR ir.user.userId = :userId
```
Returns incidents where the user is the original reporter **or** a confirmed reporter.

### `findNearestDuplicate`
Native PostGIS query using `ST_DWithin` (1 km, geography cast) filtered by `IssueType`. H2 does not support this — integration tests set `forceCreate: true` to skip it.

---

## SSE real-time updates

`IncidentSseService` maintains a `ConcurrentHashMap<UUID, SseEmitter>` keyed by user ID. On a confirm event:
1. `IncidentReporterRepository.findUserIdsByIncidentId` fetches all user IDs linked to the incident.
2. Each linked emitter receives an `incident-updated` event containing the updated incident JSON.
3. Disconnected or timed-out emitters are cleaned up on send failure.

The SSE endpoint (`GET /incidents/events`) authenticates via `?token=<jwt>` because `EventSource` cannot set custom headers.

---

## Image storage

- Upload: base64 string in the request body → decoded server-side → written to `uploads/incidents/<uuid>.jpg`.
- `SERVICES_WEB_BASE_URL` (env var) prefixes the stored URL so the frontend can fetch images via the image-proxy Route Handler.
- Cloudinary is planned as a future replacement but is not implemented.

---

## Tests

| Test class | Type |
|-----------|------|
| `IncidentServiceImplTest` | Unit (Mockito) |
| `IncidentControllerTest` | Unit (Mockito) |
| `IncidentIntegrationTest` | Integration (SpringBootTest + H2, `forceCreate: true`) |
