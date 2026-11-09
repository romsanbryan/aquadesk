package romsanbryan.aquadesk.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(annotations = RestController.class)
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AquadeskException.class)
    public ResponseEntity<ApiError> handleAquadeskException(AquadeskException exception) {
        HttpStatus status = exception.getStatus();
        if (status.is4xxClientError()) {
            log.warn("Request failed with application exception type={} status={}",
                    exception.getClass().getSimpleName(), status.value());
        } else {
            log.error("Request failed with application exception type={} status={}",
                    exception.getClass().getSimpleName(), status.value(), exception);
        }
        return ResponseEntity.status(status)
                .body(new ApiError(status.value(), status.getReasonPhrase(), exception.getMessage()));
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            BindException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            MissingServletRequestPartException.class
    })
    public ResponseEntity<ApiError> handleBadRequest(Exception exception) {
        log.warn("Rejected invalid HTTP request type={}", exception.getClass().getSimpleName());
        return ResponseEntity.badRequest()
                .body(new ApiError(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        "The request is invalid."));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleUploadTooLarge(MaxUploadSizeExceededException exception) {
        log.warn("Rejected upload exceeding configured size limit");
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new ApiError(HttpStatus.PAYLOAD_TOO_LARGE.value(),
                        HttpStatus.PAYLOAD_TOO_LARGE.getReasonPhrase(),
                        "The uploaded file exceeds the maximum allowed size."));
    }

    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public ResponseEntity<Void> handleAsyncRequestTimeout(AsyncRequestTimeoutException exception) {
        log.debug("Asynchronous request ended after timeout");
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpectedException(Exception exception) {
        log.error("Unhandled application exception", exception);
        return ResponseEntity.internalServerError()
                .body(new ApiError(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                        "An unexpected error occurred."));
    }
}
