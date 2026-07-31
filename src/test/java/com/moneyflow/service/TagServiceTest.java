package com.moneyflow.service;

import com.moneyflow.exception.BadRequestException;
import com.moneyflow.model.dto.tag.CreateTagRequest;
import com.moneyflow.model.dto.tag.TagResponse;
import com.moneyflow.model.entity.Tag;
import com.moneyflow.model.entity.User;
import com.moneyflow.repository.TagRepository;
import com.moneyflow.repository.UserRepository;
import com.moneyflow.security.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TagServiceTest {

    @Mock private TagRepository tagRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private TagService service;

    private MockedStatic<SecurityUtils> security;

    @BeforeEach
    void setUp() {
        security = mockStatic(SecurityUtils.class);
        security.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
        User u = User.builder().email("u@ex.com").build();
        u.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        when(tagRepository.save(any(Tag.class))).thenAnswer(i -> i.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        security.close();
    }

    @Test
    void createTagSucceedsWhenNameFree() {
        when(tagRepository.existsByUserIdAndName(1L, "work")).thenReturn(false);
        TagResponse res = service.createTag(CreateTagRequest.builder().name("work").color("#123").build());
        assertThat(res.getName()).isEqualTo("work");
        assertThat(res.getColor()).isEqualTo("#123");
    }

    @Test
    void createTagRejectsDuplicateName() {
        when(tagRepository.existsByUserIdAndName(1L, "work")).thenReturn(true);
        assertThatThrownBy(() -> service.createTag(CreateTagRequest.builder().name("work").build()))
                .isInstanceOf(BadRequestException.class);
    }
}
