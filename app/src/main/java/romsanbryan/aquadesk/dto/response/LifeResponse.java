package romsanbryan.aquadesk.dto.response;

public record LifeResponse(
        Integer id,
        String category,
        String commonName,
        String scientificName,
        Integer quantity,
        String recommendedParameters,
        String technicalSheetUrl,
        String notes
) {
}
