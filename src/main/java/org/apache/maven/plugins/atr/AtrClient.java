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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.settings.Server;

/**
 * Client for interacting with the ATR (Apache Test Release) API.
 *
 * @author Maven Team
 */
public class AtrClient {

    private final URL baseUrl;
    private final Server server;
    private final Log log;
    private final ObjectMapper objectMapper;
    private String jwt;

    /**
     * Create a new ATR client.
     *
     * @param baseUrl the base URL of the ATR server
     * @param server the Maven server configuration containing credentials
     * @param log the Maven logger
     */
    public AtrClient(URL baseUrl, Server server, Log log) {
        this.baseUrl = baseUrl;
        this.server = server;
        this.log = log;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Create a JWT from the PAT.
     *
     * @throws MojoExecutionException if JWT creation fails
     */
    void ensureJwt() throws MojoExecutionException {
        if (jwt != null) {
            return;
        }

        try {
            // Create JWT request
            JwtCreateRequest request = new JwtCreateRequest(server.getUsername(), server.getPassword());

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
                jwt = response.getJwt();
                log.debug("JWT created successfully");
            } else {
                String errorResponse = readErrorResponse(conn.getErrorStream());
                throw new MojoExecutionException("Failed to create JWT: HTTP " + responseCode + " - " + errorResponse);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to create JWT from PAT", e);
        }
    }

    /**
     * Check if a version exists in ATR and get its release information.
     *
     * @param project the project id
     * @param version the version
     * @return the release information, or null if the version does not exist
     * @throws MojoExecutionException if the check fails
     */
    public ReleaseInfo checkVersion(String project, String version) throws MojoExecutionException {
        // Ensure we have a valid JWT
        ensureJwt();

        try {
            // Create connection
            URL checkUrl = new URL(baseUrl, "api/release/get/" + project + "/" + version);
            HttpURLConnection conn = (HttpURLConnection) checkUrl.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + jwt);

            // Check response
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                ReleaseGetResponse response = objectMapper.readValue(conn.getInputStream(), ReleaseGetResponse.class);
                log.debug("Version check successful: " + objectMapper.writeValueAsString(response));
                return response.getRelease();
            } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                log.debug("Version does not exist: " + project + " " + version);
                return null;
            } else {
                String errorResponse = readErrorResponse(conn.getErrorStream());
                throw new MojoExecutionException(
                        "Failed to check version: HTTP " + responseCode + " - " + errorResponse);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to check version in ATR: " + project + " " + version, e);
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
     * @throws MojoExecutionException if the upload fails
     */
    public String uploadFile(String project, String version, String path, Path file) throws MojoExecutionException {
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
            conn.setRequestProperty("Authorization", "Bearer " + jwt);
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
                log.debug("Upload successful: " + objectMapper.writeValueAsString(response));
                return response.getRevision() != null ? response.getRevision().getNumber() : "unknown";
            } else {
                String errorResponse = readErrorResponse(conn.getErrorStream());
                throw new MojoExecutionException("Failed to upload file: HTTP " + responseCode + " - " + errorResponse);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to upload file to ATR: " + file, e);
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

    // DTO classes for API requests/responses

    static class JwtCreateRequest {
        @JsonProperty("asfuid")
        private String asfuid;

        @JsonProperty("pat")
        private String pat;

        JwtCreateRequest(String asfuid, String pat) {
            this.asfuid = asfuid;
            this.pat = pat;
        }

        public String getAsfuid() {
            return asfuid;
        }

        public String getPat() {
            return pat;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class JwtCreateResponse {
        @JsonProperty("jwt")
        private String jwt;

        public String getJwt() {
            return jwt;
        }

        public void setJwt(String jwt) {
            this.jwt = jwt;
        }
    }

    static class ReleaseUploadRequest {
        @JsonProperty("project")
        private String project;

        @JsonProperty("version")
        private String version;

        @JsonProperty("relpath")
        private String relpath;

        @JsonProperty("content")
        private String content;

        ReleaseUploadRequest(String project, String version, String relpath, String content) {
            this.project = project;
            this.version = version;
            this.relpath = relpath;
            this.content = content;
        }

        public String getProject() {
            return project;
        }

        public String getVersion() {
            return version;
        }

        public String getRelpath() {
            return relpath;
        }

        public String getContent() {
            return content;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ReleaseUploadResponse {
        @JsonProperty("endpoint")
        private String endpoint;

        @JsonProperty("revision")
        private Revision revision;

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public Revision getRevision() {
            return revision;
        }

        public void setRevision(Revision revision) {
            this.revision = revision;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Revision {
        @JsonProperty("number")
        private String number;

        @JsonProperty("asfuid")
        private String asfuid;

        @JsonProperty("phase")
        private String phase;

        @JsonProperty("release_name")
        private String releaseName;

        public String getNumber() {
            return number;
        }

        public void setNumber(String number) {
            this.number = number;
        }

        public String getAsfuid() {
            return asfuid;
        }

        public void setAsfuid(String asfuid) {
            this.asfuid = asfuid;
        }

        public String getPhase() {
            return phase;
        }

        public void setPhase(String phase) {
            this.phase = phase;
        }

        public String getReleaseName() {
            return releaseName;
        }

        public void setReleaseName(String releaseName) {
            this.releaseName = releaseName;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ReleaseGetResponse {
        @JsonProperty("endpoint")
        private String endpoint;

        @JsonProperty("release")
        private ReleaseInfo release;

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public ReleaseInfo getRelease() {
            return release;
        }

        public void setRelease(ReleaseInfo release) {
            this.release = release;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ReleaseInfo {
        // Phase constants
        public static final String PHASE_RELEASE_CANDIDATE_DRAFT = "release_candidate_draft";
        public static final String PHASE_RELEASE_CANDIDATE = "release_candidate";
        public static final String PHASE_RELEASE_PREVIEW = "release_preview";
        public static final String PHASE_RELEASE = "release";

        @JsonProperty("name")
        private String name;

        @JsonProperty("phase")
        private String phase;

        @JsonProperty("project_name")
        private String projectName;

        @JsonProperty("version")
        private String version;

        @JsonProperty("created")
        private String created;

        @JsonProperty("latest_revision_number")
        private String latestRevisionNumber;

        @JsonProperty("vote_started")
        private String voteStarted;

        @JsonProperty("vote_resolved")
        private String voteResolved;

        @JsonProperty("released")
        private String released;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPhase() {
            return phase;
        }

        public void setPhase(String phase) {
            this.phase = phase;
        }

        public String getProjectName() {
            return projectName;
        }

        public void setProjectName(String projectName) {
            this.projectName = projectName;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getCreated() {
            return created;
        }

        public void setCreated(String created) {
            this.created = created;
        }

        public String getLatestRevisionNumber() {
            return latestRevisionNumber;
        }

        public void setLatestRevisionNumber(String latestRevisionNumber) {
            this.latestRevisionNumber = latestRevisionNumber;
        }

        public String getVoteStarted() {
            return voteStarted;
        }

        public void setVoteStarted(String voteStarted) {
            this.voteStarted = voteStarted;
        }

        public String getVoteResolved() {
            return voteResolved;
        }

        public void setVoteResolved(String voteResolved) {
            this.voteResolved = voteResolved;
        }

        public String getReleased() {
            return released;
        }

        public void setReleased(String released) {
            this.released = released;
        }

        /**
         * Check if the release is being composed (not yet in vote).
         *
         * @return true if the release is being composed
         */
        public boolean isComposing() {
            return PHASE_RELEASE_CANDIDATE_DRAFT.equals(phase);
        }

        /**
         * Check if the release has been finalized and released.
         *
         * @return true if the release is in the release phase
         */
        public boolean isReleased() {
            return PHASE_RELEASE.equals(phase);
        }

        /**
         * Get a human-readable description of the phase.
         *
         * @return phase description
         */
        public String getPhaseDescription() {
            if (phase == null) {
                return "Unknown";
            }
            switch (phase) {
                case PHASE_RELEASE_CANDIDATE_DRAFT:
                    return "Release Candidate (Draft - Being Composed)";
                case PHASE_RELEASE_CANDIDATE:
                    return "Release Candidate (In Voting)";
                case PHASE_RELEASE_PREVIEW:
                    return "Release Preview";
                case PHASE_RELEASE:
                    return "Released";
                default:
                    return phase;
            }
        }
    }
}
