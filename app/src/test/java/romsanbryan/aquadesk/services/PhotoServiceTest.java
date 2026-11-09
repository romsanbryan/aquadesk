package romsanbryan.aquadesk.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import romsanbryan.aquadesk.exception.ResourceNotFoundException;
import romsanbryan.aquadesk.exception.ServiceException;
import romsanbryan.aquadesk.mapper.PhotoMapper;
import romsanbryan.data.entities.Event;
import romsanbryan.data.entities.Photo;
import romsanbryan.data.repositories.PhotoRepository;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhotoServiceTest {

    @Mock private EventServices eventServices;
    @Mock private PhotoRepository photoRepository;
    @Mock private PhotoMapper photoMapper;
    @Mock private DataUpdatePublisher updatePublisher;
    @InjectMocks private PhotoService service;

    private final LocalDate date = LocalDate.of(2025, 4, 12);

    @Test
    void createsAquariumPhotoAndPublishesOnlyAfterStorageSucceeds() throws Exception {
        Event event = event(52);
        Photo photo = photo(11, 52, new byte[]{3});
        MultipartFile file = new MockMultipartFile("photo", "tank.png", "image/png", new byte[]{3});
        when(eventServices.createEvent(7, "Photo", date, "photo")).thenReturn(event);
        when(photoMapper.createPhoto(event, file)).thenReturn(photo);

        service.createPhotoEvent(7, date, file);

        verify(eventServices).createEvent(7, "Photo", date, "photo");
        verify(photoRepository).save(photo);
        verify(updatePublisher).publish("photo");
    }

    @Test
    void wrapsAquariumPhotoMapperIoFailure() throws Exception {
        Event event = event(53);
        MultipartFile file = new MockMultipartFile("photo", "bad.jpg", "image/jpeg", new byte[0]);
        IOException cause = new IOException("unreadable");
        when(eventServices.createEvent(7, "Photo", date, "photo")).thenReturn(event);
        when(photoMapper.createPhoto(event, file)).thenThrow(cause);

        ServiceException thrown = assertThrows(ServiceException.class,
                () -> service.createPhotoEvent(7, date, file));

        assertSame(cause, thrown.getCause());
        verifyNoInteractions(photoRepository, updatePublisher);
    }

    @Test
    void propagatesEventCreationAndRepositoryFailuresWithoutPublishing() throws Exception {
        MultipartFile file = new MockMultipartFile("photo", "tank.jpg", "image/jpeg", new byte[]{1});
        RuntimeException eventFailure = new IllegalStateException("event failed");
        when(eventServices.createEvent(3, "Photo", date, "photo")).thenThrow(eventFailure);
        assertSame(eventFailure, assertThrows(IllegalStateException.class,
                () -> service.createPhotoEvent(3, date, file)));
        verifyNoInteractions(photoMapper, photoRepository, updatePublisher);

        Event event = event(41);
        Photo photo = photo(12, 41, new byte[]{1});
        RuntimeException storageFailure = new IllegalStateException("database failed");
        doReturn(event).when(eventServices).createEvent(3, "Photo", date, "photo");
        when(photoMapper.createPhoto(event, file)).thenReturn(photo);
        when(photoRepository.save(photo)).thenThrow(storageFailure);
        assertSame(storageFailure, assertThrows(IllegalStateException.class,
                () -> service.createPhotoEvent(3, date, file)));
        verify(updatePublisher, never()).publish(anyString());
    }

    @Test
    void listsAquariumPhotoIdsAndExcludesEmptyData() {
        Event event = event(71);
        when(eventServices.findByStartDateAndType(9, date, "photo")).thenReturn(List.of(event));
        when(photoRepository.findByEventId(71)).thenReturn(List.of(
                photo(8, 71, new byte[]{9}),
                photo(11, 71, new byte[]{2}, LocalDateTime.of(2025, 4, 12, 10, 30)),
                photo(9, 71, new byte[0]), photo(10, 71, null)));

        List<Map<String, Object>> result = service.getPhotoIdsByDay(9, date);

        assertEquals(2, result.size());
        assertEquals(8, result.get(0).get("id"));
        assertEquals(71, result.get(0).get("eventId"));
        assertEquals("", result.get(0).get("createdAt"));
        assertEquals("2025-04-12T10:30", result.get(1).get("createdAt"));
        verify(eventServices).findByStartDateAndType(9, date, "photo");
    }

    @Test
    void aquariumRecentPhotoLimitIsClampedToOne() {
        when(photoRepository.findAllByAquariumId(eq(6), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(photo(5, 8, new byte[]{1},
                                LocalDateTime.of(2025, 4, 12, 11, 0)),
                        photo(6, 8, new byte[]{2}), photo(7, 8, new byte[0]), photo(8, 8, null))));

        List<Map<String, Object>> result = service.getLastPhotos(6, 0);
        assertEquals(2, result.size());
        assertEquals("2025-04-12T11:00", result.get(0).get("createdAt"));
        assertEquals("", result.get(1).get("createdAt"));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(photoRepository).findAllByAquariumId(eq(6), pageable.capture());
        assertEquals(1, pageable.getValue().getPageSize());
    }

    @Test
    void scopedPhotoLookupVerifiesOwningAquariumAndHidesLookupFailures() {
        Photo photo = photo(17, 30, new byte[]{1});
        when(photoRepository.findById(17)).thenReturn(Optional.of(photo));
        when(eventServices.getEvent(4, 30)).thenReturn(event(30));
        assertSame(photo, service.getPhoto(4, 17));

        when(eventServices.getEvent(5, 30)).thenThrow(new ResourceNotFoundException("wrong aquarium"));
        ResourceNotFoundException thrown = assertThrows(ResourceNotFoundException.class, () -> service.getPhoto(5, 17));
        assertEquals("Photo was not found with id 17", thrown.getMessage());

        when(photoRepository.findById(19)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getPhoto(4, 19));
        verify(eventServices, times(2)).getEvent(anyInt(), eq(30));
    }

    @Test
    void retainsEventWhenOtherPhotosRemainAndSupportsAquariumScopedDeletion() {
        Photo photo = photo(21, 81, new byte[]{1});
        when(photoRepository.findById(21)).thenReturn(Optional.of(photo));
        when(eventServices.getEvent(3, 81)).thenReturn(event(81));
        when(photoRepository.findByEventId(81)).thenReturn(List.of(photo(22, 81, new byte[]{2})));

        service.deletePhoto(3, 21);

        verify(photoRepository).deleteById(21);
        verify(eventServices, never()).deleteEventById(anyInt(), anyInt());
        verify(updatePublisher).publish("photo");
    }

    @Test
    void deletesAquariumEventWhenDeletingItsLastPhoto() {
        Photo photo = photo(23, 83, new byte[]{1});
        when(photoRepository.findById(23)).thenReturn(Optional.of(photo));
        when(eventServices.getEvent(11, 83)).thenReturn(event(83));
        when(photoRepository.findByEventId(83)).thenReturn(List.of());

        service.deletePhoto(11, 23);

        verify(photoRepository).deleteById(23);
        verify(eventServices).deleteEventById(11, 83);
        verify(updatePublisher).publish("photo");
    }

    @Test
    void missingPhotoOrRepositoryDeleteFailureDoesNotPublish() {
        when(photoRepository.findById(24)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.deletePhoto(4, 24));
        verify(photoRepository, never()).deleteById(anyInt());
        verifyNoInteractions(updatePublisher);

        Photo photo = photo(25, 82, new byte[]{1});
        when(photoRepository.findById(25)).thenReturn(Optional.of(photo));
        when(eventServices.getEvent(4, 82)).thenReturn(event(82));
        doThrow(new IllegalStateException("delete failed")).when(photoRepository).deleteById(25);
        assertThrows(IllegalStateException.class, () -> service.deletePhoto(4, 25));
        verify(updatePublisher, never()).publish(anyString());
    }

    private static Event event(int id) {
        Event event = new Event();
        event.setId(id);
        return event;
    }

    private static Photo photo(int id, int eventId, byte[] data) {
        return photo(id, eventId, data, null);
    }

    private static Photo photo(int id, int eventId, byte[] data, LocalDateTime createdAt) {
        Photo photo = new Photo();
        photo.setId(id);
        photo.setEventId(eventId);
        photo.setPhotoData(data);
        photo.setCreatedAt(createdAt);
        return photo;
    }
}
