package com.payments.unit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

import com.payments.exception.IdempotencyKeyException;
import com.payments.service.IdempotencyService;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    StringRedisTemplate redis;

    @Mock
    ObjectMapper objectMapper;

    @InjectMocks
    IdempotencyService idempotencyService;

    @Test
    void missingKey_throws() {
        assertThatThrownBy(() -> idempotencyService.validateKeyFormat(null))
            .isInstanceOf(IdempotencyKeyException.class)
            .hasMessageContaining("required");
    }

    @Test
    void blankKey_throws() {
        assertThatThrownBy(() -> idempotencyService.validateKeyFormat("   "))
            .isInstanceOf(IdempotencyKeyException.class);
    }

    @Test
    void nonUuidKey_throws() {
        assertThatThrownBy(() -> idempotencyService.validateKeyFormat("not-a-uuid"))
            .isInstanceOf(IdempotencyKeyException.class)
            .hasMessageContaining("UUID");
    }

    @Test
    void tooLongKey_throws() {
        assertThatThrownBy(() -> idempotencyService.validateKeyFormat("a".repeat(256)))
            .isInstanceOf(IdempotencyKeyException.class)
            .hasMessageContaining("255");
    }

    @Test
    void validUuid_passes() {
        assertThatNoException().isThrownBy(() ->
            idempotencyService.validateKeyFormat("550e8400-e29b-41d4-a716-446655440000")
        );
    }
}