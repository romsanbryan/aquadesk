package romsanbryan.aquadesk.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import romsanbryan.aquadesk.dto.request.AquariumTaskRequest;
import romsanbryan.aquadesk.dto.request.TaskCompletionRequest;
import romsanbryan.aquadesk.dto.response.AquariumTaskResponse;
import romsanbryan.aquadesk.exception.ValidationException;
import org.springframework.mock.web.MockMultipartFile;
import romsanbryan.data.entities.Aquarium;
import romsanbryan.data.entities.AquariumTask;
import romsanbryan.data.repositories.AquariumRepository;
import romsanbryan.data.repositories.AquariumTaskRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AquariumTaskServiceTest {

    @Mock private AquariumTaskRepository taskRepository;
    @Mock private AquariumRepository aquariumRepository;
    @Mock private EventServices eventServices;
    @Mock private MaintenanceService maintenanceService;
    @Mock private ParameterServices parameterServices;
    @Mock private TreatmentService treatmentService;
    @Mock private PhotoService photoService;
    @Mock private DataUpdatePublisher updatePublisher;

    @InjectMocks private AquariumTaskService service;

    @Test
    void creatingTasksNormalizesMissingAndBlankDescriptions() {
        Aquarium aquarium = new Aquarium();
        when(aquariumRepository.findById(5)).thenReturn(Optional.of(aquarium));
        when(taskRepository.save(any(AquariumTask.class))).thenAnswer(invocation -> {
            AquariumTask task = invocation.getArgument(0);
            task.setId(1);
            return task;
        });

        service.createTask(5, new AquariumTaskRequest("Clean filter", null, LocalDate.now(), "info"));
        service.createTask(5, new AquariumTaskRequest("Clean glass", "   ", LocalDate.now(), "info"));
        service.createTask(5, new AquariumTaskRequest("Trim plants", "Remove dead leaves", LocalDate.now(), "info"));

        ArgumentCaptor<AquariumTask> saved = ArgumentCaptor.forClass(AquariumTask.class);
        verify(taskRepository, times(3)).save(saved.capture());
        List<AquariumTask> tasks = saved.getAllValues();
        assertNull(tasks.get(0).getDescription());
        assertNull(tasks.get(1).getDescription());
        assertEquals("Remove dead leaves", tasks.get(2).getDescription());
        assertEquals(List.of("info", "info", "info"),
                tasks.stream().map(AquariumTask::getEventType).toList());
        verify(updatePublisher, times(3)).publish("task");
    }

    @Test
    void completesInfoReminderWithEditedInfoAndObservation() {
        AquariumTask task = new AquariumTask();
        task.setId(3);
        task.setTitle("Clean filter");
        task.setEventType("info");
        when(taskRepository.findByIdAndAquariumIdForUpdate(3, 5)).thenReturn(Optional.of(task));

        service.completeTask(5, 3,
                completion(LocalDate.now(), "INFO", "Good", "Filter cleaned", null, null, null),
                null);

        verify(eventServices).addInfo(eq(5), argThat(request ->
                "INFO".equals(request.type())
                        && "Good".equals(request.status())
                        && "Filter cleaned".equals(request.observation())));
        verify(taskRepository).delete(task);
        verify(updatePublisher).publish("task");
    }

    @Test
    void defaultsMissingEventTypeAndUsesTaskDescriptionForBlankInfo() {
        AquariumTask task = new AquariumTask();
        task.setId(8);
        task.setDescription("Check the water");
        task.setEventType(null);
        when(taskRepository.findByIdAndAquariumIdForUpdate(8, 5)).thenReturn(Optional.of(task));

        service.completeTask(5, 8,
                completion(LocalDate.now(), "INFO", "OK", "   ", null, null, null), null);

        verify(eventServices).addInfo(eq(5), argThat(request ->
                "Check the water".equals(request.observation())));
        verify(taskRepository).delete(task);
    }

    @Test
    void rejectsInvalidInfoLiveAndUnsupportedReminderTypes() {
        String[] types = {"info", "live", "unsupported"};
        String[] subtypes = {null, "shrimp", null};
        for (int index = 0; index < types.length; index++) {
            int taskId = 40 + index;
            String subtype = subtypes[index];
            AquariumTask task = new AquariumTask();
            task.setId(taskId);
            task.setEventType(types[index]);
            when(taskRepository.findByIdAndAquariumIdForUpdate(taskId, 5)).thenReturn(Optional.of(task));

            assertThrows(ValidationException.class, () -> service.completeTask(5, taskId,
                    completion(LocalDate.now(), subtype, null, null, null, null, null), null));
        }

        verify(taskRepository, never()).delete(any(AquariumTask.class));
        verifyNoInteractions(eventServices, updatePublisher);
    }

    @Test
    void parameterCompletionRequiresAtLeastOneValueAndKeepsTaskWhenInvalid() {
        AquariumTask task = new AquariumTask();
        task.setId(4);
        task.setEventType("parameters");
        when(taskRepository.findByIdAndAquariumIdForUpdate(4, 5)).thenReturn(Optional.of(task));

        assertThrows(ValidationException.class, () -> service.completeTask(
                5, 4, completion(LocalDate.now(), null, null, null, null, null, null), null));

        verify(taskRepository, never()).delete(task);
        verify(parameterServices, never()).addParameters(any(), any());
    }

    @Test
    void completesLiveReminderWithSelectedFishOrPlantSubtype() {
        AquariumTask task = new AquariumTask();
        task.setId(6);
        task.setEventType("live");
        when(taskRepository.findByIdAndAquariumIdForUpdate(6, 5)).thenReturn(Optional.of(task));

        service.completeTask(5, 6,
                completion(LocalDate.now(), "plants", "Healthy", "Growing well", null, null, null),
                null);

        verify(eventServices).addInfo(eq(5), argThat(request -> "plants".equals(request.type())));
        verify(taskRepository).delete(task);
    }

    @Test
    void completesLiveReminderForFishSubtype() {
        AquariumTask task = new AquariumTask();
        task.setId(9);
        task.setEventType("live");
        when(taskRepository.findByIdAndAquariumIdForUpdate(9, 5)).thenReturn(Optional.of(task));

        service.completeTask(5, 9,
                completion(LocalDate.now(), "FISH", "Healthy", "Active", null, null, null), null);

        verify(eventServices).addInfo(eq(5), argThat(request -> "FISH".equals(request.type())));
    }

    @Test
    void photoReminderCannotBeDeletedWithoutAUploadedPhoto() {
        AquariumTask task = new AquariumTask();
        task.setId(7);
        task.setEventType("photo");
        when(taskRepository.findByIdAndAquariumIdForUpdate(7, 5)).thenReturn(Optional.of(task));

        assertThrows(ValidationException.class, () -> service.completeTask(
                5, 7, completion(LocalDate.now(), null, null, null, null, null, null), null));
        assertThrows(ValidationException.class, () -> service.completeTask(
                5, 7, completion(LocalDate.now(), null, null, null, null, null, null),
                new MockMultipartFile("photo", "empty.jpg", "image/jpeg", new byte[0])));

        verify(taskRepository, never()).delete(task);
        verify(photoService, never()).createPhotoEvent(any(), any(), any());
    }

    @Test
    void dispatchesMaintenanceCo2PhotoAndParameterCompletionsToTheirServices() {
        LocalDate date = LocalDate.now();
        String[] types = {"manteiment", "co2", "photo", "parameters"};
        for (int index = 0; index < types.length; index++) {
            int taskId = 10 + index;
            AquariumTask task = new AquariumTask();
            task.setId(taskId);
            task.setEventType(types[index]);
            when(taskRepository.findByIdAndAquariumIdForUpdate(taskId, 5)).thenReturn(Optional.of(task));
            TaskCompletionRequest request = completion(date, null, null, null,
                    types[index].equals("co2") ? "Fertilizer" : null,
                    types[index].equals("co2") ? "5 ml" : null,
                    types[index].equals("parameters") ? BigDecimal.ONE : null);
            if (types[index].equals("manteiment")) {
                request = completion(date, null, null, "Water changed", null, null, null);
            }
            if (types[index].equals("photo")) {
                MockMultipartFile photo = new MockMultipartFile("photo", "photo.jpg", "image/jpeg",
                        new byte[]{1});
                service.completeTask(5, taskId, request, photo);
                verify(photoService).createPhotoEvent(5, date, photo);
            } else {
                service.completeTask(5, taskId, request, null);
            }
            verify(taskRepository).delete(task);
        }
        verify(maintenanceService).addMaintenance(eq(5),
                argThat(request -> "Water changed".equals(request.observations())));
        verify(treatmentService).createTreatment(5, "Fertilizer", "5 ml", date);
        verify(parameterServices).addParameters(eq(5), any());
    }

    @Test
    void completesMaintenanceUsingTaskDescriptionWhenObservationsAreBlank() {
        AquariumTask task = new AquariumTask();
        task.setId(20);
        task.setDescription("Scheduled water change");
        task.setEventType("manteiment");
        when(taskRepository.findByIdAndAquariumIdForUpdate(20, 5)).thenReturn(Optional.of(task));

        service.completeTask(5, 20,
                completion(LocalDate.now(), null, null, "   ", null, null, null), null);

        verify(maintenanceService).addMaintenance(eq(5),
                argThat(request -> "Scheduled water change".equals(request.observations())));
    }

    @Test
    void acceptsLastParameterFieldAndValidatesEachCo2Field() {
        for (int index = 0; index < 7; index++) {
            int taskId = 21 + index;
            AquariumTask parameterTask = new AquariumTask();
            parameterTask.setId(taskId);
            parameterTask.setEventType("parameters");
            when(taskRepository.findByIdAndAquariumIdForUpdate(taskId, 5)).thenReturn(Optional.of(parameterTask));
            service.completeTask(5, taskId, completionWithSingleParameter(index), null);
            verify(taskRepository).delete(parameterTask);
        }
        verify(parameterServices, times(7)).addParameters(eq(5), any());

        String[][] invalidCo2 = {
                {null, "5 ml"},
                {"   ", "5 ml"},
                {"Fertilizer", null},
                {"Fertilizer", "   "}
        };
        for (int index = 0; index < invalidCo2.length; index++) {
            int taskId = 100 + index;
            String product = invalidCo2[index][0];
            String value = invalidCo2[index][1];
            AquariumTask task = new AquariumTask();
            task.setId(taskId);
            task.setEventType("co2");
            when(taskRepository.findByIdAndAquariumIdForUpdate(taskId, 5)).thenReturn(Optional.of(task));
            assertThrows(ValidationException.class, () -> service.completeTask(5, taskId,
                    completion(LocalDate.now(), null, null, null, product, value, null), null));
        }
        verify(taskRepository, never()).delete(argThat(task -> task.getId() >= 100));
        verify(treatmentService, never()).createTreatment(anyInt(), anyString(), anyString(), any());
    }

    private TaskCompletionRequest completionWithSingleParameter(int parameterIndex) {
        BigDecimal[] values = new BigDecimal[7];
        values[parameterIndex] = BigDecimal.TEN;
        return new TaskCompletionRequest(
                LocalDate.now(), null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null, null,
                values[0], values[1], values[2], values[3], values[4], values[5], values[6]);
    }

    @Test
    void validatesCalendarTaskDateRangeAndAllowsValidRange() {
        Aquarium aquarium = new Aquarium();
        when(aquariumRepository.findById(5)).thenReturn(Optional.of(aquarium));
        LocalDate start = LocalDate.of(2026, 10, 2);

        assertThrows(ValidationException.class, () -> service.getCalendarTasks(5, null, start));
        assertThrows(ValidationException.class, () -> service.getCalendarTasks(5, start, null));
        assertThrows(ValidationException.class, () -> service.getCalendarTasks(5, start, start));

        when(taskRepository.findByAquariumIdAndActionDateGreaterThanEqualAndActionDateLessThanOrderByActionDateAscIdAsc(
                5, start, start.plusDays(1))).thenReturn(List.of());
        assertTrue(service.getCalendarTasks(5, start, start.plusDays(1)).isEmpty());
    }

    @Test
    void getsOnlyAquariumTasksDueTodayThroughSevenDaysAhead() {
        LocalDate today = LocalDate.now();
        when(aquariumRepository.findById(5)).thenReturn(Optional.of(new Aquarium()));
        AquariumTask task = new AquariumTask();
        task.setId(2);
        task.setTitle("Clean filter");
        task.setActionDate(today.plusDays(7));
        when(taskRepository
                .findByAquariumIdAndActionDateGreaterThanEqualAndActionDateLessThanOrderByActionDateAscIdAsc(
                        5, today, today.plusDays(8)))
                .thenReturn(List.of(task));

        List<AquariumTaskResponse> upcomingTasks = service.getUpcomingTasks(5);

        assertEquals(List.of(new AquariumTaskResponse(task)), upcomingTasks);
        verify(taskRepository)
                .findByAquariumIdAndActionDateGreaterThanEqualAndActionDateLessThanOrderByActionDateAscIdAsc(
                        5, today, today.plusDays(8));
    }

    @Test
    void updatingTasksNormalizesMissingBlankAndNonblankDescriptions() {
        when(taskRepository.findByIdAndAquariumIdForUpdate(3, 5)).thenAnswer(invocation -> {
            AquariumTask task = new AquariumTask();
            task.setId(3);
            return Optional.of(task);
        });
        when(taskRepository.save(any(AquariumTask.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.updateTask(5, 3, new AquariumTaskRequest("Clean filter", null, LocalDate.now(), "info"));
        service.updateTask(5, 3, new AquariumTaskRequest("Clean glass", "   ", LocalDate.now(), "live"));
        service.updateTask(5, 3, new AquariumTaskRequest("Trim plants", "Remove dead leaves", LocalDate.now(), "parameters"));

        ArgumentCaptor<AquariumTask> saved = ArgumentCaptor.forClass(AquariumTask.class);
        verify(taskRepository, times(3)).save(saved.capture());
        List<AquariumTask> tasks = saved.getAllValues();
        assertNull(tasks.get(0).getDescription());
        assertNull(tasks.get(1).getDescription());
        assertEquals("Remove dead leaves", tasks.get(2).getDescription());
        assertEquals(List.of("info", "live", "parameters"),
                tasks.stream().map(AquariumTask::getEventType).toList());
        verify(updatePublisher, times(3)).publish("task");
    }

    private TaskCompletionRequest completion(LocalDate date, String infoType, String status,
                                             String observation, String product, String value,
                                             BigDecimal nh3) {
        return new TaskCompletionRequest(date, infoType, status, observation, product, value, null,
                null, null, null, observation,
                null, null, null, null, null, null, null, null, null, null,
                nh3, null, null, null, null, null, null);
    }
}
