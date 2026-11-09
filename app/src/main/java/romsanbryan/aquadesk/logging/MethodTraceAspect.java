package romsanbryan.aquadesk.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class MethodTraceAspect {

    private static final Logger log = LoggerFactory.getLogger(MethodTraceAspect.class);

    @Around("execution(public * romsanbryan.aquadesk.controllers..*(..))"
            + " || execution(public * romsanbryan.aquadesk.services..*(..))")
    public Object traceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        String method = joinPoint.getSignature().toShortString();
        long startTime = System.nanoTime();
        log.debug("Trace enter method={}", method);
        try {
            Object result = joinPoint.proceed();
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            log.debug("Trace exit method={} outcome=success durationMs={}", method, durationMs);
            return result;
        } catch (Throwable exception) {
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            log.debug("Trace exit method={} outcome=error exception={} durationMs={}",
                    method, exception.getClass().getSimpleName(), durationMs);
            throw exception;
        }
    }
}
