package romsanbryan.aquadesk.mapper;

import org.springframework.stereotype.Component;
import romsanbryan.aquadesk.exception.ValidationException;
import romsanbryan.data.entities.Co2;
import romsanbryan.data.entities.Event;

import java.math.BigDecimal;

@Component
public class TreatmentMapper {

    public Co2 getCo2(String productName, String value, Event treatmentEvent) {
        Co2 co2 = new Co2();
        co2.setEvent(treatmentEvent);
        co2.setProduct(productName);
        co2.setValue(parseBigDecimal(value));
        return co2;
    }


    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isBlank()) return null;
        String clean = value.replaceAll("[^0-9.,]", "").replace(",", ".");
        try {
            return new BigDecimal(clean);
        } catch (NumberFormatException e) {
            throw new ValidationException("Treatment value must contain a valid number.", e);
        }
    }
}