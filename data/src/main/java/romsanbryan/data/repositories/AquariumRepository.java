package romsanbryan.data.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import romsanbryan.data.entities.Aquarium;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;

public interface AquariumRepository extends JpaRepository<Aquarium, Integer> {

    @EntityGraph(attributePaths = "filters")
    List<Aquarium> findAllByOrderByNameAsc();

    List<Aquarium> findAllByOrderByIdAsc();
}
