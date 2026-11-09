package romsanbryan.aquadesk.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import romsanbryan.aquadesk.dto.request.AquariumTaskRequest;
import romsanbryan.aquadesk.dto.request.InfoRequest;
import romsanbryan.aquadesk.dto.request.MaintenanceRequest;
import romsanbryan.aquadesk.dto.request.ParametersRequest;
import romsanbryan.aquadesk.dto.request.TaskCompletionRequest;
import romsanbryan.aquadesk.dto.response.AquariumTaskResponse;
import romsanbryan.aquadesk.dto.response.EventResponse;
import romsanbryan.aquadesk.exception.ResourceNotFoundException;
import romsanbryan.aquadesk.exception.ValidationException;
import org.springframework.web.multipart.MultipartFile;
import romsanbryan.data.entities.Aquarium;
import romsanbryan.data.entities.AquariumTask;
import romsanbryan.data.repositories.AquariumRepository;
import romsanbryan.data.repositories.AquariumTaskRepository;

import java.time.LocalDate;
import java.util.List;

@Service
public class AquariumTaskService {
    private static final Logger log = LoggerFactory.getLogger(AquariumTaskService.class);

    private final AquariumTaskRepository taskRepository;
    private final AquariumRepository aquariumRepository;
    private final EventServices eventServices;
    private final MaintenanceService maintenanceService;
    private final ParameterServices parameterServices;
    private final TreatmentService treatmentService;
    private final PhotoService photoService;
    private final DataUpdatePublisher updatePublisher;

    public AquariumTaskService(AquariumTaskRepository taskRepository,
                               AquariumRepository aquariumRepository,
                               EventServices eventServices,
                               MaintenanceService maintenanceService,
                               ParameterServices parameterServices,
                               TreatmentService treatmentService,
                               PhotoService photoService,
                               DataUpdatePublisher updatePublisher) {
        this.taskRepository = taskRepository;
        this.aquariumRepository = aquariumRepository;
        this.eventServices = eventServices;
        this.maintenanceService = maintenanceService;
        this.parameterServices = parameterServices;
        this.treatmentService = treatmentService;
        this.photoService = photoService;
        this.updatePublisher = updatePublisher;
    }

    @Transactional(readOnly = true)
    public List<AquariumTaskResponse> getTasks(Integer aquariumId) {
        requireAquarium(aquariumId);
        return taskRepository.findByAquariumIdOrderByActionDateAscIdAsc(aquariumId)
                .stream().map(AquariumTaskResponse::new).toList();
    }

    @Transactional(readOnly = true)
    public List<AquariumTaskResponse> getUpcomingTasks(Integer aquariumId) {
        requireAquarium(aquariumId);
        LocalDate today = LocalDate.now();
        return taskRepository
                .findByAquariumIdAndActionDateGreaterThanEqualAndActionDateLessThanOrderByActionDateAscIdAsc(
                        aquariumId, today, today.plusDays(8))
                .stream().map(AquariumTaskResponse::new).toList();
    }

    @Transactional
    public AquariumTaskResponse createTask(Integer aquariumId, AquariumTaskRequest request) {
        Aquarium taskAquarium = requireAquarium(aquariumId);
        AquariumTask task = new AquariumTask();
        task.setAquarium(taskAquarium);
        task.setTitle(request.title().trim());
        task.setDescription(request.description() == null || request.description().isBlank()
                ? null : request.description().trim());
        task.setActionDate(request.actionDate());
        task.setEventType(request.eventType());
        AquariumTask saved = taskRepository.save(task);
        log.info("Created aquarium task aquariumId={} taskId={} actionDate={}",
                aquariumId, saved.getId(), saved.getActionDate());
        updatePublisher.publish("task");
        return new AquariumTaskResponse(saved);
    }

    @Transactional(readOnly = true)
    public AquariumTaskResponse getTask(Integer aquariumId, Integer taskId) {
        return new AquariumTaskResponse(taskRepository.findByIdAndAquariumId(taskId, aquariumId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Task was not found for aquarium " + aquariumId + " and task " + taskId)));
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getCalendarTasks(
            Integer aquariumId, LocalDate start, LocalDate endExclusive) {
        requireAquarium(aquariumId);
        if (start == null || endExclusive == null || !endExclusive.isAfter(start)) {
            throw new ValidationException(
                    "Reminder date range must include a start and end, with end on or after start.");
        }
        return taskRepository.findByAquariumIdAndActionDateGreaterThanEqualAndActionDateLessThanOrderByActionDateAscIdAsc(
                        aquariumId, start, endExclusive)
                .stream()
                .map(task -> new EventResponse(
                        -task.getId().longValue(),
                        "<i class='bi bi-clock'></i>",
                        task.getActionDate().atStartOfDay(),
                        task.getActionDate().plusDays(1).atStartOfDay(),
                        "#d99000",
                        task.getTitle(),
                        true,
                        "reminder",
                        1,
                        null))
                .toList();
    }

    @Transactional
    public AquariumTaskResponse updateTask(Integer aquariumId, Integer taskId, AquariumTaskRequest request) {
        AquariumTask task = findTask(aquariumId, taskId);
        task.setTitle(request.title().trim());
        task.setDescription(request.description() == null || request.description().isBlank()
                ? null : request.description().trim());
        task.setActionDate(request.actionDate());
        task.setEventType(request.eventType());
        AquariumTaskResponse response = new AquariumTaskResponse(taskRepository.save(task));
        log.info("Updated aquarium task aquariumId={} taskId={} actionDate={}",
                aquariumId, taskId, request.actionDate());
        updatePublisher.publish("task");
        return response;
    }

    @Transactional
    public void deleteTask(Integer aquariumId, Integer taskId) {
        AquariumTask task = findTask(aquariumId, taskId);
        taskRepository.delete(task);
        log.info("Deleted aquarium task aquariumId={} taskId={}", aquariumId, taskId);
        updatePublisher.publish("task");
    }

    @Transactional
    public void completeTask(Integer aquariumId, Integer taskId, TaskCompletionRequest request, MultipartFile photo) {
        AquariumTask task = taskRepository.findByIdAndAquariumIdForUpdate(taskId, aquariumId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Task was not found for aquarium " + aquariumId + " and task " + taskId));

        String observation = request.observation() == null || request.observation().isBlank()
                ? task.getDescription() : request.observation().trim();
        String maintenanceObservations = request.maintenanceObservations() == null
                || request.maintenanceObservations().isBlank()
                ? task.getDescription() : request.maintenanceObservations().trim();
        String eventType = task.getEventType() == null ? "info" : task.getEventType();
        switch (eventType) {
            case "info" -> {
                if (!"INFO".equals(request.infoType())) {
                    throw new ValidationException("An info reminder requires the INFO event subtype.");
                }
                eventServices.addInfo(aquariumId, new InfoRequest(
                        request.date(), request.infoType(), request.status(), observation));
            }
            case "live" -> {
                if (!"FISH".equals(request.infoType()) && !"plants".equals(request.infoType())) {
                    throw new ValidationException("A live reminder requires the FISH or plants event subtype.");
                }
                eventServices.addInfo(aquariumId, new InfoRequest(
                        request.date(), request.infoType(), request.status(), observation));
            }
            case "manteiment" -> maintenanceService.addMaintenance(aquariumId, new MaintenanceRequest(
                    request.date(), request.water(), request.primeDose(),
                    request.glass(), request.pruning(), maintenanceObservations,
                    request.f1Sponge(), request.f1Perlon(), request.f1Mech(), request.f1Bio(),
                    request.f1BioMech(), request.f2Sponge(), request.f2Perlon(), request.f2Mech(),
                    request.f2Bio(), request.f2BioMech()));
            case "parameters" -> {
                if (request.nh3() == null && request.no2() == null && request.no3() == null
                        && request.ph() == null && request.kh() == null
                        && request.gh() == null && request.temp() == null) {
                    throw new ValidationException("At least one parameter value is required.");
                }
                parameterServices.addParameters(aquariumId, new ParametersRequest(
                        request.date(), request.nh3(), request.no2(), request.no3(),
                        request.ph(), request.kh(), request.gh(), request.temp()));
            }
            case "co2" -> {
                if (request.product() == null || request.product().isBlank()
                        || request.value() == null || request.value().isBlank()) {
                    throw new ValidationException("A CO2 reminder requires a product and value.");
                }
                treatmentService.createTreatment(
                        aquariumId, request.product().trim(), request.value().trim(), request.date());
            }
            case "photo" -> {
                if (photo == null || photo.isEmpty()) {
                    throw new ValidationException("A photo is required to complete this reminder.");
                }
                photoService.createPhotoEvent(aquariumId, request.date(), photo);
            }
            default -> throw new ValidationException("Unsupported reminder event type.");
        }

        taskRepository.delete(task);
        log.info("Completed aquarium task aquariumId={} taskId={} eventType={} completedAt={}",
                aquariumId, taskId, eventType, request.date());
        updatePublisher.publish("task");
    }

    private Aquarium requireAquarium(Integer aquariumId) {
        return aquariumRepository.findById(aquariumId)
                .orElseThrow(() -> new ResourceNotFoundException("Aquarium was not found with id " + aquariumId));
    }

    private AquariumTask findTask(Integer aquariumId, Integer taskId) {
        return taskRepository.findByIdAndAquariumIdForUpdate(taskId, aquariumId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Task was not found for aquarium " + aquariumId + " and task " + taskId));
    }
}
