package romsanbryan.aquadesk.controllers;

import org.springframework.stereotype.Controller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;
import romsanbryan.aquadesk.services.AquariumService;

@Controller
public class AquadeskController {
    private static final Logger log = LoggerFactory.getLogger(AquadeskController.class);
    private final AquariumService aquariumService;

    @Autowired
    public AquadeskController(AquariumService aquariumService) {
        this.aquariumService = aquariumService;
    }

    @GetMapping("/")
    public String index() {
        log.debug("Redirecting to aquarium page");
        return "redirect:/acuario";
    }

    @GetMapping("/gestion")
    public String management(@RequestParam(required = false) Integer aquariumId, Model model) {
        log.debug("Serving aquarium management page for aquarium {}", aquariumId);
        if (aquariumId == null) return "redirect:/acuario";
        aquariumService.getAquarium(aquariumId);
        model.addAttribute("aquariumId", aquariumId);
        return "gestion";
    }

    @GetMapping("/acuario")
    public String aquarium() {
        log.debug("Serving aquarium page");
        return "index";
    }

}
