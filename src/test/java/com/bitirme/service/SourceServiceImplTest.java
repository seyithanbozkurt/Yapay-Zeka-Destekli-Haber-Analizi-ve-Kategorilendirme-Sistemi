package com.bitirme.service;

import com.bitirme.dto.source.SourceCreateRequest;
import com.bitirme.dto.source.SourceResponse;
import com.bitirme.entity.Source;
import com.bitirme.exception.AlreadyExistsException;
import com.bitirme.exception.NotFoundException;
import com.bitirme.repository.SourceRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SourceServiceImplTest {

    @Mock
    private SourceRepository sourceRepository;

    @InjectMocks
    private SourceServiceImpl sourceService;

    @Test
    @DisplayName("getById kaynak yoksa NotFoundException")
    void getByIdNotFoundTest() {
        when(sourceRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sourceService.getById(1))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("create aynı isimde kaynak AlreadyExistsException")
    void createDuplicateTest() {
        SourceCreateRequest req = new SourceCreateRequest();
        req.setName("AA");
        when(sourceRepository.existsByName("AA")).thenReturn(true);

        assertThatThrownBy(() -> sourceService.create(req))
                .isInstanceOf(AlreadyExistsException.class);
    }

    @Test
    @DisplayName("create minimal alanlarla kaynak oluşturur")
    void createSuccessTest() {
        SourceCreateRequest req = new SourceCreateRequest();
        req.setName("Kaynak1");
        req.setBaseUrl("https://example.com");
        req.setActive(true);
        when(sourceRepository.existsByName("Kaynak1")).thenReturn(false);
        when(sourceRepository.save(any(Source.class))).thenAnswer(inv -> {
            Source s = inv.getArgument(0);
            s.setId(7);
            return s;
        });

        SourceResponse r = sourceService.create(req);

        assertThat(r.getName()).isEqualTo("Kaynak1");
    }
}
