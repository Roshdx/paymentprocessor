//package com.payments.integration;
//
//import com.payments.api.dto.CreatePaymentRequest;
//import com.payments.api.dto.PaymentResponse;
//import com.payments.model.PaymentStatus;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.web.client.TestRestTemplate;
//import org.springframework.http.*;
//import org.springframework.test.context.DynamicPropertyRegistry;
//import org.springframework.test.context.DynamicPropertySource;
//import org.testcontainers.containers.PostgreSQLContainer;
//import org.testcontainers.containers.KafkaContainer;
//import org.testcontainers.junit.jupiter.Container;
//import org.testcontainers.junit.jupiter.Testcontainers;
//import org.testcontainers.utility.DockerImageName;
//
//import java.util.UUID;
//import java.util.concurrent.*;
//
//import static org.assertj.core.api.Assertions.*;
//
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@Testcontainers
//class PaymentFlowIntegrationTest {
//
//    @Container
//    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.2");
//
//    @Container
//    static KafkaContainer kafka = new KafkaContainer(
//        DockerImageName.parse("confluentinc/cp-kafka:7.6.0")
//    );
//
//    @DynamicPropertySource
//    static void configure(DynamicPropertyRegistry registry) {
//        registry.add("spring.datasource.url", postgres::getJdbcUrl);
//        registry.add("spring.datasource.username", postgres::getUsername);
//        registry.add("spring.datasource.password", postgres::getPassword);
//        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
//    }
//
//    @Autowired TestRestTemplate restTemplate;
//
//    @Test
//    void createPayment_returns201() {
//        HttpHeaders headers = new HttpHeaders();
//        headers.set("Idempotency-Key", UUID.randomUUID().toString());
//        headers.setContentType(MediaType.APPLICATION_JSON);
//
//        CreatePaymentRequest request = new CreatePaymentRequest();
//        request.setUserId(UUID.randomUUID());
//        request.setAmount(14999L);
//        request.setCurrency("INR");
//
//        ResponseEntity<PaymentResponse> response = restTemplate.exchange(
//            "/api/v1/payments", HttpMethod.POST,
//            new HttpEntity<>(request, headers), PaymentResponse.class
//        );
//
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
//        assertThat(response.getBody()).isNotNull();
//        assertThat(response.getBody().getStatus()).isEqualTo(PaymentStatus.PENDING);
//    }
//
//    @Test
//    void sameIdempotencyKey_returnsCachedResponse() {
//        String idempotencyKey = UUID.randomUUID().toString();
//        UUID userId = UUID.randomUUID();
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.set("Idempotency-Key", idempotencyKey);
//        headers.setContentType(MediaType.APPLICATION_JSON);
//
//        CreatePaymentRequest request = new CreatePaymentRequest();
//        request.setUserId(userId);
//        request.setAmount(5000L);
//        request.setCurrency("INR");
//
//        HttpEntity<CreatePaymentRequest> entity = new HttpEntity<>(request, headers);
//
//        ResponseEntity<PaymentResponse> first = restTemplate.exchange(
//            "/api/v1/payments", HttpMethod.POST, entity, PaymentResponse.class);
//        ResponseEntity<PaymentResponse> second = restTemplate.exchange(
//            "/api/v1/payments", HttpMethod.POST, entity, PaymentResponse.class);
//
//        assertThat(first.getBody().getId()).isEqualTo(second.getBody().getId());
//    }
//
//    @Test
//    void missingIdempotencyKey_returns400() {
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//
//        CreatePaymentRequest request = new CreatePaymentRequest();
//        request.setUserId(UUID.randomUUID());
//        request.setAmount(5000L);
//        request.setCurrency("INR");
//
//        ResponseEntity<String> response = restTemplate.exchange(
//            "/api/v1/payments", HttpMethod.POST,
//            new HttpEntity<>(request, headers), String.class
//        );
//
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
//    }
//}