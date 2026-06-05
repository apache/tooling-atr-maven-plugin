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

import java.nio.file.Path;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Client interface for interacting with the ATR (Apache Test Release) API.
 *
 * @author Maven Team
 */
public interface AtrClient {

    /**
     * Get the project information.
     *
     * @param project the project id
     * @return the project information, or null if the version does not exist
     * @throws AtrClientException if the project cannot be retrieved
     */
    ProjectInfo getProject(String project) throws AtrClientException;

    /**
     * Check if a version exists in ATR and get its release information.
     *
     * @param project the project id
     * @param version the version
     * @return the release information, or null if the version does not exist
     * @throws AtrClientException if the check fails
     */
    ReleaseInfo getRelease(String project, String version) throws AtrClientException;

    /**
     * Create a new release in ATR.
     *
     * @param project the project id
     * @param version the version
     * @return the release information of the newly created release
     * @throws AtrClientException if the release creation fails
     */
    ReleaseInfo createRelease(String project, String version) throws AtrClientException;

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
    String uploadFile(String project, String version, String path, Path file) throws AtrClientException;

    // DTO classes for API requests/responses

    class JwtCreateRequest {
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

    class JwtCreateResponse {
        @JsonProperty("jwt")
        private String jwt;

        public String getJwt() {
            return jwt;
        }

        public void setJwt(String jwt) {
            this.jwt = jwt;
        }
    }

    class ReleaseUploadRequest {
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

    class ReleaseUploadResponse {
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

    class Revision {
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

    class ReleaseResponse {
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

    class ReleaseInfo {
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

    class ReleaseCreateRequest {
        private final String project;
        private final String version;

        public ReleaseCreateRequest(String project, String version) {
            this.project = project;
            this.version = version;
        }

        public String getProject() {
            return project;
        }

        public String getVersion() {
            return version;
        }
    }

    class ProjectGetResponse {
        @JsonProperty("endpoint")
        private String endpoint;

        @JsonProperty("project")
        private ProjectInfo project;

        public String getEndpoint() {
            return endpoint;
        }

        public ProjectInfo getProject() {
            return project;
        }
    }

    class ProjectInfo {
        private String name;
        private ProjectStatus status;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public ProjectStatus getStatus() {
            return status;
        }

        public void setStatus(ProjectStatus status) {
            this.status = status;
        }
    }

    enum ProjectStatus {
        @JsonProperty("active")
        ACTIVE("active"),

        @JsonProperty("dormant")
        DORMANT("dormant"),

        @JsonProperty("retired")
        RETIRED("retired"),

        @JsonProperty("standing")
        STANDING("STANDING"),

        @JsonEnumDefaultValue
        UNKNOWN("unknown");

        private final String description;

        ProjectStatus(String description) {
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }
}
