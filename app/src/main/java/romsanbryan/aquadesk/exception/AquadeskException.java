package romsanbryan.aquadesk.exception;

import org.springframework.http.HttpStatus;

public class AquadeskException extends RuntimeException {

    private final HttpStatus status;

    protected AquadeskException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    protected AquadeskException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
