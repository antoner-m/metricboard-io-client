package io.metricboard.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.TimeZone;
import java.util.regex.Pattern;

public class CheckAlive {
    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        if (args.length < 2) {
            System.out.println("Usage: CheckAlive blockId key [full/path/to/settings.conf]");
            System.out.println("blockId - uuid of block with value type 'Map' from metricboard.io");
            System.out.println("key - any code without spaces. If block with this code not found, then it will be created.");
            return;
        }
        String dirPath = args[0];
        String blockId = args[1];
        String key = args[2];

        String credentialsFilePath = "settings.conf"; // Path to your settings file
        if (args.length == 4) {
            credentialsFilePath = args[3];
        }
        File confFile = new File(credentialsFilePath);
        if (!confFile.exists()) {
            System.out.println("Conf file not found at path:"+confFile.getAbsolutePath());
            return;
        }
        String keycloakTokenEndpoint = "http://localhost:8080/realms/your-realm/protocol/openid-connect/token"; // Replace with your Keycloak URL
        String restEndpoint = "https://sitecenter.org/api/v1/"; // Replace with your REST endpoint URL

        try {
            // Read user credentials from settings.conf
            Properties props = new Properties();
            props.load(new FileInputStream(credentialsFilePath));
            String clientId = props.getProperty("client_id");
            String clientSecret = props.getProperty("client_secret");
            keycloakTokenEndpoint = props.getProperty("keycloakTokenEndpoint");
            restEndpoint = props.getProperty("restEndpoint");

            if (!restEndpoint.endsWith("/")) restEndpoint += "/";
            restEndpoint += "watcher/checkAlive";
            System.out.println("Getting access token....");

            // Authenticate with Keycloak
            String accessToken = ClientUtils.retryableAuthenticateWithKeycloak(clientId, clientSecret, keycloakTokenEndpoint);
            if (accessToken == null) {
                System.out.println("Failed to authenticate with Keycloak.");
                return;
            }
            System.out.println("Access token found.");

            // List files in the directory and create JSON payload
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode jsonObject = mapper.createObjectNode();
            String nowStr = LocalDateTime.now().toString();
            jsonObject.put("blockId", blockId);
            jsonObject.put("datetime", nowStr);

            ArrayNode valuesArray = mapper.createArrayNode();

            ObjectNode valueObject = mapper.createObjectNode();
            valueObject.put("key", key);
            valueObject.put("value", nowStr);
            valuesArray.add(valueObject);

            jsonObject.set("values", valuesArray);

            // Transfer JSON to REST endpoint
            System.out.println("Sending result json....");
            ClientUtils.retryablePostRequest(jsonObject.toString(), restEndpoint, accessToken);
            System.out.println("Sending result json done!");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
