package romsanbryan.aquadesk.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import romsanbryan.aquadesk.dto.request.AquariumCreateRequest;
import romsanbryan.aquadesk.dto.request.AquariumFilterRequest;
import romsanbryan.aquadesk.exception.ResourceNotFoundException;
import romsanbryan.aquadesk.exception.ValidationException;
import romsanbryan.data.entities.Aquarium;
import romsanbryan.data.entities.AquariumFilter;
import romsanbryan.data.repositories.AquariumFilterRepository;
import romsanbryan.data.repositories.AquariumRepository;
import romsanbryan.data.repositories.AquariumTaskRepository;
import romsanbryan.data.repositories.Co2Repository;
import romsanbryan.data.repositories.EventRepository;
import romsanbryan.data.repositories.InfoRepository;
import romsanbryan.data.repositories.MaintenanceRepository;
import romsanbryan.data.repositories.NotificationsParameters;
import romsanbryan.data.repositories.ParametersRangeRepository;
import romsanbryan.data.repositories.ParametersRepository;
import romsanbryan.data.repositories.PhotoRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AquariumServiceTest {

    @Mock private AquariumRepository aquariumRepository;
    @Mock private AquariumFilterRepository filterRepository;
    @Mock private AquariumTaskRepository taskRepository;
    @Mock private NotificationsParameters notificationsRepository;
    @Mock private ParametersRangeRepository rangeRepository;
    @Mock private PhotoRepository photoRepository;
    @Mock private InfoRepository infoRepository;
    @Mock private ParametersRepository parametersRepository;
    @Mock private Co2Repository co2Repository;
    @Mock private MaintenanceRepository maintenanceRepository;
    @Mock private EventRepository eventRepository;

    @InjectMocks private AquariumService service;

    @Test
    void rejectsMissingOrBlankAquariumNames() {
        assertThrows(ValidationException.class, () -> service.createAquarium(null));
        assertThrows(ValidationException.class, () -> service.createAquarium(createRequest(null, null)));
        assertThrows(ValidationException.class, () -> service.createAquarium(createRequest("  ", null)));
        verifyNoInteractions(aquariumRepository, filterRepository);
    }

    @Test
    void createsAquariumWithoutOptionalFiltersAndTrimsItsName() {
        when(aquariumRepository.save(any(Aquarium.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Aquarium created = service.createAquarium(createRequest("  Reef  ", null));

        assertEquals("Reef", created.getName());
        verify(aquariumRepository).save(created);
        verifyNoInteractions(filterRepository);
    }

    @Test
    void rejectsEmptyFilterEntriesInsteadOfSavingThem() {
        when(aquariumRepository.save(any(Aquarium.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(ValidationException.class,
                () -> service.createAquarium(createRequest("Reef", java.util.Collections.singletonList(null))));

        verify(aquariumRepository).save(any(Aquarium.class));
        verifyNoInteractions(filterRepository);
    }

    @Test
    void rejectsFiltersThatAreMissingOrBelongToAnotherAquarium() {
        Aquarium otherAquarium = new Aquarium();
        otherAquarium.setId(10);
        AquariumFilter detachedFilter = new AquariumFilter();
        AquariumFilter foreignFilter = new AquariumFilter();
        foreignFilter.setAquarium(otherAquarium);
        when(filterRepository.findById(1)).thenReturn(Optional.of(detachedFilter));
        when(filterRepository.findById(2)).thenReturn(Optional.of(foreignFilter));

        assertThrows(ResourceNotFoundException.class,
                () -> service.updateFilter(9, 1, filterRequest()));
        assertThrows(ResourceNotFoundException.class,
                () -> service.updateFilter(9, 2, filterRequest()));
        when(filterRepository.findById(3)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.updateFilter(9, 3, filterRequest()));
        when(filterRepository.findByIdAndAquariumId(4, 9)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.deleteFilter(9, 4));

        verify(filterRepository, never()).save(any());
        verify(filterRepository, never()).delete(any());
    }

    @Test
    void deletesDetachedFilterWithoutUpdatingAnAquariumCollection() {
        AquariumFilter detachedFilter = new AquariumFilter();
        when(filterRepository.findByIdAndAquariumId(12, 9)).thenReturn(Optional.of(detachedFilter));

        service.deleteFilter(9, 12);

        verify(filterRepository).delete(detachedFilter);
    }

    @Test
    void rejectsLookupOfAnUnknownAquarium() {
        when(aquariumRepository.findById(44)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getAquarium(44));
    }

    private static AquariumCreateRequest createRequest(String name, java.util.List<AquariumFilterRequest> filters) {
        return new AquariumCreateRequest(name, null, null, null, null, null, null, null, filters);
    }

    private static AquariumFilterRequest filterRequest() {
        return new AquariumFilterRequest("Canister", null, null, null, false, false, false);
    }
}
