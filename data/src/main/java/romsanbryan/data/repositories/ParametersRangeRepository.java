package romsanbryan.data.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import romsanbryan.data.entities.ParametersRange;

import java.util.List;


@Repository
public interface ParametersRangeRepository extends JpaRepository<ParametersRange, Long> {
    List<ParametersRange> findByAquariumId(Integer aquariumId);
    java.util.Optional<ParametersRange> findByIdAndAquariumId(Long id, Integer aquariumId);

    @Modifying
    @Query("DELETE FROM ParametersRange r WHERE r.aquarium.id = :aquariumId")
    void deleteAllByAquariumId(@Param("aquariumId") Integer aquariumId);
}
