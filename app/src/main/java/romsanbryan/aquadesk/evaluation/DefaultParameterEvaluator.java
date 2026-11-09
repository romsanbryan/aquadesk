package romsanbryan.aquadesk.evaluation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import java.math.BigDecimal;


public class DefaultParameterEvaluator implements ParameterEvaluator {

    private static final Logger log = LoggerFactory.getLogger(DefaultParameterEvaluator.class);
    private final MessageSource messageSource;

    public DefaultParameterEvaluator(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Override
    public String evaluate(BigDecimal value, BigDecimal level1, BigDecimal level2, BigDecimal level3,
                           BigDecimal level4, BigDecimal level5, String parameterName) {


        if (value == null) {
            log.warn("Null value for parameter: {}", parameterName);
            return null;
        }

        int comparisonWithLevel3 = value.compareTo(level3);

        if (comparisonWithLevel3 < 0) {
            return evaluateOutOfRange(value, level2, level3, "below");
        } else if (comparisonWithLevel3 > 0) {
            return evaluateOutOfRange(value, level4, level3, "above");
        }

        return null;

    }

    private String evaluateOutOfRange(BigDecimal value, BigDecimal boundaryLevel, BigDecimal idealLevel,
                                      String direction) {
        int comparisonWithBoundary = value.compareTo(boundaryLevel);

        boolean moderate = ("below".equals(direction) && comparisonWithBoundary >= 0)
                || ("above".equals(direction) && comparisonWithBoundary <= 0);
        String key = "parameters.range." + (moderate ? direction : "very" + ("below".equals(direction) ? "Low" : "High"));
        return messageSource.getMessage(key, new Object[]{value, idealLevel}, LocaleContextHolder.getLocale());
    }
}
