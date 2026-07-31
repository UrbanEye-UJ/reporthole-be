# Module: aspect

AOP (Aspect-Oriented Programming) cross-cutting concerns. Currently contains one aspect for logging method execution time across the service layer.

---

## Files

```
aspect/
└── ExecutionMetricsAspect.java   — @Aspect; logs execution time for all service methods
```

---

## ExecutionMetricsAspect

Intercepts all public methods in `*.service.*` packages using a pointcut expression. For each invocation it records:
- Fully qualified method name
- Execution duration in milliseconds

Output goes to the standard Spring logger at `DEBUG` level. Enable it in `application.yml`:

```yaml
logging:
  level:
    za.co.urbaneye.reporthole.aspect: DEBUG
```

This is useful for spotting slow PostGIS queries or ONNX inference latency without adding timing code to individual services.

---

## Tests

| Test class | Type |
|-----------|------|
| `ExecutionMetricsAspectTest` | Unit (Mockito + Spring AOP proxy) |
