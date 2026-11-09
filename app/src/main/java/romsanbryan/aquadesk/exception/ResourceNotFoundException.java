package romsanbryan.aquadesk.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends AquadeskException {

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
