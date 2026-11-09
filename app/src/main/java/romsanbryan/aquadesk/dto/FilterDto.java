package romsanbryan.aquadesk.dto;

public record FilterDto(
        String filterName,
        Boolean sponge,
        Boolean perlon,
        Boolean mech,
        Boolean bio,
        Boolean bioMech) {

    public FilterDto(romsanbryan.data.entities.Filtre f) {
        this(f.getFilterName(), f.getSponge(), f.getPerlon(), f.getMech(), f.getBio(), f.getBioMech());
    }
}