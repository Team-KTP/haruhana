package org.kwakmunsu.haruhana.domain.category.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.kwakmunsu.haruhana.domain.category.entity.Category;
import org.kwakmunsu.haruhana.domain.category.entity.CategoryGroup;
import org.kwakmunsu.haruhana.domain.category.entity.CategoryTopic;
import org.kwakmunsu.haruhana.domain.category.repository.CategoryGroupJpaRepository;
import org.kwakmunsu.haruhana.domain.category.repository.CategoryJpaRepository;
import org.kwakmunsu.haruhana.domain.category.repository.CategoryTopicJpaRepository;
import org.kwakmunsu.haruhana.global.entity.EntityStatus;
import org.kwakmunsu.haruhana.global.support.error.ErrorType;
import org.kwakmunsu.haruhana.global.support.error.HaruHanaException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 카테고리 관리 서비스 (생성, 수정, 삭제)
 */
@RequiredArgsConstructor
@Service
public class CategoryManager {

    private final CategoryValidator categoryValidator;
    private final CategoryJpaRepository categoryJpaRepository;
    private final CategoryGroupJpaRepository categoryGroupJpaRepository;
    private final CategoryTopicJpaRepository categoryTopicJpaRepository;

    public Category createCategory(String name) {
        categoryValidator.validateNewCategory(name);

        return categoryJpaRepository.save(Category.create(name));
    }

    public CategoryGroup createCategoryGroup(Long categoryId, String name) {
        categoryValidator.validateNewGroup(categoryId, name);

        return categoryGroupJpaRepository.save(CategoryGroup.create(
                categoryId,
                name
        ));
    }

    public CategoryTopic createCategoryTopic(Long groupId, String name) {
        categoryValidator.validateNewTopic(groupId, name);

        return categoryTopicJpaRepository.save(CategoryTopic.create(
                groupId,
                name
        ));
    }

    @Transactional
    public void updateCategory(Long categoryId, String name) {
        Category category = categoryJpaRepository.findByIdAndStatus(categoryId, EntityStatus.ACTIVE)
                .orElseThrow(() -> new HaruHanaException(ErrorType.NOT_FOUND_CATEGORY));

        if (!category.getName().equals(name)) {
            categoryValidator.validateNewCategory(name);
        }
        category.updateName(name);
    }

    @Transactional
    public void deleteCategory(Long categoryId) {
        Category category = categoryJpaRepository.findById(categoryId)
                .orElseThrow(() -> new HaruHanaException(ErrorType.NOT_FOUND_CATEGORY));

        if (category.isDeleted()) return;

        List<CategoryGroup> groups = categoryGroupJpaRepository.findByCategoryIdAndStatus(categoryId, EntityStatus.ACTIVE);
        groups.forEach(group -> {
            categoryTopicJpaRepository.findByGroupIdAndStatus(group.getId(), EntityStatus.ACTIVE)
                    .forEach(CategoryTopic::delete);
            group.delete();
        });

        category.delete();
    }

    @Transactional
    public void updateCategoryGroup(Long groupId, String name) {
        CategoryGroup group = categoryGroupJpaRepository.findByIdAndStatus(groupId, EntityStatus.ACTIVE)
                .orElseThrow(() -> new HaruHanaException(ErrorType.NOT_FOUND_CATEGORY_GROUP));

        if (!group.getName().equals(name)) {
            categoryValidator.validateGroupName(name);
        }
        group.updateName(name);
    }

    @Transactional
    public void deleteCategoryGroup(Long groupId) {
        CategoryGroup group = categoryGroupJpaRepository.findById(groupId)
                .orElseThrow(() -> new HaruHanaException(ErrorType.NOT_FOUND_CATEGORY_GROUP));

        if (group.isDeleted()) return;

        categoryTopicJpaRepository.findByGroupIdAndStatus(groupId, EntityStatus.ACTIVE)
                .forEach(CategoryTopic::delete);

        group.delete();
    }

    @Transactional
    public void updateCategoryTopic(Long topicId, String name) {
        CategoryTopic topic = categoryTopicJpaRepository.findByIdAndStatus(topicId, EntityStatus.ACTIVE)
                .orElseThrow(() -> new HaruHanaException(ErrorType.NOT_FOUND_CATEGORY_TOPIC));

        if (!topic.getName().equals(name)) {
            categoryValidator.validateTopicName(name);
        }
        topic.updateName(name);
    }

    @Transactional
    public void deleteCategoryTopic(Long topicId) {
        CategoryTopic topic = categoryTopicJpaRepository.findById(topicId)
                .orElseThrow(() -> new HaruHanaException(ErrorType.NOT_FOUND_CATEGORY_TOPIC));

        if (topic.isDeleted()) return;

        topic.delete();
    }

}