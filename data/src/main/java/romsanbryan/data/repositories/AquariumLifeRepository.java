package romsanbryan.data.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import romsanbryan.data.entities.AquariumLife;

import java.util.List;
import java.util.Optional;

public interface AquariumLifeRepository extends JpaRepository<AquariumLife, Integer> {

    List<AquariumLife> findByAquariumIdOrderByCategoryAscCommonNameAsc(Integer aquariumId);

    Optional<AquariumLife> findByIdAndAquariumId(Integer id, Integer aquariumId);

    @Modifying
    @Query("DELETE FROM AquariumLife life WHERE life.aquarium.id = :aquariumId")
    void deleteAllByAquariumId(@Param("aquariumId") Integer aquariumId);
}
