package romsanbryan.aquadesk.controllers;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import romsanbryan.aquadesk.dto.request.MaintenanceRequest;
import romsanbryan.aquadesk.dto.response.LastMaintenanceResponse;
import romsanbryan.aquadesk.services.MaintenanceService;

@RestController
@RequestMapping("/api/aquariums/{aquariumId}/maintenances")
public class MaintenanceController {
    private final MaintenanceService maintenanceService;

    public MaintenanceController(MaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @GetMapping("/last")
    public ResponseEntity<?> getLatestMaintenance(@PathVariable Integer aquariumId) {
        LastMaintenanceResponse dto = maintenanceService.getLatestMaintenance(aquariumId);
        return dto == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(dto);
    }

    @PostMapping
    public ResponseEntity<?> addMaintenance(@PathVariable Integer aquariumId,
                                            @Valid @RequestBody MaintenanceRequest req) {
        maintenanceService.addMaintenance(aquariumId, req);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<?> getMaintenance(@PathVariable Integer aquariumId, @PathVariable Integer eventId) {
        return ResponseEntity.ok(maintenanceService.getMaintenance(aquariumId, eventId));
    }
}
