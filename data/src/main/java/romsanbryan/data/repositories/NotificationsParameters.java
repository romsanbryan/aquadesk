package romsanbryan.data.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import romsanbryan.data.entities.Notifications;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationsParameters extends JpaRepository<Notifications, Long> {
    List<Notifications> findByActive(Boolean active);
    List<Notifications> findByAquariumIdAndActive(Integer aquariumId, Boolean active);
    List<Notifications> findByAquariumId(Integer aquariumId);

    Optional<Notifications> findByIdAndAquariumId(Long id, Integer aquariumId);

    @Modifying
    @Query("DELETE FROM Notifications n WHERE n.aquarium.id = :aquariumId")
    void deleteAllByAquariumId(@Param("aquariumId") Integer aquariumId);
}