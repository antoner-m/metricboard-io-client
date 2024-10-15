package io.metricboard.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.TimeZone;

public class DirCheck {
    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        if (args.length < 2) {
            System.out.println("Please provide the directory path and watcherId as arguments.");
            System.out.println("Usage: DirCheck path watcherId [full/path/to/settings.conf]");
            return;
        }
        String dirPath = args[0];
        String watcherId = args[1];

        String credentialsFilePath = "settings.conf"; // Path to your settings file
        if (args.length == 3) {
            credentialsFilePath = args[2];
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
            restEndpoint += "watcher/dirCheck";
            System.out.println("Getting access token....");

            // Authenticate with Keycloak
            String accessToken = ClientUtils.retryableAuthenticateWithKeycloak(clientId, clientSecret, keycloakTokenEndpoint);
            if (accessToken == null) {
                System.out.println("Failed to authenticate with Keycloak.");
                return;
            }
            System.out.println("Access token found.");

            // List files in the directory and create JSON payload
            ObjectNode jsonPayload = listFilesAsJson(dirPath);
            jsonPayload.put("watcherId", watcherId);
            jsonPayload.put("datetime", LocalDateTime.now().toString());

            // Transfer JSON to REST endpoint
            System.out.println("Sending result json....");
            ClientUtils.retryablePostRequest(jsonPayload.toString(), restEndpoint, accessToken);
            System.out.println("Sending result json done!");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static ObjectNode listFilesAsJson(String dirPath) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode jsonObject = mapper.createObjectNode();
        ArrayNode fileArray = mapper.createArrayNode();

        File dir = new File(dirPath);
        if (!dir.isDirectory()) {
            System.out.println("Provided path is not a directory.");
            return null;
        }

        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    try {
                        BasicFileAttributes attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);

                        ObjectNode fileNode = mapper.createObjectNode();
                        fileNode.put("filename", file.getName());
                        fileNode.put("size", attrs.size());
                        fileNode.put("creationTime", attrs.creationTime().toString());
                        fileNode.put("lastModifiedTime", attrs.lastModifiedTime().toString());
                        fileArray.add(fileNode);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        jsonObject.set("files", fileArray);
        return jsonObject;
    }

}
