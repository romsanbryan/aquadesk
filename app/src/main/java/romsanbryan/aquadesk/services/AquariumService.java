package romsanbryan.aquadesk.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import romsanbryan.aquadesk.dto.request.AquariumCreateRequest;
import romsanbryan.aquadesk.dto.request.AquariumFilterRequest;
import romsanbryan.aquadesk.dto.request.AquariumRequest;
import romsanbryan.aquadesk.dto.response.AquariumResponse;
import romsanbryan.aquadesk.exception.ResourceNotFoundException;
import romsanbryan.aquadesk.exception.ValidationException;
import romsanbryan.data.entities.Aquarium;
import romsanbryan.data.entities.AquariumFilter;
import romsanbryan.data.repositories.AquariumFilterRepository;
import romsanbryan.data.repositories.AquariumTaskRepository;
import romsanbryan.data.repositories.AquariumRepository;
import romsanbryan.data.repositories.Co2Repository;
import romsanbryan.data.repositories.EventRepository;
import romsanbryan.data.repositories.InfoRepository;
import romsanbryan.data.repositories.MaintenanceRepository;
import romsanbryan.data.repositories.NotificationsParameters;
import romsanbryan.data.repositories.ParametersRangeRepository;
import romsanbryan.data.repositories.ParametersRepository;
import romsanbryan.data.repositories.PhotoRepository;

import java.util.List;

@Service
public class AquariumService {

    private static final Logger log = LoggerFactory.getLogger(AquariumService.class);
    private final AquariumRepository aquariumRepository;
    private final AquariumFilterRepository filterRepository;
    private final AquariumTaskRepository taskRepository;
    private final NotificationsParameters notificationsRepository;
    private final ParametersRangeRepository rangeRepository;
    private final PhotoRepository photoRepository;
    private final InfoRepository infoRepository;
    private final ParametersRepository parametersRepository;
    private final Co2Repository co2Repository;
    private final MaintenanceRepository maintenanceRepository;
    private final EventRepository eventRepository;

    public AquariumService(AquariumRepository aquariumRepository, AquariumFilterRepository filterRepository,
                           AquariumTaskRepository taskRepository,
                           NotificationsParameters notificationsRepository,
                           ParametersRangeRepository rangeRepository, PhotoRepository photoRepository,
                           InfoRepository infoRepository, ParametersRepository parametersRepository,
                           Co2Repository co2Repository, MaintenanceRepository maintenanceRepository,
                           EventRepository eventRepository) {
        this.aquariumRepository = aquariumRepository;
        this.filterRepository = filterRepository;
        this.taskRepository = taskRepository;
        this.notificationsRepository = notificationsRepository;
        this.rangeRepository = rangeRepository;
        this.photoRepository = photoRepository;
        this.infoRepository = infoRepository;
        this.parametersRepository = parametersRepository;
        this.co2Repository = co2Repository;
        this.maintenanceRepository = maintenanceRepository;
        this.eventRepository = eventRepository;
    }

    @Transactional(readOnly = true)
    public List<AquariumResponse> getAllAquariums() {
        log.debug("Loading aquariums");
        return aquariumRepository.findAllByOrderByNameAsc().stream()
                .map(AquariumResponse::new)
                .toList();
    }

    public Aquarium getAquarium(Integer id) {
        log.debug("Loading aquarium id={}", id);
        return aquariumRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Aquarium was not found with id " + id));
    }

    @Transactional
    public Aquarium createAquarium(AquariumCreateRequest request) {
        if (request == null || request.name() == null || request.name().isBlank()) {
            throw new ValidationException("Aquarium name is required.");
        }

        log.info("Creating aquarium with name {}", request.name());
        Aquarium aquarium = new Aquarium();
        aquarium.setName(request.name().trim());
        aquarium.setType(request.type());
        aquarium.setVolume(request.volume());
        aquarium.setGlassThickness(request.glassThickness());
        aquarium.setSize(request.size());
        aquarium.setHeater(request.heater());
        aquarium.setLight(request.light());
        aquarium.setSubstrate(request.substrate());
        aquarium = aquariumRepository.save(aquarium);

        if (request.filters() != null) {
            for (AquariumFilterRequest filterRequest : request.filters()) {
                if (filterRequest == null) {
                    throw new ValidationException("Aquarium filters cannot contain empty entries.");
                }
                AquariumFilter filter = new AquariumFilter();
                applyFilterRequest(filter, filterRequest);
                filter.setAquarium(aquarium);
                filterRepository.save(filter);
                aquarium.getFilters().add(filter);
            }
        }
        return aquarium;
    }

    @Transactional
    public Aquarium updateAquarium(Integer id, AquariumRequest request) {
        log.info("Updating aquarium id={}", id);
        Aquarium aquarium = aquariumRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Aquarium was not found with id " + id));
        aquarium.setName(request.name());
        aquarium.setType(request.type());
        aquarium.setVolume(request.volume());
        aquarium.setGlassThickness(request.glassThickness());
        aquarium.setSize(request.size());
        aquarium.setHeater(request.heater());
        aquarium.setLight(request.light());
        aquarium.setSubstrate(request.substrate());
        return aquariumRepository.save(aquarium);
    }

    @Transactional
    public void deleteAquarium(Integer id) {
        Aquarium aquarium = aquariumRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Aquarium was not found with id " + id));
        photoRepository.deleteAllByAquariumId(id);
        infoRepository.deleteAllByAquariumId(id);
        parametersRepository.deleteAllByAquariumId(id);
        co2Repository.deleteAllByAquariumId(id);
        maintenanceRepository.deleteFiltersByAquariumId(id);
        maintenanceRepository.deleteAllByAquariumId(id);
        eventRepository.deleteAllByAquariumId(id);
        taskRepository.deleteAllByAquariumId(id);
        notificationsRepository.deleteAllByAquariumId(id);
        rangeRepository.deleteAllByAquariumId(id);
        filterRepository.deleteAllByAquariumId(id);
        aquariumRepository.delete(aquarium);
        log.info("Deleted aquarium id={}", id);
    }

    @Transactional
    public AquariumFilter addFilter(Integer aquariumId, AquariumFilterRequest request) {
        log.info("Adding filter to aquarium id={}", aquariumId);
        Aquarium aquarium = aquariumRepository.findById(aquariumId)
                .orElseThrow(() -> new ResourceNotFoundException("Aquarium was not found with id " + aquariumId));
        AquariumFilter filter = new AquariumFilter();
        applyFilterRequest(filter, request);
        filter.setAquarium(aquarium);
        filter = filterRepository.save(filter);
        aquarium.getFilters().add(filter);
        return filter;
    }

    @Transactional
    public void updateFilter(Integer aquariumId, Integer filterId, AquariumFilterRequest request) {
        log.info("Updating filter id={} aquariumId={}", filterId, aquariumId);
        AquariumFilter filter = filterRepository.findById(filterId)
                .orElseThrow(() -> new ResourceNotFoundException("Filter was not found with id " + filterId));
        if (filter.getAquarium() == null || !aquariumId.equals(filter.getAquarium().getId())) {
            throw new ResourceNotFoundException("Filter was not found with id " + filterId);
        }
        applyFilterRequest(filter, request);
        filterRepository.save(filter);
    }

    @Transactional
    public void deleteFilter(Integer aquariumId, Integer filterId) {
        log.info("Deleting filter id={} aquariumId={}", filterId, aquariumId);
        AquariumFilter filter = filterRepository.findByIdAndAquariumId(filterId, aquariumId)
                .orElseThrow(() -> new ResourceNotFoundException("Filter was not found with id " + filterId));
        filterRepository.delete(filter);
        if (filter.getAquarium() != null) {
            filter.getAquarium().getFilters().remove(filter);
        }
    }

    private void applyFilterRequest(AquariumFilter filter, AquariumFilterRequest request) {
        filter.setName(request.name());
        filter.setMaterialBiological(request.materialBiological());
        filter.setMaterialMechanical(request.materialMechanical());
        filter.setMaterialBioMechanical(request.materialBioMechanical());
        filter.setSponge(request.sponge());
        filter.setCarbonActive(request.carbonActive());
        filter.setPerlon(request.perlon());
    }
}
