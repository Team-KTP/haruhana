package org.kwakmunsu.haruhana.domain.category.repository;

import java.util.List;
import java.util.Optional;
import org.kwakmunsu.haruhana.domain.category.entity.Category;
import org.kwakmunsu.haruhana.global.entity.EntityStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryJpaRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByIdAndStatus(Long id, EntityStatus status);
    List<Category> findAllByStatus(EntityStatus status);
    boolean existsByNameAndStatus(String name, EntityStatus status);
    boolean existsByIdAndStatus(Long id, EntityStatus status);

}