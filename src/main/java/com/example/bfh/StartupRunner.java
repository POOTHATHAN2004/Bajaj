package com.example.bfh;

import com.example.bfh.model.GenerateWebhookResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class StartupRunner {

    private final WebClient webClient = WebClient.create();

    private static final String SQL_QUERY = """
        SELECT 
            p.AMOUNT AS SALARY,
            CONCAT(e.FIRST_NAME, ' ', e.LAST_NAME) AS NAME,
            FLOOR(DATEDIFF(CURDATE(), e.DOB) / 365.25) AS AGE,
            d.DEPARTMENT_NAME
        FROM PAYMENTS p
        JOIN EMPLOYEE e ON p.EMP_ID = e.EMP_ID
        JOIN DEPARTMENT d ON e.DEPARTMENT = d.DEPARTMENT_ID
        WHERE DAY(p.PAYMENT_TIME) != 1
        ORDER BY p.AMOUNT DESC
        LIMIT 1;
    """;

    @PostConstruct
    public void onStartup() {
        System.out.println(" Application started. Generating webhook...");

        GenerateWebhookResponse response = webClient.post()
            .uri("https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of(
                "name", "Poothathan S",
                "regNo", "22BEC0963",
                "email", "poothathan.s2022@vitstudent.ac.in"
            ))
            .retrieve()
            .bodyToMono(GenerateWebhookResponse.class)
            .block();

        if (response != null) {
            System.out.println(" Webhook generated");
            submitFinalQuery(response.getWebhook(), response.getAccessToken());
        } else {
            System.err.println(" Failed to generate webhook.");
        }
    }

    private void submitFinalQuery(String webhookUrl, String accessToken) {
        System.out.println("📤 Submitting final query...");

        webClient.post()
            .uri(webhookUrl)
            .header(HttpHeaders.AUTHORIZATION, accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of("finalQuery", SQL_QUERY))
            .retrieve()
            .bodyToMono(String.class)
            .doOnError(e -> System.err.println(" Submission failed: " + e.getMessage()))
            .doOnSuccess(response -> System.out.println(" Submission response: " + response))
            .block();
    }
}
