package org.kwakmunsu.haruhana.domain.category.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.kwakmunsu.haruhana.domain.category.entity.Category;
import org.kwakmunsu.haruhana.domain.category.entity.CategoryGroup;
import org.kwakmunsu.haruhana.domain.category.entity.CategoryTopic;
import org.kwakmunsu.haruhana.domain.category.repository.CategoryGroupJpaRepository;
import org.kwakmunsu.haruhana.domain.category.repository.CategoryJpaRepository;
import org.kwakmunsu.haruhana.domain.category.repository.CategoryTopicJpaRepository;
import org.kwakmunsu.haruhana.domain.category.service.dto.response.CategoryGroupResponse;
import org.kwakmunsu.haruhana.domain.category.service.dto.response.CategoryListResponse;
import org.kwakmunsu.haruhana.domain.category.service.dto.response.CategoryResponse;
import org.kwakmunsu.haruhana.domain.category.service.dto.response.CategoryTopicResponse;
import org.kwakmunsu.haruhana.global.entity.EntityStatus;
import org.kwakmunsu.haruhana.global.support.error.ErrorType;
import org.kwakmunsu.haruhana.global.support.error.HaruHanaException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class CategoryReader {

    private final CategoryJpaRepository categoryJpaRepository;
    private final CategoryGroupJpaRepository categoryGroupJpaRepository;
    private final CategoryTopicJpaRepository categoryTopicJpaRepository;

    public CategoryTopic findCategoryTopic(Long categoryTopicId) {
        return categoryTopicJpaRepository.findByIdAndStatus(categoryTopicId, EntityStatus.ACTIVE)
                .orElseThrow(() -> new HaruHanaException(ErrorType.NOT_FOUND_CATEGORY_TOPIC));
    }

    @Transactional(readOnly = true)
    public CategoryListResponse getCategories() {
        List<Category> categories = categoryJpaRepository.findAllByStatus(EntityStatus.ACTIVE);

        List<CategoryResponse> categoryResponses = categories.stream()
                .map(this::buildCategoryResponse)
                .toList();

        return CategoryListResponse.from(categoryResponses);
    }

    private CategoryResponse buildCategoryResponse(Category category) {
        List<CategoryGroup> categoryGroups = categoryGroupJpaRepository.findByCategoryIdAndStatus(category.getId(), EntityStatus.ACTIVE);

        List<CategoryGroupResponse> groupResponses = categoryGroups.stream()
                .map(this::buildCategoryGroupResponse)
                .toList();

        return CategoryResponse.of(category, groupResponses);
    }

    private CategoryGroupResponse buildCategoryGroupResponse(CategoryGroup categoryGroup) {
        List<CategoryTopic> categoryTopics = categoryTopicJpaRepository.findByGroupIdAndStatus(categoryGroup.getId(), EntityStatus.ACTIVE);

        List<CategoryTopicResponse> topicResponses = categoryTopics.stream()
                .map(CategoryTopicResponse::from)
                .toList();

        return CategoryGroupResponse.of(categoryGroup, topicResponses);
    }

}