package romsanbryan.aquadesk.controllers;

import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import romsanbryan.aquadesk.dto.request.ParametersRequest;
import romsanbryan.aquadesk.dto.request.RangeRequest;
import romsanbryan.aquadesk.dto.response.ParametersResponse;
import romsanbryan.aquadesk.exception.ValidationException;
import romsanbryan.aquadesk.services.ParameterServices;
import romsanbryan.data.entities.Parameters;
import romsanbryan.data.entities.ParametersRange;

import java.util.List;

@RestController
@RequestMapping("/api/aquariums/{aquariumId}")
public class ParameterController {
    private final ParameterServices parameterServices;

    public ParameterController(ParameterServices parameterServices) {
        this.parameterServices = parameterServices;
    }

    @GetMapping("/parameters/summary")
    public ResponseEntity<ParametersResponse> getParametersSummary(@PathVariable Integer aquariumId) {
        return ResponseEntity.ok(parameterServices.getParametersSummary(aquariumId));
    }
    @GetMapping("/parameter-ranges")
    public List<ParametersRange> getAllParameterRanges(@PathVariable Integer aquariumId) {
        return parameterServices.getParameterRanges(aquariumId);
    }
    @DeleteMapping("/parameter-ranges/{rangeId}")
    public ResponseEntity<?> deleteParameterRange(@PathVariable Integer aquariumId,
                                                  @PathVariable Integer rangeId) {
        parameterServices.deleteParameterRange(aquariumId, rangeId);
        return ResponseEntity.noContent().build();
    }
    @PutMapping("/parameter-ranges/{rangeId}")
    public ResponseEntity<?> updateParameterRange(@PathVariable Integer aquariumId,
                                                  @PathVariable Integer rangeId,
                                                  @Valid @RequestBody RangeRequest req) {
        parameterServices.updateParameterRange(aquariumId, rangeId, req);
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/parameter-ranges")
    public ResponseEntity<?> createParameterRange(@PathVariable Integer aquariumId,
                                                  @Valid @RequestBody RangeRequest req) {
        parameterServices.createParameterRange(aquariumId, req);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
    @PostMapping("/parameters")
    public ResponseEntity<?> addParameters(@PathVariable Integer aquariumId,
                                           @RequestBody ParametersRequest req) {
        parameterServices.addParameters(aquariumId, req);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
    @GetMapping("/parameters/{eventId}")
    public ResponseEntity<Parameters> getParameters(@PathVariable Integer aquariumId,
                                                    @PathVariable Integer eventId) {
        return ResponseEntity.ok(parameterServices.getParameters(aquariumId, eventId));
    }
    @GetMapping("/parameters/table")
    public ResponseEntity<?> getParametersTable(@RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "5") int size,
                                                 @PathVariable Integer aquariumId) {
        return ResponseEntity.ok(parameterServices.getAllParameters(aquariumId, createPageRequest(page, size)).getContent());
    }
    @GetMapping("/parameters/table/current-page")
    public ResponseEntity<?> getCurrentPage(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "5") int size,
                                            @PathVariable Integer aquariumId) {
        return ResponseEntity.ok(parameterServices.getAllParameters(aquariumId, createPageRequest(page, size)).getNumber());
    }
    @GetMapping("/parameters/table/total-pages")
    public ResponseEntity<?> getTotalPages(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "5") int size,
                                           @PathVariable Integer aquariumId) {
        return ResponseEntity.ok(parameterServices.getAllParameters(aquariumId, createPageRequest(page, size)).getTotalPages());
    }
    @GetMapping("/parameters/table/total-elements")
    public ResponseEntity<?> getTotalElements(@PathVariable Integer aquariumId) {
        return ResponseEntity.ok(parameterServices.getAllParameters(aquariumId, PageRequest.of(0, Integer.MAX_VALUE)).getTotalElements());
    }
    private PageRequest createPageRequest(int page, int size) {
        if (page < 0 || size <= 0) {
            throw new ValidationException("Page must be zero or greater and size must be greater than zero.");
        }
        return PageRequest.of(page, size);
    }
}
