package org.kwakmunsu.haruhana.domain.category.repository;

import java.util.List;
import java.util.Optional;
import org.kwakmunsu.haruhana.domain.category.entity.CategoryTopic;
import org.kwakmunsu.haruhana.global.entity.EntityStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryTopicJpaRepository extends JpaRepository<CategoryTopic, Long> {

    Optional<CategoryTopic> findByIdAndStatus(Long id, EntityStatus status);
    Optional<CategoryTopic> findByName(String name);
    List<CategoryTopic> findByGroupIdAndStatus(Long groupId, EntityStatus status);
    boolean existsByNameAndStatus(String name, EntityStatus status);

}