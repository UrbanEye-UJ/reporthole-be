package za.co.urbaneye.reporthole.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * AOP aspect that records execution duration, thread name, and authenticated principal
 * for every method call in the controller, service, and repository layers.
 *
 * <p>Logs a single structured line per call at INFO level on success and ERROR level
 * on exception. Method arguments are intentionally omitted to prevent base64 image
 * payloads and encrypted PII from appearing in logs.</p>
 */
@Aspect
@Component
@Slf4j
public class ExecutionMetricsAspect {

    @Pointcut("execution(* za.co.urbaneye.reporthole..controller..*.*(..))")
    private void controllerLayer() {}

    @Pointcut("execution(* za.co.urbaneye.reporthole..service..*.*(..))")
    private void serviceLayer() {}

    @Pointcut("execution(* za.co.urbaneye.reporthole..repository..*.*(..))")
    private void repositoryLayer() {}

    /**
     * Wraps every matched method to measure wall-clock duration and log a structured
     * metrics line. Exceptions are re-thrown unchanged so callers see the original error.
     *
     * @param pjp the intercepted join point
     * @return whatever the target method returned
     * @throws Throwable re-throws any exception the target method raised
     */
    @Around("controllerLayer() || serviceLayer() || repositoryLayer()")
    public Object measure(ProceedingJoinPoint pjp) throws Throwable {
        String className  = pjp.getSignature().getDeclaringTypeName();
        String methodName = pjp.getSignature().getName();
        String layer      = resolveLayer(className);
        String thread     = Thread.currentThread().getName();
        String principal  = resolvePrincipal();

        long start = System.nanoTime();
        try {
            Object result = pjp.proceed();
            long ms = (System.nanoTime() - start) / 1_000_000;
            log.info("[METRICS] layer={} class={} method={} duration={}ms thread={} user={}",
                    layer, className, methodName, ms, thread, principal);
            return result;
        } catch (Throwable t) {
            long ms = (System.nanoTime() - start) / 1_000_000;
            log.error("[METRICS] layer={} class={} method={} duration={}ms thread={} user={} error={}",
                    layer, className, methodName, ms, thread, principal, t.getMessage());
            throw t;
        }
    }

    /**
     * Returns the authenticated user's principal string, or {@code "anonymous"} when
     * no authentication is present (e.g. public endpoints, SSE token handshake).
     */
    String resolvePrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return "anonymous";
        Object principal = auth.getPrincipal();
        if ("anonymousUser".equals(principal)) return "anonymous";
        return principal.toString();
    }

    /**
     * Derives a short layer label from the fully-qualified class name for readability.
     */
    String resolveLayer(String className) {
        if (className.contains(".controller.")) return "CONTROLLER";
        if (className.contains(".repository.")) return "REPOSITORY";
        if (className.contains(".service."))    return "SERVICE";
        return "UNKNOWN";
    }
}
