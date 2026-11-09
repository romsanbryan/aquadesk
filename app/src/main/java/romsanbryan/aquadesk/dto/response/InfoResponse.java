package romsanbryan.aquadesk.dto.response;

public record InfoResponse(
        Integer id,
        Integer idEvent,
        String observation,
        String type,
        String status
) {


}
