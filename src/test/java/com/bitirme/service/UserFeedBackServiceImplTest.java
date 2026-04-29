package com.bitirme.service;

import com.bitirme.dto.feed.UserFeedBackCreateRequest;
import com.bitirme.entity.Category;
import com.bitirme.entity.News;
import com.bitirme.entity.User;
import com.bitirme.entity.UserFeedBack;
import com.bitirme.exception.NotFoundException;
import com.bitirme.repository.CategoryRepository;
import com.bitirme.repository.ModelVersionRepository;
import com.bitirme.repository.NewsRepository;
import com.bitirme.repository.UserFeedBackRepository;
import com.bitirme.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserFeedBackServiceImplTest {

    @Mock
    private UserFeedBackRepository userFeedBackRepository;
    @Mock
    private NewsRepository newsRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ModelVersionRepository modelVersionRepository;
    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private UserFeedBackServiceImpl service;

    @Test
    @DisplayName("create haber yoksa NotFoundException")
    void createNewsMissingTest() {
        UserFeedBackCreateRequest req = buildRequest();
        when(newsRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(req,"metehan"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("create başarılı")
    void createSuccessTest() {
        UserFeedBackCreateRequest req = buildRequest();
        News news = new News();
        news.setId(1L);
        User user = new User();
        user.setId(2L);
        Category selected = new Category();
        selected.setId(3);

        when(newsRepository.findById(1L)).thenReturn(Optional.of(news));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(3)).thenReturn(Optional.of(selected));
        when(userFeedBackRepository.save(any(UserFeedBack.class))).thenAnswer(inv -> {
            UserFeedBack f = inv.getArgument(0);
            f.setId(50L);
            return f;
        });

        service.create(req,"metehan");

        verify(userFeedBackRepository).save(any(UserFeedBack.class));
    }

    private static UserFeedBackCreateRequest buildRequest() {
        UserFeedBackCreateRequest r = new UserFeedBackCreateRequest();
        r.setNewsId(1L);
        r.setUserSelectedCategoryId(3);
        r.setFeedbackType("NEGATIVE");
        return r;
    }
}
