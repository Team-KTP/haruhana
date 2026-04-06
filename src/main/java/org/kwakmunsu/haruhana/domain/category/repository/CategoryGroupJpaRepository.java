package org.kwakmunsu.haruhana.domain.category.repository;

import java.util.List;
import java.util.Optional;
import org.kwakmunsu.haruhana.domain.category.entity.CategoryGroup;
import org.kwakmunsu.haruhana.global.entity.EntityStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryGroupJpaRepository extends JpaRepository<CategoryGroup, Long> {

    Optional<CategoryGroup> findByIdAndStatus(Long id, EntityStatus status);
    List<CategoryGroup> findByCategoryIdAndStatus(Long categoryId, EntityStatus status);
    boolean existsByNameAndStatus(String name, EntityStatus status);
    boolean existsByIdAndStatus(Long groupId, EntityStatus status);

}