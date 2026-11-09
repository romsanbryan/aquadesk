package romsanbryan.aquadesk.evaluation;

import romsanbryan.data.entities.Parameters;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;


public class ParameterValueExtractor {

    private final Parameters parameters;
    private final Map<String, BigDecimal> parameterValues;

    public ParameterValueExtractor(Parameters parameters) {
        this.parameters = parameters;
        this.parameterValues = buildParameterMap();
    }

    private Map<String, BigDecimal> buildParameterMap() {
        Map<String, BigDecimal> map = new HashMap<>();
        map.put("NH3", parameters.getNh3());
        map.put("NO2", parameters.getNo2());
        map.put("NO3", parameters.getNo3());
        map.put("pH", parameters.getPh());
        map.put("KH", parameters.getKh());
        map.put("GH", parameters.getGh());
        return map;
    }


    public BigDecimal getValue(String parameterName) {
        return parameterValues.get(parameterName);
    }


    public boolean hasParameter(String parameterName) {
        return parameterValues.containsKey(parameterName);
    }


    public java.util.Set<String> getAvailableParameters() {
        return parameterValues.keySet();
    }
}

