/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.plugins.atr;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

/**
 * Client for interacting with the ATR (Apache Test Release) API.
 *
 * @author Maven Team
 */
public class AtrClient {

    private final URL baseUrl;
    private final String pat;
    private final String asfuid;
    private final Log log;
    private String jwt;

    /**
     * Create a new ATR client.
     *
     * @param baseUrl the base URL of the ATR server
     * @param pat the Personal Access Token for authentication
     * @param asfuid the ASF user ID
     * @param log the Maven logger
     */
    public AtrClient(URL baseUrl, String pat, String asfuid, Log log) {
        this.baseUrl = baseUrl;
        this.pat = pat;
        this.asfuid = asfuid;
        this.log = log;
    }

    /**
     * Create a JWT from the PAT.
     *
     * @throws MojoExecutionException if JWT creation fails
     */
    private void ensureJwt() throws MojoExecutionException {
        if (jwt != null) {
            return;
        }

        try {
            // Build JSON request for JWT creation
            String jsonRequest =
                    "{" + "\"asfuid\":\"" + escapeJson(asfuid) + "\"," + "\"pat\":\"" + escapeJson(pat) + "\"" + "}";

            // Create connection
            URL jwtUrl = new URL(baseUrl, "api/jwt/create");
            HttpURLConnection conn = (HttpURLConnection) jwtUrl.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            // Send request
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonRequest.getBytes(StandardCharsets.UTF_8));
            }

            // Check response
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                String response = readResponse(conn.getInputStream());
                jwt = parseJwt(response);
                log.debug("JWT created successfully");
            } else {
                String errorResponse = readResponse(conn.getErrorStream());
                throw new MojoExecutionException("Failed to create JWT: HTTP " + responseCode + " - " + errorResponse);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to create JWT from PAT", e);
        }
    }

    /**
     * Upload a file to ATR.
     *
     * @param project the project name
     * @param version the version
     * @param relpath the relative path within the release (e.g., "artifactId-version-source-release.zip")
     * @param file the file to upload
     * @return the revision number
     * @throws MojoExecutionException if the upload fails
     */
    public String uploadFile(String project, String version, String relpath, Path file) throws MojoExecutionException {
        // Ensure we have a valid JWT
        ensureJwt();
        log.warn("upload to " + relpath);

        try {
            // Read file content and encode as base64
            byte[] fileBytes = Files.readAllBytes(file);
            String content = Base64.getEncoder().encodeToString(fileBytes);

            // Build JSON request
            String jsonRequest = buildUploadJsonRequest(project, version, relpath, content);

            // Create connection
            URL uploadUrl = new URL(baseUrl, "api/release/upload");
            HttpURLConnection conn = (HttpURLConnection) uploadUrl.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + jwt);
            conn.setDoOutput(true);

            // Send request
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonRequest.getBytes(StandardCharsets.UTF_8));
            }

            // Check response
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpURLConnection.HTTP_ACCEPTED) {
                String response = readResponse(conn.getInputStream());
                log.debug("Upload successful: " + response);
                return parseRevisionNumber(response);
            } else {
                String errorResponse = readResponse(conn.getErrorStream());
                throw new MojoExecutionException("Failed to upload file: HTTP " + responseCode + " - " + errorResponse);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to upload file to ATR: " + file, e);
        }
    }

    private String buildUploadJsonRequest(String project, String version, String relpath, String content) {
        // Simple JSON construction - in production, consider using a JSON library
        return "{"
                + "\"project\":\"" + escapeJson(project) + "\","
                + "\"version\":\"" + escapeJson(version) + "\","
                + "\"relpath\":\"" + escapeJson(relpath) + "\","
                + "\"content\":\"" + escapeJson(content) + "\""
                + "}";
    }

    private String escapeJson(String str) {
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String readResponse(InputStream is) throws IOException {
        if (is == null) {
            return "";
        }
        byte[] buffer = new byte[8192];
        int bytesRead;
        StringBuilder response = new StringBuilder();
        while ((bytesRead = is.read(buffer)) != -1) {
            response.append(new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
        }
        return response.toString();
    }

    private String parseRevisionNumber(String jsonResponse) {
        // Simple JSON parsing - extract revision number
        // In production, consider using a JSON library
        int numberIndex = jsonResponse.indexOf("\"number\"");
        if (numberIndex != -1) {
            int startQuote = jsonResponse.indexOf("\"", numberIndex + 9);
            if (startQuote != -1) {
                int endQuote = jsonResponse.indexOf("\"", startQuote + 1);
                if (endQuote != -1) {
                    return jsonResponse.substring(startQuote + 1, endQuote);
                }
            }
        }
        return "unknown";
    }

    private String parseJwt(String jsonResponse) {
        // Simple JSON parsing - extract JWT token
        // In production, consider using a JSON library
        int jwtIndex = jsonResponse.indexOf("\"jwt\"");
        if (jwtIndex != -1) {
            int startQuote = jsonResponse.indexOf("\"", jwtIndex + 6);
            if (startQuote != -1) {
                int endQuote = jsonResponse.indexOf("\"", startQuote + 1);
                if (endQuote != -1) {
                    return jsonResponse.substring(startQuote + 1, endQuote);
                }
            }
        }
        throw new IllegalArgumentException("Failed to parse JWT from response: " + jsonResponse);
    }
}
