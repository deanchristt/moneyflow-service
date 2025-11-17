package com.moneyflow.service;

import com.moneyflow.exception.BadRequestException;
import com.moneyflow.exception.ResourceNotFoundException;
import com.moneyflow.model.dto.category.CategoryResponse;
import com.moneyflow.model.dto.category.CreateCategoryRequest;
import com.moneyflow.model.dto.category.UpdateCategoryRequest;
import com.moneyflow.model.entity.Category;
import com.moneyflow.model.entity.User;
import com.moneyflow.model.enums.CategoryType;
import com.moneyflow.repository.CategoryRepository;
import com.moneyflow.repository.UserRepository;
import com.moneyflow.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();

        if (categoryRepository.existsByUserIdAndName(userId, request.getName())) {
            throw new BadRequestException("Category with this name already exists");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Category category = Category.builder()
                .user(user)
                .name(request.getName())
                .type(request.getType())
                .icon(request.getIcon())
                .color(request.getColor())
                .isDefault(false)
                .build();

        category = categoryRepository.save(category);
        return mapToResponse(category);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        Long userId = SecurityUtils.getCurrentUserId();
        return categoryRepository.findAllAvailableForUser(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategoriesByType(CategoryType type) {
        Long userId = SecurityUtils.getCurrentUserId();
        return categoryRepository.findAllAvailableForUserByType(userId, type)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        // Check if user has access to this category
        Long userId = SecurityUtils.getCurrentUserId();
        if (!category.getIsDefault() && !category.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Category", "id", id);
        }

        return mapToResponse(category);
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, UpdateCategoryRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        // Cannot update default categories or other user's categories
        if (category.getIsDefault() || !category.getUser().getId().equals(userId)) {
            throw new BadRequestException("Cannot update this category");
        }

        if (request.getName() != null && !request.getName().equals(category.getName())) {
            if (categoryRepository.existsByUserIdAndName(userId, request.getName())) {
                throw new BadRequestException("Category with this name already exists");
            }
            category.setName(request.getName());
        }

        if (request.getType() != null) {
            category.setType(request.getType());
        }

        if (request.getIcon() != null) {
            category.setIcon(request.getIcon());
        }

        if (request.getColor() != null) {
            category.setColor(request.getColor());
        }

        category = categoryRepository.save(category);
        return mapToResponse(category);
    }

    @Transactional
    public void deleteCategory(Long id) {
        Long userId = SecurityUtils.getCurrentUserId();

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        // Cannot delete default categories or other user's categories
        if (category.getIsDefault() || !category.getUser().getId().equals(userId)) {
            throw new BadRequestException("Cannot delete this category");
        }

        // Soft delete
        category.setIsActive(false);
        categoryRepository.save(category);
    }

    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .type(category.getType())
                .icon(category.getIcon())
                .color(category.getColor())
                .isDefault(category.getIsDefault())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}
