package romsanbryan.aquadesk.controllers;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import romsanbryan.aquadesk.dto.request.AquariumCreateRequest;
import romsanbryan.aquadesk.dto.request.AquariumFilterRequest;
import romsanbryan.aquadesk.dto.request.AquariumRequest;
import romsanbryan.aquadesk.dto.response.AquariumResponse;
import romsanbryan.aquadesk.services.AquariumService;

import java.util.List;

@RestController
@RequestMapping("/api/aquariums")
public class AquariumController {

    private final AquariumService aquariumService;

    public AquariumController(AquariumService aquariumService) {
        this.aquariumService = aquariumService;
    }

    @GetMapping
    public List<AquariumResponse> getAquariums() {
        return aquariumService.getAllAquariums();
    }

    @PostMapping
    public ResponseEntity<?> createAquarium(@Valid @RequestBody AquariumCreateRequest request) {
        aquariumService.createAquarium(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{aquariumId}")
    public ResponseEntity<?> updateAquarium(@PathVariable Integer aquariumId, @Valid @RequestBody AquariumRequest request) {
        aquariumService.updateAquarium(aquariumId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{aquariumId}")
    public ResponseEntity<?> deleteAquarium(@PathVariable Integer aquariumId) {
        aquariumService.deleteAquarium(aquariumId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{aquariumId}/filters")
    public ResponseEntity<?> addFilter(@PathVariable Integer aquariumId,
                                       @Valid @RequestBody AquariumFilterRequest request) {
        aquariumService.addFilter(aquariumId, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{aquariumId}/filters/{filterId}")
    public ResponseEntity<?> updateFilter(@PathVariable Integer aquariumId, @PathVariable Integer filterId,
                                          @Valid @RequestBody AquariumFilterRequest request) {
        aquariumService.updateFilter(aquariumId, filterId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{aquariumId}/filters/{filterId}")
    public ResponseEntity<?> deleteFilter(@PathVariable Integer aquariumId, @PathVariable Integer filterId) {
        aquariumService.deleteFilter(aquariumId, filterId);
        return ResponseEntity.noContent().build();
    }
}
