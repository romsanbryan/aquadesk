package romsanbryan.aquadesk.controllers;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import romsanbryan.aquadesk.dto.request.TreatmentRequest;
import romsanbryan.aquadesk.services.TreatmentService;

@RestController
@RequestMapping("/api/aquariums/{aquariumId}/treatments")
public class TreatmentController {
    private final TreatmentService treatmentService;
    public TreatmentController(TreatmentService treatmentService) { this.treatmentService = treatmentService; }

    @PostMapping
    public ResponseEntity<?> addTreatment(@PathVariable Integer aquariumId,
                                          @Valid @RequestBody TreatmentRequest request) {
        treatmentService.createTreatment(aquariumId, request.name(), request.value(), request.date());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
