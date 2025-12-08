package com.moneyflow.controller;

import com.moneyflow.model.dto.ApiResponse;
import com.moneyflow.model.dto.tag.CreateTagRequest;
import com.moneyflow.model.dto.tag.TagResponse;
import com.moneyflow.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/tags")
@RequiredArgsConstructor
@Tag(name = "Tags", description = "Transaction tag management endpoints")
public class TagController {

    private final TagService tagService;

    @PostMapping
    @Operation(summary = "Create a tag")
    public ResponseEntity<ApiResponse<TagResponse>> createTag(@Valid @RequestBody CreateTagRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tag created successfully", tagService.createTag(request)));
    }

    @GetMapping
    @Operation(summary = "Get all tags")
    public ResponseEntity<ApiResponse<List<TagResponse>>> getTags() {
        return ResponseEntity.ok(ApiResponse.success(tagService.getTags()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a tag")
    public ResponseEntity<ApiResponse<TagResponse>> updateTag(
            @PathVariable Long id, @Valid @RequestBody CreateTagRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Tag updated successfully", tagService.updateTag(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a tag")
    public ResponseEntity<ApiResponse<Void>> deleteTag(@PathVariable Long id) {
        tagService.deleteTag(id);
        return ResponseEntity.ok(ApiResponse.success("Tag deleted successfully", null));
    }
}
