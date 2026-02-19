package io.pinkspider.leveluptogethermvp.profanity.infrastructure;

import io.pinkspider.leveluptogethermvp.profanity.domain.entity.ProfanityWord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProfanityWordRepository extends JpaRepository<ProfanityWord, Long> {

    List<ProfanityWord> findAllByIsActiveTrue();

    Optional<ProfanityWord> findByWord(String word);

    boolean existsByWord(String word);

    @Query("SELECT pw FROM ProfanityWord pw WHERE " +
           "(:keyword IS NULL OR pw.word LIKE %:keyword% " +
           "OR pw.description LIKE %:keyword%)")
    Page<ProfanityWord> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
