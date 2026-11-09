package romsanbryan.aquadesk.dto.response;


public record NotificationParameterResponse(
        String parameter,
        String value,
        String aquariumName
) {
    public NotificationParameterResponse(String parameter, String value) {
        this(parameter, value, null);
    }
}
