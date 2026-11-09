package romsanbryan.aquadesk.mapper;

import org.springframework.stereotype.Component;
import romsanbryan.aquadesk.dto.FilterDto;
import romsanbryan.aquadesk.dto.request.MaintenanceRequest;
import romsanbryan.aquadesk.dto.response.LastMaintenanceResponse;
import romsanbryan.data.entities.Event;
import romsanbryan.data.entities.Maintenance;

import java.util.List;

@Component
public class MaintenanceMapper {

    public romsanbryan.data.entities.Filtre mapFilter(String name, Boolean sponge, Boolean perlon, Boolean mech, Boolean bio, Boolean bioMec, Integer maintenanceId) {
        romsanbryan.data.entities.Filtre f = new romsanbryan.data.entities.Filtre();
        f.setMaintenanceId(maintenanceId);
        f.setFilterName(name);
        f.setSponge(sponge);
        f.setPerlon(perlon);
        f.setMech(mech);
        f.setBio(bio);
        f.setBioMech(bioMec);
        return f;
    }

    public Maintenance mapMaintenance(MaintenanceRequest req, Event event) {
        Maintenance m = new Maintenance();
        m.setEvent(event);
        m.setWater(req.water());
        m.setPrimeDose(req.primeDose());
        m.setGlass(req.glass() != null ? req.glass() : false);
        m.setPruning(req.pruning());
        m.setObservations(req.observations() != null ? req.observations() : "");
        return m;
    }

    public LastMaintenanceResponse mapLatestMaintenance(Maintenance m, List<romsanbryan.data.entities.Filtre> filters) {
        return new LastMaintenanceResponse(
                m.getEvent().getStartDate(),
                m.getWater(),
                m.getPrimeDose(),
                m.getObservations(),
                m.getGlass(),
                m.getPruning(),
                filters.stream()
                        .map(this::mapFilterToDto)
                        .toList()
        );
    }

    public FilterDto mapFilterToDto(romsanbryan.data.entities.Filtre f) {
        return new FilterDto(
                f.getFilterName(),
                f.getSponge(),
                f.getPerlon(),
                f.getMech(),
                f.getBio(),
                f.getBioMech());

    }

}