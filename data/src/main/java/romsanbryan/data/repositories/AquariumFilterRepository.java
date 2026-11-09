package romsanbryan.data.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import romsanbryan.data.entities.AquariumFilter;

import java.util.Optional;

public interface AquariumFilterRepository extends JpaRepository<AquariumFilter, Integer> {
    Optional<AquariumFilter> findByIdAndAquariumId(Integer id, Integer aquariumId);

    @Modifying
    @Query("DELETE FROM AquariumFilter f WHERE f.aquarium.id = :aquariumId")
    void deleteAllByAquariumId(@Param("aquariumId") Integer aquariumId);
}
