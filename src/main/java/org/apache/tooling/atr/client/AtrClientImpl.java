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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
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
        this.objectMapper = JsonMapper.builder()
                // not strict as ATR api is in work in progress
                // and may return extra fields or change enum values without notice
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
                .build();
    }

    /**
     * Create a JWT from the PAT, using cached JWT if available.
     *
     * @throws AtrClientException if JWT creation fails
     */
    private void ensureJwt() throws AtrClientException {
        if (jwtCache.get() != null) {
            logger.debug("Using cached JWT");
            return;
        }

        try {
            // Create JWT request
            JwtCreateRequest request = new JwtCreateRequest(username, password);

            // Execute POST request
            JwtCreateResponse jwtCreateResponse = executePost("api/jwt/create", request, JwtCreateResponse.class);
            String jwt = jwtCreateResponse.getJwt();
            logger.debug("JWT created successfully");

            // Cache JWT for reuse across goals
            jwtCache.set(jwt);
        } catch (IOException e) {
            throw new AtrClientException("Failed to create JWT from PAT", e);
        }
    }

    @Override
    public ProjectInfo getProject(String project) throws AtrClientException {
        // Ensure we have a valid JWT
        ensureJwt();

        try {
            ProjectGetResponse projectGetResponse = executeGet("api/project/get/" + project, ProjectGetResponse.class);
            return projectGetResponse.getProject();
        } catch (AtrClientException e) {
            if (e.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                return null;
            }
            throw e;
        } catch (IOException e) {
            throw new AtrClientException("Failed to get project from ATR: " + project, e);
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
            ReleaseResponse releaseResponse =
                    executeGet("api/release/get/" + project + "/" + version, ReleaseResponse.class);
            return releaseResponse.getRelease();
        } catch (AtrClientException e) {
            if (e.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                return null;
            }
            throw e;
        } catch (IOException e) {
            throw new AtrClientException("Failed to get release in ATR: " + project + " " + version, e);
        }
    }

    @Override
    public ReleaseInfo createRelease(String project, String version) throws AtrClientException {
        // Ensure we have a valid JWT
        ensureJwt();

        try {
            ReleaseCreateRequest request = new ReleaseCreateRequest(project, version);
            ReleaseResponse releaseResponse = executePost("api/release/create", request, ReleaseResponse.class);
            return releaseResponse.getRelease();
        } catch (IOException e) {
            throw new AtrClientException("Failed to create release in ATR: " + project + " " + version, e);
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

            // Execute POST request
            ReleaseUploadResponse response = executePost("api/release/upload", request, ReleaseUploadResponse.class);

            return response.getRevision() != null ? response.getRevision().getNumber() : "unknown";
        } catch (IOException e) {
            throw new AtrClientException("Failed to upload file to ATR: " + file, e);
        }
    }

    private <T> T executeGet(String url, Class<T> responseClass) throws IOException, AtrClientException {

        // Create connection
        URL uploadUrl = new URL(baseUrl, url);
        HttpURLConnection conn = (HttpURLConnection) uploadUrl.openConnection();
        conn.setRequestMethod("GET");
        if (jwtCache.get() != null) {
            conn.setRequestProperty("Authorization", "Bearer " + jwtCache.get());
        }

        // Check response
        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            return objectMapper.readValue(conn.getInputStream(), responseClass);
        } else {
            String errorResponse = readErrorResponse(conn.getErrorStream());
            throw new AtrClientException(responseCode, errorResponse);
        }
    }

    private <T> T executePost(String url, Object requestData, Class<T> responseClass)
            throws IOException, AtrClientException {

        // Create connection
        URL uploadUrl = new URL(baseUrl, url);
        HttpURLConnection conn = (HttpURLConnection) uploadUrl.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        if (jwtCache.get() != null) {
            conn.setRequestProperty("Authorization", "Bearer " + jwtCache.get());
        }
        conn.setDoOutput(true);

        // Send request
        try (OutputStream os = conn.getOutputStream()) {
            objectMapper.writeValue(os, requestData);
        }

        // Check response
        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK
                || responseCode == HttpURLConnection.HTTP_CREATED
                || responseCode == HttpURLConnection.HTTP_ACCEPTED) {
            return objectMapper.readValue(conn.getInputStream(), responseClass);
        } else {
            String errorResponse = readErrorResponse(conn.getErrorStream());
            throw new AtrClientException(responseCode, errorResponse);
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
