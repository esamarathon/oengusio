package app.oengus.adapter.jpa.repository;

import app.oengus.adapter.jpa.entity.CategoryEntity;
import app.oengus.adapter.jpa.entity.GameEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GameRepository extends CrudRepository<GameEntity, Integer> {
    @Query(value = "SELECT g FROM GameEntity g WHERE g.submission.id = :submissionId AND g.submission.marathon.id = :marathonId ORDER BY g.id ASC")
    List<GameEntity> findBySubmissionId(@Param("marathonId") String marathonId, @Param("submissionId") int submissionId);

    @Query(value = "SELECT g FROM GameEntity g WHERE g.submission.marathon.id = :marathonId ORDER BY g.id ASC")
    List<GameEntity> findByMarathon(@Param("marathonId") String marathonId);

    Optional<GameEntity> findByCategoriesContaining(CategoryEntity category);

}
