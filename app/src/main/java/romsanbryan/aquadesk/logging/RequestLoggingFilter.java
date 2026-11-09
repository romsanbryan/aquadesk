package romsanbryan.aquadesk.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String TRACE_ID_HEADER = "X-Trace-ID";
    private static final Pattern SAFE_TRACE_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = resolveTraceId(request.getHeader(TRACE_ID_HEADER), request.getHeader(REQUEST_ID_HEADER));
        long startTime = System.nanoTime();
        MDC.put("traceId", traceId);
        MDC.put("requestId", traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        response.setHeader(REQUEST_ID_HEADER, traceId);
        log.debug("HTTP request started method={} path={}", request.getMethod(), request.getRequestURI());

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            log.info("HTTP request completed method={} path={} status={} durationMs={}",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), durationMs);
            MDC.remove("traceId");
            MDC.remove("requestId");
        }
    }

    private String resolveTraceId(String traceId, String requestId) {
        if (isSafeTraceId(traceId)) {
            return traceId;
        }
        if (isSafeTraceId(requestId)) {
            return requestId;
        }
        return UUID.randomUUID().toString();
    }

    private boolean isSafeTraceId(String traceId) {
        return traceId != null && SAFE_TRACE_ID.matcher(traceId).matches();
    }
}
