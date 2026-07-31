package com.moneyflow.service;

import com.moneyflow.exception.BadRequestException;
import com.moneyflow.exception.ResourceNotFoundException;
import com.moneyflow.model.dto.tag.CreateTagRequest;
import com.moneyflow.model.dto.tag.TagResponse;
import com.moneyflow.model.entity.Tag;
import com.moneyflow.model.entity.User;
import com.moneyflow.repository.TagRepository;
import com.moneyflow.repository.UserRepository;
import com.moneyflow.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;
    private final UserRepository userRepository;

    @Transactional
    public TagResponse createTag(CreateTagRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (tagRepository.existsByUserIdAndName(userId, request.getName())) {
            throw new BadRequestException("Tag with this name already exists");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Tag tag = Tag.builder()
                .user(user)
                .name(request.getName())
                .color(request.getColor())
                .build();
        tag = tagRepository.save(tag);
        return mapToResponse(tag);
    }

    @Transactional(readOnly = true)
    public List<TagResponse> getTags() {
        Long userId = SecurityUtils.getCurrentUserId();
        return tagRepository.findByUserIdAndIsActiveTrue(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TagResponse updateTag(Long id, CreateTagRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        Tag tag = tagRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag", "id", id));

        if (request.getName() != null && !request.getName().equals(tag.getName())) {
            if (tagRepository.existsByUserIdAndName(userId, request.getName())) {
                throw new BadRequestException("Tag with this name already exists");
            }
            tag.setName(request.getName());
        }
        if (request.getColor() != null) {
            tag.setColor(request.getColor());
        }
        tag = tagRepository.save(tag);
        return mapToResponse(tag);
    }

    @Transactional
    public void deleteTag(Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        Tag tag = tagRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag", "id", id));
        tag.setIsActive(false);
        tagRepository.save(tag);
    }

    private TagResponse mapToResponse(Tag tag) {
        return TagResponse.builder()
                .id(tag.getId())
                .name(tag.getName())
                .color(tag.getColor())
                .build();
    }
}
