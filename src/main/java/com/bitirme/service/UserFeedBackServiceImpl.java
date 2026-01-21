package com.bitirme.service;

import com.bitirme.dto.feed.UserFeedBackCreateRequest;
import com.bitirme.dto.feed.UserFeedBackResponse;
import com.bitirme.dto.feed.UserFeedBackUpdateRequest;
import com.bitirme.entity.Category;
import com.bitirme.entity.ModelVersion;
import com.bitirme.entity.News;
import com.bitirme.entity.User;
import com.bitirme.entity.UserFeedBack;
import com.bitirme.exception.NotFoundException;
import com.bitirme.repository.CategoryRepository;
import com.bitirme.repository.ModelVersionRepository;
import com.bitirme.repository.NewsRepository;
import com.bitirme.repository.UserFeedBackRepository;
import com.bitirme.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserFeedBackServiceImpl implements UserFeedBackService {

    private final UserFeedBackRepository userFeedBackRepository;
    private final NewsRepository newsRepository;
    private final UserRepository userRepository;
    private final ModelVersionRepository modelVersionRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public UserFeedBackResponse create(UserFeedBackCreateRequest request) {
        News news = newsRepository.findById(request.getNewsId())
                .orElseThrow(() -> new NotFoundException("Haber bulunamadı: " + request.getNewsId()));

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new NotFoundException("Kullanıcı bulunamadı: " + request.getUserId()));

        UserFeedBack feedback = new UserFeedBack();
        feedback.setNews(news);
        feedback.setUser(user);
        feedback.setFeedbackType(request.getFeedbackType());
        feedback.setComment(request.getComment());

        if (request.getModelVersionId() != null) {
            ModelVersion modelVersion = modelVersionRepository.findById(request.getModelVersionId())
                    .orElseThrow(() -> new NotFoundException("Model versiyonu bulunamadı: " + request.getModelVersionId()));
            feedback.setModelVersion(modelVersion);
        }

        if (request.getCurrentPredictedCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCurrentPredictedCategoryId())
                    .orElseThrow(() -> new NotFoundException("Kategori bulunamadi: " + request.getCurrentPredictedCategoryId()));
            feedback.setCurrentPredictedCategory(category);
        }

        Category userSelectedCategory = categoryRepository.findById(request.getUserSelectedCategoryId())
                .orElseThrow(() -> new NotFoundException("Kategori bulunamadi: " + request.getUserSelectedCategoryId()));
        feedback.setUserSelectedCategory(userSelectedCategory);

        UserFeedBack saved = userFeedBackRepository.save(feedback);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public UserFeedBackResponse getById(Long id) {
        UserFeedBack feedback = userFeedBackRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Kullanıcı geri bildirimi bulunamadi: " + id));
        return toResponse(feedback);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserFeedBackResponse> getAll() {
        return userFeedBackRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public UserFeedBackResponse update(Long id, UserFeedBackUpdateRequest request) {
        UserFeedBack feedback = userFeedBackRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Kullanıcı geri bildirimi bulunamadi: " + id));

        if (request.getModelVersionId() != null) {
            ModelVersion modelVersion = modelVersionRepository.findById(request.getModelVersionId())
                    .orElseThrow(() -> new NotFoundException("Model versiyonu bulunamadi: " + request.getModelVersionId()));
            feedback.setModelVersion(modelVersion);
        }

        if (request.getCurrentPredictedCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCurrentPredictedCategoryId())
                    .orElseThrow(() -> new NotFoundException("Kategori bulunamadı: " + request.getCurrentPredictedCategoryId()));
            feedback.setCurrentPredictedCategory(category);
        }

        if (request.getUserSelectedCategoryId() != null) {
            Category category = categoryRepository.findById(request.getUserSelectedCategoryId())
                    .orElseThrow(() -> new NotFoundException("Kategori bulunamadı: " + request.getUserSelectedCategoryId()));
            feedback.setUserSelectedCategory(category);
        }

        if (request.getFeedbackType() != null) {
            feedback.setFeedbackType(request.getFeedbackType());
        }

        if (request.getComment() != null) {
            feedback.setComment(request.getComment());
        }

        UserFeedBack saved = userFeedBackRepository.save(feedback);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!userFeedBackRepository.existsById(id)) {
            throw new NotFoundException("Kullanıcı geri bildirimi bulunamadı: " + id);
        }
        userFeedBackRepository.deleteById(id);
    }

    private UserFeedBackResponse toResponse(UserFeedBack feedback) {
        UserFeedBackResponse response = new UserFeedBackResponse();
        response.setId(feedback.getId());
        response.setNewsTitle(feedback.getNews().getTitle());
        response.setUsername(feedback.getUser().getUsername());
        if (feedback.getModelVersion() != null) {
            response.setModelVersionName(feedback.getModelVersion().getName());
        }
        if (feedback.getCurrentPredictedCategory() != null) {
            response.setCurrentPredictedCategoryName(feedback.getCurrentPredictedCategory().getName());
        }
        response.setUserSelectedCategoryName(feedback.getUserSelectedCategory().getName());
        response.setFeedbackType(feedback.getFeedbackType());
        response.setComment(feedback.getComment());
        return response;
    }
}


