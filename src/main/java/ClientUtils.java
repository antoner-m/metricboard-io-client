import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.IOException;
import java.util.Map;

public class ClientUtils {

    /**
     * Authenticates with Keycloak and returns the access token. Retries the authentication up to 5 times in case of failure.
     *
     * @param clientId      the client ID
     * @param clientSecret  the client secret
     * @param authUrl       the Keycloak authentication URL
     * @return the access token
     * @throws RuntimeException if the authentication fails after 5 retries
     */
    public static String retryableAuthenticateWithKeycloak(String clientId, String clientSecret, String authUrl) {
        for (int i = 0; i < 5; i++)
            try {
                return authenticateWithKeycloak(clientId, clientSecret, authUrl);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        throw new RuntimeException("Failed to authenticate with Keycloak! ");
    }

    /**
     * Authenticates with Keycloak and returns the access token.
     *
     * @param clientId      the client ID
     * @param clientSecret  the client secret
     * @param authUrl       the Keycloak authentication URL
     * @return the access token
     * @throws IOException  if an I/O error occurs
     * @throws RuntimeException if the authentication fails
     */
    public static String authenticateWithKeycloak(String clientId, String clientSecret, String authUrl) throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(authUrl);
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");

            String body = String.format("grant_type=client_credentials&client_id=%s&client_secret=%s",
                    clientId, clientSecret);
            httpPost.setEntity(new StringEntity(body, ContentType.APPLICATION_FORM_URLENCODED));

            try (CloseableHttpResponse response = client.execute(httpPost)) {
                if (response.getCode() == 200) {
                    HttpEntity entity = response.getEntity();
                    ObjectMapper objectMapper = new ObjectMapper();
                    Map<String, Object> responseMap = objectMapper.readValue(entity.getContent(), Map.class);
                    return responseMap.get("access_token").toString();
                } else {
                    throw new RuntimeException("Failed to authenticate with Keycloak: " + response.getCode());
                }
            }
        }
    }

    /**
     * Sends a POST request to a REST endpoint with the provided JSON data, URL, and authentication token.
     * Retries the post request up to 5 times in case of failure.
     *
     * @param jsonData         the JSON data to be sent
     * @param restEndpointUrl  the URL of the REST endpoint
     * @param token            the authentication token
     * @throws RuntimeException if the post request fails after 5 retries
     */
    public static void retryablePostRequest(String jsonData, String restEndpointUrl, String token) {
        for (int i = 0; i < 5; i++)
            try {
                postRequest(jsonData, restEndpointUrl, token);
                return;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        throw new RuntimeException("Failed to post json to endpoint!");
    }

    /**
     * Sends a POST request to a REST endpoint with the provided JSON data, URL, and authentication token.
     *
     * @param jsonData         the JSON data to be sent
     * @param restEndpointUrl  the URL of the REST endpoint
     * @param token            the authentication token
     * @throws IOException     if an I/O error occurs while sending the request
     */
    public static void postRequest(String jsonData, String restEndpointUrl, String token) throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(restEndpointUrl);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("Authorization", "Bearer " + token);

            httpPost.setEntity(new StringEntity(jsonData, ContentType.APPLICATION_JSON));

            try (CloseableHttpResponse response = client.execute(httpPost)) {
                if (response.getCode() == 200) {
                    System.out.println("Data successfully sent to REST endpoint.");
                } else {
                    throw new RuntimeException("Failed to send data to REST endpoint: " + response.getCode());
                }
            }
        }
    }

}
