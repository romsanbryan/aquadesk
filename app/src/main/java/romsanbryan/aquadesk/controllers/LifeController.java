package romsanbryan.aquadesk.controllers;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import romsanbryan.aquadesk.dto.request.LifeQuantityRequest;
import romsanbryan.aquadesk.dto.request.LifeRequest;
import romsanbryan.aquadesk.dto.response.LifeResponse;
import romsanbryan.aquadesk.services.LifeService;

import java.util.List;

@RestController
@RequestMapping("/api/aquariums/{aquariumId}/life")
public class LifeController {

    private final LifeService lifeService;

    public LifeController(LifeService lifeService) {
        this.lifeService = lifeService;
    }

    @GetMapping
    public List<LifeResponse> getLife(@PathVariable Integer aquariumId) {
        return lifeService.getLife(aquariumId);
    }

    @PostMapping
    public ResponseEntity<LifeResponse> addLife(@PathVariable Integer aquariumId,
                                                @Valid @RequestBody LifeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(lifeService.addLife(aquariumId, request));
    }

    @PatchMapping("/{lifeId}/quantity")
    public LifeResponse changeQuantity(@PathVariable Integer aquariumId,
                                       @PathVariable Integer lifeId,
                                       @Valid @RequestBody LifeQuantityRequest request) {
        return lifeService.changeQuantity(aquariumId, lifeId, request);
    }
}
