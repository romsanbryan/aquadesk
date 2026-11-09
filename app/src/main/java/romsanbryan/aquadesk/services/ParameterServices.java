package romsanbryan.aquadesk.services;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import romsanbryan.aquadesk.dto.request.ParametersRequest;
import romsanbryan.aquadesk.dto.request.RangeRequest;
import romsanbryan.aquadesk.dto.response.NotificationParameterResponse;
import romsanbryan.aquadesk.dto.response.ParametersResponse;
import romsanbryan.aquadesk.evaluation.DefaultParameterEvaluator;
import romsanbryan.aquadesk.evaluation.ParameterEvaluator;
import romsanbryan.aquadesk.evaluation.ParameterValueExtractor;
import romsanbryan.aquadesk.exception.AquadeskException;
import romsanbryan.aquadesk.exception.ResourceNotFoundException;
import romsanbryan.aquadesk.exception.ServiceException;
import romsanbryan.aquadesk.mapper.ParameterMapper;
import romsanbryan.data.entities.Aquarium;
import romsanbryan.data.entities.Event;
import romsanbryan.data.entities.Parameters;
import romsanbryan.data.entities.ParametersRange;
import romsanbryan.data.repositories.AquariumRepository;
import romsanbryan.data.repositories.ParametersRangeRepository;
import romsanbryan.data.repositories.ParametersRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Service
public class ParameterServices {

    private static final Logger log = LoggerFactory.getLogger(ParameterServices.class);
    private final ParametersRepository parametersRepository;
    private final ParametersRangeRepository rangeRepository;


    private final ParameterMapper parameterMapper;
    private final EventServices eventServices;
    private final DataUpdatePublisher updatePublisher;
    private final AquariumRepository aquariumRepository;
    private final MessageSource messageSource;


    @Autowired
    public ParameterServices(ParametersRepository parametersRepository, ParametersRangeRepository rangeRepository,
                             ParameterMapper parameterMapper, @Lazy EventServices eventServices,
                             DataUpdatePublisher updatePublisher, AquariumRepository aquariumRepository,
                             MessageSource messageSource) {
        this.parametersRepository = parametersRepository;
        this.rangeRepository = rangeRepository;
        this.parameterMapper = parameterMapper;
        this.eventServices = eventServices;
        this.updatePublisher = updatePublisher;
        this.aquariumRepository = aquariumRepository;
        this.messageSource = messageSource;
    }

    public ParametersResponse getParametersSummary(Integer aquariumId) {
        log.debug("Loading latest parameter summary");
        Parameters last = parametersRepository.findFirstByAquariumIdOrderByIdDesc(aquariumId);
        boolean healthy = this.getNotificationParameters(aquariumId).isEmpty();

        return parameterMapper.toParametersSummaryDto(last, parameterStatus(healthy));
    }


    public List<ParametersRange> getParameterRanges(Integer aquariumId) {
        return rangeRepository.findByAquariumId(aquariumId);
    }

    public void deleteParameterRange(Integer aquariumId, Integer id) {
        log.info("Deleting parameter range with id {}", id);
        ParametersRange parametersRange = rangeRepository.findByIdAndAquariumId(Long.valueOf(id), aquariumId)
                .orElseThrow(() -> new ResourceNotFoundException("Parameter range was not found with id " + id));

        rangeRepository.delete(parametersRange);
        updatePublisher.publish("parameter-range");

    }

    public void updateParameterRange(Integer aquariumId, Integer id, RangeRequest req) {
        log.info("Updating parameter range with id {}", id);

        ParametersRange parametersRange = rangeRepository.findByIdAndAquariumId(Long.valueOf(id), aquariumId)
                .orElseThrow(() -> new ResourceNotFoundException("Parameter range was not found with id " + id));


        rangeRepository.save(parameterMapper.mapParametersRange(req, parametersRange));
        updatePublisher.publish("parameter-range");

    }

    public void createParameterRange(Integer aquariumId, RangeRequest req) {
        log.info("Creating parameter range for {}", req.parameter());
        try {
            rangeRepository.save(
                    assignAquarium(parameterMapper.mapParametersRange(req, new ParametersRange()), aquariumId));
            updatePublisher.publish("parameter-range");
        } catch (AquadeskException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ServiceException("Unable to create parameter range.", e);
        }
    }


    public void addParameters(Integer aquariumId, ParametersRequest req) {
        try {
            log.info("Recording aquarium parameters");

            LocalDate date = req.date() != null ? req.date() : LocalDate.now();

            List<String> provided = Stream.of(
                    req.nh3() != null ? "nh3" : null,
                    req.no2() != null ? "no2" : null,
                    req.ph() != null ? "ph" : null,
                    req.kh() != null ? "kh" : null,
                    req.gh() != null ? "gh" : null,
                    req.temp() != null ? "temp" : null
            ).filter(Objects::nonNull).toList();

            String type = provided.size() == 1 ? provided.get(0) : "parameters";
            String title = type.equals("parameters") ? "Parameters"
                    : type.substring(0, 1).toUpperCase() + type.substring(1);

            Event event = eventServices.createEvent(aquariumId, title, date, type);


            parametersRepository.save(parameterMapper.mapParameters(req, event.getId()));
            updatePublisher.publish("parameters");


        } catch (AquadeskException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ServiceException("Unable to create parameters.", e);

        }
    }

    public Parameters getParameters(Integer aquariumId, Integer idEvent) {
        return parametersRepository.findByIdEventAndAquariumId(idEvent, aquariumId)
                .orElseThrow(() -> new ResourceNotFoundException("Parameters were not found for event " + idEvent));
    }

    public Page<Parameters> getAllParameters(Integer aquariumId, Pageable pageable) {
        Page<Parameters> parameters = parametersRepository.findByAquariumId(aquariumId, pageable);
        return parameters;
    }


    public List<ParametersRange> getAllParameterRanges(Integer aquariumId) {
        List<ParametersRange> parameters = rangeRepository.findByAquariumId(aquariumId);
        return parameters;
    }

    public List<NotificationParameterResponse> getNotificationParameters(Integer aquariumId) {
        log.debug("Evaluating parameter ranges for anomalies");
        Parameters lastParameter = parametersRepository.findFirstByAquariumIdOrderByIdDesc(aquariumId);
        List<NotificationParameterResponse> response = new ArrayList<>();
        if (lastParameter == null) {
            return response;
        }

        ParameterEvaluator evaluator = new DefaultParameterEvaluator(messageSource);
        ParameterValueExtractor extractor = new ParameterValueExtractor(lastParameter);

        for (ParametersRange parameterRange : rangeRepository.findByAquariumId(aquariumId)) {
            String paramName = parameterRange.getParameter();

            if (extractor.hasParameter(paramName)) {
                String anomaly = evaluator.evaluate(
                        extractor.getValue(paramName),
                        parameterRange.getLevel1(),
                        parameterRange.getLevel2(),
                        parameterRange.getLevel3(),
                        parameterRange.getLevel4(),
                        parameterRange.getLevel5(),
                        paramName);
                if (anomaly != null) {

                    response.add(new NotificationParameterResponse(paramName, anomaly));

                }
            }
        }

        return response;

    }

    private ParametersRange assignAquarium(ParametersRange range, Integer aquariumId) {
        Aquarium aquarium = aquariumRepository.findById(aquariumId)
                .orElseThrow(() -> new ResourceNotFoundException("Aquarium was not found with id " + aquariumId));
        aquarium.setId(aquariumId);
        range.setAquarium(aquarium);
        return range;
    }

    private String parameterStatus(boolean healthy) {
        String key = healthy ? "parameters.status.healthy" : "parameters.status.issue";
        return messageSource.getMessage(key, null, LocaleContextHolder.getLocale());
    }

}