package romsanbryan.aquadesk.services;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import romsanbryan.aquadesk.mapper.TreatmentMapper;
import romsanbryan.data.entities.Co2;
import romsanbryan.data.entities.Event;
import romsanbryan.data.repositories.Co2Repository;

import java.time.LocalDate;

@Service
public class TreatmentService {

    private static final Logger log = LoggerFactory.getLogger(TreatmentService.class);
    private final EventServices eventServices;
    private final Co2Repository co2Repository;
    private final TreatmentMapper treatmentMapper;
    private final DataUpdatePublisher updatePublisher;

    @Autowired
    public TreatmentService(EventServices eventServices, Co2Repository co2Repository, TreatmentMapper treatmentMapper, DataUpdatePublisher updatePublisher) {
        this.eventServices = eventServices;
        this.co2Repository = co2Repository;
        this.treatmentMapper = treatmentMapper;
        this.updatePublisher = updatePublisher;
    }

    public void createTreatment(Integer aquariumId, String name, String value, LocalDate date) {
        log.info("Creating CO2 treatment for product {}", name);
        date = date != null ? date : LocalDate.now();
        log.debug("Recording CO2 treatment aquariumId={} date={}", aquariumId, date);

        Event treatmentEvent = eventServices.createEvent(aquariumId, name, date, "co2");

        Co2 co2 = treatmentMapper.getCo2(name, value, treatmentEvent);

        co2Repository.save(co2);
        updatePublisher.publish("treatment");
    }

}