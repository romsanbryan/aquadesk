package romsanbryan.aquadesk.exception;

import org.springframework.http.HttpStatus;

public class ExternalServiceException extends AquadeskException {

    public ExternalServiceException(String message, Throwable cause) {
        super(HttpStatus.BAD_GATEWAY, message, cause);
    }
}
