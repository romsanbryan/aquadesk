package romsanbryan.data.repositories;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import romsanbryan.data.entities.AquariumTask;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

public interface AquariumTaskRepository extends JpaRepository<AquariumTask, Integer> {

    List<AquariumTask> findByAquariumIdOrderByActionDateAscIdAsc(Integer aquariumId);

    List<AquariumTask> findByAquariumIdAndActionDateGreaterThanEqualAndActionDateLessThanOrderByActionDateAscIdAsc(
            Integer aquariumId, LocalDate start, LocalDate endExclusive);

    Optional<AquariumTask> findByIdAndAquariumId(Integer taskId, Integer aquariumId);

    @Modifying
    @Query("DELETE FROM AquariumTask t WHERE t.aquarium.id = :aquariumId")
    void deleteAllByAquariumId(@Param("aquariumId") Integer aquariumId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM AquariumTask t WHERE t.id = :taskId AND t.aquarium.id = :aquariumId")
    Optional<AquariumTask> findByIdAndAquariumIdForUpdate(@Param("taskId") Integer taskId,
                                                         @Param("aquariumId") Integer aquariumId);
}
