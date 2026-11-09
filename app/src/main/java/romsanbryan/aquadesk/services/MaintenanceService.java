package romsanbryan.aquadesk.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import romsanbryan.aquadesk.dto.request.MaintenanceRequest;
import romsanbryan.aquadesk.dto.response.LastMaintenanceResponse;
import romsanbryan.aquadesk.dto.response.MaintenanceResponse;
import romsanbryan.aquadesk.exception.ResourceNotFoundException;
import romsanbryan.aquadesk.exception.ValidationException;
import romsanbryan.aquadesk.mapper.MaintenanceMapper;
import romsanbryan.data.entities.Event;
import romsanbryan.data.entities.Filtre;
import romsanbryan.data.entities.Maintenance;
import romsanbryan.data.repositories.FiltreRepository;
import romsanbryan.data.repositories.MaintenanceRepository;

import java.time.LocalDate;
import java.util.List;

@Service
public class MaintenanceService {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceService.class);

    private final MaintenanceMapper maintenanceMapper;
    private final MaintenanceRepository maintenanceRepository;
    private final FiltreRepository filterRepository;
    private final EventServices eventServices;
    private final DataUpdatePublisher updatePublisher;


    @Autowired
    public MaintenanceService(MaintenanceMapper maintenanceMapper,
                              MaintenanceRepository maintenanceRepository,
                              FiltreRepository filterRepository, EventServices eventServices,
                              DataUpdatePublisher updatePublisher) {

        this.maintenanceMapper = maintenanceMapper;
        this.maintenanceRepository = maintenanceRepository;
        this.filterRepository = filterRepository;
        this.eventServices = eventServices;
        this.updatePublisher = updatePublisher;
    }


    public LastMaintenanceResponse getLatestMaintenance(Integer aquariumId) {
        log.debug("Loading latest maintenance record");

        Maintenance maintenance = maintenanceRepository.findLatestMaintenanceByAquariumId(aquariumId).orElse(null);
        if (maintenance == null) {
            return null;
        }

        List<Filtre> filters = filterRepository.findByMaintenanceId(maintenance.getId());

        return maintenanceMapper.mapLatestMaintenance(maintenance, filters);
    }


    @Transactional
    public void addMaintenance(Integer aquariumId, MaintenanceRequest req) {
        if (req.water() == null) {
            throw new ValidationException("Water amount is required.");
        }
        if (req.water() < 0 || (req.primeDose() != null && req.primeDose().signum() < 0)) {
            throw new ValidationException("Water and Prime dose must not be negative.");
        }
        log.info("Creating maintenance record for {}", req.date());
        LocalDate date = req.date() != null ? req.date() : LocalDate.now();

        Event event = eventServices.createEvent(aquariumId, "Maintenance", date, "manteiment");

        Maintenance m = maintenanceMapper.mapMaintenance(req, event);

        maintenanceRepository.save(m);

        filterRepository.save(maintenanceMapper.mapFilter("Filter 1", req.f1Sponge(), req.f1Perlon(), req.f1Mech(), req.f1Bio(), req.f1BioMech(), m.getId()));

        filterRepository.save(maintenanceMapper.mapFilter("Filter 2", req.f2Sponge(), req.f2Perlon(), req.f2Mech(), req.f2Bio(), req.f2BioMech(), m.getId()));
        updatePublisher.publish("maintenance");


    }


    public MaintenanceResponse getMaintenance(Integer aquariumId, Integer eventId) {
        log.debug("Loading maintenance for event {}", eventId);
        Maintenance m = maintenanceRepository.findByIdEventAndAquariumId(eventId, aquariumId)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance was not found for event " + eventId));
        List<Filtre> filters = filterRepository.findByMaintenanceId(m.getId());
        return new MaintenanceResponse(m, filters);
    }


}