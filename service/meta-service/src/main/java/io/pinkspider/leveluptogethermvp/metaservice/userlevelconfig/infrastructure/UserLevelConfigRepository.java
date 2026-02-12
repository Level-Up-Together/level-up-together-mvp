package io.pinkspider.leveluptogethermvp.metaservice.userlevelconfig.infrastructure;

import io.pinkspider.leveluptogethermvp.metaservice.userlevelconfig.domain.entity.UserLevelConfig;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserLevelConfigRepository extends JpaRepository<UserLevelConfig, Long> {

    Optional<UserLevelConfig> findByLevel(Integer level);

    List<UserLevelConfig> findAllByOrderByLevelAsc();

    @Query("SELECT lc FROM UserLevelConfig lc WHERE lc.level = " +
           "(SELECT MAX(lc2.level) FROM UserLevelConfig lc2 WHERE lc2.cumulativeExp <= :totalExp)")
    Optional<UserLevelConfig> findLevelByTotalExp(@Param("totalExp") Integer totalExp);

    @Query("SELECT MAX(lc.level) FROM UserLevelConfig lc")
    Integer findMaxLevel();

    boolean existsByLevel(Integer level);

    @Query("SELECT lc FROM UserLevelConfig lc WHERE " +
           "(:keyword IS NULL OR CAST(lc.level AS string) LIKE %:keyword% " +
           "OR CAST(lc.requiredExp AS string) LIKE %:keyword%)")
    Page<UserLevelConfig> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
