package romsanbryan.aquadesk.exception;

import org.springframework.http.HttpStatus;

public class ServiceException extends AquadeskException {

    public ServiceException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    public ServiceException(String message, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message, cause);
    }
}
