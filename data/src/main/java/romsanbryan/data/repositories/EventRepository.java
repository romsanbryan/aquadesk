package romsanbryan.data.repositories;

// EventRepository.java

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import romsanbryan.data.entities.Event;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Integer> {

    // Eventos que empiezan exactamente en esa fecha
    List<Event> findByStartDate(LocalDate startDate);
    List<Event> findByAquariumIdAndStartDate(Integer aquariumId, LocalDate startDate);

    // Eventos a partir de una fecha (>=)
    List<Event> findByStartDateGreaterThanEqual(LocalDate startDate);

    // Eventos entre dos fechas
    List<Event> findByStartDateBetween(LocalDate from, LocalDate to);
    List<Event> findByAquariumIdAndStartDateBetween(Integer aquariumId, LocalDate from, LocalDate to);
    List<Event> findByAquariumIdOrderByStartDateAsc(Integer aquariumId);

    Optional<Event> findFirstByTypeOrderByStartDateDesc(String type);
    Optional<Event> findFirstByAquariumIdAndTypeOrderByStartDateDesc(Integer aquariumId, String type);

    // EventoRepository
    List<Event> findByStartDateAndType(LocalDate startDate, String type);
    List<Event> findByAquariumIdAndStartDateAndType(Integer aquariumId, LocalDate startDate, String type);

    Optional<Event> findByIdAndAquariumId(Integer id, Integer aquariumId);

    @Modifying
    @Query("DELETE FROM Event e WHERE e.aquarium.id = :aquariumId")
    void deleteAllByAquariumId(@Param("aquariumId") Integer aquariumId);

}