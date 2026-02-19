package io.pinkspider.leveluptogethermvp.metaservice.guildlevelconfig.infrastructure;

import io.pinkspider.leveluptogethermvp.metaservice.guildlevelconfig.domain.entity.GuildLevelConfig;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GuildLevelConfigRepository extends JpaRepository<GuildLevelConfig, Long> {

    Optional<GuildLevelConfig> findByLevel(Integer level);

    List<GuildLevelConfig> findAllByOrderByLevelAsc();

    @Query("SELECT MAX(glc.level) FROM GuildLevelConfig glc")
    Integer findMaxLevel();

    boolean existsByLevel(Integer level);

    @Query("SELECT g FROM GuildLevelConfig g WHERE :keyword IS NULL OR "
        + "CAST(g.level AS string) LIKE CONCAT('%', :keyword, '%') OR "
        + "g.title LIKE CONCAT('%', :keyword, '%') OR "
        + "g.description LIKE CONCAT('%', :keyword, '%')")
    Page<GuildLevelConfig> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
