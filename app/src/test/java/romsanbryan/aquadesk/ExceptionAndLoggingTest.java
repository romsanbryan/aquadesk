package romsanbryan.aquadesk;

import jakarta.servlet.ServletException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import romsanbryan.aquadesk.exception.AquadeskException;
import romsanbryan.aquadesk.exception.ExternalServiceException;
import romsanbryan.aquadesk.exception.GlobalExceptionHandler;
import romsanbryan.aquadesk.exception.ResourceNotFoundException;
import romsanbryan.aquadesk.exception.ServiceException;
import romsanbryan.aquadesk.exception.ValidationException;
import romsanbryan.aquadesk.logging.MethodTraceAspect;
import romsanbryan.aquadesk.logging.RequestLoggingFilter;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExceptionAndLoggingTest {

    @Test
    void mapsKnownUnknownAndBadRequestsToExpectedApiErrors() {
        assertExceptionResponse(new ResourceNotFoundException("missing"), HttpStatus.NOT_FOUND, "missing");
        assertExceptionResponse(new ValidationException("invalid"), HttpStatus.BAD_REQUEST, "invalid");
        assertExceptionResponse(new ServiceException("failed"), HttpStatus.INTERNAL_SERVER_ERROR, "failed");
        assertExceptionResponse(new ExternalServiceException("upstream", new IOException("offline")),
                HttpStatus.BAD_GATEWAY, "upstream");

        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        var unexpected = handler.handleUnexpectedException(new IllegalStateException("private detail"));
        assertEquals(500, unexpected.getStatusCode().value());
        assertEquals("An unexpected error occurred.", unexpected.getBody().message());

        var badRequest = handler.handleBadRequest(new IllegalArgumentException("bad input"));
        assertEquals(400, badRequest.getStatusCode().value());
        assertEquals("The request is invalid.", badRequest.getBody().message());
        var tooLarge = handler.handleUploadTooLarge(new MaxUploadSizeExceededException(1024));
        assertEquals(413, tooLarge.getStatusCode().value());
        assertEquals("The uploaded file exceeds the maximum allowed size.", tooLarge.getBody().message());
        var asyncTimeout = handler.handleAsyncRequestTimeout(new AsyncRequestTimeoutException());
        assertEquals(200, asyncTimeout.getStatusCode().value());
        assertNull(asyncTimeout.getBody());
    }

    @Test
    void propagatesTraceHeadersAndClearsMdcForSuccessAndFailure() throws Exception {
        RequestLoggingFilter filter = new RequestLoggingFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/probe");
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("X-Trace-ID", "trace-safe_42");
        filter.doFilter(request, response, (req, res) -> {
            assertEquals("trace-safe_42", MDC.get("traceId"));
            assertEquals("trace-safe_42", MDC.get("requestId"));
        });
        assertEquals("trace-safe_42", response.getHeader("X-Trace-ID"));
        assertEquals("trace-safe_42", response.getHeader("X-Request-ID"));
        assertNull(MDC.get("traceId"));
        assertNull(MDC.get("requestId"));

        request = new MockHttpServletRequest("GET", "/fallback");
        response = new MockHttpServletResponse();
        request.addHeader("X-Trace-ID", "invalid trace");
        request.addHeader("X-Request-ID", "request-77");
        filter.doFilter(request, response, (req, res) -> { });
        assertEquals("request-77", response.getHeader("X-Trace-ID"));

        request = new MockHttpServletRequest("GET", "/generated");
        response = new MockHttpServletResponse();
        request.addHeader("X-Trace-ID", "x".repeat(65));
        filter.doFilter(request, response, (req, res) -> { });
        String generatedTrace = response.getHeader("X-Trace-ID");
        assertNotNull(generatedTrace);
        assertNotEquals("x".repeat(65), generatedTrace);
        assertTrue(generatedTrace.matches("[A-Za-z0-9._-]{1,64}"));

        MockHttpServletRequest failureRequest = new MockHttpServletRequest("GET", "/failure");
        MockHttpServletResponse failureResponse = new MockHttpServletResponse();
        assertThrows(ServletException.class, () -> filter.doFilter(failureRequest, failureResponse,
                (req, res) -> { throw new ServletException("downstream"); }));
        assertNull(MDC.get("traceId"));
        assertNull(MDC.get("requestId"));
    }

    @Test
    void tracesSuccessfulAndExceptionalMethodCallsWithoutChangingTheirResults() throws Throwable {
        MethodTraceAspect aspect = new MethodTraceAspect();
        ProceedingJoinPoint success = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(success.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("Service.call()");
        when(success.proceed()).thenReturn("result");
        assertEquals("result", aspect.traceMethod(success));

        ProceedingJoinPoint failure = mock(ProceedingJoinPoint.class);
        when(failure.getSignature()).thenReturn(signature);
        when(failure.proceed()).thenThrow(new IllegalStateException("expected"));
        assertThrows(IllegalStateException.class, () -> aspect.traceMethod(failure));
    }

    private void assertExceptionResponse(AquadeskException exception, HttpStatus status, String message) {
        var response = new GlobalExceptionHandler().handleAquadeskException(exception);
        assertEquals(status, response.getStatusCode());
        assertEquals(status.value(), response.getBody().status());
        assertEquals(message, response.getBody().message());
    }
}
