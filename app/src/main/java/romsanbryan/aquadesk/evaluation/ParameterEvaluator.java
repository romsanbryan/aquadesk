package romsanbryan.aquadesk.evaluation;

import java.math.BigDecimal;


public interface ParameterEvaluator {

    String evaluate(BigDecimal value, BigDecimal level1, BigDecimal level2, BigDecimal level3,
                  BigDecimal level4, BigDecimal level5, String parameterName);
}

