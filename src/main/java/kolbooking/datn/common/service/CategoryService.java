package kolbooking.datn.common.service;

import jakarta.transaction.Transactional;
import kolbooking.datn.common.domain.Category;
import kolbooking.datn.common.dto.CategoryRequest;
import kolbooking.datn.common.dto.CategoryResponse;
import kolbooking.datn.common.exception.BusinessException;
import kolbooking.datn.common.exception.ErrorCode;
import kolbooking.datn.common.exception.ResourceNotFoundException;
import kolbooking.datn.common.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<CategoryResponse> listTree() {
        List<Category> all = categoryRepository.findAll();
        Map<Long, List<Category>> byParent = all.stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(Category::getParentId));
        return all.stream()
                .filter(c -> c.getParentId() == null)
                .map(c -> toTree(c, byParent))
                .toList();
    }

    @Transactional
    public CategoryResponse create(CategoryRequest req) {
        if (categoryRepository.existsBySlug(req.slug())) {
            throw new BusinessException("Category slug already exists",
                    ErrorCode.BUSINESS_ERROR, HttpStatus.CONFLICT);
        }
        if (req.parentId() != null && !categoryRepository.existsById(req.parentId())) {
            throw ResourceNotFoundException.of("Category", req.parentId());
        }
        Category c = Category.builder()
                .name(req.name())
                .slug(req.slug())
                .parentId(req.parentId())
                .build();
        c = categoryRepository.save(c);
        return toFlat(c);
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest req) {
        Category c = categoryRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Category", id));
        if (!c.getSlug().equals(req.slug()) && categoryRepository.existsBySlug(req.slug())) {
            throw new BusinessException("Category slug already exists",
                    ErrorCode.BUSINESS_ERROR, HttpStatus.CONFLICT);
        }
        if (req.parentId() != null) {
            if (req.parentId().equals(id)) {
                throw new BusinessException("Category cannot be its own parent",
                        ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST);
            }
            if (!categoryRepository.existsById(req.parentId())) {
                throw ResourceNotFoundException.of("Category", req.parentId());
            }
        }
        c.setName(req.name());
        c.setSlug(req.slug());
        c.setParentId(req.parentId());
        return toFlat(categoryRepository.save(c));
    }

    @Transactional
    public void delete(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw ResourceNotFoundException.of("Category", id);
        }
        List<Category> children = categoryRepository.findAllByParentId(id);
        if (!children.isEmpty()) {
            throw new BusinessException("Category has children",
                    ErrorCode.BUSINESS_ERROR, HttpStatus.CONFLICT);
        }
        categoryRepository.deleteById(id);
    }

    private CategoryResponse toTree(Category c, Map<Long, List<Category>> byParent) {
        List<CategoryResponse> children = byParent.getOrDefault(c.getId(), new ArrayList<>())
                .stream().map(ch -> toTree(ch, byParent)).toList();
        return new CategoryResponse(c.getId(), c.getName(), c.getSlug(), c.getParentId(), children);
    }

    private CategoryResponse toFlat(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getSlug(), c.getParentId(), List.of());
    }
}
