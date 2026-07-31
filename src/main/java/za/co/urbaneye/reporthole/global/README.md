# Module: global

Cross-cutting concerns that all other modules depend on: the shared API response envelope, the error model, and the global exception handler.

---

## Package structure

```
global/
├── entity/
│   ├── AppResponse.java           — standard JSON envelope: { status, message, data }
│   └── ErrorObject.java           — structured error payload: { field, message }
├── exception/
│   └── GlobalExceptionHandler.java — @ControllerAdvice; maps exceptions to AppResponse
└── util/
    └── (shared utility classes)
```

---

## AppResponse

All controller responses are wrapped in `AppResponse<T>`:

```json
{
  "status": 200,
  "message": "Incident created successfully",
  "data": { ... }
}
```

Use `AppResponse.success(data)` and `AppResponse.error(message)` factory methods instead of constructing the object directly.

---

## GlobalExceptionHandler

`@ControllerAdvice` that intercepts:

| Exception type | HTTP status | Notes |
|---------------|-------------|-------|
| `UserServiceException` | 400 / 409 | Business-rule violations in the user module |
| `IncidentServiceException` | 400 | Incident validation failures |
| `DeviceServiceException` | 400 / 404 | Device registration errors |
| `InferenceQueueFullException` | 503 | Inference queue at capacity |
| `MethodArgumentNotValidException` | 400 | Bean Validation failures; converts `BindingResult` into a list of `ErrorObject` |
| `AccessDeniedException` | 403 | Spring Security; unauthenticated requests |
| `Exception` (fallback) | 500 | Logs the stack trace and returns a generic error message |

Each module should throw its own typed exception (e.g. `UserServiceException`) rather than generic `RuntimeException` so the handler can return the correct HTTP status.
