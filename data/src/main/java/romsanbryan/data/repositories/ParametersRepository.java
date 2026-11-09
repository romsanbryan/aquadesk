package romsanbryan.data.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import romsanbryan.data.entities.Parameters;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParametersRepository extends JpaRepository<Parameters, Long> {
    Parameters findFirstByOrderByIdDesc();

    Optional<Parameters> findByIdEvent(Integer idEvent);

    @Query("SELECT p FROM Parameters p JOIN romsanbryan.data.entities.Event e ON e.id = p.idEvent " +
            "WHERE p.idEvent = :idEvent AND e.aquarium.id = :aquariumId")
    Optional<Parameters> findByIdEventAndAquariumId(@Param("idEvent") Integer idEvent,
                                                    @Param("aquariumId") Integer aquariumId);

    @Query("SELECT p FROM Parameters p JOIN romsanbryan.data.entities.Event e ON e.id = p.idEvent " +
            "WHERE e.aquarium.id = :aquariumId ORDER BY p.id DESC")
    Page<Parameters> findByAquariumId(@Param("aquariumId") Integer aquariumId, Pageable pageable);

    @Query("SELECT p FROM Parameters p JOIN romsanbryan.data.entities.Event e ON e.id = p.idEvent " +
            "WHERE e.aquarium.id = :aquariumId ORDER BY p.id DESC")
    List<Parameters> findAllByAquariumId(@Param("aquariumId") Integer aquariumId);

    @Query("SELECT p FROM Parameters p JOIN romsanbryan.data.entities.Event e ON e.id = p.idEvent " +
            "WHERE e.aquarium.id = :aquariumId ORDER BY p.id DESC LIMIT 1")
    Parameters findFirstByAquariumIdOrderByIdDesc(@Param("aquariumId") Integer aquariumId);

    @Modifying
    @Query("DELETE FROM Parameters p WHERE p.idEvent IN " +
            "(SELECT e.id FROM romsanbryan.data.entities.Event e WHERE e.aquarium.id = :aquariumId)")
    void deleteAllByAquariumId(@Param("aquariumId") Integer aquariumId);
}
