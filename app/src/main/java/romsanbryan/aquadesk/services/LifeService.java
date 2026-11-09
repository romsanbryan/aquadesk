package romsanbryan.aquadesk.services;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import romsanbryan.aquadesk.dto.request.LifeQuantityRequest;
import romsanbryan.aquadesk.dto.request.LifeRequest;
import romsanbryan.aquadesk.dto.response.LifeResponse;
import romsanbryan.aquadesk.exception.AquadeskException;
import romsanbryan.aquadesk.exception.ResourceNotFoundException;
import romsanbryan.aquadesk.exception.ServiceException;
import romsanbryan.aquadesk.exception.ValidationException;
import romsanbryan.data.entities.Aquarium;
import romsanbryan.data.entities.AquariumLife;
import romsanbryan.data.repositories.AquariumLifeRepository;
import romsanbryan.data.repositories.AquariumRepository;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
public class LifeService {

    private final AquariumLifeRepository lifeRepository;
    private final AquariumRepository aquariumRepository;
    private final EventServices eventServices;
    private final DataUpdatePublisher updatePublisher;
    private final MessageSource messageSource;

    public LifeService(AquariumLifeRepository lifeRepository,
                       AquariumRepository aquariumRepository,
                       EventServices eventServices,
                       DataUpdatePublisher updatePublisher,
                       MessageSource messageSource) {
        this.lifeRepository = lifeRepository;
        this.aquariumRepository = aquariumRepository;
        this.eventServices = eventServices;
        this.updatePublisher = updatePublisher;
        this.messageSource = messageSource;
    }

    @Transactional(readOnly = true)
    public List<LifeResponse> getLife(Integer aquariumId) {
        try {
            requireAquarium(aquariumId);
            return lifeRepository.findByAquariumIdOrderByCategoryAscCommonNameAsc(aquariumId).stream()
                    .map(this::toResponse)
                    .toList();
        } catch (AquadeskException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ServiceException("Unable to retrieve aquarium life.", exception);
        }
    }

    @Transactional
    public LifeResponse addLife(Integer aquariumId, LifeRequest request) {
        try {
            if (request == null) {
                throw new ValidationException("Life details are required.");
            }
            if (request.commonName() == null || request.commonName().isBlank()) {
                throw new ValidationException("Life common name is required.");
            }
            if (request.quantity() == null || request.quantity() <= 0) {
                throw new ValidationException("Initial quantity must be greater than zero.");
            }
            Aquarium aquarium = requireAquarium(aquariumId);
            if (request.category() == null) {
                throw new ValidationException("Life category is required.");
            }
            String category = request.category().trim().toLowerCase(Locale.ROOT);
            if (!category.equals("fish") && !category.equals("plant")) {
                throw new ValidationException("Life category must be fish or plant.");
            }
            String sheetUrl = normalizeOptional(request.technicalSheetUrl());
            validateTechnicalSheetUrl(sheetUrl);

            AquariumLife life = new AquariumLife();
            life.setAquarium(aquarium);
            life.setCategory(category);
            life.setCommonName(request.commonName().trim());
            life.setScientificName(normalizeOptional(request.scientificName()));
            life.setQuantity(request.quantity());
            life.setRecommendedParameters(normalizeOptional(request.recommendedParameters()));
            life.setTechnicalSheetUrl(sheetUrl);
            life.setNotes(normalizeOptional(request.notes()));
            life = lifeRepository.save(life);

            LocalDate date = request.date() == null ? LocalDate.now() : request.date();
            createChangeEvent(aquariumId, life, request.quantity(), date);
            updatePublisher.publish("event");
            return toResponse(life);
        } catch (AquadeskException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ServiceException("Unable to add aquarium life.", exception);
        }
    }

    @Transactional
    public LifeResponse changeQuantity(Integer aquariumId, Integer lifeId, LifeQuantityRequest request) {
        try {
            AquariumLife life = lifeRepository.findByIdAndAquariumId(lifeId, aquariumId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Life entry was not found with id " + lifeId));
            if (request == null || request.change() == null || request.change() == 0) {
                throw new ValidationException("Quantity change must not be zero.");
            }
            long updatedQuantity = (long) life.getQuantity() + request.change();
            if (updatedQuantity < 0 || updatedQuantity > Integer.MAX_VALUE) {
                throw new ValidationException("Quantity cannot be negative or exceed the supported limit.");
            }
            life.setQuantity((int) updatedQuantity);
            life = lifeRepository.save(life);

            LocalDate date = request.date() == null ? LocalDate.now() : request.date();
            createChangeEvent(aquariumId, life, request.change(), date);
            updatePublisher.publish("event");
            return toResponse(life);
        } catch (AquadeskException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ServiceException("Unable to update aquarium life quantity.", exception);
        }
    }

    private Aquarium requireAquarium(Integer aquariumId) {
        return aquariumRepository.findById(aquariumId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aquarium was not found with id " + aquariumId));
    }

    private void createChangeEvent(Integer aquariumId, AquariumLife life, int change, LocalDate date) {
        String action = change > 0 ? "added" : "removed";
        String category = life.getCategory().equals("fish") ? "Fish" : "Plant";
        String title = messageSource.getMessage("life.event." + action + category,
                new Object[]{Math.abs((long) change), life.getCommonName()}, LocaleContextHolder.getLocale());
        if (title.length() > 100) {
            title = title.substring(0, 100);
        }
        eventServices.createEvent(aquariumId, title, date, "life-" + life.getCategory());
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void validateTechnicalSheetUrl(String url) {
        if (url == null) {
            return;
        }
        try {
            URI uri = URI.create(url);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null) {
                throw new ValidationException("Technical sheet URL must be an HTTP or HTTPS link.");
            }
        } catch (IllegalArgumentException exception) {
            throw new ValidationException("Technical sheet URL must be a valid HTTP or HTTPS link.", exception);
        }
    }

    private LifeResponse toResponse(AquariumLife life) {
        return new LifeResponse(life.getId(), life.getCategory(), life.getCommonName(), life.getScientificName(),
                life.getQuantity(), life.getRecommendedParameters(), life.getTechnicalSheetUrl(), life.getNotes());
    }
}
