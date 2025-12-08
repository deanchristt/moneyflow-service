package com.moneyflow.repository;

import com.moneyflow.model.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    List<Tag> findByUserIdAndIsActiveTrue(Long userId);

    Optional<Tag> findByIdAndUserId(Long id, Long userId);

    List<Tag> findByUserIdAndIdIn(Long userId, Set<Long> ids);

    boolean existsByUserIdAndName(Long userId, String name);
}
