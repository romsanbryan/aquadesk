package romsanbryan.aquadesk.exception;

import org.springframework.http.HttpStatus;

public class ValidationException extends AquadeskException {

    public ValidationException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }

    public ValidationException(String message, Throwable cause) {
        super(HttpStatus.BAD_REQUEST, message, cause);
    }
}
