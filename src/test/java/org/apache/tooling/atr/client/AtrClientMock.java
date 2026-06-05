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

import java.net.URL;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mock implementation of {@link AtrClient}.
 * <p>
 * Used in ITs.
 */
class AtrClientMock implements AtrClient {

    private final Logger logger = LoggerFactory.getLogger(AtrClientMock.class);

    public AtrClientMock(URL baseUrl, String username, String password, AtomicReference<String> jwtCache) {
        String jwtCacheValue = jwtCache.get();
        if (jwtCacheValue != null) {
            logger.info("[Mock ATR client] using cached JWT: {}", jwtCacheValue);
        } else {
            logger.info("[Mock ATR client] creating new JWT for username: {}", username);
            jwtCache.set("mock-jwt-" + username);
        }

        logger.info("[Mock ATR client] created base URL: {}, username: {}, password: {}", baseUrl, username, password);
    }

    @Override
    public ProjectInfo getProject(String project) {
        logger.info("[Mock ATR client] getProject: {}", project);
        ProjectInfo projectInfo = new ProjectInfo();
        projectInfo.setName(project);
        projectInfo.setStatus(ProjectStatus.ACTIVE);
        return projectInfo;
    }

    @Override
    public ReleaseInfo getRelease(String project, String version) {
        logger.info("[Mock ATR client] getRelease: {}, {}", project, version);
        ReleaseInfo releaseInfo = new ReleaseInfo();
        releaseInfo.setProjectName(project);
        releaseInfo.setVersion(version);
        releaseInfo.setPhase(ReleaseInfo.PHASE_RELEASE_CANDIDATE_DRAFT);
        return releaseInfo;
    }

    @Override
    public ReleaseInfo createRelease(String project, String version) throws AtrClientException {
        logger.info("[Mock ATR client] createRelease: {}, {}", project, version);
        ReleaseInfo releaseInfo = new ReleaseInfo();
        releaseInfo.setProjectName(project);
        releaseInfo.setVersion(version);
        releaseInfo.setPhase(ReleaseInfo.PHASE_RELEASE_CANDIDATE_DRAFT);
        return releaseInfo;
    }

    @Override
    public String uploadFile(String project, String version, String path, Path file) {
        logger.info("[Mock ATR client] uploadFile: {}, {}, {}, {}", project, version, path, file);
        return "1";
    }
}
