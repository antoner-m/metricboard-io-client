package io.metricboard.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.IOException;
import java.net.http.HttpHeaders;
import java.util.Map;

public class ClientUtils {
    private static final HttpClient CLIENT = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

    public static String retryableAuthenticateWithKeycloak(String clientId, String clientSecret, String authUrl) {
        for (int i = 1; i < 6; i++) {
            try {
                return authenticateWithKeycloak(clientId, clientSecret, authUrl);
            } catch (Exception ex) {
                System.out.println("retryablePostRequest: failed ("+ex.getMessage()+") to get access token. Retry:"+i);
                try {
                    Thread.sleep(i*1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        throw new RuntimeException("Failed to authenticate with Keycloak!");
    }

    public static String authenticateWithKeycloak(String clientId, String clientSecret, String authUrl) throws IOException, InterruptedException {
        String body = String.format("grant_type=client_credentials&client_id=%s&client_secret=%s", clientId, clientSecret);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(authUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> responseMap = objectMapper.readValue(response.body(), Map.class);
            return responseMap.get("access_token").toString();
        } else {
            throw new RuntimeException("Failed to authenticate with Keycloak. Got response status code: " + response.statusCode());
        }

    }

    public static void retryablePostRequest(String jsonData, String restEndpointUrl, String token) {
        for (int i = 1; i < 6; i++) {
            try {
                postRequest(jsonData, restEndpointUrl, token);
                return;
            } catch (Exception ex) {
               System.out.println("retryablePostRequest: failed ("+ex.getMessage()+") post to:"+restEndpointUrl+". Retry:"+i);
                try {
                    Thread.sleep(i*1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        throw new RuntimeException("Failed to post json to endpoint!");
    }

    public static void postRequest(String jsonData, String restEndpointUrl, String token) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(restEndpointUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(jsonData))
                .build();

        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to send data to REST endpoint. Got response status code: " + response.statusCode());
        }
    }
}
