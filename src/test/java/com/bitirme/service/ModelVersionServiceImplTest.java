package com.bitirme.service;

import com.bitirme.dto.model.ModelVersionCreateRequest;
import com.bitirme.dto.model.ModelVersionResponse;
import com.bitirme.entity.ModelVersion;
import com.bitirme.exception.NotFoundException;
import com.bitirme.repository.ModelVersionRepository;
import com.bitirme.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModelVersionServiceImplTest {

    @Mock
    private ModelVersionRepository modelVersionRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ModelVersionServiceImpl service;

    @Test
    @DisplayName("getById yoksa NotFoundException")
    void getById_not_found() {
        when(modelVersionRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(1))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("create createdById geçersizse NotFoundException")
    void create_user_missing() {
        ModelVersionCreateRequest req = new ModelVersionCreateRequest();
        req.setName("v1");
        req.setCreatedById(99L);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("create kullanıcı olmadan kaydeder")
    void create_without_user() {
        ModelVersionCreateRequest req = new ModelVersionCreateRequest();
        req.setName("v2");
        req.setDescription("d");
        when(modelVersionRepository.save(any(ModelVersion.class))).thenAnswer(inv -> {
            ModelVersion m = inv.getArgument(0);
            m.setId(4);
            return m;
        });

        ModelVersionResponse r = service.create(req);

        assertThat(r.getName()).isEqualTo("v2");
        verify(modelVersionRepository).save(any(ModelVersion.class));
    }
}
