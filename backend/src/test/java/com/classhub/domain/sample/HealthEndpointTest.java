package com.classhub.domain.sample;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class HealthEndpointTest {

    @LocalServerPort
    private int port;

    private RestClient restClient;

    @BeforeEach
    void setUp() {
        restClient = RestClient.builder().baseUrl("http://localhost:" + port).build();
    }

    @Test
    void actuatorHealth_returnsOk() {
        ResponseEntity<String> response = restClient.get().uri("/actuator/health").retrieve().toEntity(String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
