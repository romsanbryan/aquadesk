package romsanbryan.aquadesk.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.http.ResponseEntity;
import romsanbryan.aquadesk.dto.request.TreatmentRequest;
import romsanbryan.aquadesk.exception.GlobalExceptionHandler;
import romsanbryan.aquadesk.exception.ResourceNotFoundException;
import romsanbryan.aquadesk.services.AquariumService;
import romsanbryan.aquadesk.services.AquariumTaskService;
import romsanbryan.aquadesk.services.EventServices;
import romsanbryan.aquadesk.services.MaintenanceService;
import romsanbryan.aquadesk.services.NotificationServices;
import romsanbryan.aquadesk.services.ParameterServices;
import romsanbryan.aquadesk.services.PhotoService;
import romsanbryan.aquadesk.services.TreatmentService;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class ControllerErrorHandlingTest {

    @Test
    void aquariumMutationFailuresPropagateToGlobalAdvice() {
        AquariumService service = mock(AquariumService.class);
        AquariumController controller = new AquariumController(service);
        RuntimeException failure = new IllegalStateException("service failed");
        when(service.createAquarium(null)).thenThrow(failure);
        doThrow(failure).when(service).updateFilter(4, 8, null);
        doThrow(failure).when(service).deleteFilter(4, 8);

        assertPropagatedFailure(() -> controller.createAquarium(null));
        assertPropagatedFailure(() -> controller.updateFilter(4, 8, null));
        assertPropagatedFailure(() -> controller.deleteFilter(4, 8));
    }

    @Test
    void taskCreationFailurePropagatesToGlobalAdvice() {
        AquariumTaskService service = mock(AquariumTaskService.class);
        AquariumTaskController controller = new AquariumTaskController(service);
        when(service.createTask(2, null)).thenThrow(new IllegalStateException("service failed"));

        assertPropagatedFailure(() -> controller.createTask(2, null));
    }

    @Test
    void parameterFailuresPropagateToGlobalAdvice() {
        ParameterServices service = mock(ParameterServices.class);
        ParameterController controller = new ParameterController(service);
        RuntimeException failure = new IllegalStateException("service failed");
        doThrow(failure).when(service).deleteParameterRange(5, 7);
        doThrow(failure).when(service).createParameterRange(5, null);
        doThrow(failure).when(service).addParameters(5, null);
        when(service.getAllParameters(eq(5), any())).thenThrow(failure);
        when(service.getParameterRanges(5)).thenThrow(failure);

        assertPropagatedFailure(() -> controller.deleteParameterRange(5, 7));
        assertPropagatedFailure(() -> controller.createParameterRange(5, null));
        assertPropagatedFailure(() -> controller.addParameters(5, null));
        assertPropagatedFailure(() -> controller.getTotalElements(5));
        assertPropagatedFailure(() -> controller.getAllParameterRanges(5));
    }

    @Test
    void eventFailuresPropagateToGlobalAdvice() {
        EventServices service = mock(EventServices.class);
        EventController controller = new EventController(service);
        LocalDate date = LocalDate.of(2026, 2, 3);
        RuntimeException failure = new IllegalStateException("service failed");
        when(service.getCo2ByDay(5, date)).thenThrow(failure);
        doThrow(failure).when(service).addInfo(5, null);
        when(service.getInfoByDay(5, date)).thenThrow(failure);

        assertPropagatedFailure(() -> controller.getCo2ByDay(date, 5));
        assertPropagatedFailure(() -> controller.addInfo(null, 5));
        assertPropagatedFailure(() -> controller.getInfoByDay(date, 5));
    }

    @Test
    void maintenanceFailuresPropagateToGlobalAdvice() {
        MaintenanceService service = mock(MaintenanceService.class);
        MaintenanceController controller = new MaintenanceController(service);
        RuntimeException failure = new IllegalStateException("service failed");
        when(service.getLatestMaintenance(5)).thenThrow(failure);
        doThrow(failure).when(service).addMaintenance(5, null);

        assertPropagatedFailure(() -> controller.getLatestMaintenance(5));
        assertPropagatedFailure(() -> controller.addMaintenance(5, null));
    }

    @Test
    void maintenanceWithNoPreviousRecordReturnsNoContent() {
        MaintenanceService service = mock(MaintenanceService.class);
        when(service.getLatestMaintenance(5)).thenReturn(null);

        ResponseEntity<?> response = new MaintenanceController(service).getLatestMaintenance(5);

        assertTrue(response.getStatusCode().is2xxSuccessful());
        org.junit.jupiter.api.Assertions.assertEquals(204, response.getStatusCode().value());
    }

    @Test
    void photoQueryFailuresPropagateToGlobalAdvice() {
        PhotoService service = mock(PhotoService.class);
        PhotoController controller = new PhotoController(service);
        RuntimeException failure = new IllegalStateException("service failed");
        LocalDate date = LocalDate.of(2026, 2, 3);
        when(service.getPhotoIdsByDay(5, date)).thenThrow(failure);
        when(service.getPhoto(5, 7)).thenThrow(failure);
        when(service.getLastPhotos(5, 10)).thenThrow(failure);

        assertPropagatedFailure(() -> controller.getPhotosByDay(date, 5));
        assertPropagatedFailure(() -> controller.getImage(7, 5));
        assertPropagatedFailure(() -> controller.getLastPhotos(10, 5));
    }

    @Test
    void notificationQueryFailuresPropagateToGlobalAdvice() {
        NotificationServices service = mock(NotificationServices.class);
        NotificationController controller = new NotificationController(service);
        RuntimeException failure = new IllegalStateException("service failed");
        when(service.getAllNotificationsActives(5)).thenThrow(failure);
        when(service.getNotifications()).thenThrow(failure);

        assertPropagatedFailure(() -> controller.getNotificationsConfig(5));
        assertPropagatedFailure(() -> controller.getNotifications());
    }

    @Test
    void treatmentFailurePropagatesToGlobalAdvice() {
        TreatmentService service = mock(TreatmentService.class);
        TreatmentController controller = new TreatmentController(service);
        TreatmentRequest request = new TreatmentRequest("CO2", "2.5", LocalDate.of(2026, 2, 3));
        doThrow(new IllegalStateException("service failed"))
                .when(service).createTreatment(5, "CO2", "2.5", request.date());

        assertPropagatedFailure(() -> controller.addTreatment(5, request));
    }

    @Test
    void domainExceptionsAreConvertedToTheStandardApiError() {
        var response = new GlobalExceptionHandler()
                .handleAquadeskException(new ResourceNotFoundException("missing"));

        assertTrue(response.getStatusCode().is4xxClientError());
    }

    private static void assertPropagatedFailure(Executable action) {
        assertThrows(RuntimeException.class, action);
    }
}
