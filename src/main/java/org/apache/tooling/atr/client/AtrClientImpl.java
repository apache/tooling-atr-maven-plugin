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
package org.apache.tooling.atr.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client implementation for interacting with the ATR (Apache Test Release) API.
 *
 * @author Maven Team
 */
class AtrClientImpl implements AtrClient {

    private final Logger logger = LoggerFactory.getLogger(AtrClientImpl.class);

    private final URL baseUrl;

    private final String username;

    private final String password;

    private final AtomicReference<String> jwtCache;

    private final ObjectMapper objectMapper;

    /**
     * Create a new ATR client.
     *
     * @param baseUrl the base URL of the ATR server
     * @param jwtCache the reference for caching JWT across goals
     */
    AtrClientImpl(URL baseUrl, String username, String password, AtomicReference<String> jwtCache) {
        this.baseUrl = baseUrl;
        this.username = username;
        this.password = password;
        this.jwtCache = jwtCache;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Create a JWT from the PAT, using cached JWT if available.
     *
     * @throws AtrClientException if JWT creation fails
     */
    void ensureJwt() throws AtrClientException {
        if (jwtCache.get() != null) {
            logger.debug("Using cached JWT");
            return;
        }

        try {
            // Create JWT request
            JwtCreateRequest request = new JwtCreateRequest(username, password);

            // Create connection
            URL jwtUrl = new URL(baseUrl, "api/jwt/create");
            HttpURLConnection conn = (HttpURLConnection) jwtUrl.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            // Send request
            try (OutputStream os = conn.getOutputStream()) {
                objectMapper.writeValue(os, request);
            }

            // Check response
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                JwtCreateResponse response = objectMapper.readValue(conn.getInputStream(), JwtCreateResponse.class);
                String jwt = response.getJwt();
                logger.debug("JWT created successfully");

                // Cache JWT for reuse across goals
                jwtCache.set(jwt);
                logger.debug("JWT cached");
            } else {
                String errorResponse = readErrorResponse(conn.getErrorStream());
                throw new AtrClientException("Failed to create JWT: HTTP " + responseCode + " - " + errorResponse);
            }
        } catch (IOException e) {
            throw new AtrClientException("Failed to create JWT from PAT", e);
        }
    }

    /**
     * Check if a version exists in ATR and get its release information.
     *
     * @param project the project id
     * @param version the version
     * @return the release information, or null if the version does not exist
     * @throws AtrClientException if the check fails
     */
    @Override
    public ReleaseInfo getRelease(String project, String version) throws AtrClientException {
        // Ensure we have a valid JWT
        ensureJwt();

        try {
            // Create connection
            URL checkUrl = new URL(baseUrl, "api/release/get/" + project + "/" + version);
            HttpURLConnection conn = (HttpURLConnection) checkUrl.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + jwtCache.get());

            // Check response
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                ReleaseGetResponse response = objectMapper.readValue(conn.getInputStream(), ReleaseGetResponse.class);
                logger.debug("Get release successful: " + objectMapper.writeValueAsString(response));
                return response.getRelease();
            } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                logger.debug("Release does not exist: " + project + " " + version);
                return null;
            } else {
                String errorResponse = readErrorResponse(conn.getErrorStream());
                throw new AtrClientException("Failed to get release: HTTP " + responseCode + " - " + errorResponse);
            }
        } catch (IOException e) {
            throw new AtrClientException("Failed to get release in ATR: " + project + " " + version, e);
        }
    }

    /**
     * Upload a file to ATR.
     *
     * @param project the project id
     * @param version the version
     * @param path the relative path within the release (e.g., "artifactId-version-source-release.zip")
     * @param file the file to upload
     * @return the revision number
     * @throws AtrClientException if the upload fails
     */
    @Override
    public String uploadFile(String project, String version, String path, Path file) throws AtrClientException {
        // Ensure we have a valid JWT
        ensureJwt();

        try {
            // Read file content and encode as base64
            byte[] fileBytes = Files.readAllBytes(file);
            String content = Base64.getEncoder().encodeToString(fileBytes);

            // Create upload request
            ReleaseUploadRequest request = new ReleaseUploadRequest(project, version, path, content);

            // Create connection
            URL uploadUrl = new URL(baseUrl, "api/release/upload");
            HttpURLConnection conn = (HttpURLConnection) uploadUrl.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + jwtCache.get());
            conn.setDoOutput(true);

            // Send request
            try (OutputStream os = conn.getOutputStream()) {
                objectMapper.writeValue(os, request);
            }

            // Check response
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpURLConnection.HTTP_ACCEPTED) {
                ReleaseUploadResponse response =
                        objectMapper.readValue(conn.getInputStream(), ReleaseUploadResponse.class);
                logger.debug("Upload successful: " + objectMapper.writeValueAsString(response));
                return response.getRevision() != null ? response.getRevision().getNumber() : "unknown";
            } else {
                String errorResponse = readErrorResponse(conn.getErrorStream());
                throw new AtrClientException("Failed to upload file: HTTP " + responseCode + " - " + errorResponse);
            }
        } catch (IOException e) {
            throw new AtrClientException("Failed to upload file to ATR: " + file, e);
        }
    }

    private String readErrorResponse(InputStream is) throws IOException {
        if (is == null) {
            return "";
        }
        byte[] buffer = new byte[8192];
        int bytesRead;
        StringBuilder response = new StringBuilder();
        while ((bytesRead = is.read(buffer)) != -1) {
            response.append(new String(buffer, 0, bytesRead));
        }
        return response.toString();
    }
}
